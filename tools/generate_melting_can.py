#!/usr/bin/env python3
"""
Generates melting_can.json - a progress-scrubbed Lottie animation of a can
buried in ice. The ice level drops steadily as a focus session runs,
uncovering the can from the top down.

Timeline (frame 0..100 == session elapsed 0%..100%):
  0      can fully buried in a heap of ice; "GOOD THINGS TAKE TIME" faintly
         visible through it
  ~25    top of the can uncovered, frosted over
  ~50    half uncovered; frost clearing; puddle forming
  ~75    condensation droplets, a drip or two running down, low ice
  100    no ice, can standing in a puddle, "YOU EARNED THIS"

Compose drives it with LottieAnimation(progress = { elapsedFraction }).

Only features fully supported by lottie-android: shape layers,
paths/ellipses, solid + gradient fills (with opacity stops), strokes,
layer masks, keyframed transforms/opacity/paths. No text layers (the label is
converted to outlines here, so no font asset), no expressions, no images,
no merge paths, no effects.

Tint hooks (dynamic properties from Compose):
  KeyPath("CanBody", "Accent", "AccentFill")        - can color
  KeyPath("CanReflection", "Accent", "AccentFill")  - its reflection in the puddle

Usage:  python3 generate_melting_can.py [out.json]
Settings at the top: label copy, font, ice timing.
"""
import json
import math
import random
import sys

from fontTools.pens.basePen import BasePen
from fontTools.ttLib import TTFont

# ---------------------------------------------------------------- settings
W, H = 400, 560
FPS = 60
END = 100  # last frame == 100% elapsed
FONT_PATH = "/usr/share/fonts/truetype/google-fonts/Poppins-Medium.ttf"
LABEL_START = ["GOOD", "THINGS", "TAKE", "TIME"]
LABEL_END = ["YOU", "EARNED", "THIS"]
DEFAULT_ACCENT = (0.36, 0.49, 0.66)  # dusty blue like the reference; tinted at runtime

# Ice level: the heap's top edge travels from ICE_TOP_Y to ICE_GONE_Y,
# linearly from frame ICE_START to ICE_DONE (i.e. a constant melt rate).
ICE_TOP_Y, ICE_GONE_Y = 62, 528
ICE_START, ICE_DONE = 0, 96

K = 0.5523  # cubic bezier circle constant

# ---------------------------------------------------------------- lottie helpers


def rgb(c, a=1.0):
    return [round(c[0], 4), round(c[1], 4), round(c[2], 4), a]


def static(v):
    return {"a": 0, "k": v}


def anim(keys, linear=False):
    """keys: list of (frame, value). value: number, list, or path dict."""
    out = []
    for idx, (t, v) in enumerate(keys):
        if isinstance(v, dict):
            vv, dim = [v], 1
        elif isinstance(v, list):
            vv, dim = v, len(v)
        else:
            vv, dim = [v], 1
        kf = {"t": round(t, 3), "s": vv}
        if idx < len(keys) - 1:
            if linear:
                kf.update({"i": {"x": [1] * dim, "y": [1] * dim}, "o": {"x": [0] * dim, "y": [0] * dim}})
            else:
                kf.update({"i": {"x": [0.42] * dim, "y": [1] * dim}, "o": {"x": [0.58] * dim, "y": [0] * dim}})
        out.append(kf)
    return {"a": 1, "k": out}


def path(verts, ins=None, outs=None, closed=True):
    n = len(verts)
    ins = ins or [[0, 0]] * n
    outs = outs or [[0, 0]] * n
    r2 = lambda p: [round(float(p[0]), 2), round(float(p[1]), 2)]
    return {"c": closed, "v": [r2(p) for p in verts], "i": [r2(p) for p in ins], "o": [r2(p) for p in outs]}


def sh(p, nm="Path"):
    return {"ty": "sh", "nm": nm, "ks": p if "a" in p else static(p)}


def ellipse(cx, cy, w, h, nm="Ellipse"):
    return {"ty": "el", "nm": nm, "d": 1, "p": static([round(cx, 2), round(cy, 2)]),
            "s": static([round(w, 2), round(h, 2)])}


def fill(c, opacity=100, nm="Fill"):
    return {"ty": "fl", "nm": nm, "c": static(rgb(c)), "o": static(opacity), "r": 1}


