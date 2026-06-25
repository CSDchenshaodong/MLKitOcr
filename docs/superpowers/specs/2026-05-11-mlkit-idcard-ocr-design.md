# ML Kit Android Offline ID Card OCR Design

## Overview

This design defines a feasible first version of an Android offline OCR feature for Chinese mainland second-generation resident ID cards. The current project is a fresh Android XML-based template app with a single `MainActivity`, so the design stays close to that baseline and avoids premature complexity.

The first version targets the front side of the ID card only. It uses guided capture, offline OCR, fixed-layout field extraction, and user correction as a fallback. The goal is not perfect automation under all conditions. The goal is a stable offline workflow that can extract the main fields with high probability under normal shooting conditions.

## Product Scope

### In Scope

- Android offline recognition for the front side of Chinese mainland second-generation resident ID cards
- Guided camera preview with a visible framing area
- Capture after alignment rather than continuous full-frame OCR
- Offline OCR using Google ML Kit on-device text recognition
- Structured extraction of these fields:
  - `name`
  - `gender`
  - `ethnicity`
  - `birthDate`
  - `address`
  - `idNumber`
- Confidence-based fallback behavior
- Manual review and correction before final confirmation
- Internal structure that can later support more document types

### Out of Scope for V1

- Back side recognition
- Multi-document support in the first release
- Cloud OCR
- Batch import
- Full real-time continuous OCR on the preview stream
- Guaranteed zero-edit recognition

## Success Criteria

The first version is successful if it meets these goals:

- Works completely offline on-device
- Guides the user into taking a usable photo
- Extracts the main front-side fields in normal conditions
- Produces especially strong results for `idNumber`, `name`, and `birthDate`
- Provides a fast manual correction step when OCR is incomplete or imperfect
- Fails clearly with retake guidance rather than silently returning low-quality structured data

## Recommended Approach

Three implementation approaches were considered:

1. Direct whole-image OCR and regex parsing
2. Guided capture, post-capture OCR, layout-aware parsing, and user review
3. Live preview analysis with continuous recognition

The recommended approach is option 2.

### Why Option 2

- It provides the best balance between implementation complexity, offline feasibility, user experience, and accuracy.
- It avoids the runtime and compatibility overhead of continuous preview OCR.
- It improves OCR quality by constraining the image to the card area before parsing.
- It leaves room for later enhancements without forcing a redesign.

## Technical Feasibility

This feature is feasible with Google ML Kit, but the correct OCR dependency choice matters.

For Chinese ID cards, the preferred ML Kit dependency is the Chinese offline recognizer rather than a generic Latin-focused text recognizer:

- `com.google.mlkit:text-recognition-chinese`

Reference documentation:

- ML Kit Text Recognition v2: <https://developers.google.com/ml-kit/vision/text-recognition/v2>
- Android integration guide: <https://developers.google.com/ml-kit/vision/text-recognition/v2/android>
- Chinese recognizer options: <https://developers.google.com/android/reference/com/google/mlkit/vision/text/chinese/ChineseTextRecognizerOptions>

The OCR engine alone is not sufficient for robust ID card extraction. Practical accuracy depends on capture guidance, document area cropping, field heuristics, and field validation.

## Architecture

The system should be split into small modules with clear responsibilities.

### 1. Capture Layer

Responsibilities:

- Camera permission handling
- CameraX preview
- Framing overlay for the front side of the ID card
- Capture button
- Light pre-capture guidance such as:
  - move closer
  - align card within frame
  - avoid glare

This layer is responsible for obtaining a usable image, not for parsing OCR fields.

### 2. Image Preprocessing Layer

Responsibilities:

- Normalize rotation from the captured image
- Crop the image based on the framing area
- Optionally apply lightweight correction for alignment and composition

This layer should remain lightweight. V1 should not depend on a heavy custom computer vision stack.

### 3. OCR Layer

Responsibilities:

- Build an ML Kit `InputImage`
- Run the offline Chinese recognizer
- Return full OCR structure including blocks, lines, elements, and bounding boxes

Bounding boxes are important because field extraction depends not only on recognized text, but also on relative location.

### 4. Field Parsing Layer

Responsibilities:

- Convert OCR text and bounding boxes into a structured front-side ID card model
- Detect label anchors such as:
  - `姓名`
  - `性别`
  - `民族`
  - `出生`
  - `住址`
  - `公民身份号码`
- Extract field values from likely adjacent regions
- Merge multi-line address content
- Tolerate common OCR character confusion

### 5. Validation Layer

Responsibilities:

- Validate ID number length and check digit
- Validate birth date shape and plausibility
- Rate recognition confidence
- Decide whether to:
  - return high-confidence structured data
  - return partial results for user correction
  - request a retake

### 6. Review Layer

Responsibilities:

- Show the structured recognition result
- Allow field-by-field editing
- Support final confirmation or retake

Manual correction is a deliberate design choice, not a workaround. It is a necessary quality-control step for V1.

## Data Flow

The end-to-end flow for V1 should be:

`camera preview -> alignment guidance -> capture -> crop/normalize -> ML Kit OCR -> field parsing -> validation -> review/edit -> confirm`

This flow keeps responsibilities separated and makes debugging easier. If output quality is poor, the team can determine whether the problem came from capture quality, cropping, OCR quality, or field parsing.

## Recognition Strategy

