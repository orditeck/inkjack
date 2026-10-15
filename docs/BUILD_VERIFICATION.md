# Build verification

Verified October 15, 2026 in a Linux build environment with JDK 17, Android platform 35, AGP 8.9.2, and Gradle 8.13.

- `testDebugUnitTest lintDebug assembleDebug`: **passed**.
- **20 tests**, zero failures or errors. Includes two-axis dragging, safe bounds and cutout insets, saved position restoration, size changes, edge-jump controls, OCR fallback and password exclusion, toolbar behavior, settings persistence, empty states, and native Skia renders of the actual Android home, editor, capture toolbar, and customization views. The toolbar is also checked at 1.5× font scale on a narrow screen.
- Android Lint: **zero errors, zero warnings**.
- Debug APK: Android 11+ (minSdk 30), targetSdk 35, version 0.3.0 (3).
- APK signature verified with Android SDK `apksigner` (v2 signature).
- APK alignment verified with `zipalign -c -P 16 -v 4`; ARM64 native libraries use 16 KB-aligned load segments.
- Bundled English/French models match the pinned SHA-256 checksums. License notices are included in the APK.
- Final merged manifest requests only the optional `POST_NOTIFICATIONS` permission. Accessibility and Quick Settings services are protected by their system binding permissions. No internet permission is present.

The screenshots in `docs/screenshots` were rendered from Android views using Robolectric's native graphics engine. They are not emulator/device screenshots.

Physical-device testing, cross-app accessibility interaction, actual device OCR execution, secure-window behavior, notification-shade timing, and fold/unfold behavior have **not** been validated in this environment. Use the device checklist in TESTING.md before treating this as a stable release.