def stroke(c, w, opacity=100, nm="Stroke"):
    return {"ty": "st", "nm": nm, "c": static(rgb(c)), "o": static(opacity), "w": static(round(w, 2)),
            "lc": 2, "lj": 2, "ml": 4}


def gfill(start, end, color_stops, alpha_stops, radial=False, nm="Gradient"):
    """color_stops: [(pos, (r,g,b))], alpha_stops: [(pos, alpha 0..1)]"""
    k = []
    for p, c in color_stops:
        k += [p, c[0], c[1], c[2]]
    for p, a in alpha_stops:
        k += [p, a]
    g = {"ty": "gf", "nm": nm, "o": static(100), "r": 1, "t": 2 if radial else 1,
         "s": static([round(x, 2) for x in start]), "e": static([round(x, 2) for x in end]),
         "g": {"p": len(color_stops), "k": static(k)}}
    if radial:
        g["h"] = static(0)
        g["a"] = static(0)
    return g


def tr(p=(0, 0), a=(0, 0), s=(100, 100), r=0, o=100):
    def v(x):
        return x if isinstance(x, dict) else static(list(x) if isinstance(x, tuple) else x)
    return {"ty": "tr", "nm": "Transform", "p": v(p), "a": v(a), "s": v(s), "r": v(r),
            "o": v(o), "sk": static(0), "sa": static(0)}


def group(nm, items, transform=None):
    return {"ty": "gr", "nm": nm, "it": items + [transform or tr()]}


_ind = [0]


def layer(nm, shapes, o=100, p=(0, 0), a=(0, 0), s=(100, 100), mask=None):
    _ind[0] += 1

    def v(x):
        if isinstance(x, dict):
            return x
        return static(list(x) + [0] if isinstance(x, tuple) else x)
    L = {"ddd": 0, "ind": _ind[0], "ty": 4, "nm": nm, "sr": 1,
         "ks": {"o": v(o), "r": static(0), "p": v(p), "a": v(a),
                "s": s if isinstance(s, dict) else static(list(s) + [100])},
         "ao": 0, "shapes": shapes, "ip": 0, "op": END + 1, "st": 0, "bm": 0}
    if mask is not None:
        L["hasMask"] = True
        L["masksProperties"] = [{"inv": False, "mode": "a", "pt": static(mask),
                                 "o": static(100), "x": static(0), "nm": "Mask 1"}]
    return L


def smooth_closed(pts, tension=1 / 6):
    """Catmull-Rom -> cubic bezier closed path through pts."""
    n = len(pts)
    ins, outs = [], []
    for i in range(n):
        p0, p2 = pts[i - 1], pts[(i + 1) % n]
        tx, ty = (p2[0] - p0[0]) * tension, (p2[1] - p0[1]) * tension
        ins.append((-tx, -ty)); outs.append((tx, ty))
    return path(pts, ins, outs)


def rounded_poly(pts, r):
    """Polygon with rounded corners (radius ~r, clamped to 40% of each edge)."""
    v, ins, outs = [], [], []
    n = len(pts)
    for i in range(n):
        P, A, B = pts[i], pts[i - 1], pts[(i + 1) % n]
        da, db = math.dist(P, A), math.dist(P, B)
        ra, rb = min(r, da * 0.4), min(r, db * 0.4)
        a = (P[0] + (A[0] - P[0]) / da * ra, P[1] + (A[1] - P[1]) / da * ra)
        b = (P[0] + (B[0] - P[0]) / db * rb, P[1] + (B[1] - P[1]) / db * rb)
        v += [a, b]
        ins += [(0, 0), ((P[0] - b[0]) * K, (P[1] - b[1]) * K)]
        outs += [((P[0] - a[0]) * K, (P[1] - a[1]) * K), (0, 0)]
    return path(v, ins, outs)


def lerp(a, b, t):
    return a + (b - a) * t


WHITE = (1, 1, 1)
BLACK = (0, 0, 0)
SILVER = (0.80, 0.82, 0.85)
SILVER_DARK = (0.52, 0.55, 0.60)
ICE_HI = (0.95, 0.98, 1.0)
ICE_MID = (0.80, 0.89, 0.97)
ICE_DEEP = (0.55, 0.72, 0.90)
ICE_RIM = (0.50, 0.66, 0.84)
WATER = (0.76, 0.86, 0.95)

