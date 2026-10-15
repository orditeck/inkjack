# Third-party components

| Component | Version | License | Source |
| --- | --- | --- | --- |
| Tesseract4Android, OpenMP variant | 4.9.0 | Apache-2.0 | https://github.com/adaptech-cz/Tesseract4Android/tree/4.9.0 |
| Tesseract OCR, included by Tesseract4Android | 5.5.1 | Apache-2.0 | https://github.com/tesseract-ocr/tesseract |
| Leptonica, included by Tesseract4Android | 1.85.0 | BSD-2-Clause | https://github.com/DanBloomberg/leptonica |
| libjpeg, included by Tesseract4Android | 9f | IJG license | https://www.ijg.org/ |
| libpng, included by Tesseract4Android | 1.6.48 | libpng-2.0 | http://www.libpng.org/pub/png/libpng.html |
| tessdata_fast English and French models | 4.1.0 | Apache-2.0 | https://github.com/tesseract-ocr/tessdata_fast/tree/4.1.0 |
| Android core library desugaring | 2.1.5 | See upstream notices | https://github.com/google/desugar_jdk_libs |
| Gradle wrapper | 8.13 | Apache-2.0 | https://github.com/gradle/gradle/tree/v8.13.0 |

Build/test tools include Android Gradle Plugin 8.9.2, JUnit 4.13.2 (EPL-1.0), and Robolectric 4.14.1 (MIT). AndroidX annotations are a transitive dependency of Tesseract4Android. Native dependency notices are provided by their upstream distributions; the project-level Apache license does not replace those terms.

REA and the reference APK are development/reference tools only. They are not bundled or copied into Inkjack.
