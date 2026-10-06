# مرجع نظام المشغل والشعار والإعلانات في تطبيق Beout TV
**تاريخ التوثيق:** سبتمبر 2026  
**الإصدار:** Beout TV 1.0 (Build 1) — SDK 35 (Android 15 Ready)  
**الحالة:** مستقر ومعتمد بالكامل  

---

## فهرس المحتويات
1. [مقدمة ونطاق التعديلات](#1-مقدمة-ونطاق-التعديلات)
2. [حل انهيار الخدمة الأمامية (LogSenderService Crash Fix)](#2-حل-انهيار-الخدمة-الأمامية-logsenderservice-crash-fix)
3. [بنية الإعلانات وتكامل Adsterra مع Firebase](#3-بنية-الإعلانات-وتكامل-adsterra-مع-firebase)
4. [معالجة أخطاء بناء Kotlin البرمجية (PlayerScreen.kt)](#4-معالجة-أخطاء-بناء-kotlin-البرمجية-playerscreenkt)
5. [تصميم ومعالجة زوايا الشعار (beout_watermark.png)](#5-تصميم-ومعالجة-زوايا-الشعار-beout_watermarkpng)
6. [معالجة انعكاس الإحداثيات بين العربية والانجليزية (RTL vs LTR)](#6-معالجة-انعكاس-الإحداثيات-بين-العربية-والانجليزية-rtl-vs-ltr)
7. [القياسات الرقمية ومعادلات التمركز فوق شعار الأسطورة](#7-القياسات-الرقمية-ومعادلات-التمركز-فوق-شعار-الأسطورة)
8. [حصر ظهور الشعار حصرياً على قنوات بي إن سبورت](#8-حصر-ظهور-الشعار-حصرياً-على-قنوات-بي-إن-سبورت)
9. [دليل الصيانة السريعة والتعديل المستقبلي](#9-دليل-الصيانة-السريعة-والتعديل-المستقبلي)

---

## 1. مقدمة ونطاق التعديلات
يوثق هذا المرجع كافة التحديثات والتحسينات الهندسية التي تم تطبيقها على مشغل الفيديو الرئيسي لنظام Beout TV ([`PlayerScreen.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/ui/screens/player/PlayerScreen.kt))، بما يضمن استقرار البث، حماية الهوية البصرية الرسمية، توافقية العرض عبر جميع أبعاد الشاشات (Fit / Fill / Zoom)، والتكامل الإعلاني غير المزعج.

---

## 2. حل انهيار الخدمة الأمامية (LogSenderService Crash Fix)

### العطل المرصود:
```text
android.app.RemoteServiceException$ForegroundServiceDidNotStopInTimeException: 
A foreground service of type dataSync did not stop within its timeout: 
ComponentInfo{com.beouttv.app/com.itsaky.androidide.logsender.LogSenderService}
```

### السبب الجذري:
في نظام أندرويد 15 (SDK 35)، تفرض جوجل قيوداً زمنية صارمة على خدمات `dataSync` في الخلفية. كانت مكتبة التطوير المدمجة لـ AndroidIDE تحقن خدمة `LogSenderService` تلقائياً دون إيقافها، مما يسبب إغلاقاً إجبارياً بعد انقضاء المهلة.

### الحل المطبق:
1. إزالة الخدمة نهائياً من البيان عبر [`app/src/main/AndroidManifest.xml`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/AndroidManifest.xml):
   ```xml
   <service
       android:name="com.itsaky.androidide.logsender.LogSenderService"
       tools:node="remove" />
   ```
2. تعطيلها صراحة في موارد القيم [`app/src/main/res/values/bools.xml`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/res/values/bools.xml):
   ```xml
   <bool name="logsender_enabled">false</bool>
   ```

---

## 3. بنية الإعلانات وتكامل Adsterra مع Firebase

### التكوين المعتمد:
- **نطاق الاستضافة:** `https://digite-bda04.firebaseapp.com/`
- **معرف الوحدة الإعلانية (Zone ID):** `31138665`
- **صيغة البنر:** `Banner 320x50` (Iframe)
- **مفتاح Adsterra البرمجي:** `498e06cd635a3e8dc62c065f77491ce2`
- **رابط الاستدعاء الخارجي:** `https://www.highrevenueformat.com/498e06cd635a3e8dc62c065f77491ce2/invoke.js`

### إعدادات السيرفر السحابي (Firebase Realtime Database):
- العقدة: `ads_config`
- `streamLaunchClickFrequency = 1`: إظهار كبسولة الإعلان المخصصة داخل المشغل عند كل نقرة لتشغيل القناة (100% نسبة ظهور).
- `bannerHtml`: يحتوي كود الإعلان المضمن بصيغة Iframe المتوافقة مع أجهزة الموبايل.
- **تنبيه:** تم التأكيد على عدم عرض لوحات الأرباح أو أدوات الإحصائيات الحساسة داخل واجهة المستخدم العامة للتطبيق.

---

## 4. معالجة أخطاء بناء Kotlin البرمجية (PlayerScreen.kt)

أثناء التجميع عبر المهام البرمجية لمشروع أندرويد، ظهرت أخطاء عدم تعرف على مراجع معينة تم حلها كالتالي:

| الخطأ البرمجي | سببه | الحل المطبق |
|---|---|---|
| `Unresolved reference: category` | محاولة التحقق من تصنيف القناة داخل المشغل رغم عدم تمرير الوسيط `category` في معاملات دالة `PlayerScreen` | تم تعديل التحقق ليعتمد بدقة على `title` و `mediaId` و `streamUrl` دون الحاجة لوسيط إضافي. |
| `Unresolved reference: clipToBounds` | نقص استيراد الدالة من حزمة Compose Draw | تم إضافة `import androidx.compose.ui.draw.clipToBounds` |
| `Type inference / detectTapGestures` | ناتج ثانوي لخطأ `clipToBounds` الذي قطع سلسلة معدّلات واجهة المستخدم (Modifier chain) | زال تلقائياً بمجرد اكتمال الاستيرادات للمعدّلات |
| `Unresolved reference: isInPipMode` | محاولة الوصول لخاصية غير معرّفة داخل سياق العرض المحلي | تم تعريف المتغير محلياً: `(context as? Activity)?.isInPictureInPictureMode == true` |

---

## 5. تصميم ومعالجة زوايا الشعار (beout_watermark.png)

### الإجراء الفني:
1. **الصورة المصدرية:** [`file_0000000075b0820aa7f91b3a93e78546.png`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/file_0000000075b0820aa7f91b3a93e78546.png) بأبعاد `2172 x 724` بنسبة عرض إلى ارتفاع ثابتة `3:1`.
2. **معالجة الحواف الحادة:** تم تطبيق قناع تنعيم نصف قطري (Anti-aliased Rounded Corner Mask) بنصف قطر تقويس ناعم وبشفافية ألفا تامة للزوايا الخارجية لإكسابه طابع قنوات التلفزة الفضائية الرسمية.
3. **موقع المورد المعتمد:** [`app/src/main/res/drawable/beout_watermark.png`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/res/drawable/beout_watermark.png).

---

## 6. معالجة انعكاس الإحداثيات بين العربية والانجليزية (RTL vs LTR)

### المشكلة التقنية:
عندما تكون لغة التطبيق عربية، تكون قيمة `LocalLayoutDirection` هي `LayoutDirection.Rtl`. في نظام Jetpack Compose:
- الدالة `Modifier.offset(x = ...)` تطبق داخلياً `placeRelative(x, y)`.
- في بيئة RTL، يتم قياس الإزاحة `x` **نسبة إلى نقطة البداية (حافة الشاشة اليمنى)** باتجاه اليسار!
- بالتالي، عند إعطاء إزاحة موجبة بمقدار عرض الشاشة تقريباً لتثبيت الشعار على اليمين، كان المحرك يدفع بالشعار إلى أقصى اليسار الفيزيائي!

### الحل الهندسي المعتمد:
تغليف حاوية الشعار داخل فضاء إحداثيات فيزيائي مستقل وثابت باتجاه قراءة يسار-إلى-يمين (LTR)، لكون سطح الفيديو مساحة هندسية مطلقة:
```kotlin
CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(x = watermarkX, y = watermarkY)
                .width(watermarkWidth)
                .height(watermarkHeight)
                .clip(RoundedCornerShape(5.dp))
                .zIndex(10f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.beout_watermark),
                contentDescription = "BeoutTV Watermark",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

---

## 7. القياسات الرقمية ومعادلات التمركز فوق شعار الأسطورة

تم فحص لقطات الشاشة الميدانية من هواتف المستخدمين بدقة البكسل (`Screenshot_20260908-100154.jpg` بدقة `2436 x 1080` مع كثافة بكسلات `density = 3.0`، وإطار فيديو بدقة `1920 x 1080`):

```
                        إطار الفيديو (1920x1080)
┌─────────────────────────────────────────────────────────────┐
│                                       [TV][الأسطورة 6]      │
│                                       X: 1860 → 2115        │
│                                       Y: 75 → 113           │
│                                       المركز: X=1987, Y=94  │
│                                                             │
│                                                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### الثوابت الرياضية لشعار الأسطورة نسبةً إلى إطار الفيديو:
- **المركز الأفقي:** يقع على بعد **`9.87%`** من الحافة اليمنى لإطار الفيديو:
  $$\text{ostoraCenterX} = \text{effectiveVideoLeft} + \text{currentVideoWidth} - (\text{currentVideoWidth} \times 0.0987)$$
- **المركز الرأسي:** يقع على بعد **`8.70%`** من الحافة العلوية لإطار الفيديو:
  $$\text{ostoraCenterY} = \text{effectiveVideoTop} + (\text{currentVideoHeight} \times 0.0870)$$

### أبعاد الشعار وهوامش التغطية:
- **العرض:** `watermarkWidth = (currentVideoWidth * 0.168f).coerceIn(60.dp, 114.dp)`  
  *(في الوضع الأفقي: 108.dp أي 324 بكسل في شاشات 1080p)*
- **الارتفاع:** `watermarkHeight = watermarkWidth / 3.0f`  
  *(في الوضع الأفقي: 36.dp أي 108 بكسل)*
- **التمركز:**
  ```kotlin
  val watermarkX = (ostoraCenterX - (watermarkWidth / 2f))
      .coerceIn(0.dp, (maxWidth - watermarkWidth).coerceAtLeast(0.dp))
  val watermarkY = (ostoraCenterY - (watermarkHeight / 2f))
      .coerceIn(0.dp, (maxHeight - watermarkHeight).coerceAtLeast(0.dp))
  ```

### النتيجة الهندسية:
يوفر هذا التمركز تغطية محكمة مع هامش أمان متماثل بحوالي **35 بكسل** في جميع الاتجاهات الأربعة (يمين، يسار، أعلى، أسفل)، مما يخفي شريط الأسطورة كلياً مع بقاء الشعار داخل الفيديو دون الاصطدام بالحواف السوداء (`Pillarbox`).

---

## 8. حصر ظهور الشعار حصرياً على قنوات بي إن سبورت

### كود الفلترة النهائي في [`PlayerScreen.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/ui/screens/player/PlayerScreen.kt#L319-L346):
```kotlin
val isBeinSportsChannel = remember(title, mediaId, streamUrl) {
    val t = title.lowercase(java.util.Locale.ROOT)
    val m = mediaId.lowercase(java.util.Locale.ROOT)
    val u = streamUrl.lowercase(java.util.Locale.ROOT)

    val isOtherChannel = t.contains("ثمانية") || t.contains("thmanyah") ||
            t.contains("أون") || t.contains("اون") || t.contains("ontime") || t.contains("on sport") ||
            t.contains("ssc") || t.contains("alkass") || t.contains("كاس") || t.contains("كأس") ||
            t.contains("أبوظبي") || t.contains("ابوظبي") || t.contains("ad sport") ||
            t.contains("دبي") || t.contains("dubai") || t.contains("mbc") || t.contains("rotana") ||
            m.contains("thmanyah") || m.contains("ontime") || m.contains("onsport") || m.contains("ssc")

    if (isOtherChannel) {
        false
    } else {
        t.contains("bein") ||
                t.contains("بي ان") ||
                t.contains("بي إن") ||
                t.contains("بين سبورت") ||
                (t.contains("بين") && (t.contains("سبورت") || t.any { it.isDigit() })) ||
                m.contains("bein") ||
                u.contains("bein")
    }
}

val shouldShowWatermark = remember(isLive, isLiveStream, isBeinSportsChannel) {
    (isLive || isLiveStream) && isBeinSportsChannel
}
```

### القنوات المستثناة صراحة:
- قنوات ثمانية (`Thmanyah`).
- قنوات أون تايم سبورت / أون سبورت (`ON Time / ON Sport`).
- شبكة قنوات SSC السعودية.
- قنوات الكأس الرياضية القطرية (`Alkass`).
- قنوات أبوظبي الرياضية (`AD Sports`).
- قنوات دبي الرياضية (`Dubai Sports`).
- قنوات MBC وروتانا وكافة الباقات المنوعة.

---

## 9. دليل الصيانة السريعة والتعديل المستقبلي

1. **لتعديل أبعاد الشعار:**
   - توجه إلى السطر `983` في [`PlayerScreen.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/ui/screens/player/PlayerScreen.kt#L983) وعدّل المعامل `0.168f` أو حدود `coerceIn(60.dp, 114.dp)`.
2. **لتعديل إزاحة الشعار الرأسية أو الأفقية:**
   - توجه إلى السطرين `989-990` وعدّل نسب التمركز:
     - أفقي: `0.0987f` (زيادة الرقم تحرك الشعار يساراً، نقصانه يحركه يميناً).
     - رأسي: `0.0870f` (زيادة الرقم تنزل الشعار لأسفل، نقصانه يرفعه لأعلى).
3. **لإضافة قناة إلى قائمة الاستثناءات:**
   - أضف اسم القناة في متغير `isOtherChannel` في السطر `324`.
