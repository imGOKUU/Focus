package com.demo.myapplication.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Two corner radii for the whole app, so every surface feels like it came
// from the same kit: 16dp for anything that sits on the page (cards,
// buttons, inputs, banners), 12dp for anything nested inside one of those
// (icon tiles, chips, the wheel's selection band) - the inner radius is
// smaller so the curves stay optically parallel with the card around them.
val CardCornerRadius = 16.dp
val TileCornerRadius = 12.dp

val CardShape = RoundedCornerShape(CardCornerRadius)
val TileShape = RoundedCornerShape(TileCornerRadius)
val ButtonShape = CardShape
val ChipShape = TileShape

val FocusShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = TileShape,
    medium = TileShape,
    large = CardShape,
    extraLarge = CardShape
)
