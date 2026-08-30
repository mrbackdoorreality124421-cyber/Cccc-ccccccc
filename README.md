# Chess Master Pro

A modern, full-featured Chess application for Android built using **Jetpack Compose**, **Kotlin Coroutines & Flow**, **Room Database**, and integrated with standard **UCI / OEX Chess Engines**.

---

## 🌟 Key Features

- **Standard Chess Game Modes**:
  - **Play Against Bot**: Challenge the integrated engine with selectable difficulty levels and instant AI move responses.
  - **Bot Helper Mode**: Have the bot act as an advisor/friend for your side, rendering best-move visual arrows with optional auto-play.
  - **2-Player Local (Pass & Play)**: Offline board with complete move validation, check/checkmate detection, and clock support.
  - **Analysis Mode**: Full board exploration with live engine evaluations, depth tracking, and win probability graphs.

- **FEN Position Loader**:
  - Load any standard 6-field FEN position string.
  - Interactive 2D board preview with side-to-move, castling rights, and move number badges.
  - Seamlessly transition into either *Play Against Bot* or *Bot Helper* mode from any custom position.

- **Chess Variants (8 Modes)**:
  - **Fischer Random (Chess960)**: Randomized back-rank pieces maintaining opposite-colored bishops and king between rooks.
  - **King of the Hill**: Race your King to control the four central squares (d4, d5, e4, e5).
  - **Three-Check**: Deliver check 3 times to win the game instantly.
  - **Crazyhouse**: Captured enemy pieces enter your reserve and can be dropped onto any open square.
  - **Antichess (Giveaway)**: Captures are mandatory; the first player to lose all pieces (or get stalemated) wins.
  - **Atomic Chess**: Captures trigger an atomic explosion obliterating surrounding non-pawn pieces.
  - **Horde Chess**: White commands a vast swarm of 36 pawns against Black's conventional army.
  - **Racing Kings**: Both players start on ranks 1 & 2 and race their kings to the 8th rank with check prohibited.
  - **Variant Bot AI**: Dedicated minimax engine with rule evaluations supporting all variant modes.

- **Board Visuals & Customization**:
  - 2D Canvas board with multiple themes (Classic Wood, Modern Blue, Dark Charcoal, Glass).
  - Piece drag-and-drop and tap-to-move animations.
  - Legal move hints, capture indicators, and previous move highlights.

- **Match History & Puzzles**:
  - Match recording with Room database persistence.
  - Tactical puzzles with interactive solutions and position-from-image OCR recognition.

---

## 🚀 Building the Project

### Prerequisites
- **Android Studio** (Koala / Ladybug or newer recommended)
- **JDK 17**
- **Android SDK** (API Level 34 / compileSdk 34)

### Command Line Build
To build the debug APK using Gradle:

```bash
# Build the application APK
gradle assembleDebug

# Run unit and JVM tests
gradle testDebugUnitTest
```

The compiled APK will be available in:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🤖 Continuous Integration (GitHub Actions)

The repository includes a ready-to-use CI workflow at `.github/workflows/android.yml` that:
1. Sets up JDK 17 & Android SDK.
2. Runs all JVM unit tests (`testDebugUnitTest`).
3. Assembles the debug APK (`assembleDebug`).
4. Automatically uploads the generated APK as a downloadable workflow artifact.

---

## 📄 License
Open source and ready for deployment.
