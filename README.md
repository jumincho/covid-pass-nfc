# CV-PASS

> NFC 태그로 출입명부를 1초 만에 등록하는 코로나-19 시대의 안드로이드 앱

코로나-19 팬데믹 기간 동안 가게마다 종이 명부와 QR 체크인이 혼재하면서
**개인정보 노출**과 **느린 인식 속도** 문제가 반복적으로 지적되었습니다.
CV-PASS는 QR 대신 NFC 태그를 사용해 사용자가 단말기를 한 번 갖다 대는 것만으로
출입명부 등록을 완료하도록 만든 안드로이드 앱입니다.
역학조사관용 모드도 함께 제공하여 특정 매장의 출입 기록을 즉시 조회할 수 있습니다.

본 프로젝트는 **2021년 전북대학교 컴퓨터공학부 작품경진대회에서 은상**을 수상했습니다.

---

## 주요 기능

- **NFC 출입명부 등록** : NFC 태그를 단말에 접촉하면 `Nfc_pass_check` 액티비티가 사용자 정보를 Firestore에 즉시 기록
- **백신 접종 인증서 검증** : Google Cloud Vision API로 카드를 분석해 진위 여부를 판단 (`Check_user_doc`)
- **사업자 정보 진위 확인** : Jsoup으로 국세청 사업자 조회 결과를 파싱해 검증 (`Check_shop_doc`)
- **역학조사관 모드** : 매장별 출입 기록을 시간 순으로 조회 (`history`)
- **최초 1회 가입** : 첫 실행 시 `Creat_user`로 사용자 정보를 단말 내부 저장소(`UserDate.dat`)에 캐싱

## 화면 구성

| 화면 | 설명 |
|---|---|
| `MainActivity` | 스플래시. 저장된 사용자 정보 유무에 따라 가입/메인 분기 |
| `Creat_user` | 최초 사용자 등록 |
| `Nfc_pass_check` | NFC 태그 인식 및 출입 기록 |
| `Check_user_doc` | 백신 접종 인증서 이미지 검증 (Cloud Vision) |
| `Check_shop_doc` | 사업자 번호 진위 검증 (Open API) |
| `history` | 출입 기록 조회 |

## 기술 스택

- **언어** : Java
- **플랫폼** : Android (minSdk 28 / targetSdk 31)
- **빌드** : Gradle 7.0.3
- **DB** : Cloud Firestore
- **이미지 분석** : Google Cloud Vision API
- **웹 파싱** : Jsoup 1.14.3
- **NFC** : `android.nfc.NfcAdapter` (NDEF)

## 프로젝트 구조

```
cv_pass/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/nfcpass/
│       │   ├── MainActivity.java           # 스플래시 + 분기
│       │   ├── Creat_user.java             # 최초 사용자 등록
│       │   ├── Nfc_pass_check.java         # NFC 태그 처리
│       │   ├── Check_user_doc.java         # 백신 인증서 검증
│       │   ├── Check_shop_doc.java         # 사업자 정보 검증
│       │   ├── history.java                # 출입 기록 조회
│       │   ├── DataParser.java             # Vision API 응답 파싱
│       │   ├── RequestHttpURLConnection.java
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

## 보안 주의사항

> [!IMPORTANT]
> **이전 머지된 커밋의 git history 에 다음 4개 시크릿이 평문으로 노출되었습니다.**
> 모두 즉시 발급 기관에서 회전(rotate)·재발급해 주세요. 본 PR 의 소스 코드에서는
> 모두 placeholder / `BuildConfig` 로 가렸지만, 과거 커밋에는 그대로 남아 있습니다.
>
> | 시크릿 | 노출된 값 (prefix) | 발급 기관 |
> | --- | --- | --- |
> | Cloud Vision API key | `AIzaSyAXpv-...` | GCP 콘솔 → API 및 서비스 → 사용자 인증 정보 |
> | Firebase API key | `AIzaSyAgyb...` | Firebase 콘솔 → 프로젝트 설정 |
> | odcloud 사업자조회 serviceKey | `EEA0ZjjF...` | 공공데이터포털 |
> | 조사관 코드 (클라이언트 비밀번호) | `112255` | 운영 규칙 — 더 강한 코드로 |

현재 빌드 시스템은 `local.properties` 에서 다음 키들을 읽어 `BuildConfig` 로 주입합니다.
키 문자열은 더 이상 소스에 포함되지 않습니다.

| local.properties 키 | BuildConfig 필드 | 사용처 |
| --- | --- | --- |
| `VISION_API_KEY` | `BuildConfig.API_KEY` | Cloud Vision OCR |
| `BUSINESS_API_KEY` | `BuildConfig.BUSINESS_API_KEY` | odcloud 사업자번호 진위 검증 |
| `INSPECTOR_CODE` | `BuildConfig.INSPECTOR_CODE` | 역학조사관 진입 코드 |

`local.properties` 와 `app/google-services.json` 은 `.gitignore` 권장 — 본인의 키를
다른 저장소나 PR 에 다시 커밋하지 마세요.

## 빌드 방법

1. Android Studio (Arctic Fox 이상) 로 프로젝트 열기
2. `app/google-services.json` 을 본인 Firebase 프로젝트 콘솔에서 다운로드 받아 배치
   (현재 파일은 placeholder)
3. 프로젝트 루트의 `local.properties` 에 다음 줄들을 추가
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

| 학교 | 역할 | 이름 | 담당 |
|---|---|---|---|
| 전북대 | 팀장 | 이정환 | 개발 / 디자인 |
| 전북대 | 팀원 | 김연호 | 개발 / 디자인 |
| 전북대 | 팀원 | 정재영 | 개발 / 디자인 |
| 전북대 | 팀원 | 조주민 | 발표 / 디자인 |
