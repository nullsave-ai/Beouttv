package com.beouttv.app.data.source.stream

import androidx.media3.common.C
import androidx.media3.common.ParserException
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.IOException

/**
 * سياسة معالجة أخطاء شبكة وقوائم HLS المرنة المقاومة للانقطاع (Resilient HLS Fault-Tolerance Policy):
 * 1. حماية البث من التوقف عند حدوث أخطاء HTTP 404 أو 503 العابرة أثناء تدوير خوادم CDN أو انزلاق نافذة البث المباشر.
 * 2. تطبيق خوارزمية Exponential Backoff سريعة وذكية لإعادة المحاولة دون تعليق واجهة المستخدم أو إظهار شاشة الخطأ.
 * 3. زيادة عدد محاولات استرجاع قوائم التشغيل (Manifests) إلى 8 محاولات قبل الاستسلام لضمان استمرارية المشاهدة.
 */
class ResilientHlsLoadErrorHandlingPolicy(
    private val minRetries: Int = 6
) : DefaultLoadErrorHandlingPolicy(minRetries) {

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val exception = loadErrorInfo.exception
        val errorCount = loadErrorInfo.errorCount

        // 1. أخطاء استجابة HTTP (مثل 404 Not Found أو 502 Bad Gateway أو 503 Service Unavailable)
        if (exception is InvalidResponseCodeException) {
            val code = exception.responseCode
            if (code == 404 || code == 410 || code in 500..599) {
                if (errorCount <= minRetries) {
                    // فواصل إعادة محاولة سريعة جداً (من 250ms إلى 2000ms كحد أقصى) للحفاظ على تدفق البث
                    return minOf(250L * (1L shl (errorCount - 1)), 2000L)
                }
            }
        }

        // 2. أخطاء معالجة القوائم أو انقطاعات المقبس العابرة
        if (exception is ParserException || exception is IOException) {
            if (errorCount <= minRetries) {
                return minOf(400L * errorCount, 2500L)
            }
        }

        return super.getRetryDelayMsFor(loadErrorInfo)
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int {
        return when (dataType) {
            C.DATA_TYPE_MANIFEST -> 8  // محاولة حتى 8 مرات لقوائم التشغيل لمنع التوقف أثناء تحديث الـ CDN
            C.DATA_TYPE_MEDIA -> 6     // محاولة حتى 6 مرات لمقاطع وتجزئات الفيديو
            else -> minRetries
        }
    }
}