The recognition strategy should combine OCR with fixed-layout parsing rather than treating the card like arbitrary document text.

### Core Principle

Do not rely on whole-image raw text matching alone. Use:

- label anchors
- relative coordinates
- expected field shapes
- post-OCR validation

### Anchor-Driven Parsing

The most important anchors are:

- `姓名`
- `性别`
- `民族`
- `出生`
- `住址`
- `公民身份号码`
- any detected 18-character ID-number candidate

The strongest anchor is typically the ID number region because its format is highly constrained and can be validated.

### Field Extraction Rules

#### `idNumber`

Preferred strategy:

- Search for 18-character candidates
- Correct common OCR substitutions such as:
  - `O` to `0`
  - `I` to `1`
  - `B` to `8`
- Validate:
  - total length is 18
  - first 17 characters are numeric
  - birth date segment is parseable
  - final checksum character is correct

This is the highest-value field and should receive the strongest validation logic.

#### `name`

Preferred strategy:

- Use the `姓名` label as an anchor
- Search primarily to the right of that anchor
- Prefer a short Chinese string of plausible length, typically 2 to 4 characters

Do not simply choose the shortest Chinese token from the whole OCR output.

#### `gender` and `ethnicity`

Preferred strategy:

- Parse these together because they usually appear on the same line
- Use coordinate proximity to the `性别` and `民族` labels
- Validate ethnicity against a known legal ethnicity list when possible

#### `birthDate`

Preferred strategy:

- Use the `出生` label as an anchor
- Accept OCR fragmentation across year, month, and day
- Reconstruct a normalized date value rather than requiring a single clean date string

#### `address`

Preferred strategy:

- Use the `住址` label as a starting anchor
- Merge adjacent lines below or to the right in the expected address area
- Stop merging before the ID number region

Address is expected to be the weakest structured field in V1 and should be editable.

## Error Handling and Confidence Levels

V1 should use clear failure states instead of pretending weak OCR is acceptable.

### Pre-Capture Guidance

The preview page should try to reduce bad inputs by prompting for issues such as:

- card not aligned with the frame
- card too far away
- obvious glare
- insufficient lighting

V1 can keep this simple and heuristic-driven.

### Post-Capture Rejection

After capture and preprocessing, the app should request a retake if:

- the cropped card region is too small
- the image is too blurry
- the result does not resemble the front side of an ID card
- key anchors are completely missing

### OCR Result Confidence

Suggested confidence tiers:

- `high`
  - ID number validated successfully
  - major fields present
- `partial`
  - some major fields present
  - user review required
- `low`
  - sparse or inconsistent OCR
  - recommend retake

## User Experience Design

V1 should use a simple three-screen flow.

### 1. Capture Screen

Elements:

- camera preview
- card framing overlay
- short capture instructions
- capture button

Goal:

- help the user produce a good input image

### 2. Processing Screen

Elements:

- short loading state

Goal:

- cover preprocessing, OCR, parsing, and validation

### 3. Review Screen

Elements:

- editable structured fields:
  - `name`
  - `gender`
  - `ethnicity`
  - `birthDate`
  - `address`
  - `idNumber`
- confirm button
- retake button

Goal:

- allow fast correction without forcing the user to start over for minor OCR issues

## Module Plan

The app should be organized around these responsibilities, whether implemented as packages or small classes:

- `camera`
- `documentguide`
- `imagepreprocess`
- `ocr`
- `idcardparser`
- `idcardvalidator`
- `resultreview`

The exact packaging can follow the existing project style, but responsibilities should remain separate.

## Dependency Plan

All dependency versions must be added through `gradle/libs.versions.toml` rather than hardcoded in Gradle build scripts.

Planned dependency groups:

- ML Kit Chinese text recognition
- CameraX core
- CameraX camera2
- CameraX lifecycle
- CameraX view

The project already uses:

- AppCompat
- Material
- Activity KTX
- ConstraintLayout

These are sufficient for the V1 UI.

## Delivery Plan for MVP

### Phase 1: App Flow Skeleton

Build:

- camera permission flow
- preview screen
- capture action
- result screen navigation

Goal:

- validate the interaction loop before real OCR

### Phase 2: Offline OCR Integration

Build:

- ML Kit Chinese recognizer setup
- captured image to OCR pipeline
- raw OCR output inspection

Goal:

- confirm device-side offline OCR quality

### Phase 3: Structured Parsing

Build:

- anchor detection
- field extraction
- ID number checksum validation
- date reconstruction
- address line merge

Goal:

- produce a usable structured front-side result

### Phase 4: Reliability and UX Polish

Build:

- retake prompts
- low-confidence handling
- result editing
- better capture guidance

Goal:

- move from technical demo to usable workflow

## Risks

Main risks for V1:

- plastic glare obscuring key fields
- blur from hand movement
- strong perspective distortion
- incomplete address recognition
- OCR confusion in short fields with very little context

These risks are manageable if the app sets expectations correctly and includes review and retake flows.

## Final Recommendation

Proceed with a V1 focused on:

- Chinese mainland second-generation resident ID card front side
- guided capture
- post-capture offline OCR
- rule-based structured parsing
- strong validation for ID number
- user review and correction

This is a realistic, technically feasible, and implementation-friendly plan for the current Android project. It avoids over-engineering while still establishing a structure that can later support the back side of the ID card and other document types.