# ---------------------------------------------------------------- can geometry
CX = 200
L, R = 115, 285            # body sides
LID_L, LID_R = 125, 275    # lid / bottom rim width
LID_Y, LID_RY = 100, 11    # lid ellipse center & y-radius
SHOULDER_Y = 140
BASE_Y = 440               # where the bottom taper starts
BOT_Y, BOT_RY = 466, 11    # bottom ellipse center & y-radius
RX = (LID_R - LID_L) / 2


def can_body_path():
    v = [(LID_L, LID_Y), (CX, LID_Y - LID_RY), (LID_R, LID_Y), (R, SHOULDER_Y), (R, BASE_Y),
         (LID_R, BOT_Y), (CX, BOT_Y + BOT_RY), (LID_L, BOT_Y), (L, BASE_Y), (L, SHOULDER_Y)]
    i = [(-3, 14), (-RX * K, 0), (0, -LID_RY * K), (0, -16), (0, 0),
         (5, -6), (RX * K, 0), (0, BOT_RY * K), (0, 12), (0, 0)]
    o = [(0, -LID_RY * K), (RX * K, 0), (3, 14), (0, 0), (0, 12),
         (0, BOT_RY * K), (-RX * K, 0), (-5, -6), (0, 0), (0, -16)]
    return path(v, i, o)


def band_path(y_edge, sag, top=True):
    """Silver band: neck (top=True) or base (top=False) section of the can."""
    if top:
        v = [(LID_L, LID_Y), (CX, LID_Y - LID_RY), (LID_R, LID_Y), (R, y_edge), (CX, y_edge + sag), (L, y_edge)]
        i = [(-3, 14), (-RX * K, 0), (0, -LID_RY * K), (0, -16), (85 * K, 0), (0, sag * K)]
        o = [(0, -LID_RY * K), (RX * K, 0), (3, 14), (0, sag * K), (-85 * K, 0), (0, -16)]
    else:
        v = [(L, y_edge), (CX, y_edge + sag), (R, y_edge), (R, BASE_Y), (LID_R, BOT_Y),
             (CX, BOT_Y + BOT_RY), (LID_L, BOT_Y), (L, BASE_Y)]
        i = [(0, 0), (-85 * K, 0), (0, sag * K), (0, 0), (5, -6), (RX * K, 0), (0, BOT_RY * K), (0, 12)]
        o = [(0, sag * K), (85 * K, 0), (0, 0), (0, 12), (0, BOT_RY * K), (-RX * K, 0), (-5, -6), (0, 0)]
    return path(v, i, o)


def edge_line(y, sag, x0=L, x1=R):
    hw = (x1 - x0) / 2
    return path([(x0, y), ((x0 + x1) / 2, y + sag), (x1, y)], [(0, 0), (-hw * K, 0), (0, sag * K)],
                [(0, sag * K), (hw * K, 0), (0, 0)], closed=False)


# ---------------------------------------------------------------- text -> outlines
class LottiePen(BasePen):
    def __init__(self, glyphset, tx, ty, scale):
        super().__init__(glyphset)
        self.tx, self.ty, self.sc = tx, ty, scale
        self.contours, self.cur = [], None

    def _t(self, p):
        return (self.tx + p[0] * self.sc, self.ty - p[1] * self.sc)

    def _moveTo(self, p):
        self.cur = {"v": [self._t(p)], "i": [(0, 0)], "o": [(0, 0)]}
        self.last = p

    def _lineTo(self, p):
        self.cur["v"].append(self._t(p)); self.cur["i"].append((0, 0)); self.cur["o"].append((0, 0))
        self.last = p

    def _curveToOne(self, c1, c2, p):
        tc1, tc2, tp = self._t(c1), self._t(c2), self._t(p)
        pv = self.cur["v"][-1]
        self.cur["o"][-1] = (tc1[0] - pv[0], tc1[1] - pv[1])
        self.cur["v"].append(tp); self.cur["i"].append((tc2[0] - tp[0], tc2[1] - tp[1])); self.cur["o"].append((0, 0))
        self.last = p

    def _qCurveToOne(self, q, p):
        p0 = self.last
        c1 = (p0[0] + 2 / 3 * (q[0] - p0[0]), p0[1] + 2 / 3 * (q[1] - p0[1]))
        c2 = (p[0] + 2 / 3 * (q[0] - p[0]), p[1] + 2 / 3 * (q[1] - p[1]))
        self._curveToOne(c1, c2, p)

    def _closePath(self):
        c = self.cur
        if len(c["v"]) > 1 and math.dist(c["v"][0], c["v"][-1]) < 0.01:
            c["i"][0] = c["i"][-1]
            for k in ("v", "i", "o"):
                c[k].pop()
        self.contours.append(c)
        self.cur = None

    _endPath = _closePath


