# الدليل المحدث لبناء وتوقيع BeoutTV عبر GitHub Actions وإدارة الأسرار (Secrets) ⚡🚀
### GitHub Actions CI/CD Automated Build, Keystore Signing & Secrets Architecture Guide

---

## 📑 فهرس المحتويات
1. [نظرة عامة على التحديث والمعمارية](#1-نظرة-عامة-على-التحديث-والمعمارية)
2. [التغييرات الجوهرية في هذا الإصدار (v3.0 Universal)](#2-التغييرات-الجوهرية-في-هذا-الإصدار-v30-universal)
3. [معمارية سير العمل (Workflow Architecture)](#3-معمارية-سير-العمل-workflow-architecture)
4. [قائمة أسرار غيتهب (GitHub Repository Secrets) وطريقة إعدادها](#4-قائمة-أسرار-غيتهب-github-repository-secrets-وطريقة-إعدادها)
5. [طرق إطلاق البناء التلقائي واليدوي (Triggering Builds)](#5-طرق-إطلاق-البناء-التلقائي-واليدوي-triggering-builds)
6. [استكشاف الأخطاء وحلولها الشائعة (Troubleshooting)](#6-استكشاف-الأخطاء-وحلولها-الشائعة-troubleshooting)
7. [خطوات تثبيت النسخة الناتجة على الشاشات والهواتف](#7-خطوات-تثبيت-النسخة-الناتجة-على-الشاشات-والهواتف)

---

## 1. نظرة عامة على التحديث والمعمارية

يوفر هذا الدليل المرجع الأحدث لعملية **التكامل والنشر المستمر (CI/CD Pipeline)** لتطبيق **BeoutTV Universal v3.0** عبر منصة **GitHub Actions**.

يقوم نظام البناء التلقائي بـ:
1. استدعاء الكود المصدري وإعداد بيئة **JDK 17 (Eclipse Temurin)** و **Gradle 8.9**.
2. فك تشفير وتثبيت مفاتيح التوقيع وملفات المصادقة السرية (Keystore & Secrets) بشكل آمن دون كشفها في المستودع العام.
3. تجميع حزمة تطبيق موحدة وشاملة (**Universal Release APK**) موقّعة بالتوقيع الرباعي المعتمد (`V1 + V2 + V3 + V4`) ومحاذاة الذاكرة (`4-byte Zipalign`).
4. رفع الحزمة الناتجة كملف **Artifact** جاهز للتحميل، وإنشاء **GitHub Release** رسمي تلقائياً برقم الإصدار وتاريخ التحديث وملاحظات الإصدار باللغة العربية.

---

## 2. التغييرات الجوهرية في هذا الإصدار (v3.0 Universal)

تم إجراء تحسينات معمارية وتنظيف شامل للكود قبل تفعيل البناء:
* **استئصال ميزة "تلفزيون قصير" (Short TV / Short Drama) نهائياً**:
  * حذف كافة الشاشات والنماذج (`ShortDramaScreen`, `ShortDramaViewModel`, `ShortDramaBottomSheet`).
  * حذف الكتالوجات والخوادم والتخزين الخاص بالدراما القصيرة (`ShortDramaRepository`, `ShortDramaMediaCache`, `ShortDramaDownloadManager`, `MovieBoxShortDramaCatalog`).
  * إزالة التبويب من القوائم السفلية والشريط الجانبي لشاشات التلفاز، وإزالة التبويب من القائمة الجانبية (Drawer).
  * خفض حجم الـ APK وتسريع وقت تشغيل التطبيق واستهلاك الذاكرة العشوائية.
* **تفعيل الفلتر الأخلاقي الإجباري الدائم (+18)**:
  * قفل تفعيل الفلتر الأخلاقي الأسري إجبارياً ودائماً (`isAdultFilterEnabled = true`).
  * إزالة أزرار التبديل والتعطيل من شاشات الإعدادات لمنع إلغاء الفلتر تحت أي ظرف.
  * توسيع محرك الفلترة [`ContentFilterEngine.kt`](file:///storage/emulated/0/Filterlanguage/Beouttv/Beouttv/app/src/main/java/com/beouttv/app/data/filter/ContentFilterEngine.kt) ليشمل فلاتر ذكية عربية وعالمية للألفاظ الخادشة والمحتوى الإباحي مع تسوية الحروف العربية ومطابقة دقيقة لحدود الكلمات لمنع الحجب الخاطئ.
* **إطلاق منظومة ONE+ (وان+) الكاملة**:
  * استبدال كافة التسميات والعلامات في المشغل وسيرفرات البث وقوائم الترجمة والشاشة الرئيسية والكتالوج وشاشة التحميلات بهوية **ONE+ (وان+)**.

---

## 3. معمارية سير العمل (Workflow Architecture)

يقع ملف التكوين في المسار:
[`.github/workflows/build.yml`](file:///storage/emulated/0/Filterlanguage/Beouttv/Beouttv/.github/workflows/build.yml)

### محتوى سير العمل المعتمد:
```yaml
name: Build BeoutTV Universal APK

on:
  push:
    branches: [ master, main ]
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    name: Build APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'

      - name: Configure Keystore & Signing Secrets
        run: |
          # 1. استعادة ملف release.keystore في حال تم وضعه في أسرار غيتهب
          if [ -n "${{ secrets.RELEASE_KEYSTORE_BASE64 }}" ]; then
            echo "Restoring release.keystore from GitHub Secrets..."
            echo "${{ secrets.RELEASE_KEYSTORE_BASE64 }}" | base64 -d > release.keystore
          fi

          # 2. توليد release.properties من أسرار غيتهب
          if [ -n "${{ secrets.KEYSTORE_STORE_PASSWORD }}" ] && [ -n "${{ secrets.KEYSTORE_KEY_ALIAS }}" ]; then
            echo "Generating release.properties from GitHub Secrets..."
            cat <<EOF > release.properties
          storePassword=${{ secrets.KEYSTORE_STORE_PASSWORD }}
          keyAlias=${{ secrets.KEYSTORE_KEY_ALIAS }}
          keyPassword=${{ secrets.KEYSTORE_KEY_PASSWORD }}
          storeFile=release.keystore
          EOF
          fi

          # 3. استعادة google-services.json إن وجد في الأسرار
          if [ -n "${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}" ]; then
            echo "Restoring google-services.json from GitHub Secrets..."
            echo "${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}" | base64 -d > app/google-services.json
          fi

      - name: Build Release APK
        run: |
          chmod +x ./gradlew || true
          ./gradlew assembleRelease --stacktrace

          WORKSPACE_DIR=$PWD
          mkdir -p "$WORKSPACE_DIR/dist"
          
          # استخراج ونسخ حزمة الإصدار الرسمية
          find app/build/outputs/apk/release/ -name "*.apk" -exec cp {} "$WORKSPACE_DIR/dist/BeoutTV-Universal-v3.0.apk" \;
          ls -lh "$WORKSPACE_DIR/dist/"

      - name: Upload Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: BeoutTV_apk
          path: dist/*.apk

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        if: startsWith(github.ref, 'refs/tags/') || github.ref == 'refs/heads/master' || github.ref == 'refs/heads/main'
        with:
          tag_name: v3.0.${{ github.run_number }}
          name: "BeoutTV Universal v3.0.${{ github.run_number }} (Release)"
          body: |
            ### 🚀 تحديثات BeoutTV Universal v3.0 الجديدة:
            * **إزالة تلفزيون قصير بالكامل**: استئصال ميزة الدراما والريلز وحذف كافة الأكواد الميتة لتسريع التطبيق وتخفيف وزنه.
            * **الفلتر الأخلاقي الإجباري الدائم**: تفعيل محرك الحماية الأسرية (+18) دائماً وإزالة زر الإلغاء وحظر المحتوى الإباحي والألفاظ الخادشة عربياً وعالمياً.
            * **إطلاق منظومة ONE+ (وان+)**: استبدال كافة خدمات موفي بوكس برمجياً بسيرفرات وترجمات وتصنيفات ONE+ في المشغل، القائمة، والرئيسية.
            * **توافق كلي ومثالي**: دعم شامل وموحد لأجهزة التلفاز الذكية (Android TV / Smart TV / TV Box) والهواتف لكافة المعماريات.
          files: dist/*.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

---

## 4. قائمة أسرار غيتهب (GitHub Repository Secrets) وطريقة إعدادها

لضمان بناء الحزمة وتوقيعها بشكل آمن ومحمي، يمكنك إضافة الأسرار التالية في مستودع GitHub:

### مسار إعداد الأسرار في غيتهب:
1. توجه إلى صفحة مستودعك على GitHub.
2. ادخل إلى: **Settings** ⚙️ ➔ **Secrets and variables** ➔ **Actions**.
3. انقر على الزر الأخضر: **New repository secret**.

---

### جدول الأسرار المعتمدة:

| اسم السر (Secret Name) | القيمة (Value / Description) | هل هو إلزامي؟ |
|---|---|---|
| `RELEASE_KEYSTORE_BASE64` | محتوى ملف `release.keystore` محولاً إلى كود Base64 من سطر واحد. | إلزامي إذا لم يكن المفتاح مرفوعاً في المستودع |
| `KEYSTORE_STORE_PASSWORD` | `rschqrIo` (كلمة سر المخزن). | إلزامي إذا تم إنشاء `release.properties` عبر الأسرار |
| `KEYSTORE_KEY_ALIAS` | `Jm8bvOq3oo4vBF` (الاسم المستعار للمفتاح). | إلزامي |
| `KEYSTORE_KEY_PASSWORD` | `rschqrIo` (كلمة سر المفتاح). | إلزامي |
| `GOOGLE_SERVICES_JSON_BASE64` | محتوى ملف `google-services.json` مشفراً بصيغة Base64. | اختياري (إن أردت إخفاء تكوين Firebase) |

---

### أمر توليد كود Base64 لملف التوقيع (`release.keystore`):
إذا أردت الحصول على النص المشفر لوضعه في السر `RELEASE_KEYSTORE_BASE64`، نفذ الأمر التالي في بيئة لينكس أو تيرمكس:

```bash
# توليد نص Base64 في سطر واحد ونسخه
base64 -w 0 release.keystore > keystore_base64.txt
cat keystore_base64.txt
```
> انسخ محتوى الملف وضع قيمته في السر `RELEASE_KEYSTORE_BASE64`.

---

## 5. طرق إطلاق البناء التلقائي واليدوي (Triggering Builds)

### الطريقة الأولى: البناء اليدوي بضغطة زر (Workflow Dispatch):
1. في صفحة المستودع على غيتهب، اضغط على تبويب **Actions**.
2. من القائمة اليسرى، اختر سير العمل: **Build BeoutTV Universal APK**.
3. اضغط على القائمة المنسدلة **Run workflow** على اليمين.
4. اختر الفرع (مثلاً `master` أو `main`) ثم اضغط على الزر الأخضر **Run workflow**.

### الطريقة الثانية: الدفع التلقائي (Git Push):
بمجرد عمل `git push` إلى الفرع `master` أو `main`، يبدأ البناء تلقائياً في ثوانٍ.

### الطريقة الثالثة: إنشاء Tag لإصدار جديد:
عند عمل Tag جديد مثل:
```bash
git tag v3.0.1
git push origin v3.0.1
```
سيقوم سير العمل ببناء الحزمة وربطها بالـ Tag المخصص تلقائياً.

---

## 6. استكشاف الأخطاء وحلولها الشائعة (Troubleshooting)

### خطأ 1: فشل إنشاء الإصدار (`403 Resource not accessible by integration`):
* **السبب:** صلاحيات التوكن الافتراضي للـ Actions تكون أحياناً "Read only".
* **الحل:**
  1. ادخل إلى: **Settings** ➔ **Actions** ➔ **General**.
  2. انزل إلى قسم **Workflow permissions**.
  3. اختر الخيار: **Read and write permissions**.
  4. فعّل خيار: **Allow GitHub Actions to create and approve pull requests**.
  5. اضغط **Save**.

### خطأ 2: فشل العثور على المفتاح (`keystore file not found`):
* **السبب:** عدم وجود `release.keystore` في المجلد الرئيسي وعدم توفر السر `RELEASE_KEYSTORE_BASE64`.
* **الحل:** التأكد من إضافة السر `RELEASE_KEYSTORE_BASE64` أو وجود ملف `release.keystore` محلياً في المستودع.

### خطأ 3: مشاكل الذاكرة أو تعطل الـ Daemon أثناء `assembleRelease`:
* **الحل:** تم ضبط إعدادات الـ Heap بدقة في `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=768m -XX:+UseG1GC
  org.gradle.daemon=false
  ```
  حيث يتكفل رانر `ubuntu-latest` بتنفيذ البناء الكامل في أقل من دقيقتين.

---

## 7. خطوات تثبيت النسخة الناتجة على الشاشات والهواتف

بعد انتهاء البناء بنجاح، ستجد ملف الـ APK متاحاً في:
* **صفحة الإصدارات (GitHub Releases):** تحت اسم `BeoutTV-Universal-v3.0.apk`.
* **مرفقات تشغيل الأكشن (Run Artifacts):** تحت اسم `BeoutTV_apk.zip`.

> [!IMPORTANT]
> **قاعدة أمان أندرويد لتجنب خطأ التثبيت (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`):**  
> إذا كانت هناك نسخة سابقة مثبتة على الهاتف أو الشاشة موقعة بمفتاح مختلف أو نسخة تجريبية، يجب **إلغاء تثبيت النسخة القديمة أولاً** قبل تثبيت النسخة الرسمية الجديدة v3.0 لضمان تثبيت نقي وسلس 100%.
