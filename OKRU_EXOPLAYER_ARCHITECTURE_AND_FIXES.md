# هندسة ومعمارية تشغيل البث المباشر (OK.ru & ExoPlayer Ultra-Resilience Engine)

## 1. تحليل الأسباب الجذرية للمشكلات السابقة (Root Cause Analysis)

### أ. خطأ `ERROR_CODE PARSING_MANIFEST_MALFORMED`
- **السبب التقني:** وفقاً للمعيار القياسي للبث التكيفي RFC 8216 (HLS Specification)، يجب أن تحتوي كل قائمة وسائط (Media Playlist) على وسم `#EXT-X-TARGETDURATION:<s>` يحدد الحد الأقصى لمدة مقاطع الفيديو.
- خوادم منصة OK.ru أثناء انزلاق نافذة البث المباشر (Sliding Live Window) أو عند إنشاء أول أجزاء البث تقوم بحذف هذا الوسم مؤقتاً أو إرسال مقاطع بمدة زمنية تفوق القيمة المعلنة (مثال: مقطع بمدة `6.440s` بينما الترويسة `6s`).
- مشغل ExoPlayer يلتزم التزاماً صارماً بالمعيار، وبمجرد غياب هذا الوسم يرمي استثناء `ParserException.createForMalformedManifest` ويتوقف البث فوراً.

### ب. خطأ `السيرفر أعاد استجابة خطأ (HTTP Error)` و 404
- **السبب التقني:** شبكة توزيع المحتوى (CDN) التابعة لمنصة OK.ru تستخدم نظام تدوير خوادم الحافة (Edge Node Rotation) مثل التحويل بين `vsd115.okcdn.ru` و `vsd123.okcdn.ru` و `vsd233.okcdn.ru`.
- بدون سياسة مرنة لمعالجة أخطاء التحميل (`LoadErrorHandlingPolicy`)، يعتبر مشغل ExoPlayer أي استجابة 404 عابرة خطأً قاتلاً (Fatal Error) ويوقف المشغل بدلاً من إعادة المحاولة.
- على جانب السيرفر السحابي، كان مدخل البث يعتمد على `.m3u8` مع خاصية `-re` مما كان يؤخر قراءة ملفات التشغيل ويؤدي لانتهاء صلاحية المقاطع في المصدر وحدوث انقطاع مؤقت في تدفق RTMP.

### ج. تكرار مؤشر التحميل والتوقف المستمر (Buffer Starvation)
- **السبب التقني:** كان إعداد `playStartMs` داخل `PlayerScreen.kt` مضبوطاً على `400ms` فقط (أي 0.4 ثانية).
- نظراً لأن مقاطع بث HLS تبلغ مدتها من ثانيتين إلى 6 ثوانٍ، فإن 0.4 ثانية من الذاكرة المؤقتة تستنفد في لمح البصر بمجرد حدوث أي تذبذب طبيعي في الشبكة، مما كان يدخل المشغل في حلقة مفرغة من إعادة التخزين المؤقت (Rebuffering Loop).

---

## 2. المعمارية الهندسية المتكاملة التي تم بناؤها

```
[ IPTV Source (.ts Continuous Pipe) ]
                 │
                 ▼
[ GitHub Actions Cloud Relay (Azure Runner) ]
   - Continuous MPEG-TS Ingestion
   - Strict CBR 2200k + Fixed 2.0s Closed GOP
   - Low-latency RTMP Stream to OK.ru
                 │
                 ▼
[ OK.ru Transcoding CDN (vsdXXX.okcdn.ru) ]
                 │
                 ▼
[ Beouttv Android App (Client-Side Resolution) ]
   ├── OkRuStreamResolver.kt
   │      - Instant Video ID & Options Extraction
   │      - IP-Bound Signed HLS Token Generation
   ├── SanitizedHlsPlaylistParserFactory.kt
   │      - Injects Missing #EXT-X-TARGETDURATION
   │      - Ensures TargetDuration >= Max Segment Duration
   │      - Cleans Invisible BOM & Malformed Headers
   ├── ResilientHlsLoadErrorHandlingPolicy.kt
   │      - Tolerates Transient 404 / 503 Edge Failovers
   │      - Exponential Backoff Auto-Retry (Up to 8 attempts)
   ├── PlayerScreen.kt (Broadcast Buffer Optimization)
   │      - Min Buffer: 15s | Max Buffer: 35s
   │      - Play Start Cushion: 2.5s | Rebuffer: 3.5s
   │      - Smart Auto-Healing on onPlayerError
   └── OstoraStreamDataSource.kt
          - Dynamic Ingestion of Referer: https://ok.ru/
          - Modern Browser User-Agent Header Injection
```

---

## 3. تفاصيل المكونات المحدثة في التطبيق

### 1. مصنع تطهير وترميم القوائم (`SanitizedHlsPlaylistParserFactory.kt`)
- فحص القائمة قبل تسليمها إلى مفسر ExoPlayer.
- حساب أطول مدة مقطع في القائمة (`maxSegmentDuration`) وحقن وسم `#EXT-X-TARGETDURATION` بالقيمة الصحيحة رياضياً فورياً.
- حقن وسمي `#EXT-X-VERSION:3` و `#EXT-X-MEDIA-SEQUENCE:0` وتطهير أحرف BOM غير المرئية.

### 2. سياسة معالجة الأخطاء المرنة (`ResilientHlsLoadErrorHandlingPolicy.kt`)
- معالجة أخطاء HTTP 404 و 502 و 503 العابرة ومنع انهيار البث.
- جدولة إعادة المحاولة بفواصل ذكية تبدأ من 250 ملي ثانية وتصل إلى 2000 ملي ثانية.
- رفع عدد محاولات استرجاع ملفات المانيفست إلى 8 محاولات.

### 3. تحسين سعة التخزين المؤقت وحماية التدفق (`PlayerScreen.kt`)
- تعديل `playStartMs` من 400ms إلى 2500ms لتأمين استلام مقطع كامل قبل بدء العرض.
- تعديل `rebufferMs` إلى 3500ms لضمان عودة سلسة وثابتة بدون تقطيع.
- إضافة ميزة التعافي الذاتي التلقائي داخل `onPlayerError`: في حال حدوث أي انقطاع غير متوقع، يقوم المشغل تلقائياً وفي الخلفية بإعادة حل الرابط وجلب توقيع جديد ومواصلة العرض دون إزعاج المستخدم.

### 4. محرك البث السحابي المحدث (`stream_relay.py` في مستودع السحابة)
- التحويل التلقائي لمدخل البث من ملفات التجزئة `.m3u8` إلى أنبوب البث المستمر `.ts` لضمان عدم حدوث أي فجوات زمنية أو انقطاعات في إشارة RTMP.
- ضبط حجم طابور المعالجة على 2048 حزمة لمنع سقوط الفريمات.
- تفعيل معدل بت ثابت (CBR) ومجموعة صور مغلقة (Closed GOP) كل ثانيتين بدقة 25 إطار في الثانية.
