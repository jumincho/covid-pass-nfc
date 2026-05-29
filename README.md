<div align="center">

# CV-PASS

**NFC-based contactless entry log app for the COVID-19 era**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Java-007396?logo=java&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-28-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Award](https://img.shields.io/badge/2021_JBNU_silver_award-%F0%9F%A5%88-silver)

**한국어** · [English](#english) · [中文](./README.zh-CN.md)

</div>

---

## 개요

> NFC 태그로 출입명부를 1초 만에 등록하는 코로나-19 시대의 안드로이드 앱

코로나-19 팬데믹 기간 동안 가게마다 종이 명부와 QR 체크인이 혼재하면서
**개인정보 노출**과 **느린 인식 속도** 문제가 반복적으로 지적되었습니다.
CV-PASS는 QR 대신 NFC 태그를 사용해 사용자가 단말기를 한 번 갖다 대는 것만으로
출입명부 등록을 완료하도록 만든 안드로이드 앱입니다.
역학조사관용 모드도 함께 제공하여 특정 매장의 출입 기록을 즉시 조회할 수 있습니다.

본 프로젝트는 **2021년 전북대학교 컴퓨터공학부 작품경진대회에서 은상**을 수상했습니다.

## 주요 기능

- **NFC 출입명부 등록** — NFC 태그를 단말에 접촉하면 `NfcEntryActivity`가 사용자 정보를 Firestore에 즉시 기록
- **백신 접종 인증서 검증** — Google Cloud Vision API로 카드를 분석해 진위 여부를 판단 (`VaccineCardCheckActivity`)
- **사업자 정보 진위 확인** — odcloud 공공 API 사업자 조회 결과를 파싱해 검증 (`BusinessVerifyActivity`)
- **역학조사관 모드** — 매장별 출입 기록을 시간 순으로 조회 (`HistoryActivity`)
- **최초 1회 가입** — 첫 실행 시 `ProfileSetupActivity`로 사용자 정보를 단말 내부 저장소(`UserDate.dat`)에 캐싱

## 화면 구성

| 화면 | 설명 |
|---|---|
| `MainActivity` | 스플래시. 저장된 사용자 정보 유무에 따라 가입/메인 분기 |
| `ProfileSetupActivity` | 최초 사용자 등록 |
| `NfcEntryActivity` | NFC 태그 인식 및 출입 기록 |
| `VaccineCardCheckActivity` | 백신 접종 인증서 이미지 검증 (Cloud Vision) |
| `BusinessVerifyActivity` | 사업자 번호 진위 검증 (Open API) |
| `HistoryActivity` | 출입 기록 조회 |

## 기술 스택

- **언어** : Java
- **플랫폼** : Android (minSdk 28 / targetSdk 31)
- **빌드** : Gradle wrapper 7.0.2 · Android Gradle Plugin 7.0.3
- **DB** : Cloud Firestore
- **이미지 분석** : Google Cloud Vision API
- **웹 파싱** : Jsoup 1.14.3
- **NFC** : `android.nfc.NfcAdapter` (NDEF)

## 프로젝트 구조

```
covid-pass-nfc/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jumincho/cvpass/
│       │   ├── MainActivity.java                # 스플래시 + 분기
│       │   ├── ProfileSetupActivity.java        # 최초 사용자 등록
│       │   ├── NfcEntryActivity.java            # NFC 태그 처리
│       │   ├── VaccineCardCheckActivity.java    # 백신 인증서 검증
│       │   ├── BusinessVerifyActivity.java      # 사업자 정보 검증
│       │   ├── HistoryActivity.java             # 출입 기록 조회
│       │   ├── BusinessLookupParser.java        # 사업자 조회 API 응답 파싱
│       │   ├── HttpClient.java                  # POST JSON 클라이언트
│       │   ├── PackageManagerUtils.java
│       │   └── PermissionUtils.java
│       └── res/
│           ├── layout/                     # 액티비티 레이아웃
│           ├── drawable/                   # 벡터 / 비트맵 자원
│           └── values/                     # strings, colors, themes
├── docs/
│   └── presentation.pptx                   # 작품경진대회 발표 자료
├── build.gradle
└── settings.gradle
```

> 이 저장소에서는 저작권 및 보안상의 이유로 일부 이미지 자원, 폰트,
> Firebase `google-services.json`, Cloud Vision API 키가 제거되어 있습니다.
> 직접 빌드하려면 자신의 Firebase 프로젝트와 API 키를 채워 넣어야 합니다.

## 시크릿 처리

빌드 시스템은 `local.properties`에서 다음 키를 읽어 `BuildConfig`로 주입하며,
키 문자열은 소스에 포함되지 않습니다.

| `local.properties` 키 | `BuildConfig` 필드 | 사용처 |
| --- | --- | --- |
| `VISION_API_KEY` | `BuildConfig.API_KEY` | Cloud Vision OCR |
| `BUSINESS_API_KEY` | `BuildConfig.BUSINESS_API_KEY` | odcloud 사업자번호 진위 검증 |
| `INSPECTOR_CODE` | `BuildConfig.INSPECTOR_CODE` | 역학조사관 진입 코드 |

`local.properties`와 `app/google-services.json`은 `.gitignore`에 포함되어 있습니다.

## 빌드 방법

1. Android Studio (Arctic Fox 이상) 로 프로젝트 열기
2. `app/google-services.json`을 본인 Firebase 프로젝트 콘솔에서 다운로드 받아 배치
   (현재 파일은 placeholder)
3. 프로젝트 루트의 `local.properties`에 다음 줄들을 추가
   ```properties
   VISION_API_KEY=여기에_본인의_Cloud_Vision_API_KEY
   BUSINESS_API_KEY=여기에_본인의_odcloud_serviceKey
   INSPECTOR_CODE=여기에_본인이_정한_조사관_코드
   ```
4. Gradle Sync 후 `Run 'app'`

```bash
./gradlew assembleDebug
```

## 발표 자료

- 시연 영상 : [YouTube — 2021 전북대 컴공 작품경진대회](https://www.youtube.com/watch?v=LHE4dr8aTKQ&list=PLFAjt9goCKzyHfSKoV9AnDuxl1U9w1mLL&index=13)
- 발표 슬라이드 : [`docs/presentation.pptx`](./docs/presentation.pptx)

## 스크린샷

<table align="center">
<tr>
<td><img src="https://user-images.githubusercontent.com/93726941/176481050-1c6acb2c-4d15-4c1f-a039-8b3b74251569.png" width="280"/></td>
<td><img src="https://user-images.githubusercontent.com/93726941/176481320-b1f82186-2de0-43a9-8df7-b73973614fa4.png" width="280"/></td>
<td><img src="https://user-images.githubusercontent.com/93726941/176481365-d3fd1e10-963b-418b-be95-9d7654d9dda3.png" width="280"/></td>
</tr>
</table>

## 수상

- **2021년 전북대학교 컴퓨터공학부 작품경진대회 은상** (2021. 11. 26.)

## 팀원

| 소속 | 역할 | 이름 | 담당 |
|---|---|---|---|
| 전북대 | 팀장 | 이정환 | 개발 / 디자인 |
| 전북대 | 팀원 | 김연호 | 개발 / 디자인 |
| 전북대 | 팀원 | 정재영 | 개발 / 디자인 |
| 전북대 | 팀원 | 조주민 | 발표 / 디자인 |

## 라이선스

본 저장소의 소스 코드는 [MIT License](./LICENSE)를 따릅니다.
다만 발표 자료(`docs/presentation.pptx`)와 스크린샷의 저작권은 팀원 공동 소유입니다.

---

<a name="english"></a>

## English

> A 1-second NFC entry-log app, built for the COVID-19 era.

During the COVID-19 pandemic, every storefront in Korea juggled paper sign-in
sheets and QR check-ins, repeatedly raising concerns about **personal data
exposure** and **slow scan times**. CV-PASS replaces QR with NFC: users tap
their phone to the tag once, and the entry is logged. An epidemiologist mode
also lets contact tracers query a venue's entry history on demand.

This project received the **silver award at the 2021 JBNU CS Student Project Competition**.

### Features

- **NFC entry logging** — tapping an NFC tag triggers `NfcEntryActivity`, which writes the user record to Firestore.
- **Vaccine card verification** — `VaccineCardCheckActivity` uses Google Cloud Vision API to OCR and validate vaccination cards.
- **Business legitimacy check** — `BusinessVerifyActivity` validates business registrations via the odcloud public API.
- **Epidemiologist mode** — `HistoryActivity` lists a venue's entry log chronologically.
- **One-time sign-up** — `ProfileSetupActivity` caches the profile in device storage (`UserDate.dat`) on first launch.

### Screens

| Activity | Description |
|---|---|
| `MainActivity` | Splash. Branches to sign-up or main based on cached profile. |
| `ProfileSetupActivity` | First-time user registration. |
| `NfcEntryActivity` | NFC tag scan + entry log write. |
| `VaccineCardCheckActivity` | Vaccine card image verification (Cloud Vision). |
| `BusinessVerifyActivity` | Business number validation (Open API). |
| `HistoryActivity` | Entry log lookup. |

### Tech Stack

- **Language**: Java
- **Platform**: Android (minSdk 28 / targetSdk 31)
- **Build**: Gradle wrapper 7.0.2 · Android Gradle Plugin 7.0.3
- **DB**: Cloud Firestore
- **Image analysis**: Google Cloud Vision API
- **Web parsing**: Jsoup 1.14.3
- **NFC**: `android.nfc.NfcAdapter` (NDEF)

> For copyright and security reasons, this repository has some image assets,
> fonts, the Firebase `google-services.json`, and the Cloud Vision API key
> removed. To build it yourself, plug in your own Firebase project and API
> keys.

### Secrets handling

The build system reads keys from `local.properties` and injects them via
`BuildConfig`; no secrets are present in source.

| `local.properties` key | `BuildConfig` field | Use site |
| --- | --- | --- |
| `VISION_API_KEY` | `BuildConfig.API_KEY` | Cloud Vision OCR |
| `BUSINESS_API_KEY` | `BuildConfig.BUSINESS_API_KEY` | Business-number lookup |
| `INSPECTOR_CODE` | `BuildConfig.INSPECTOR_CODE` | Epidemiologist mode gate |

`local.properties` and `app/google-services.json` are gitignored.

### Build

1. Open the project in Android Studio (Arctic Fox or later).
2. Drop your own `app/google-services.json` from the Firebase console
   (the committed file is a placeholder).
3. Add the following lines to `local.properties` at the repo root:
   ```properties
   VISION_API_KEY=your_cloud_vision_api_key
   BUSINESS_API_KEY=your_odcloud_service_key
   INSPECTOR_CODE=your_inspector_code
   ```
4. Gradle Sync, then `Run 'app'`.

```bash
./gradlew assembleDebug
```

### Materials

- Demo video: [YouTube — 2021 JBNU CS Student Project Competition](https://www.youtube.com/watch?v=LHE4dr8aTKQ&list=PLFAjt9goCKzyHfSKoV9AnDuxl1U9w1mLL&index=13)
- Slides: [`docs/presentation.pptx`](./docs/presentation.pptx)

### Screenshots

<table align="center">
<tr>
<td><img src="https://user-images.githubusercontent.com/93726941/176481050-1c6acb2c-4d15-4c1f-a039-8b3b74251569.png" width="280"/></td>
<td><img src="https://user-images.githubusercontent.com/93726941/176481320-b1f82186-2de0-43a9-8df7-b73973614fa4.png" width="280"/></td>
<td><img src="https://user-images.githubusercontent.com/93726941/176481365-d3fd1e10-963b-418b-be95-9d7654d9dda3.png" width="280"/></td>
</tr>
</table>

### Award

- **Silver award, 2021 JBNU CS Student Project Competition** (Nov 26, 2021)

### Team

| Affiliation | Role | Name | Responsibility |
|---|---|---|---|
| JBNU | Lead | Lee Jeonghwan | Development / Design |
| JBNU | Member | Kim Yeonho | Development / Design |
| JBNU | Member | Jeong Jaeyoung | Development / Design |
| JBNU | Member | Cho Jumin | Presentation / Design |

### License

Source code is released under [MIT License](./LICENSE). The presentation
deck (`docs/presentation.pptx`) and screenshots remain jointly owned by the
team.
