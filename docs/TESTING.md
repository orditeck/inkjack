# Verification and device checks

Automated verification runs `testDebugUnitTest`, `lintDebug`, and `assembleDebug` locally and in GitHub Actions. Tests cover Unicode entities, repeated-capture deduplication, collection capacity, offscreen clipping, password exclusion, startup without an accessibility service, clipboard substring selection, and editor recreation.

Automated tests do not replace exercising Android's accessibility service and native OCR on a device. Before considering a release stable, run this checklist on an Android phone and a foldable:

1. Install the APK; import a clear English image and a French image containing accents. Repeat in airplane mode. Confirm no model download is requested.
2. Import a small PDF, a rotated photo, and an encrypted PDF. Confirm the last one displays an error. Verify the 20-page limit.
3. Read the disclosure, enable the service, and add the tile. Capture a browser and a native app. Confirm the shade closes before text is read.
4. Select individual regions, select all, long-press a region, review, edit, and copy a substring. Verify the clipboard in another app.
5. Use Scan image on a screenshot or image-only view. Confirm region outlines align. Close without copying and verify no screenshot is written to storage.
6. Verify Collect text across two screens and two apps. Add the same selection twice and confirm it is not duplicated. Clear the collection.
7. Test a secure screen and password inputs. Normal mode must exclude password fields; secure screenshot requests must fail clearly.
8. Rotate and fold/unfold while selecting, scanning, and editing. Rotate during a PDF import. Check Back, home, lock/unlock, and service revocation.
9. Deny notification permission. The tile and image import must still work. Disable accessibility and confirm the shortcut is removed.

## Known limits

- English/French OCR is less accurate for handwriting, stylized fonts, small text, and low-contrast screenshots. Other writing systems need their own trained models.
- Only the default display is captured. OEM secure-screen and accessibility restrictions still apply.
- Normal capture reads only visible nodes exposed by the app. Canvas-rendered content may require OCR.
- The collection deduplicates identical whole selections, not overlapping sentences from partially overlapping screenshots.
- UI accessibility rectangles depend on the source app's node bounds. Edit text provides native text selection when the bounds are imprecise or hidden beneath the toolbar.
- Capture is manual. There is no unattended harvest mode or automatic scrolling.
- OCR requests are serialized. There is no persistent OCR job queue or process-death recovery for imported content.
- Debug builds are for personal testing; release signing and physical-device checks are required before store distribution.

## Capture toolbar customization

- Verify Copy text and Collect text are disabled with no selection; Edit all text opens all detected text. Select blocks and verify Edit text opens only those blocks.
- Verify Select all changes to Clear selection when everything is selected. An empty capture should explain Scan screen and keep it available.
- Open Customize capture toolbar from the home screen and from the overlay menu. Change position, spacing, color, dimming, selection preview, shortcuts, and close-after-copy. Confirm the live preview updates and settings survive an app restart.
- Hide Scan screen and Collect text shortcuts, then confirm they remain in the overlay menu.
- Move the toolbar to the opposite edge during a capture. Test portrait/landscape, a display cutout, gesture navigation, large fonts, and a foldable display.
- Turn off close-after-copy and verify multiple selections can be copied without leaving capture. Reset defaults and confirm the collection is kept.
