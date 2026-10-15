# Reference scope and provenance

The functional reference is Universal Copy by Camel Corporation, package `com.camel.corp.universalcopy`. Its public listing describes normal accessibility text extraction, scanner OCR, entity detection, copy/share actions, multi-screen collection, and an unattended harvest mode.

Inkjack is a new implementation using documented Android APIs and the open-source Tesseract engine. No reference APK, decompiled source, proprietary resources, or reference branding is included in this repository.

## REA attempt

REA 6.5.0 was installed as a development tool. Its pinned `jadx-headless-mcp` 0.7.1 engine was downloaded and verified against SHA-256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`.

`inspect-android-package` was attempted against a downloaded reference base APK (version 7.1.0, version code 104) (SHA-256 `afb1bd6417f964d44c27c86cfcdd1914ea173a82a07924a2924fd5c460981351`). REA refused to launch its analysis process because the build environment's PID namespace and `/proc` do not agree, causing its required procps ownership checks to fail. No APK inspection findings are claimed. The implementation is based on the public functional description and documented platform APIs.

On a regular Linux/macOS/Windows development machine with a working JDK and REA's Android prerequisites, the independent reference analysis can be repeated using:

```sh
npx rea-agents@6.5.0 inspect-android-package /path/to/reference.apk --format json
```

See the [REA Android analysis documentation](https://github.com/morluto/rea/blob/main/docs/android-analysis.md) for the pinned engine and setup. Keep analysis output and reference binaries outside this repository.

## Public implementation references

- [Universal Copy feature description](https://play.google.com/store/apps/details?id=com.camel.corp.universalcopy)
- [Android AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Android Quick Settings TileService](https://developer.android.com/reference/android/service/quicksettings/TileService)
- [Tesseract4Android 4.9.0](https://github.com/adaptech-cz/Tesseract4Android/tree/4.9.0)
- [Tesseract tessdata_fast 4.1.0](https://github.com/tesseract-ocr/tessdata_fast/tree/4.1.0)
