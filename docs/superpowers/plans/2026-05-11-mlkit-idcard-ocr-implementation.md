# ML Kit ID Card OCR Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a first usable Android offline OCR flow for the front side of Chinese mainland second-generation ID cards with guided capture, offline ML Kit recognition, structured parsing, validation, and manual review.

**Architecture:** Keep the app XML-based and centered on a `MainActivity` capture screen plus a `ReviewActivity` result screen. Put OCR parsing logic in small Kotlin classes that can be unit-tested independently from Android UI code, then connect those classes to a CameraX + ML Kit pipeline.

**Tech Stack:** Android XML views, AppCompat, Material Components, CameraX, ML Kit Text Recognition Chinese, Kotlin, JUnit4

---

## File Structure

### Existing files to modify

- `D:\GitDemo\MLKitOcr\gradle\libs.versions.toml`
- `D:\GitDemo\MLKitOcr\app\build.gradle.kts`
- `D:\GitDemo\MLKitOcr\app\src\main\AndroidManifest.xml`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\MainActivity.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\res\layout\activity_main.xml`
- `D:\GitDemo\MLKitOcr\app\src\main\res\values\strings.xml`
- `D:\GitDemo\MLKitOcr\app\src\main\res\values\colors.xml`
- `D:\GitDemo\MLKitOcr\app\src\main\res\values\themes.xml`

### New production files

- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\ocr\OcrTextLine.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\ocr\TextBounds.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\IdCardFrontFields.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\ParsedField.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\RecognitionConfidence.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\IdCardFrontParser.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\ChinaIdNumberValidator.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\IdCardFrontRecognizer.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\mlkit\MlKitOcrEngine.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\camera\BitmapCropper.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\camera\CapturedFrame.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\camera\IdCardOverlayView.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\review\ReviewActivity.kt`
- `D:\GitDemo\MLKitOcr\app\src\main\res\layout\activity_review.xml`
- `D:\GitDemo\MLKitOcr\app\src\main\res\drawable\bg_capture_hint.xml`
- `D:\GitDemo\MLKitOcr\app\src\main\res\drawable\bg_overlay_cutout.xml`

### New test files

- `D:\GitDemo\MLKitOcr\app\src\test\java\com\example\mlkitocr\idcard\ChinaIdNumberValidatorTest.kt`
- `D:\GitDemo\MLKitOcr\app\src\test\java\com\example\mlkitocr\idcard\IdCardFrontParserTest.kt`

## Task 1: Add Dependencies And App Wiring

**Files:**
- Modify: `D:\GitDemo\MLKitOcr\gradle\libs.versions.toml`
- Modify: `D:\GitDemo\MLKitOcr\app\build.gradle.kts`
- Modify: `D:\GitDemo\MLKitOcr\app\src\main\AndroidManifest.xml`
- Modify: `D:\GitDemo\MLKitOcr\app\src\main\res\values\strings.xml`

- [ ] Add version catalog entries for CameraX `1.5.3` and bundled ML Kit Chinese text recognition `16.0.1`.
- [ ] Add module dependencies for `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`, and `text-recognition-chinese`.
- [ ] Add `kotlin-parcelize` plugin only if parcelable transport becomes necessary; otherwise keep intent extras primitive and serializable.
- [ ] Add camera permission and declare `ReviewActivity` in the manifest.
- [ ] Add user-visible strings for capture guidance, processing errors, and review labels.

## Task 2: Build And Test The Parsing Core With TDD

