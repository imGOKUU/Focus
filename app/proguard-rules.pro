# Meltide release keep rules.
#
# Hilt, Room, DataStore, Compose and Lottie ship consumer rules with their
# libraries, so nothing is needed for them here. Components referenced from
# the manifest (activities, the accessibility service, receivers) are kept
# automatically by AAPT's generated rules.
#
# Keep line numbers so release crash stack traces stay readable (map them
# back with the mapping.txt in app/build/outputs/mapping/release/).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
