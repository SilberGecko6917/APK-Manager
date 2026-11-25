# Third-party assets

| Asset | Location | License |
|---|---|---|
| [Google Sans Flex](https://github.com/googlefonts/googlesans-flex) | `core/designsystem/src/main/res/font/core_designsystem_google_sans_flex.ttf` | SIL Open Font License 1.1, see [google-sans-flex/OFL.txt](google-sans-flex/OFL.txt) |
| [Material Symbols](https://github.com/google/material-design-icons) (Rounded) | `core/designsystem/src/main/res/drawable/core_designsystem_ic_*.xml` | Apache License 2.0 |

## Modifications

- **Google Sans Flex:** instanced with [fontTools](https://github.com/fonttools/fonttools) to `slnt=0` and `wdth=100` (the `wght`, `opsz` and `ROND` axes are kept) and subset to Latin, Latin Extended, Vietnamese, punctuation, currency and arrows to reduce the APK size.
- **Material Symbols:** the `android:tint` attribute was removed so Compose can tint the icons.