**Files:**
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\ocr\TextBounds.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\ocr\OcrTextLine.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\ParsedField.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\RecognitionConfidence.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\IdCardFrontFields.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\ChinaIdNumberValidator.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\IdCardFrontParser.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\test\java\com\example\mlkitocr\idcard\ChinaIdNumberValidatorTest.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\test\java\com\example\mlkitocr\idcard\IdCardFrontParserTest.kt`

- [ ] Write failing tests for valid and invalid 18-digit ID number checksum behavior.
- [ ] Run `./gradlew.bat test --tests com.example.mlkitocr.idcard.ChinaIdNumberValidatorTest` and verify the test fails for the expected missing-class reason.
- [ ] Implement the minimal validator to pass checksum and birth-date plausibility tests.
- [ ] Re-run `./gradlew.bat test --tests com.example.mlkitocr.idcard.ChinaIdNumberValidatorTest`.
- [ ] Write failing parser tests that feed synthetic OCR lines for `姓名`, `性别`, `民族`, `出生`, `住址`, and `公民身份号码`.
- [ ] Run `./gradlew.bat test --tests com.example.mlkitocr.idcard.IdCardFrontParserTest` and verify failure.
- [ ] Implement the minimal parser that uses anchor labels, relative positions, address merging, and ID-number normalization.
- [ ] Re-run `./gradlew.bat test --tests com.example.mlkitocr.idcard.IdCardFrontParserTest`.

## Task 3: Connect ML Kit OCR To The Parsing Core

**Files:**
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\mlkit\MlKitOcrEngine.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\idcard\IdCardFrontRecognizer.kt`

- [ ] Add a small OCR engine that builds a `TextRecognizer` with `ChineseTextRecognizerOptions`.
- [ ] Convert ML Kit `Text.Line` output into the testable `OcrTextLine` model with bounding boxes.
- [ ] Add a recognizer class that takes a `Bitmap`, runs OCR, invokes the parser, and classifies the result as `HIGH`, `PARTIAL`, or `LOW`.
- [ ] Return user-readable failure reasons when no useful anchors are found.

## Task 4: Build The Capture UI

**Files:**
- Modify: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\MainActivity.kt`
- Modify: `D:\GitDemo\MLKitOcr\app\src\main\res\layout\activity_main.xml`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\camera\CapturedFrame.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\camera\BitmapCropper.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\camera\IdCardOverlayView.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\res\drawable\bg_capture_hint.xml`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\res\drawable\bg_overlay_cutout.xml`

- [ ] Replace the hello-world layout with a `PreviewView`, a custom overlay, guidance text, a capture button, a retake-status text area, and a loading indicator.
- [ ] Request camera permission at runtime and show a blocking message if it is denied.
- [ ] Bind `Preview` and `ImageCapture` use cases with `ProcessCameraProvider`.
- [ ] Capture to memory, rotate the frame correctly, and crop the center card area based on the overlay proportions before OCR.
- [ ] Disable repeated taps while a frame is being processed.

## Task 5: Build The Review Screen

**Files:**
- Create: `D:\GitDemo\MLKitOcr\app\src\main\java\com\example\mlkitocr\review\ReviewActivity.kt`
- Create: `D:\GitDemo\MLKitOcr\app\src\main\res\layout\activity_review.xml`
- Modify: `D:\GitDemo\MLKitOcr\app\src\main\AndroidManifest.xml`

- [ ] Create a review screen with editable fields for `name`, `gender`, `ethnicity`, `birthDate`, `address`, and `idNumber`.
- [ ] Show a confidence summary at the top so the user knows whether the result is high-confidence or partial.
- [ ] Add a confirm action that closes the flow and a retake action that returns to capture.

## Task 6: End-To-End Verification

**Files:**
- Modify any touched files above if verification exposes gaps

- [ ] Run `./gradlew.bat test` and confirm unit tests pass.
- [ ] Run `./gradlew.bat assembleDebug` and confirm the app compiles.
- [ ] If a device is available later, smoke-test the flow manually with one clear front-side ID-card image and one intentionally poor image.
- [ ] Report any remaining known limits clearly instead of implying full production accuracy.

## Plan Notes

- This directory is currently not a git repository, so commit steps are intentionally omitted from the plan.
- TDD will be applied to the parser and validator core where host unit tests are practical.
- CameraX and ML Kit integration will still be verified with Gradle build evidence before completion.