FONT = TTFont(FONT_PATH)
GS = FONT.getGlyphSet()
CMAP = FONT.getBestCmap()
UPM = FONT["head"].unitsPerEm
WRAP_R = 108  # radius used to wrap the label around the cylinder


def wrap(p):
    """Wrap a flat point onto the can's curved surface (seen slightly from above)."""
    th = (p[0] - CX) / WRAP_R
    return (CX + WRAP_R * math.sin(th), p[1] - 16 * (1 - math.cos(th)))


def text_shapes(lines, size, line_h, center_y, tracking):
    sc = size / UPM
    shapes = []
    total = line_h * (len(lines) - 1)
    for li, line in enumerate(lines):
        names = [CMAP[ord(ch)] for ch in line]
        adv = [GS[n].width * sc for n in names]
        width = sum(adv) + tracking * (len(line) - 1)
        x = CX - width / 2
        baseline = center_y - total / 2 + li * line_h + size * 0.36
        for n, a in zip(names, adv):
            pen = LottiePen(GS, x, baseline, sc)
            GS[n].draw(pen)
            for c in pen.contours:
                v, ins, outs = [], [], []
                for P, I, O in zip(c["v"], c["i"], c["o"]):
                    wp = wrap(P)
                    wi = wrap((P[0] + I[0], P[1] + I[1]))
                    wo = wrap((P[0] + O[0], P[1] + O[1]))
                    v.append(wp); ins.append((wi[0] - wp[0], wi[1] - wp[1])); outs.append((wo[0] - wp[0], wo[1] - wp[1]))
                shapes.append(sh(path(v, ins, outs), line))
            x += a + tracking
    return shapes


# ---------------------------------------------------------------- ice
def level_at(frame):
    t = min(max((frame - ICE_START) / (ICE_DONE - ICE_START), 0), 1)
    return lerp(ICE_TOP_Y, ICE_GONE_Y, t)


def frame_at(level_y):
    t = (level_y - ICE_TOP_Y) / (ICE_GONE_Y - ICE_TOP_Y)
    return ICE_START + min(max(t, 0), 1) * (ICE_DONE - ICE_START)


def heap_half_width(y):
    """Half-width of the ice heap at height y: narrow over the lid, wide at the base."""
    t = min(max((y - 70) / (500 - 70), 0), 1)
    return lerp(96, 152, t ** 1.3)


