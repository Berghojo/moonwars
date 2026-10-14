# Moonwars

**Moonwars** is a fork of [Moonlight for Android](https://github.com/moonlight-stream/moonlight-android)
built for playing **Guild Wars 2** (or any mouse-and-keyboard game) from an Android phone or tablet.
It streams your PC with Moonlight as usual. It also adds a **Gamepad → Keyboard & Mouse** mode that turns the on-screen
controller, or a physical controller, into keyboard and mouse input:

| Controller | Sent to the PC |
|---|---|
| Left stick | `W` `A` `S` `D` (8-way, diagonals press two keys) |
| Right stick | Mouse movement while **holding the right mouse button**, so the GW2 camera turns |
| Buttons / triggers / D-pad | Any key, mouse button or mouse wheel (configurable, two layers) |

The host only sees a keyboard and mouse. No virtual Xbox controller is created.

## Install

1. Download `Moonwars-*.apk` from the [Releases](../../releases) page, or from the **Build APK**
   workflow run's artifacts under the **Actions** tab.
2. Sideload it on your phone. You may need to allow "install unknown apps".
   Moonwars has its own app ID, so it installs alongside the official Moonlight app.
3. On your PC, run [Sunshine](https://github.com/LizardByte/Sunshine) (recommended) or GeForce Experience,
   then pair Moonwars with it the same way you would pair Moonlight.

## Settings

Open **Settings → Gamepad to Keyboard & Mouse**:

* **Map controller to keyboard & mouse**: on by default. Turn it off to get normal Moonlight gamepad behavior.
* **Button bindings**: choose what every button sends. There are two columns:
  * *Bar 1*: the binding used normally.
  * *Bar 2*: used after tapping a button bound to **Swap skill bar** (the on-screen **BAR** button),
    or while holding one bound to **Layer shift**. If this is set to *None*, the Bar 1 binding is used.
* **Hold right mouse button for camera**: on by default, which GW2 needs. Turn it off if you use GW2's
  *Action Camera*, or want the right stick to move the mouse cursor. The **Guide** button switches between
  the two modes while you play.
* **Camera sensitivity**: right stick speed while turning the camera.
* **Cursor sensitivity**: right stick speed in cursor mode (after pressing Guide).
* **Invert camera Y axis**, **Movement stick deadzone**.
* **Touch sensitivity** (under *Input Settings*): how fast the camera turns when you drag a finger
  on the screen.

The on-screen controller is enabled by default. Next to the gamepad buttons, it adds:

**Touching the stream** (anywhere outside the buttons):

* **Tap**: left click exactly where you touched (NPCs, loot, UI, menus).
* **Drag one finger**: holds the right mouse button and turns the camera, like right-click dragging
  in GW2.
* **Drag two fingers up / down**: zoom in / out (mouse wheel), like GeForce NOW.

**On-screen layout (keyboard/mouse mode)** is styled after Guild Wars 2's own HUD:

* **Move stick**: bottom left.
* **Skill bar**: bottom center. The `1-5` / `6-0` slot on its left swaps the bar, and the five square
  slots are the skills. **F1 / F2 / F3** sit above it like GW2's profession bar.
* **Dodge (V), jump (Space)**: big round buttons in the bottom right corner, under your right thumb.
* **Interact (F), target (Tab), inventory (I), friend list (Z)**: round buttons in the top right
  corner, with interact in the corner.
* **Tray** along the top left: keyboard, sensitivity, hide-all (eye), Esc and Map.
  The small arrow tab at its left end folds the tray away to the edge and back. The app remembers
  whether it's folded.

Every on-screen button shows the key it sends. All controls have white outlines on a dark backdrop,
so they stay readable over the game. Use the gear icon (top left) to move or resize anything. This
layout is saved separately from the gamepad layout. The tray buttons:

* **Keyboard icon**: opens Android's keyboard so you can type to the PC, e.g. in chat. Tap it again to
  close it. A three-finger tap on the stream does the same.
* **Sliders icon**: opens sensitivity settings without leaving the game: touch camera sensitivity,
  movement deadzone, and settings for a physical controller's right stick. Changes apply immediately
  and are saved.
* **Eye icon**: hides every other control, so you can click something underneath them. Tap it again to
  bring them back.

Double-tapping the move stick presses L3 (zoom out in the Bladesworn preset).
 To move or resize buttons, use the gear icon in the top-left
of the stream; this is the standard Moonlight on-screen controller editor. By default, dragging on the stream
outside the controls turns the camera. Switch to direct click with the touch mode button to click
UI, loot and dialogs.

## Presets

Pick a preset under **Settings → Gamepad to Keyboard & Mouse → Button bindings**. Bladesworn is the default.
Each on-screen button shows the key it currently sends, and the labels change when you swap bars.

### Bladesworn (default)

| Input | Bar 1 | Bar 2 (after tapping **BAR**) |
|---|---|---|
| A / B / X / Y / RB | `1` `2` `3` `4` `5` | `6` `7` `8` `9` `0` |
| **BAR** (on-screen) | Swap to bar 2 | Swap to bar 1 |
| **F1** (on-screen extra button) | `F1` Dragon Trigger | (same) |
| LT / RT | `V` dodge / `Space` jump | (same) |
| D-pad ↑ → ↓ ← (on screen: F2, F3, interact, target buttons) | `F2` gunsaber, `F3`, `F` interact, `Tab` next target | (same) |
| Start / Back | `Esc` / `M` map | (same) |
| Inventory / friend list (on-screen) | `I` / `Z` | (same) |
| Left stick | W A S D | (same) |
| Touch | tap = left click, drag = camera, 2 fingers = zoom | (same) |

On a physical controller, the right stick turns the camera, LB / L3 zoom in / out, and Guide toggles
the right stick between camera and cursor.

Holding a button holds the key on the PC, so charged skills charge while held and fire on release.
If you swap bars while holding a skill, that skill is still released correctly.

### GW2 core

| Input | Normal | With LB held (or after tapping BAR) |
|---|---|---|
| Left stick | W / A / S / D | (same) |
| Right stick | Camera (mouse + right button) | (same) |
| A / B / X / Y | `1` / `2` / `3` / `4` (weapon skills) | `6` / `7` / `8` / `9` (heal + utilities) |
| RB | `5` | `0` (elite) |
| LB | Layer shift (hold) | |
| LT | `V` (dodge) | |
| RT | `Space` (jump) | |
| D-pad ↑ → ↓ ← | `F1` `F2` `F3` `F4` (profession skills) | ↑ `F5`, → `` ` `` (weapon swap), ↓ wheel down (zoom out), ← wheel up (zoom in) |
| L3 | `Tab` (next target) | |
| R3 | `F` (interact) | |
| Start | `Esc` | |
| Back / Select | `M` (map) | `I` (inventory) |
| Guide | Toggle camera / cursor mode | |

These match GW2's default keybinds. If you changed your in-game keybinds, change the bindings in the app to match.

> **Tip:** In GW2's default keybinds, `A`/`D` *turn* the character and `Q`/`E` *strafe*. While the camera
> stick is in use, the right mouse button is held, and then `A`/`D` strafe as well. If you prefer
> always strafing, bind *Left stick left/right* to `Q`/`E`.

## Building

GitHub Actions builds every push (`.github/workflows/build.yml`). Pushing a tag like `v1.0.0` also publishes
a GitHub release with the APK attached.

Release APKs are signed with a throwaway debug key unless you add these repository secrets.
If you plan to install updates over an existing install, add them, so every build uses the same key:

| Secret | Value |
|---|---|
| `MOONWARS_KEYSTORE_BASE64` | `base64 -w0 my.jks` |
| `MOONWARS_KEYSTORE_PASSWORD` | keystore password |
| `MOONWARS_KEY_ALIAS` | key alias |
| `MOONWARS_KEY_PASSWORD` | key password |

You can create a keystore with:
`keytool -genkeypair -v -keystore my.jks -alias moonwars -keyalg RSA -keysize 4096 -validity 10000`

To build locally, you need Android Studio (or the Android SDK) and the NDK:

```sh
git submodule update --init --recursive
./gradlew assembleNonRootRelease          # APK in app/build/outputs/apk/nonRoot/release/
./gradlew testNonRootDebugUnitTest        # unit tests for the keyboard/mouse mapper
```

The mapping code is in `app/src/main/java/com/limelight/binding/input/kbm/`.

## Updating from upstream Moonlight

This repository keeps Moonlight's full git history, so upstream updates can be merged:

```sh
git remote add upstream https://github.com/moonlight-stream/moonlight-android.git
git fetch upstream && git merge upstream/master
```

## License

Moonwars, like Moonlight, is licensed under the GPLv3 (see `LICENSE.txt`).
It is not affiliated with the Moonlight project, NVIDIA, or ArenaNet.
Guild Wars 2 is a trademark of ArenaNet.

---

*The original Moonlight README follows.*

# Moonlight Android

[![AppVeyor Build Status](https://ci.appveyor.com/api/projects/status/232a8tadrrn8jv0k/branch/master?svg=true)](https://ci.appveyor.com/project/cgutman/moonlight-android/branch/master)
[![Translation Status](https://hosted.weblate.org/widgets/moonlight/-/moonlight-android/svg-badge.svg)](https://hosted.weblate.org/projects/moonlight/moonlight-android/)

[Moonlight for Android](https://moonlight-stream.org) is an open source client for NVIDIA GameStream and [Sunshine](https://github.com/LizardByte/Sunshine).

Moonlight for Android will allow you to stream your full collection of games from your Windows PC to your Android device,
whether in your own home or over the internet.

Moonlight also has a [PC client](https://github.com/moonlight-stream/moonlight-qt) and [iOS/tvOS client](https://github.com/moonlight-stream/moonlight-ios).

You can follow development on our [Discord server](https://moonlight-stream.org/discord) and help translate Moonlight into your language on [Weblate](https://hosted.weblate.org/projects/moonlight/moonlight-android/).

## Downloads
* [Google Play Store](https://play.google.com/store/apps/details?id=com.limelight)
* [Amazon App Store](https://www.amazon.com/gp/product/B00JK4MFN2)
* [F-Droid](https://f-droid.org/packages/com.limelight)
* [APK](https://github.com/moonlight-stream/moonlight-android/releases)

## Building
* Install Android Studio and the Android NDK
* Run ‘git submodule update --init --recursive’ from within moonlight-android/
* In moonlight-android/, create a file called ‘local.properties’. Add an ‘ndk.dir=’ property to the local.properties file and set it equal to your NDK directory.
* Build the APK using Android Studio or gradle

## Authors

* [Cameron Gutman](https://github.com/cgutman)  
* [Diego Waxemberg](https://github.com/dwaxemberg)  
* [Aaron Neyer](https://github.com/Aaronneyer)  
* [Andrew Hennessy](https://github.com/yetanothername)

Moonlight is the work of students at [Case Western](http://case.edu) and was
started as a project at [MHacks](http://mhacks.org).
