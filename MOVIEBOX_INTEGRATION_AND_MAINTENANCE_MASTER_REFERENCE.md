# الدليل الفني الشامل لدمج وصيانة مكتبة MovieBox في تطبيق BeoutTV
## التوثيق المعماري، شجرة الملفات، وهندسة الـ API وخدمات الترجمة

---

## 📌 الفهرس
1. [مصدر الاستخراج والبيانات الأصلية](#1-مصدر-الاستخراج-والبيانات-الأصلية)
2. [الهندسة المعمارية لـ MovieBox والربط البرمجي](#2-الهندسة-المعمارية-لـ-moviebox-والربط-البرمجي)
3. [شجرة الملفات الكاملة للمكونات المضافة والمعدلة](#3-شجرة-الملفات-الكاملة-للمكونات-المضافة-والمعدلة)
4. [تفاصيل المسارات والـ Endpoints والترويسات](#4-تفاصيل-المسارات-والـ-endpoints-والترويسات)
5. [محرك الترجمات المتقدم وحل مشكلة الأفلام القديمة (Day of the Dead)](#5-محرك-الترجمات-المتقدم-وحل-مشكلة-الأفلام-القديمة-day-of-the-dead)
6. [إصلاحات تجربة المشغل والكبسولات وأزرار التحميل](#6-إصلاحات-تجربة-المشغل-والكبسولات-وأزرار-التحميل)
7. [دليل الصيانة الدورية واستكشاف الأخطاء (Maintenance & Troubleshooting)](#7-دليل-الصيانة-الدورية-واستكشاف-الأخطاء-maintenance--troubleshooting)

---

## 1. مصدر الاستخراج والبيانات الأصلية

- **اسم التطبيق الأصلي:** MovieBox
- **اسم الحزمة (Package Name):** `com.community.moviebox` (بالتكامل مع `com.community.oneroom`)
- **ملف الحزمة المصدر:** `MovieBox_4.0.02.0831.03.apk` (الحجم: ~70 ميجابايت)
- **الجهة المطورة:** منظومة Transsion Holdings (الهواتف الذكية Tecno / Infinix / itel) ومنصة OneRoom / Shalltry.
- **تقنية الحماية:** مغلف SecShell (Bangcle v4/v5 Packer) - تم تحليل ملف الـ DEX واستخراج طبقة شبكة الخوادم والـ BFF Endpoints.

---

## 2. الهندسة المعمارية لـ MovieBox والربط البرمجي

أثبتت الهندسة العكسية وجود بوابتين رئيسيتين:
1. **بوابة التطبيق الأصلي (Native Gateway):** `https://i-api.aoneroom.com`  
   تتطلب توقيع RSA وتشفير مخصص عبر `x-tr-signature` و `X-Gslb-Sign`.
2. **بوابة الويب والتطبيقات المصغرة (H5 MiniApp Gateway):** `https://h5.aoneroom.com/wefeed-h5-bff/mini/`  
   تعتمد ترويسات قياسية خفيفة (`x-tr-devtype: h5`, `x-tr-region: CN`, `x-md-global-color: lane4`) دون الحاجة لتوليد مفاتيح RSA معقدة، وتوفر سرعة فائقة في استرداد الكتالوج، التصنيفات، تفاصيل الأعمال الفنية، وسيرفرات البث بجودات متعددة (1080p, 720p, 480p).

### قاعدة بيانات MovieBox المعتمدة:
- **الأفلام (Movies):** `channelId = 1`, `subjectType = 1`
- **المسلسلات (Series):** `channelId = 2`, `subjectType = 2`
- **الأنمي (Anime):** `genre = Anime` مدمج مع تصنيفات الأفلام والمسلسلات.
- **حد الصفحة الأقصى (Pagination limit):** إلزامي بحد أقصى `20` عنصر لكل طلب (`perPage <= 20`).

---

## 3. شجرة الملفات الكاملة للمكونات المضافة والمعدلة

```
app/src/main/java/com/beouttv/app/
├── data/
│   ├── api/
│   │   ├── CoreApiClient.kt                      [معدل: محرك ترجمات OpenSubtitles v3 وسيرفرات البث الاحتياطية]
│   │   └── moviebox/                             [جديد بالكامل: مكتبة MovieBox]
│   │       ├── MovieBoxApiClient.kt              [عميل شبكة OkHttp لاستدعاء H5 BFF ومسارات MovieBox]
│   │       ├── MovieBoxModels.kt                 [نماذج بيانات استجابة الـ JSON لكافة الأنماط]
│   │       └── MovieBoxMapper.kt                 [تحويل كائنات MovieBox لكائنات Movie و Series الخاصة بـ BeoutTV]
│   ├── imdb/
│   │   └── ImdbMetadataEngine.kt                 [استخراج وتوليد معرفات IMDb بدقة لربط الترجمات التلقائية]
│   └── repository/
│       ├── RemoteMediaRepository.kt              [معدل: دمج نتائج MovieBox مع مكتبة BeoutTV والبحث الشامل]
│       └── MovieBoxDownloadManager.kt            [جديد: إدارة تحميل ملفات MovieBox المباشرة عبر DownloadManager]
└── ui/
    ├── components/
    │   └── AdsterraSocialBar.kt                  [معدل: كبسولة صلبة بنمط وهوية BeoutTV دون شفافية زجاجية]
    ├── navigation/
    │   └── BeoutNavGraph.kt                      [معدل: مسارات الكتالوج الشامل، الأنمي، والأفلام/المسلسلات]
    └── screens/
        ├── home/
        │   └── HomeScreen.kt                     [معدل: كبسولات وأقسام MovieBox بنمط BeoutColors.ElevatedSurface]
        ├── movies/
        │   ├── MovieBoxCatalogScreen.kt          [جديد: شاشة الكتالوج الشامل للفلترة والتنقل السلس]
        │   └── MovieDetailsScreen.kt             [معدل: نقل زر التحميل للأعلى بجوار المشاركة وحذف الزر السفلي]
        ├── series/
        │   └── SeriesScreens.kt                  [معدل: زر التحميل العلوي وترقية التوافق ومسارات الحلقات]
        └── player/
            └── PlayerScreen.kt                   [معدل: عزل المشغل المدمج عن طبقة تحكم ExoPlayer الخارجية لمنع التداخل]
```

---

## 4. تفاصيل المسارات والـ Endpoints والترويسات

### البوابة الأساسية: `https://h5.aoneroom.com`

| الوظيفة | نوع الطلب | المسار (Path) | المعاملات الأساسية |
|---------|-----------|----------------|-------------------|
| **الأعمال الرائجة (Trending)** | `POST` | `/wefeed-h5-bff/mini/trending` | `page=1&perPage=20` |
| **قوائم الأفلام / المسلسلات** | `POST` | `/wefeed-h5-bff/mini/subject-list` | `channelId=1&subjectType=1&page=1&perPage=20&genre=Action` |
| **تفاصيل العمل ومصادر البث** | `GET` | `/wefeed-h5-bff/mini/subject_detail` | `subjectId=12345` |
| **محرك البحث المباشر** | `GET` | `/wefeed-h5-bff/mini/search` | `keyword=Spider-Man` |

### الترويسات المطلوبة (Headers):
```http
User-Agent: Mozilla/5.0 (Linux; Android 15; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36
x-tr-devtype: h5
x-tr-region: CN
x-md-global-color: lane4
Accept: application/json, text/plain, */*
```

---

## 5. محرك الترجمات المتقدم وحل مشكلة الأفلام القديمة (Day of the Dead)

### المشكلة السابقة:
- كانت الاستعلامات تتوجه إلى نطاق `rest.opensubtitles.org` الذي تم إيقافه عالمياً وقطع الـ DNS عنه.
- كان النطاق البديل `sub.wyzie.io` يطلب مفاتيح مصادقة (401 Unauthorized)، مما حرم الأفلام الكلاسيكية مثل `Day of the Dead (1985)` وغيرها من الحصول على ترجمات عربية.

### الحل الجذري المنفذ:
تم ترقية محرك `CoreApiClient.fetchSubtitles` للاعتماد على محرك **Stremio OpenSubtitles v3** السحابي المجاني والمفتوح:
- **أفلام:** `https://opensubtitles-v3.strem.io/subtitles/movie/{imdbId}.json`
- **حلقات مسلسلات:** `https://opensubtitles-v3.strem.io/subtitles/series/{imdbId}:{season}:{episode}.json`
- **التجربة العملية على فيلم Day of the Dead (`tt0088993`):**
  - استعاد المحرك فورياً 65 ملف ترجمة عالي الجودة.
  - تم استخراج ملفات الترجمة العربية المباشرة بتنسيق UTF-8 من سيرفرات `https://subs5.strem.io/.../sub.srt`.
  - ترتيب الترجمات يعطي الأولوية للغة العربية أولاً `العربية (مترجم)` ثم الإنجليزية ثم اللغات الأخرى.
  - في حال غياب معرّف IMDb، يقوم `ImdbMetadataEngine` بالبحث التلقائي عن اسم العمل واستخراج الـ ID الصحيح آلياً.

---

## 6. إصلاحات تجربة المشغل والكبسولات وأزرار التحميل

1. **إلغاء تداخل المشغلين (Player Overlay Fix):**
   - في المشغل المدمج (Web Embed مثل VidLink و Videasy)، يحتوي المتصفح على أدوات تحكم HTML5 كاملة.
   - تم قفل إظهار أزرار التحكم الخارجية لتطبيق BeoutTV (`PlayerControlsOverlay`) واختصارات الريموت عند تشغيل الـ Web Embed (`!isWebEmbed`)، مع الاكتفاء بشريط علوي طافي بسيط يحتوي على زر الرجوع وزر تبديل السيرفر.
2. **توحيد زر التحميل:**
   - تم حذف زر التحميل السفلي المكرر بجانب زر الإعلان في شاشة تفاصيل الفيلم.
   - الزر الرئيسي المعتمد للتحميل يتواجد في الشريط العلوي مباشرة بجوار زر المشاركة المفضلة.
3. **تصميم الكبسولات وفق هوية BeoutTV:**
   - استبدال الكبسولات الشفافة (`alpha = 0.2f`) والبيضاوية الدائرية (`CircleShape`) ببطاقات رصينة متطابقة مع نمط التطبيق:
     - الخلفية: `BeoutColors.ElevatedSurface` (`0xFF161F2C`).
     - الإطار: `1.dp` بحدود `BeoutColors.Divider` (`0xFF1E293B`).
     - الزوايا: `BeoutShapes.Medium` (نصف قطر 12.dp قياسي).

---

## 7. دليل الصيانة الدورية واستكشاف الأخطاء (Maintenance & Troubleshooting)

### أ. اختبار صلاحية سيرفرات MovieBox يدوياً:
يمكن فحص استجابة الخادم في أي وقت عبر سطر الأوامر (Termux):
```bash
curl -s -X POST "https://h5.aoneroom.com/wefeed-h5-bff/mini/trending" \
  -H "User-Agent: Mozilla/5.0" \
  -H "x-tr-devtype: h5" \
  -H "x-tr-region: CN" \
  -H "x-md-global-color: lane4" \
  -d "page=1&perPage=20" | jq .code
```
- إذا كانت النتيجة `0` أو `"0"`، فإن الخوادم والترويسات تعمل بكفاءة 100%.

### ب. في حال توقف الـ API أو حظر المنطقة (Region Blocking):
- توجه إلى ملف `MovieBoxApiClient.kt` وقم بتعديل ترويسة الدولة:
  - استبدل `x-tr-region: CN` بـ `x-tr-region: US` أو `x-tr-region: NG`.

### ج. فحص وتحديث روابط الترجمة:
- خادم الترجمات يعمل عبر بروتوكول HTTP مفتوح بدون أي حاجة لمفاتيح سرية:
```bash
curl -s "https://opensubtitles-v3.strem.io/subtitles/movie/tt0088993.json" | grep -o '"lang":"ara"' | wc -l
```
- إذا أظهر الرقم أكثر من 0، فالترجمة العربية نشطة ومتاحة فورياً داخل التطبيق.