def ice_cube(name, cx, cy, s, rot, seed, start, end, sink=14):
    """A rounded, chipped, translucent ice cube. Melts between frames start..end."""
    rng = random.Random(seed)
    jt = lambda: rng.uniform(-0.06, 0.06) * s
    sq = rng.uniform(0.85, 1.1)  # squash: some chunks flatter, some taller
    top = (jt(), -0.56 * s * sq + jt())
    tr_ = (0.5 * s + jt(), -0.27 * s * sq + jt())
    tl = (-0.5 * s + jt(), -0.27 * s * sq + jt())
    mid = (jt() * 0.6, -0.01 * s * sq + jt() * 0.6)
    dep = 0.5 * s * sq * rng.uniform(0.85, 1.05)
    br = (tr_[0] + jt() * 0.5, tr_[1] + dep)
    bl = (tl[0] + jt() * 0.5, tl[1] + dep)
    bot = (mid[0], mid[1] + dep * 1.04)
    rad = 0.14 * s

    def shrink(pts, f):
        cx_ = sum(p[0] for p in pts) / len(pts); cy_ = sum(p[1] for p in pts) / len(pts)
        return [(cx_ + (p[0] - cx_) * f, cy_ + (p[1] - cy_) * f) for p in pts]

    def seg(a, b, t0=0.18, t1=0.82):
        return sh(path([(lerp(a[0], b[0], t0), lerp(a[1], b[1], t0)), (lerp(a[0], b[0], t1), lerp(a[1], b[1], t1))],
                       closed=False))

    outline = [top, tr_, br, bot, bl, tl]
    top_face = shrink([top, tr_, mid, tl], 0.94)
    left_face = shrink([tl, mid, bot, bl], 0.94)
    right_face = shrink([mid, tr_, br, bot], 0.94)
    sil = rounded_poly(outline, rad)

    items = [
        group("Sparkle", [ellipse(top[0] - 0.12 * s, top[1] + 0.13 * s, 0.10 * s, 0.05 * s), fill(WHITE, 100)]),
        group("Glints", [seg(tl, mid), seg(mid, bot, 0.1, 0.7), stroke(WHITE, max(1.2, s * 0.022), 80)]),
        group("Facet", [sh(rounded_poly(shrink([top, tr_, mid, tl], 0.5), rad * 0.4)), stroke(WHITE, 1.1, 45)]),
        group("Streak", [sh(path([(tl[0] + 0.12 * s, tl[1] + 0.12 * s), (tl[0] + 0.2 * s, tl[1] + 0.10 * s),
                                   (bl[0] + 0.24 * s, bl[1] - 0.08 * s), (bl[0] + 0.15 * s, bl[1] - 0.06 * s)])),
                         fill(WHITE, 40)]),
        group("Top", [sh(rounded_poly(top_face, rad * 0.7)), fill(ICE_HI, 60)]),
        group("Right", [sh(rounded_poly(right_face, rad * 0.7)), fill(ICE_DEEP, 30)]),
        group("Left", [sh(rounded_poly(left_face, rad * 0.7)), fill(ICE_MID, 25)]),
        group("Core", [sh(sil), gfill((0.12 * s, 0.18 * s), (0.12 * s + 0.6 * s, 0.18 * s),
                                      [(0, ICE_DEEP), (1, ICE_MID)], [(0, 0.45), (1, 0)], radial=True)]),
        group("Body", [sh(sil), gfill((0, -0.6 * s), (0, 0.55 * s), [(0, ICE_HI), (1, ICE_MID)],
                                      [(0, 0.62), (1, 0.42)]),
                       stroke(ICE_RIM, 1.3, 65)]),
    ]
    mid_t = lerp(start, end, 0.55)
    return group(name, items, tr(
        p=anim([(start, [cx, cy]), (end, [cx, cy + sink])]),
        s=anim([(start, [100, 100]), (mid_t, [74, 66]), (end, [24, 14])]),
        r=rot,
        o=anim([(start, 100), (mid_t, 88), (end, 0)]),
    ))


def build_ice():
    """Rows of cubes heaped over the can + a translucent mass filling the gaps."""
    rng = random.Random(1234)
    rows = []
    y = 88
    while y <= 500:
        hw = heap_half_width(y)
        n = max(3, round(2 * hw / 58))
        row = []
        for k in range(n):
            x = CX - hw + 2 * hw * (k + 0.5) / n + rng.uniform(-10, 10)
            cy = y + rng.uniform(-8, 8)
            s = rng.uniform(46, 84) * (0.85 if y < 120 else 1)
            start = frame_at(cy - 0.45 * s) - 2
            end = frame_at(cy + 0.35 * s) + 8
            row.append(ice_cube(f"Cube{len(rows)}_{k}", x, cy, s, rng.uniform(-24, 24),
                                rng.randrange(10 ** 6), max(0, start), min(END, end)))
        rng.shuffle(row)
        rows.append(row)
        y += 40

    # translucent mass behind the cubes; its lumpy top edge tracks the ice level
    bumps = [rng.uniform(-14, 8) for _ in range(9)]

    def mass_path(level):
        level = min(level, 500)
        hw = heap_half_width(level) - 20
        pts = []
        for i, b in enumerate(bumps):
            t = i / (len(bumps) - 1)
            edge = 1 - (2 * t - 1) ** 2  # lower at the sides, so the heap is mound-shaped
            pts.append((CX - hw + 2 * hw * t, level + 44 - 22 * edge + b))
        pts += [(CX + 128, 496), (CX + 90, 510), (CX, 514), (CX - 90, 510), (CX - 128, 496)]
        return smooth_closed(pts)

    frames = list(range(0, END + 1, 4))
    mass_anim = anim([(f, mass_path(level_at(f))) for f in frames], linear=True)
    mass = layer("IceMass", [
        group("Mass", [sh(mass_anim),
                       gfill((CX, 60), (CX, 530), [(0, ICE_HI), (0.6, ICE_MID), (1, ICE_DEEP)],
                             [(0, 0.30), (1, 0.38)])]),
    ], o=anim([(0, 100), (86, 100), (96, 0)]))

    # lower rows overlap upper ones: bottom row is the top-most layer
    row_layers = [layer(f"IceRow{i}", rows[i]) for i in range(len(rows))]
    return list(reversed(row_layers)), mass


