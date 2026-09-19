<div align="center">

# CV-PASS

**NFC-based contactless entry-log Android app for the COVID-19 era**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Java-007396?logo=java&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-28-blue)
[![Verify](https://github.com/jumincho/covid-pass-nfc/actions/workflows/verify.yml/badge.svg)](https://github.com/jumincho/covid-pass-nfc/actions/workflows/verify.yml)
![License](https://img.shields.io/badge/license-MIT-green)
![Award](https://img.shields.io/badge/2021_JBNU_silver_award-%F0%9F%A5%88-silver)

</div>

---

## Overview

During the COVID-19 pandemic, every storefront in Korea juggled paper sign-in
sheets and QR check-ins, repeatedly raising concerns about **personal data
exposure** and **slow scan times**. CV-PASS replaces QR with NFC: users tap
their phone to the tag once, and the entry is logged. An epidemiologist mode
also lets contact tracers query a venue's entry history on demand.

This project received the **silver award at the 2021 JBNU CS Student Project Competition**.

## Features

- **NFC entry logging** — tapping an NFC tag triggers `NfcEntryActivity`, which writes the user record to Firestore.
- **Vaccine card verification** — `VaccineCardCheckActivity` uses the Google Cloud Vision API to OCR and validate vaccination cards.
- **Business legitimacy check** — `BusinessVerifyActivity` validates business registrations via the odcloud public API.
- **Epidemiologist mode** — `HistoryActivity` lists a venue's entry log chronologically.
- **One-time sign-up** — `ProfileSetupActivity` caches the profile in device storage (`UserDate.dat`) on first launch.

## Screens

| Activity | Description |
|---|---|
| `MainActivity` | Splash. Branches to sign-up or main based on the cached profile. |
| `ProfileSetupActivity` | First-time user registration. |
| `NfcEntryActivity` | NFC tag scan + entry log write. |
| `VaccineCardCheckActivity` | Vaccine card image verification (Cloud Vision). |
| `BusinessVerifyActivity` | Business number validation (Open API). |
| `HistoryActivity` | Entry log lookup. |

## Tech stack

- **Language**: Java
- **Platform**: Android (minSdk 28 / targetSdk 31)
- **Build**: Gradle wrapper 7.0.2 · Android Gradle Plugin 7.0.3
- **DB**: Cloud Firestore
- **Image analysis**: Google Cloud Vision API
- **NFC**: `android.nfc.NfcAdapter` (NDEF)

## Project layout

```
covid-pass-nfc/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jumincho/cvpass/
│       │   ├── MainActivity.java                # splash + routing
│       │   ├── ProfileSetupActivity.java        # first-time registration
│       │   ├── NfcEntryActivity.java            # NFC tag handling
│       │   ├── VaccineCardCheckActivity.java    # vaccine card verification
│       │   ├── BusinessVerifyActivity.java      # business registration check
│       │   ├── HistoryActivity.java             # entry log lookup
│       │   ├── BusinessLookupParser.java        # parses the business lookup API response
│       │   ├── HttpClient.java                  # POST JSON client
│       │   ├── PackageManagerUtils.java
│       │   └── PermissionUtils.java
│       └── res/
│           ├── layout/                     # activity layouts
│           ├── drawable/                   # vector / bitmap assets
│           └── values/                     # strings, colors, themes
├── docs/
│   └── presentation.pptx                   # competition slides
├── build.gradle
└── settings.gradle
```

> For copyright reasons some image assets and fonts are removed, and no secret
> values (e.g. the Cloud Vision API key) are included. The Firebase
> `google-services.json` is committed as a **placeholder** so the project builds
> out of the box — swap in your own Firebase file and API keys to actually run it.

## Secrets handling

The build system reads keys from `local.properties` and injects them via
`BuildConfig`; no secrets are present in source.

| `local.properties` key | `BuildConfig` field | Use site |
| --- | --- | --- |
| `VISION_API_KEY` | `BuildConfig.API_KEY` | Cloud Vision OCR |
| `BUSINESS_API_KEY` | `BuildConfig.BUSINESS_API_KEY` | Business-number lookup |
| `INSPECTOR_CODE` | `BuildConfig.INSPECTOR_CODE` | Epidemiologist mode gate |

`local.properties` is gitignored; `app/google-services.json` is committed as a
build-only placeholder (replace it with your own).

## Build

1. Open the project in Android Studio (Arctic Fox or later).
2. Drop your own `app/google-services.json` from the Firebase console
   (the committed file is a placeholder).
3. Add the following lines to `local.properties` at the repo root:

   ```properties
   VISION_API_KEY=your_cloud_vision_api_key
   BUSINESS_API_KEY=your_odcloud_service_key
   INSPECTOR_CODE=your_inspector_code
   ```

4. Gradle Sync, then `Run 'app'`, or build from the command line:

   ```bash
   ./gradlew assembleDebug
   ```

## Materials

- Demo video: [YouTube — 2021 JBNU CS Student Project Competition](https://www.youtube.com/watch?v=LHE4dr8aTKQ&list=PLFAjt9goCKzyHfSKoV9AnDuxl1U9w1mLL&index=13)
- Slides: [`docs/presentation.pptx`](./docs/presentation.pptx)

## Screenshots

<table align="center">
<tr>
<td><img src="https://user-images.githubusercontent.com/93726941/176481050-1c6acb2c-4d15-4c1f-a039-8b3b74251569.png" width="280"/></td>
<td><img src="https://user-images.githubusercontent.com/93726941/176481320-b1f82186-2de0-43a9-8df7-b73973614fa4.png" width="280"/></td>
<td><img src="https://user-images.githubusercontent.com/93726941/176481365-d3fd1e10-963b-418b-be95-9d7654d9dda3.png" width="280"/></td>
</tr>
</table>

## Award

- **Silver award, 2021 JBNU CS Student Project Competition** (Nov 26, 2021)

## Team

| Affiliation | Role | Name | Responsibility |
|---|---|---|---|
| JBNU | Lead | Lee Jeonghwan | Development / Design |
| JBNU | Member | Kim Yeonho | Development / Design |
| JBNU | Member | Jeong Jaeyoung | Development / Design |
| JBNU | Member | Cho Jumin | Presentation / Design |

## License

Source code is released under the [MIT License](./LICENSE). The presentation
deck (`docs/presentation.pptx`) and screenshots remain jointly owned by the
team.
