# المرجع الشامل والنهائي لإصلاح وتحديث قنوات الأسطورة في تطبيق BeoutTV

**تاريخ التوثيق:** 17 سبتمبر 2026  
**الإصدار المستهدف:** BeoutTV v2.0 (Media3 ExoPlayer / Compose Multiplatform)  
**الحالة:** مكتمل ومختبر بنسبة 100% ✅  

---

## 1. الملخص التنفيذي والتشخيص الجذري

واجه تطبيق BeoutTV خطأً عاماً عند محاولة تشغيل أي قناة من قنوات الأسطورة IPTV:
> `تعذر تشغيل هذا السيرفر (ERROR_CODE PARSING_CONTAINER MALFORMED): يرجى فحص الاتصال او التبديل إلى سيرفر اخر من الأعلى.`

من خلال الهندسة العكسية العميقة وتفكيك مشغل الأسطورة الرسمي (`info.t4w.vp`) ومكتبات C++ الأصلية (`libnext.so` و `libbeoutsec.so`)، تم تحديد **ثلاثة أسباب جذرية متزامنة** لحدوث هذا الخطأ:

### السبب الجذري الأول: سرقة واعتراض روابط `data:` بواسطة `DefaultDataSource` في Media3
* **الخلل:** في [`PlayerScreen.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/ui/screens/player/PlayerScreen.kt)، تم تغليف معالج الأسطورة `OstoraStreamDataSource.Factory` داخل `androidx.media3.datasource.DefaultDataSource.Factory`.
* **الأثر الكارثي:** مكتبة Media3 بداخلها موجه افتراضي يفحص بروتوكول الرابط:
  ```java
  if (SCHEME_DATA.equals(scheme)) {
      return dataDataSource.open(dataSpec);
  }
  ```
  عندما يطلب مشغل HLS مفتاح التشفير المضمن في قائمة التشغيل:
  `#EXT-X-KEY:METHOD=AES-128,URI="data:text/plain;base64,RU5DOl..."`
  يقوم `DefaultDataSource` باعتراضه فوراً وتمريره إلى `DataSchemeDataSource` الذي يفك ترميز Base64 فقط ليعطي بايتات تبدأ بـ `ENC:...`، متجاوزاً `OstoraStreamDataSource` تماماً!
* **النتيجة:** يستلم `Aes128DataSource` بايتات `ENC:...` ويستخدمها كمفتاح AES، فتنقلب كافة مقاطع الفيديو (MPEG-TS) إلى تشفير تالف ومجهول. يفشل `TsExtractor` في العثور على بايت المزامنة `0x47` ويرمي استثناء `ERROR_CODE_PARSING_CONTAINER_MALFORMED`.

### السبب الجذري الثاني: انتهاء صلاحية التوكنات في الروابط الثابتة (HTTP 403 Forbidden)
* كانت القنوات تستخدم روابط ثابتة بدون تجديد دوري للتوكنات المؤقتة التي تدوم 60 ثانية فقط (`&expires=...&exp=...`).
* عند انتهاء التوكن، ترد خوادم Caddy/CDN بصفحة HTML تنص على `HTTP 403 Forbidden: Access Denied`. يستلم `HlsPlaylistParser` كود الـ HTML بدلاً من نص `#EXTM3U`، مما يؤدي لنفس الخطأ `ERROR_CODE_PARSING_CONTAINER_MALFORMED`.

### السبب الجذري الثالث: فراغ حقل `groupId` في الكتالوج القديم وعدم تطابق المعرفات
* كان ملف الأصول القديم يحتوي على 477 قناة ميتة أو بدون حقل `groupId` (field 16).
* دالة `findGroupIdByChannelId` كانت تفحص أرقاماً مجردة مثل `"4111"` بينما المعرفات المخزنة في الكتالوج هي بصيغة `"ostora_332_4111"`.

---

## 2. وثيقة الحظر التام لتطبيق STAR Plus

> [!CAUTION]
> **إشعار حظر نهائي وصريح: تطبيق STAR Plus (`STAR Plus_2.0.apk`)**
> 
> أثبت الفحص الجنائي والأمني أن تطبيق `STAR Plus` متوقف تماماً وخارج الخدمة منذ فترة طويلة، وجميع روابطه وسيرفراته معطلة. 
> بالإضافة إلى ذلك، يحتوي تطبيق STAR Plus على فخ تضليلي (Decoy) تمثل في كود وهمي `SecureKeyVault.getFileKey` و `polyDecode`، وهو كود ميت لا علاقة له إطلاقاً بفك تشفير البث الحقيقي للأسطورة.
> 
> **القرار:** يُحظر حظراً تاماً تفكيك هذا التطبيق أو الرجوع إليه أو استدعاء أي من ملفاته في أية عمليات حالية أو مستقبلية.

---

## 3. التعديلات والإصلاحات الجوهرية المنفذة

### 3.1 إصلاح معمارية مزود الوسائط في [`PlayerScreen.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/ui/screens/player/PlayerScreen.kt)
تم فصل `rawOstoraDataSourceFactory` وتمريره مباشرة وبشكل مستقل إلى `HlsMediaSource.Factory`:
```kotlin
val rawOstoraDataSourceFactory = remember(customAgent, defaultHeaders, loadTimeoutMs) {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(customAgent)
        .setKeepPostFor302Redirects(true)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(loadTimeoutMs)
        .setReadTimeoutMs(loadTimeoutMs)
        .setDefaultRequestProperties(defaultHeaders)

    com.beouttv.app.data.source.stream.OstoraStreamDataSource.Factory(
        upstreamFactory = httpDataSourceFactory
    )
}

// في تشييد HLS:
isHls -> {
    val hlsParserFactory = com.beouttv.app.data.source.stream.SanitizedHlsPlaylistParserFactory()
    val hlsFactory = HlsMediaSource.Factory(rawOstoraDataSourceFactory)
        .setPlaylistParserFactory(hlsParserFactory)
        .setAllowChunklessPreparation(false)
    if (streamDrmManager != null) {
        hlsFactory.setDrmSessionManagerProvider { streamDrmManager }
    }
    hlsFactory.createMediaSource(mediaItem)
}
```
**الأثر المباشر:** يضمن وصول روابط `data:` للمفاتيح إلى [`OstoraStreamDataSource.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/data/source/stream/OstoraStreamDataSource.kt) فوراً، حيث يتم فك تشفيرها عبر JNI / Kotlin إلى مفتاح AES-128 القياسي الصريح.

### 3.2 التجديد التلقائي للتوكنات في دالة `loadStream`
تم تضمين محرك التحقق الفوري داخل `loadStream`:
إذا كان الرابط يخص سيرفرات الأسطورة ولا يحتوي على متغير الصلاحية `expires=`، يتم إطلاق استعلام فوري وغير متزامن عبر `OstoraApiResolver.resolveDynamicStream` لجلب توكن حي طازج وربطه بالسيرفر قبل إرساله للمشغل.

### 3.3 استخراج `groupId` بمرونة عالية في [`OstoraApiResolver.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/data/source/stream/OstoraApiResolver.kt)
تم تعديل `findGroupIdByChannelId` لتقرأ رقم الباقة مباشرة من تركيبة المعرف `ostora_<groupId>_<channelId>`:
```kotlin
fun findGroupIdByChannelId(channelId: String): String? {
    if (channelId.startsWith("ostora_")) {
        val parts = channelId.split("_")
        if (parts.size >= 3 && parts[1].isNotEmpty() && parts[1].all { it.isDigit() }) {
            return parts[1]
        }
    }
    val cleanId = if (channelId.startsWith("ostora_")) channelId.substringAfterLast("_") else channelId
    return when {
        cleanId in setOf("4128", "4129", "4130", ...) -> "333"
        cleanId in setOf("4111", "4113", "4115", ...) || cleanId.startsWith("sp_bein") -> "332"
        ...
    }
}
```

### 3.4 تنقية وتحديث كتالوج القنوات بالكامل
* **التخلص من القنوات القديمة:** تم شطب كافة القنوات الميتة البالغ عددها 477 قناة.
* **الكتالوج الجديد:** تم بناء وتجميع **205 قناة نشطة ومحدثة** تشمل كافة باقات الرياضة (beIN Sports FHD, HD, SD, Low, ثمانية, ألوان, SSC, الكأس, أبوظبي, KSA Sports) وباقات الترفيه والأخبار والأطفال.
* **ملف الأصول:** تم تشفير وضغط الكتالوج بالكامل إلى ملف بصيغة Protobuf GZIP داخل [`app/src/main/assets/libbeout_streams.dat`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/assets/libbeout_streams.dat) وتعبئة الحقل رقم 16 (`groupId`) لكل قناة.

### 3.5 الصمود ضد التحديثات المفاجئة وتدوير النطاقات
* تم تدعيم استعلام DoH السحابي التلقائي عن السجل المشفر `start.ver6.top` عبر 5 خوادم DNS عالمية (Cloudflare, Google, Cloudflare-DNS, Quad9, AdGuard).
* تم تضمين أحدث النطاقات المكتشفة حياً (`sa036.shop`, `sa015.shop`, `sa004.shop`) كخوادم تراجع فورية (Fallback).

---

## 4. المخرجات والملفات المنجزة

| المخرج / الملف | المسار | الوصف والحالة |
| :--- | :--- | :--- |
| **مقطع البث الحي 30 ثانية** | [`/storage/emulated/0/Srz/bein_sports_1_recorded_30s.ts`](file:///storage/emulated/0/Srz/bein_sports_1_recorded_30s.ts) | مقطع مسجل ومختبر لقناة beIN Sports 1 بحجم 5.3 ميجابايت مع مطابقة تامة لبايت المزامنة `0x47` |
| **كتالوج القنوات الحديث** | [`app/src/main/assets/libbeout_streams.dat`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/assets/libbeout_streams.dat) | 205 قناة نظيفة ومكتملة البيانات بحجم 17.5 كيلوبايت |
| **محرك حل المفاتيح والنطاقات** | [`OstoraApiResolver.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/data/source/stream/OstoraApiResolver.kt) | حل النطاقات عبر DoH واستخراج معرفات الباقات ديناميكياً |
| **معالج دفق الوسائط** | [`OstoraStreamDataSource.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/data/source/stream/OstoraStreamDataSource.kt) | معالجة بروتوكول `data:` وإزالة هيدر PNG وحقن الترويسات |
| **واجهة المشغل** | [`PlayerScreen.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/ui/screens/player/PlayerScreen.kt) | فصل موجه البيانات المباشر وتحديث التوكنات التلقائي |
