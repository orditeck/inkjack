# Inkjack privacy policy

Effective October 15, 2026. This policy describes the source code in this repository.

Inkjack has no account system, advertising, analytics, crash-reporting endpoint, or backend. Its manifest does not request internet permission. OCR runs on the device using bundled Tesseract models.

When you explicitly activate a capture, Inkjack reads visible text exposed through Android accessibility. It excludes nodes marked as password fields and does not traverse their children. When no accessible text is found, Inkjack automatically tries one screenshot using Android's accessibility screenshot API and runs OCR locally. This fallback is skipped if visible password fields were excluded or traversal reached its safety limit. You can explicitly request another method through Rescan with OCR when text is missing. Password exclusion cannot identify arbitrary sensitive text rendered into an image, so select only the text you intend to use. Android may refuse screenshots of protected windows; Inkjack reports that refusal.

Images and PDFs are accessed through user-granted content URIs. Inkjack processes their content locally and does not save copies. Screenshots and temporary image buffers stay in memory and are released after use. OCR data files are copied from the APK to app-private storage for the OCR engine.

Recognized text, editor content, and the multi-screen collection remain in process memory. Android can terminate that process, which discards the collection. An editor draft may temporarily be held in Android's activity saved state to survive a configuration change. There is no permanent capture history. Language, notification-shortcut, and capture-toolbar presentation preferences are stored in app-private settings. App backups are disabled.

When you tap Copy, selected text is placed on Android's system clipboard, which is governed by Android's clipboard controls. When you tap Share, text is passed to the recipient app you select, which has its own privacy practices. Your keyboard may process text entered in the editor according to its own settings.

You can revoke accessibility access in Android Settings at any time. Uninstalling Inkjack removes its private data. Clear the system clipboard separately if necessary.

Report code or privacy issues through this repository's issue tracker without including private captured content.
