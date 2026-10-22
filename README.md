# Number System Converter

An Android app that converts between **binary (2), octal (8), decimal (10) and hexadecimal (16)**.
It shows the answer together with a full step-by-step solution that a student can copy onto paper.

## How it works

| From → To | Method shown |
|---|---|
| Decimal → Binary / Octal / Hex | Repeated division by the base (e.g. `394 ÷ 8 = 49.25`, then `0.25 × 8 = 2` gives the remainder), remainders read bottom to top. Fractions: repeated multiplication, digits read top to bottom. |
| Binary / Octal / Hex → Decimal | Positional notation: digit × base^position, negative powers after the point, then add the results. |
| Binary → Octal / Hex | Group bits in 3s / 4s from the point, pad with zeros, convert each group. |
| Octal / Hex → Binary | Replace each digit with its 3-bit / 4-bit code, join, drop extra zeros. |
| Octal ↔ Hex | Stage 1: to binary. Stage 2: binary to target. |
| Same base | Normalised only (no conversion). |

Every conversion that doesn't end in decimal also gets a **check** step, where both numbers are converted to
decimal and compared, as in the class notes.

Other details:
- Negative numbers, leading zeros, lowercase hex and fractions are all supported. Inputs can be up to 64 digits long.
- All arithmetic uses `BigInteger` / `BigDecimal`, so there are no floating-point errors.
- When a decimal fraction never ends in the target base (e.g. 0.1₁₀ in binary), the repeating part is detected
  and the answer is cut off at the chosen number of digits (8 / 12 / 16 / 24) and marked **≈ approximate**.
- The app works fully offline.

## Code layout

All files are in `app/src/main/java/com/bsit1c/numbersystemconverter/`.

**Conversion engine** (plain Kotlin with no Android imports, so it can be unit-tested on the JVM):
- `NumberBase.kt`: the four bases and their digit rules
- `InputValidator.kt`: validation, normalisation, helpful error messages (`Digits`, `ParsedNumber`)
- `Solution.kt`: the step model (`SolutionStep`, `StepContent`, `ConversionResult`)
- `DecimalToBaseMethod.kt`, `PositionalMethod.kt`, `BinaryGroupingMethod.kt`: the conversion methods.
  Each one writes its steps while it computes, so the steps you see are always the actual calculation.
- `NumberConverter.kt`: picks the method for each pair, handles the sign, adds the check and final answer
- `MathFormat.kt`: subscripts and superscripts (348₁₀, 16⁻²)

**UI** (Jetpack Compose + Material 3, no extra dependencies):
- `AppTheme.kt`: the design system (light and dark colour schemes, type, shapes, spacing, monospaced math styles)
- `AppIcons.kt`: the few vector icons the app uses (copy, check, swap, chevron, close)
- `ConverterViewModel.kt`: screen state. It runs conversions on a background thread, skips repeated
  identical requests, keeps state across rotation and process death, and collapses steps
- `SolutionRows.kt`: flattens a solution into small keyed rows (header, paragraph, formula line,
  table row, end), so long solutions scroll smoothly in a `LazyColumn`
- `ConverterScreen.kt`: one column on phones, two panes (input | solution) on wide screens.
  It has the answer card with Copy, the conditional precision picker and animations
- `SolutionViews.kt`: the composables for each row type
- `MainActivity.kt`

The `ui/theme` files from the Android Studio template are no longer used; they can be deleted.

**Tests**: `NumberConverterTest.kt` covers the slide examples, edge cases, validation,
and thousands of randomised round-trips across all 12 directions. `SolutionRowsTest.kt` covers the row flattening
(unique keys, shared table layouts, collapsing).

## Run

Open the folder in Android Studio, let Gradle sync, then run the `app` configuration.
To run the unit tests, right-click `NumberConverterTest` and choose **Run**, or use `gradlew test`.
