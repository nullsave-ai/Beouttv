# 📖 المرجع الشامل لقسم الرياضة والمباريات واستضافة عطارد ولوحة التحكم 🚀
### Beout TV - Sports Engine, Hosting, Panel & Maintenance Reference

---

## 📌 الفهرس
1. [بيانات استضافة عطارد (Atared / Serv00) وسكربتات المباريات](#1-بيانات-استضافة-عطارد-atared--serv00-وسكربتات-المباريات)
2. [بيانات سيرفر البث ولوحة التحكم XUI.ONE و BeoutPanel](#2-بيانات-سيرفر-البث-ولوحة-التحكم-xuione-و-beoutpanel)
3. [معمارية وهندسة قسم المباريات في تطبيق Beout TV](#3-معمارية-وهندسة-قسم-المباريات-في-تطبيق-beout-tv)
4. [محرك المطابقة الذكي للقنوات (Smart Channel Matcher)](#4-محرك-المطابقة-الذكي-للقنوات-smart-channel-matcher)
5. [نظام النادي المفضل (Favorite Club Hub & BottomSheet)](#5-نظام-النادي-المفضل-favorite-club-hub--bottomsheet)
6. [دليل التحديث والصيانة وإضافة بطولات جديدة](#6-دليل-التحديث-والصيانة-وإضافة-بطولات-جديدة)

---

## 🌐 1. بيانات استضافة عطارد (Atared / Serv00) وسكربتات المباريات

### 🔗 روابط ونقاط الـ API الرئيسية (API Endpoints):

| البيان | الرابط / القيمة | الغرض والاستخدام |
| :--- | :--- | :--- |
| **الرابط الرئيسي (365Scores API)** | `https://atared.serv00.net/365.php` | جلب المباريات، الجداول، الترتيب، الأندية، والقنوات الناقلة بدون null وبترتيب أولويات البطولات |
| **الرابط الاحتياطي (YSScores API)** | `https://camillecyrm.serv00.net/ysscores.php` | مصدر بديل لجلب المباريات والتشكيلات وأحداث المباريات |
| **السيرفر المباشر (Direct WebWS)** | `https://webws.365scores.com/web/` | نظام الفولباك (Fallback) المباشر داخل التطبيق في حال تعطل الاستضافة |

---

### 📡 معلمات الطلب (API Request Parameters):

#### 1️⃣ جلب جدول المباريات (Matches Schedule):
```http
GET https://atared.serv00.net/365.php?action=matches&date=today
```
* **المعلمات المدعومة (`date`):**
  - `today`: مباريات اليوم الحالية والمباشرة.
  - `tomorrow`: مباريات الغد.
  - `yesterday`: مباريات الأمس ونتائجها.
  - `YYYY-MM-DD`: مباريات تاريخ محدد (مثال: `2026-08-28`).

#### 2️⃣ جلب جدول الترتيب لبطولة محددة (Standings Table):
```http
GET https://atared.serv00.net/365.php?action=standings&competition_id=11
```
* **أهم معرفات البطولات (`competition_id`):**
  - `17`: دوري أبطال أوروبا (UEFA Champions League)
  - `7`: الدوري الإنجليزي الممتاز (Premier League)
  - `11`: الدوري الإسباني (La Liga)
  - `572`: دوري روشن السعودي (Saudi Pro League)
  - `6822`: دوري نجوم العراق (Iraq Stars League)
  - `25`: الدوري الإيطالي (Serie A)
  - `26`: الدوري الألماني (Bundesliga)
  - `27`: الدوري الفرنسي (Ligue 1)
  - `549`: دوري أبطال آسيا للنخبة (AFC Champions League Elite)
  - `550`: دوري أبطال إفريقيا (CAF Champions League)
  - `552`: الدوري المصري الممتاز (Egyptian Premier League)

#### 3️⃣ جلب أندية بطولة محددة لاختيار النادي المفضل (Teams for Selection):
```http
GET https://atared.serv00.net/365.php?action=teams&competition_id=11
```

#### 4️⃣ جلب قائمة البطولات المرتبة حسب الأولوية (Competitions List):
```http
GET https://atared.serv00.net/365.php?action=competitions
```

---

## 🖥️ 2. بيانات سيرفر البث ولوحة التحكم XUI.ONE و BeoutPanel

### 🔑 بيانات لوحة تحكم السيرفر (XUI Admin Panel):

| البيان | التفاصيل والروابط |
| :--- | :--- |
| **رابط الخادم الرئيسي** | `https://vps-production-8622.up.railway.app` |
| **رابط دخول الأدمن** | [https://vps-production-8622.up.railway.app/PJ37AlnY/login](https://vps-production-8622.up.railway.app/PJ37AlnY/login) |
| **كود الوصول (Access Code)** | `PJ37AlnY` |
| **اسم مستخدم الأدمن (Owner)** | `bountyify` |
| **كلمة مرور الأدمن (Owner)** | `bountyify1` |
| **حساب الأدمن الاحتياطي** | `admin` / `admin123` |
| **اتصال SSH المباشر** | `sakura.proxy.rlwy.net:58627` (المستخدم: `root` / كلمة المرور: `final1997@@@`) |

---

### 🛡️ لوحة التحكم السحابية لتطبيق Beout TV (`BeoutPanel`):
* **المسار في الهاتف:** `/storage/emulated/0/XUI/BeoutPanel/`
* **ملف الإعدادات (`config.json`):**
  - التحكم بحالة التطبيق (`status: normal | maintenance | update`).
  - رابط الـ API الرئيسي ورابط الـ Backup.
  - رسائل الصيانة والتحديث الإجباري ورابط الـ APK الجديد.
  - التوكنات المسموح لها بالاتصال (`allowed_tokens`).

---

## 🏗️ 3. معمارية وهندسة قسم المباريات في تطبيق Beout TV

يتكون قسم الرياضة داخل المشروع من بنية معمارية متكاملة (Clean Architecture + MVVM):

```
app/src/main/java/com/beouttv/app/
├── data/
│   ├── model/
│   │   └── Models.kt                  # كلاسات Match, LeagueStanding, ClubInfo, LeagueInfo
│   ├── api/
│   │   └── SportsApiClient.kt         # محرك الاتصال بـ 365.php والفولباك المباشر مع 365Scores WebWS
│   ├── filter/
│   │   └── ChannelMatcher.kt          # محرك المطابقة الذكي بين القنوات الناقلة وقنوات البث الحية
│   ├── favorites/
│   │   └── FavoriteClubManager.kt     # إدارة وحفظ النادي المفضل وإشعارات المباريات
│   └── repository/
│       ├── BeoutRepository.kt         # واجهة البيانات
│       └── RemoteMediaRepository.kt   # تنفيذ دوال جلب المباريات والترتيب ومباريات النادي
└── ui/
    ├── screens/sports/
    │   ├── SportsScreen.kt            # الشاشة الرئيسية الرياضية (تبويبات: جدول المباريات | الترتيب | ناديي المفضل)
    │   ├── FavoriteClubBottomSheet.kt # نافذة اختيار الدوري ثم النادي مع البحث الحي
    │   └── FavoriteClubHubView.kt     # مركز النادي المفضل (الإحصائيات، الإشعارات، القادمة، السابقة، الترتيب)
    └── components/
        └── BeoutCards.kt              # بطاقة BeoutMatchCard الموحدة وشعارات الدوريات وحالة المباريات
```

---

## ⚡ 4. محرك المطابقة الذكي للقنوات (Smart Channel Matcher)

تم بناء كلاس [`ChannelMatcher.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/data/filter/ChannelMatcher.kt) ليقوم بالمهام التالية:
1. **تنظيف أسماء القنوات:** إزالة الكلمات الزائدة مثل (`باقة`, `قناة`, `1080p FHD`, `HD`, `(4K)`).
2. **توحيد الأرقام واللغة:** تحويل الأرقام الهندية/العربية (`١, ٢, ٣`) إلى أرقام قياسية (`1, 2, 3`) ومطابقة القنوات مثل `beIN Sports 1 HD` مع `beIN Sports 1 (1080p FHD)`.
3. **التشغيل المباشر بضغطة زر:** عندما تكون المباراة جارية (Live) ويتم العثور على القناة في التطبيق، تظهر شارة تفاعلية `🔴 بث مباشر • beIN Sports 1`، وعند الضغط عليها يفتح التطبيق مشغل الفيديو فورياً لمشاهدة البث المباشر.
4. **منع ظهور نصوص `null`:** يتم فلترة أي نصوص فارغة أو غير معروفة بحيث لا يظهر أي نص غير لائق في الواجهة.

---

## ⭐ 5. نظام النادي المفضل (Favorite Club Hub & BottomSheet)

### 1️⃣ نافذة الاختيار (`FavoriteClubBottomSheet`):
* **الخطوة الأولى:** عرض شبكة الدوريات والبطولات الكبرى بشعاراتها الرسمية.
* **الخطوة الثانية:** عند النقر على الدوري، يتم جلب أنديته بشعاراتها الحقيقية ومراكزها ونقاطها، مع وجود شريط بحث حي بالاسم.

### 2️⃣ مركز ناديي المفضل (`FavoriteClubHubView`):
* **بطاقة هوية النادي:** شعار النادي، الدوري، الملعب، المركز، النقاط، وعدد المباريات (لعب، فوز، تعادل، خسارة).
* **مفتاح الإشعارات الذكي:** تفعيل أو تعطيل التنبيه التلقائي لمباريات الفريق قبل 15 دقيقة من الانطلاق.
* **مباريات النادي القادمة:** جدول مواعيد مباريات النادي مع روابط البث المباشر.
* **النتائج السابقة والأخيرة:** نتائج مواجهات النادي الأخيرة مع أهداف كل مباراة.
* **موقع النادي في جدول الترتيب:** عرض جدول الدوري المحيط بالنادي.

---

## 🛠️ 6. دليل التحديث والصيانة وإضافة بطولات جديدة

### ➕ إضافة دوري أو بطولة جديدة:
1. افتح ملف [`FavoriteClubBottomSheet.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/ui/screens/sports/FavoriteClubBottomSheet.kt).
2. أضف معرف وشعار البطولة في قائمة `AVAILABLE_LEAGUES`:
   ```kotlin
   LeagueInfo("معرف_البطولة", "اسم البطولة", "رابط الشعار الرسمي", "الدولة")
   ```
3. افتح ملف [`BeoutCards.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/ui/components/BeoutCards.kt) وأضف اسم البطولة في خريطة `LeagueLogoHelper.leagueLogos` لضمان ظهور الشعار دائماً في رأس الجدول والبطاقات.
4. أضف المعرف في قائمة `topLeagues` في [`SportsScreen.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/ui/screens/sports/SportsScreen.kt).

---

### 🔄 تحديث وتعديل سكربت `365.php` على استضافة عطارد:
* **مسار الملف المحلي للاحتفاظ والتعديل:** `/storage/emulated/0/jdol/365.php`
* يحتوي السكربت على مصفوفة الأولويات `$priorityCompetitions` التي تحدد ترتيب ظهور الدوريات الكبرى في أعلى الجدول تلقائياً.
* عند الرغبة في تغيير رابط الاستضافة، يتم تحديث المتغير `PRIMARY_HOST` في ملف [`SportsApiClient.kt`](file:///storage/emulated/0/CodeOnTheGoProjects/Beouttv/app/src/main/java/com/beouttv/app/data/api/SportsApiClient.kt):
  ```kotlin
  private const val PRIMARY_HOST = "https://your-domain.serv00.net/365.php"
  ```

---

> [!TIP]
> **ملاحظة أمان واستقرار:** تم تزويد التطبيق بنظام استعادة تلقائي (Fallback System) يتصل مباشرة بـ 365Scores WebWS في حال حدوث أي انقطاع في الاستضافة الخارجية، مما يضمن عمل قسم المباريات والرياضة في تطبيق Beout TV دون انقطاع على مدار الساعة.