# ---------------------------------------------------------------- build
def build():
    body = can_body_path()
    ice_rows, ice_mass = build_ice()

    # ---- label text (outlined glyphs, wrapped around the can)
    label_start = layer("LabelStart", [group("Text", text_shapes(LABEL_START, 30, 41, 290, 2.5) +
                                               [fill(WHITE, 88, "TextFill")])],
                        o=anim([(0, 100), (64, 100), (84, 0)]))
    label_end = layer("LabelEnd", [group("Text", text_shapes(LABEL_END, 30, 41, 290, 2.5) +
                                             [fill(WHITE, 92, "TextFill")])],
                      o=anim([(0, 0), (86, 0), (98, 100)]))

    # ---- condensation droplets: dark lower edge, light body, bright highlight
    def drop(nm, x, y, r, extra_tr=None):
        return group(nm, [
            group("Hi", [ellipse(x - r * 0.32, y - r * 0.45, r * 0.62, r * 0.55), fill(WHITE, 100)]),
            group("Hi2", [ellipse(x + r * 0.35, y + r * 0.5, r * 0.35, r * 0.22), fill(WHITE, 55)]),
            group("Body", [ellipse(x, y, r * 2, r * 2.25), fill(WHITE, 26), stroke(WHITE, 0.7, 45)]),
            group("Shade", [ellipse(x + r * 0.18, y + r * 0.28, r * 2.1, r * 2.3), fill(BLACK, 16)]),
        ], extra_tr)

    rng = random.Random(42)
    drops = []
    for d in range(46):
        x = rng.uniform(L + 8, R - 8)
        y = rng.uniform(SHOULDER_Y + 12, BASE_Y - 4)
        r = rng.choice([1.4, 1.8, 2.2, 2.6, 3.2, 4.0, 5.0]) * rng.uniform(0.9, 1.1)
        drops.append(drop(f"Drop{d}", x, y, r))
    # drops that run down, leaving a wet trail that grows behind them
    for d, (x, y0, r, f0, dist) in enumerate([(152, 175, 5.2, 56, 95), (240, 205, 5.8, 64, 110),
                                              (198, 160, 4.6, 74, 80)]):
        f1 = min(END, f0 + 30)

        def trail(dy, x=x, y0=y0, r=r):
            return path([(x - 1, y0 - 6), (x + 1, y0 - 6), (x + r * 0.75, y0 + dy), (x - r * 0.75, y0 + dy)])
        drops.append(group(f"Trail{d}", [sh(anim([(f0, trail(0)), (f1, trail(dist))], linear=True)),
                                         fill(WHITE, 16)]))
        drops.append(drop(f"Runner{d}", x, y0, r, tr(p=anim([(f0, [0, 0]), (f1, [0, dist])], linear=True))))
    droplets = layer("Condensation", drops, o=anim([(0, 0), (40, 0), (62, 100)]), mask=body)

    # ---- frost: crisp, crystalline; peaks as the top of the can is uncovered
    rng = random.Random(7)
    frost_items = []
    specks = []
    for i in range(110):
        s_ = rng.uniform(1.0, 2.6)
        specks.append(ellipse(rng.uniform(L, R), rng.uniform(LID_Y, BOT_Y), s_, s_, f"S{i}"))
    frost_items.append(group("Specks", specks + [fill(WHITE, 80)]))
    crystals = []
    for c in range(26):
        x, y, s = rng.uniform(L, R), rng.uniform(SHOULDER_Y - 20, BOT_Y), rng.uniform(4, 9)
        for a in (0, 60, 120):
            th = math.radians(a + rng.uniform(-8, 8))
            crystals.append(sh(path([(x - s * math.cos(th), y - s * math.sin(th)),
                                     (x + s * math.cos(th), y + s * math.sin(th))], closed=False)))
    frost_items.append(group("Crystals", crystals + [stroke(WHITE, 0.9, 75)]))
    patches = []
    for b in range(11):
        pts = []
        bx, by = rng.uniform(L - 10, R + 10), rng.uniform(SHOULDER_Y - 30, BASE_Y + 20)
        bw, bh = rng.uniform(40, 90), rng.uniform(26, 60)
        for k in range(8):
            a = 2 * math.pi * k / 8
            rr = rng.uniform(0.7, 1.1)
            pts.append((bx + bw * rr * math.cos(a), by + bh * rr * math.sin(a)))
        patches.append(sh(smooth_closed(pts)))
    frost_items.append(group("Patches", patches + [fill(WHITE, 24), stroke(WHITE, 0.8, 40)]))
    frost_items.append(group("Haze", [sh(body), gfill((0, LID_Y), (0, BOT_Y), [(0, WHITE), (1, WHITE)],
                                                       [(0, 0.42), (0.5, 0.30), (1, 0.45)])]))
    frost = layer("Frost", frost_items, o=anim([(0, 30), (10, 100), (36, 100), (60, 0)]), mask=body)

    # ---- lid: brushed aluminium
    rings = [ellipse(CX, LID_Y + 1, 128 * f, 16 * f, f"Ring{i}") for i, f in enumerate((0.92, 0.78, 0.62, 0.45))]
    lid = layer("Lid", [
        group("TabHole", [ellipse(CX + 15, LID_Y - 0.5, 10, 4), fill((0.42, 0.45, 0.5))]),
        group("Rivet", [ellipse(CX - 3, LID_Y, 6, 3.4), fill((0.92, 0.93, 0.95)), stroke(SILVER_DARK, 0.6, 80)]),
        group("Tab", [sh(rounded_poly([(CX - 12, LID_Y - 4), (CX + 24, LID_Y - 4.5), (CX + 24, LID_Y + 3.5),
                                       (CX - 12, LID_Y + 4)], 3.5)),
                      gfill((CX - 12, 0), (CX + 24, 0), [(0, (0.95, 0.96, 0.97)), (1, (0.74, 0.77, 0.8))],
                            [(0, 1), (1, 1)]),
                      stroke(SILVER_DARK, 0.9, 90)]),
        group("Brush", rings + [stroke(WHITE, 0.6, 35)]),
        group("Well", [ellipse(CX, LID_Y + 1, 128, 16),
                       gfill((CX - 64, LID_Y - 6), (CX + 64, LID_Y + 8),
                             [(0, (0.86, 0.88, 0.9)), (0.45, (0.70, 0.73, 0.77)), (1, (0.58, 0.61, 0.66))],
                             [(0, 1), (1, 1)])]),
        group("Rim", [ellipse(CX, LID_Y, LID_R - LID_L, LID_RY * 2),
                      gfill((LID_L, LID_Y), (LID_R, LID_Y),
                            [(0, (0.62, 0.65, 0.69)), (0.3, (0.97, 0.98, 0.99)), (0.55, (0.82, 0.84, 0.87)),
                             (1, (0.56, 0.59, 0.63))],
                            [(0, 1), (1, 1)]),
                      stroke(SILVER_DARK, 1.1, 85)]),
    ])

    # ---- shading over the whole can (cylinder lit from the upper-left)
    shading = layer("CanShading", [
        group("Outline", [sh(body), stroke((0.18, 0.2, 0.24), 1.0, 35)]),
        group("BandLines", [sh(edge_line(SHOULDER_Y + 4, 8)), sh(edge_line(BASE_Y + 4, 8)),
                            stroke(SILVER_DARK, 1.2, 75)]),
        group("BandHi", [sh(edge_line(SHOULDER_Y + 6.5, 8)), sh(edge_line(BASE_Y + 6.5, 8)),
                         stroke(WHITE, 0.9, 45)]),
        group("Specular", [sh(body), gfill((L, 0), (R, 0), [(0, WHITE), (1, WHITE)],
                                            [(0, 0), (0.25, 0), (0.285, 0.62), (0.31, 0.70), (0.345, 0.08),
                                             (0.4, 0), (1, 0)])]),
        group("Soft", [sh(body), gfill((L, 0), (R, 0), [(0, WHITE), (1, WHITE)],
                                        [(0, 0), (0.12, 0.06), (0.3, 0.22), (0.5, 0.04), (1, 0)])]),
        group("Rimlight", [sh(body), gfill((L, 0), (R, 0), [(0, WHITE), (1, WHITE)],
                                            [(0, 0), (0.88, 0), (0.94, 0.20), (0.985, 0)])]),
        group("Edges", [sh(body), gfill((L, 0), (R, 0), [(0, BLACK), (1, BLACK)],
                                         [(0, 0.40), (0.12, 0.10), (0.24, 0), (0.6, 0), (0.8, 0.16),
                                          (0.93, 0.36), (1, 0.55)])]),
        group("Vertical", [sh(body), gfill((0, LID_Y), (0, BOT_Y), [(0, WHITE), (0.5, BLACK), (1, BLACK)],
                                            [(0, 0.10), (0.15, 0), (0.85, 0), (1, 0.18)])]),
    ])

    bands = layer("CanBands", [
        group("Neck", [sh(band_path(SHOULDER_Y + 4, 8, True)), fill(SILVER)]),
        group("Base", [sh(band_path(BASE_Y + 4, 8, False)), fill(SILVER)]),
    ])

    can_body = layer("CanBody", [group("Accent", [sh(body), fill(DEFAULT_ACCENT, 100, "AccentFill")])])

    contact_shadow = layer("ContactShadow", [
        group("Shadow", [ellipse(CX, BOT_Y + 7, 210, 28),
                         gfill((CX, BOT_Y + 7), (CX + 105, BOT_Y + 7), [(0, BLACK), (1, BLACK)],
                               [(0, 0.34), (0.55, 0.14), (1, 0)], radial=True)])])

    # reflection of the can in the water (accent tinted), fading downward
    reflection = layer("CanReflection", [
        group("Glint", [ellipse(CX - 34, BOT_Y + 22, 8, 20), fill(WHITE, 55)]),
        group("Accent", [sh(path([(LID_L + 4, BOT_Y + 12), (LID_R - 4, BOT_Y + 12), (LID_R - 16, BOT_Y + 40),
                                  (LID_L + 16, BOT_Y + 40)])), fill(DEFAULT_ACCENT, 100, "AccentFill")]),
    ], o=anim([(0, 0), (50, 0), (100, 30)]))

    # irregular puddle outline
    rng = random.Random(3)
    n = 12
    pts = []
    for k in range(n):
        ang = 2 * math.pi * k / n
        rr = rng.uniform(0.84, 1.08)
        pts.append((CX + 176 * rr * math.cos(ang), 494 + 36 * rr * math.sin(ang)))
    puddle_shape = smooth_closed(pts)
    puddle = layer("Puddle", [
        group("Sheen", [ellipse(CX + 78, 503, 74, 7), fill(WHITE, 70)]),
        group("Sheen2", [ellipse(CX - 112, 487, 36, 4.5), fill(WHITE, 55)]),
        group("Sheen3", [ellipse(CX + 128, 486, 18, 3), fill(WHITE, 50)]),
        group("Water", [sh(puddle_shape),
                        gfill((CX, 494), (CX + 180, 494), [(0, WATER), (1, (0.9, 0.95, 0.99))],
                              [(0, 0.60), (0.7, 0.48), (1, 0.30)], radial=True),
                        stroke(WHITE, 1.8, 80)]),
        group("Edge", [sh(puddle_shape), stroke((0.5, 0.65, 0.82), 3.2, 20)]),
    ], p=(CX, 494), a=(CX, 494),
        s=anim([(0, [55, 55, 100]), (30, [62, 65, 100]), (70, [84, 88, 100]), (100, [100, 100, 100])]),
        o=anim([(0, 0), (8, 0), (34, 100)]))

    layers = (ice_rows + [ice_mass, droplets, frost, label_end, label_start, lid, shading, bands, can_body,
                          contact_shadow, reflection, puddle])
    return {
        "v": "5.7.4", "fr": FPS, "ip": 0, "op": END, "w": W, "h": H, "nm": "MeltingCan",
        "ddd": 0, "assets": [], "layers": layers, "markers": [
            {"tm": 0, "cm": "start", "dr": 0}, {"tm": 25, "cm": "frosted", "dr": 0},
            {"tm": 75, "cm": "condensation", "dr": 0}, {"tm": END, "cm": "complete", "dr": 0}],
    }


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "melting_can.json"
    with open(out, "w") as f:
        json.dump(build(), f, separators=(",", ":"))
    print("wrote", out)
