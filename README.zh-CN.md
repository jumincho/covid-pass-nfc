<div align="center">

# CV-PASS

**新冠疫情时代基于 NFC 的非接触出入登记 Android 应用**

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/language-Java-007396?logo=java&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-28-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Award](https://img.shields.io/badge/2021_JBNU_silver_award-%F0%9F%A5%88-silver)

[한국어](./README.md) · [English](./README.md#english) · **中文**

</div>

---

## 概览

> 用一秒完成的 NFC 出入名单登记 Android 应用 —— 面向新冠疫情时代

在新冠疫情期间,韩国的店铺普遍混用纸质签到簿和二维码登记,反复出现
**个人信息泄露**与**扫码速度慢**的问题。CV-PASS 用 NFC 替代二维码:
用户只需将手机轻触一次 NFC 标签,登记即完成。同时还提供流行病调查员模式,
让接触者追踪者可以按需查询某门店的出入记录。

本项目获得 **2021 年全北大学计算机工程系作品竞赛银奖**。

## 主要功能

- **NFC 出入登记** —— 手机触碰 NFC 标签时,`NfcEntryActivity` 会立即把用户记录写入 Firestore。
- **疫苗接种证书验证** —— `VaccineCardCheckActivity` 使用 Google Cloud Vision API 对接种凭证图片进行 OCR 并验证。
- **企业资质验证** —— `BusinessVerifyActivity` 通过 odcloud 公共 API 验证营业执照的真伪。
- **流行病调查员模式** —— `HistoryActivity` 按时间顺序列出某门店的出入记录。
- **一次性注册** —— 首次启动时,`ProfileSetupActivity` 会把用户资料缓存到设备存储 (`UserDate.dat`)。

## 屏幕

| Activity | 说明 |
|---|---|
| `MainActivity` | 启动画面;根据是否已缓存用户资料分支到注册或主界面。 |
| `ProfileSetupActivity` | 首次用户注册。 |
| `NfcEntryActivity` | NFC 标签扫描 + 出入记录写入。 |
| `VaccineCardCheckActivity` | 疫苗证书图片验证 (Cloud Vision)。 |
| `BusinessVerifyActivity` | 营业执照号码验证 (公共 API)。 |
| `HistoryActivity` | 出入记录查询。 |

## 技术栈

- **语言**: Java
- **平台**: Android (minSdk 28 / targetSdk 31)
- **构建**: Gradle 7.0.3
- **数据库**: Cloud Firestore
- **图像分析**: Google Cloud Vision API
- **网页解析**: Jsoup 1.14.3
- **NFC**: `android.nfc.NfcAdapter` (NDEF)

## 密钥管理

构建系统从 `local.properties` 读取密钥,通过 `BuildConfig` 注入,源代码中不包含任何密钥。

| `local.properties` 键 | `BuildConfig` 字段 | 使用位置 |
| --- | --- | --- |
| `VISION_API_KEY` | `BuildConfig.API_KEY` | Cloud Vision OCR |
| `BUSINESS_API_KEY` | `BuildConfig.BUSINESS_API_KEY` | 工商登记号码查询 |
| `INSPECTOR_CODE` | `BuildConfig.INSPECTOR_CODE` | 流行病调查员模式入口码 |

`local.properties` 和 `app/google-services.json` 已加入 `.gitignore`。

## 项目结构

```
covid-pass-nfc/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jumincho/cvpass/
│       │   ├── MainActivity.java                # 启动 + 分支
│       │   ├── ProfileSetupActivity.java        # 首次用户注册
│       │   ├── NfcEntryActivity.java            # NFC 标签处理
│       │   ├── VaccineCardCheckActivity.java    # 疫苗证书验证
│       │   ├── BusinessVerifyActivity.java      # 营业执照号验证
│       │   ├── HistoryActivity.java             # 出入记录查询
│       │   ├── BusinessLookupParser.java        # 工商查询 JSON 响应解析
│       │   ├── HttpClient.java                  # POST JSON 客户端
│       │   ├── PackageManagerUtils.java
│       │   └── PermissionUtils.java
│       └── res/                            # 布局 / 资源 / 字符串等
├── docs/
│   └── presentation.pptx                   # 作品竞赛演示文档
├── build.gradle
└── settings.gradle
```

> 出于版权和安全考虑,本仓库移除了部分图像资源、字体、Firebase `google-services.json`
> 及 Cloud Vision API 密钥。若要自行构建,需要填入您自己的 Firebase 项目与 API 密钥。

## 构建方法

1. 用 Android Studio (Arctic Fox 及以上) 打开项目。
2. 从您自己的 Firebase 控制台下载 `app/google-services.json` 并放到该目录
   (当前仓库中的文件是占位符)。
3. 在仓库根目录的 `local.properties` 中加入下列内容:
   ```properties
   VISION_API_KEY=您的_Cloud_Vision_API_KEY
   BUSINESS_API_KEY=您的_odcloud_serviceKey
   INSPECTOR_CODE=自定义_调查员_入口码
   ```
4. Gradle Sync 之后 `Run 'app'`。

```bash
./gradlew assembleDebug
```

## 演示资料

- 演示视频: [YouTube —— 2021 全北大学计算机系作品竞赛](https://www.youtube.com/watch?v=LHE4dr8aTKQ&list=PLFAjt9goCKzyHfSKoV9AnDuxl1U9w1mLL&index=13)
- 演讲幻灯片: [`docs/presentation.pptx`](./docs/presentation.pptx)

## 获奖

- **2021 年全北大学计算机工程系作品竞赛银奖** (2021.11.26)

## 团队

| 所属 | 角色 | 姓名 | 负责 |
|---|---|---|---|
| JBNU | 队长 | 李正焕 | 开发 / 设计 |
| JBNU | 队员 | 金年浩 | 开发 / 设计 |
| JBNU | 队员 | 郑在荣 | 开发 / 设计 |
| JBNU | 队员 | 赵主民 | 演讲 / 设计 |

## 许可证

本仓库的源代码遵循 [MIT License](./LICENSE)。
但演示文档 (`docs/presentation.pptx`) 和屏幕截图的版权由全体队员共同持有。
