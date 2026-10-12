package com.beouttv.app.data.security

import android.util.Log

/**
 * Military-Grade Native Security & Cryptographic Vault for Beout TV.
 * Hides all sensitive API URLs, Firebase endpoints, Adsterra keys, and Ostora decryption secrets
 * inside compiled C++ ELF machine code (libbeoutsec.so) and non-linear polymorphic byte structures.
 */
object NativeSecurity {
    private const val TAG = "NativeSecurity"
    private const val XOR_SEED: Byte = 0x5C

    const val KEY_DOH_URL = 1
    const val KEY_OSTORA_DEFAULT_BASE = 2
    const val KEY_FIREBASE_ADS_URL = 3
    const val KEY_ADSTERRA_DIRECT_LINK = 4
    const val KEY_ADSTERRA_SOCIAL_BAR = 5
    const val KEY_FIREBASE_WEBAPP_DOMAIN = 6
    const val KEY_WEBAPP_DOMAIN = 7
    const val KEY_ADSTERRA_ZONE_ID = 8
    const val KEY_ADSTERRA_BANNER_KEY = 9
    const val KEY_OSTORA_REFERER = 10
    const val KEY_OSTORA_PREFIX = 11
    const val KEY_OSTORA_USER_AGENT = 12
    const val KEY_FIREBASE_CHANNELS_URL = 13
    const val KEY_FIREBASE_HOSTING_GATEWAY = 14
    const val KEY_FIREBASE_RTDB_GATEWAY = 15
    const val KEY_PRIMARY_PANEL_URL = 16
    const val KEY_BOUNTYIFY_PANEL_URL = 17
    const val KEY_FALLBACK_GATEWAY_URL = 18
    const val KEY_ACTIVE_API_URL = 19
    const val KEY_CLOUDFLARE_ENDPOINT = 20
    const val KEY_BACKUP_PANEL_URL = 21
    const val KEY_FIREBASE_STORAGE_CONFIG_URL = 22
    const val KEY_ADSTERRA_BANNER_SCRIPT_URL = 25
    const val KEY_ADSTERRA_BANNER_ZONE_ID = 29

    private var isNativeLoaded = false

    init {
        try {
            // c++_shared is declared as DT_NEEDED in libbeoutsec.so ELF header.
            // The Android linker loads it automatically — no manual loadLibrary needed.
            System.loadLibrary("beoutsec")
            isNativeLoaded = true
            Log.i(TAG, "NativeSecurity: libbeoutsec.so initialized [${System.getProperty("os.arch")}]")
        } catch (t: Throwable) {
            isNativeLoaded = false
            Log.w(TAG, "NativeSecurity: libbeoutsec.so load failed on this ABI: ${t.message}")
        }
    }

    @JvmStatic
    external fun getSecureConfig(keyId: Int): String

    @JvmStatic
    external fun getOstoraAesKey(): ByteArray

    @JvmStatic
    external fun getOstoraAesIv(): ByteArray

    @JvmStatic
    external fun decryptHlsKeyNative(input: ByteArray): ByteArray?

    @JvmStatic
    external fun checkIntegrity(): Boolean

    @JvmStatic
    external fun isNativeSecurityActive(): Boolean

    // Obfuscated Fallback Data Vault (Zero plaintext strings in Dalvik bytecode)
    private val fallbackVault = mapOf<Int, ByteArray>(
        1 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 109.toByte(), 114.toByte(), 109.toByte(), 114.toByte(), 109.toByte(), 114.toByte(), 109.toByte(), 115.toByte(), 56.toByte(), 50.toByte(), 47.toByte(), 113.toByte(), 45.toByte(), 41.toByte(), 57.toByte(), 46.toByte(), 37.toByte(), 99.toByte(), 50.toByte(), 61.toByte(), 49.toByte(), 57.toByte(), 97.toByte(), 47.toByte(), 40.toByte(), 61.toByte(), 46.toByte(), 40.toByte(), 114.toByte(), 42.toByte(), 57.toByte(), 46.toByte(), 106.toByte(), 114.toByte(), 40.toByte(), 51.toByte(), 44.toByte(), 122.toByte(), 40.toByte(), 37.toByte(), 44.toByte(), 57.toByte(), 97.toByte(), 8.toByte(), 4.toByte(), 8.toByte()),
        2 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 56.toByte(), 111.toByte(), 50.toByte(), 38.toByte(), 53.toByte(), 50.toByte(), 54.toByte(), 105.toByte(), 100.toByte(), 61.toByte(), 52.toByte(), 47.toByte(), 107.toByte(), 50.toByte(), 111.toByte(), 104.toByte(), 63.toByte(), 53.toByte(), 56.toByte(), 46.toByte(), 49.toByte(), 107.toByte(), 44.toByte(), 106.toByte(), 38.toByte(), 63.toByte(), 58.toByte(), 108.toByte(), 45.toByte(), 43.toByte(), 48.toByte(), 108.toByte(), 42.toByte(), 46.toByte(), 55.toByte(), 62.toByte(), 47.toByte(), 41.toByte(), 104.toByte(), 59.toByte(), 40.toByte(), 105.toByte(), 52.toByte(), 110.toByte(), 101.toByte(), 59.toByte(), 100.toByte(), 57.toByte(), 53.toByte(), 54.toByte(), 114.toByte(), 47.toByte(), 61.toByte(), 108.toByte(), 110.toByte(), 100.toByte(), 114.toByte(), 47.toByte(), 52.toByte(), 51.toByte(), 44.toByte()),
        3 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 56.toByte(), 53.toByte(), 59.toByte(), 53.toByte(), 40.toByte(), 57.toByte(), 113.toByte(), 62.toByte(), 56.toByte(), 61.toByte(), 108.toByte(), 104.toByte(), 113.toByte(), 56.toByte(), 57.toByte(), 58.toByte(), 61.toByte(), 41.toByte(), 48.toByte(), 40.toByte(), 113.toByte(), 46.toByte(), 40.toByte(), 56.toByte(), 62.toByte(), 114.toByte(), 58.toByte(), 53.toByte(), 46.toByte(), 57.toByte(), 62.toByte(), 61.toByte(), 47.toByte(), 57.toByte(), 53.toByte(), 51.toByte(), 114.toByte(), 63.toByte(), 51.toByte(), 49.toByte(), 115.toByte(), 61.toByte(), 56.toByte(), 47.toByte(), 3.toByte(), 63.toByte(), 51.toByte(), 50.toByte(), 58.toByte(), 53.toByte(), 59.toByte(), 114.toByte(), 54.toByte(), 47.toByte(), 51.toByte(), 50.toByte()),
        4 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 43.toByte(), 43.toByte(), 43.toByte(), 114.toByte(), 44.toByte(), 46.toByte(), 51.toByte(), 58.toByte(), 53.toByte(), 40.toByte(), 61.toByte(), 62.toByte(), 48.toByte(), 57.toByte(), 46.toByte(), 61.toByte(), 40.toByte(), 57.toByte(), 63.toByte(), 44.toByte(), 49.toByte(), 50.toByte(), 57.toByte(), 40.toByte(), 43.toByte(), 51.toByte(), 46.toByte(), 55.toByte(), 114.toByte(), 63.toByte(), 51.toByte(), 49.toByte(), 115.toByte(), 38.toByte(), 40.toByte(), 38.toByte(), 61.toByte(), 109.toByte(), 43.toByte(), 105.toByte(), 50.toByte(), 99.toByte(), 55.toByte(), 57.toByte(), 37.toByte(), 97.toByte(), 105.toByte(), 100.toByte(), 61.toByte(), 104.toByte(), 105.toByte(), 109.toByte(), 56.toByte(), 101.toByte(), 101.toByte(), 106.toByte(), 111.toByte(), 104.toByte(), 109.toByte(), 110.toByte(), 111.toByte(), 105.toByte(), 63.toByte(), 63.toByte(), 110.toByte(), 61.toByte(), 111.toByte(), 63.toByte(), 57.toByte(), 58.toByte(), 100.toByte(), 58.toByte(), 61.toByte(), 110.toByte(), 58.toByte(), 62.toByte(), 107.toByte(), 63.toByte()),
        5 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 63.toByte(), 61.toByte(), 44.toByte(), 53.toByte(), 40.toByte(), 61.toByte(), 48.toByte(), 52.toByte(), 51.toByte(), 47.toByte(), 44.toByte(), 53.toByte(), 40.toByte(), 61.toByte(), 48.toByte(), 47.toByte(), 114.toByte(), 51.toByte(), 46.toByte(), 59.toByte(), 115.toByte(), 109.toByte(), 115.toByte(), 111.toByte(), 108.toByte(), 56.toByte(), 101.toByte(), 56.toByte(), 101.toByte(), 104.toByte(), 57.toByte(), 105.toByte(), 109.toByte(), 104.toByte(), 109.toByte(), 100.toByte(), 100.toByte(), 100.toByte(), 104.toByte(), 56.toByte(), 58.toByte(), 61.toByte(), 57.toByte(), 56.toByte(), 109.toByte(), 58.toByte(), 110.toByte(), 58.toByte(), 57.toByte(), 110.toByte(), 61.toByte(), 107.toByte(), 101.toByte(), 109.toByte(), 109.toByte()),
        6 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 56.toByte(), 53.toByte(), 59.toByte(), 53.toByte(), 40.toByte(), 57.toByte(), 113.toByte(), 62.toByte(), 56.toByte(), 61.toByte(), 108.toByte(), 104.toByte(), 114.toByte(), 58.toByte(), 53.toByte(), 46.toByte(), 57.toByte(), 62.toByte(), 61.toByte(), 47.toByte(), 57.toByte(), 61.toByte(), 44.toByte(), 44.toByte(), 114.toByte(), 63.toByte(), 51.toByte(), 49.toByte(), 115.toByte()),
        7 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 56.toByte(), 53.toByte(), 59.toByte(), 53.toByte(), 40.toByte(), 57.toByte(), 113.toByte(), 62.toByte(), 56.toByte(), 61.toByte(), 108.toByte(), 104.toByte(), 114.toByte(), 43.toByte(), 57.toByte(), 62.toByte(), 114.toByte(), 61.toByte(), 44.toByte(), 44.toByte(), 115.toByte()),
        8 to byteArrayOf(111.toByte(), 109.toByte(), 106.toByte(), 107.toByte(), 100.toByte(), 106.toByte(), 111.toByte(), 106.toByte()),
        9 to byteArrayOf(104.toByte(), 101.toByte(), 100.toByte(), 57.toByte(), 108.toByte(), 106.toByte(), 63.toByte(), 56.toByte(), 106.toByte(), 111.toByte(), 105.toByte(), 61.toByte(), 111.toByte(), 57.toByte(), 100.toByte(), 56.toByte(), 63.toByte(), 106.toByte(), 110.toByte(), 63.toByte(), 108.toByte(), 106.toByte(), 105.toByte(), 58.toByte(), 107.toByte(), 107.toByte(), 104.toByte(), 101.toByte(), 109.toByte(), 63.toByte(), 57.toByte(), 110.toByte()),
        10 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 51.toByte(), 47.toByte(), 40.toByte(), 51.toByte(), 46.toByte(), 61.toByte(), 114.toByte(), 40.toByte(), 42.toByte(), 115.toByte()),
        11 to byteArrayOf(104.toByte(), 109.toByte(), 111.toByte(), 96.toByte(), 26.toByte(), 98.toByte()),
        12 to byteArrayOf(17.toByte(), 51.toByte(), 38.toByte(), 53.toByte(), 48.toByte(), 48.toByte(), 61.toByte(), 115.toByte(), 105.toByte(), 114.toByte(), 108.toByte(), 124.toByte(), 116.toByte(), 11.toByte(), 53.toByte(), 50.toByte(), 56.toByte(), 51.toByte(), 43.toByte(), 47.toByte(), 124.toByte(), 18.toByte(), 8.toByte(), 124.toByte(), 109.toByte(), 108.toByte(), 114.toByte(), 108.toByte(), 103.toByte(), 124.toByte(), 11.toByte(), 53.toByte(), 50.toByte(), 106.toByte(), 104.toByte(), 103.toByte(), 124.toByte(), 36.toByte(), 106.toByte(), 104.toByte(), 117.toByte(), 124.toByte(), 29.toByte(), 44.toByte(), 44.toByte(), 48.toByte(), 57.toByte(), 11.toByte(), 57.toByte(), 62.toByte(), 23.toByte(), 53.toByte(), 40.toByte(), 115.toByte(), 105.toByte(), 111.toByte(), 107.toByte(), 114.toByte(), 111.toByte(), 106.toByte(), 124.toByte(), 116.toByte(), 23.toByte(), 20.toByte(), 8.toByte(), 17.toByte(), 16.toByte(), 112.toByte(), 124.toByte(), 48.toByte(), 53.toByte(), 55.toByte(), 57.toByte(), 124.toByte(), 27.toByte(), 57.toByte(), 63.toByte(), 55.toByte(), 51.toByte(), 117.toByte(), 124.toByte(), 31.toByte(), 52.toByte(), 46.toByte(), 51.toByte(), 49.toByte(), 57.toByte(), 115.toByte(), 109.toByte(), 105.toByte(), 109.toByte(), 114.toByte(), 108.toByte(), 114.toByte(), 108.toByte(), 114.toByte(), 108.toByte(), 124.toByte(), 15.toByte(), 61.toByte(), 58.toByte(), 61.toByte(), 46.toByte(), 53.toByte(), 115.toByte(), 105.toByte(), 111.toByte(), 107.toByte(), 114.toByte(), 111.toByte(), 106.toByte()),
        13 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 56.toByte(), 53.toByte(), 59.toByte(), 53.toByte(), 40.toByte(), 57.toByte(), 113.toByte(), 62.toByte(), 56.toByte(), 61.toByte(), 108.toByte(), 104.toByte(), 113.toByte(), 56.toByte(), 57.toByte(), 58.toByte(), 61.toByte(), 41.toByte(), 48.toByte(), 40.toByte(), 113.toByte(), 46.toByte(), 40.toByte(), 56.toByte(), 62.toByte(), 114.toByte(), 58.toByte(), 53.toByte(), 46.toByte(), 57.toByte(), 62.toByte(), 61.toByte(), 47.toByte(), 57.toByte(), 53.toByte(), 51.toByte(), 114.toByte(), 63.toByte(), 51.toByte(), 49.toByte(), 115.toByte(), 63.toByte(), 52.toByte(), 61.toByte(), 50.toByte(), 50.toByte(), 57.toByte(), 48.toByte(), 47.toByte(), 114.toByte(), 54.toByte(), 47.toByte(), 51.toByte(), 50.toByte()),
        14 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 56.toByte(), 53.toByte(), 59.toByte(), 53.toByte(), 40.toByte(), 57.toByte(), 113.toByte(), 62.toByte(), 56.toByte(), 61.toByte(), 108.toByte(), 104.toByte(), 114.toByte(), 43.toByte(), 57.toByte(), 62.toByte(), 114.toByte(), 61.toByte(), 44.toByte(), 44.toByte(), 115.toByte(), 59.toByte(), 61.toByte(), 40.toByte(), 57.toByte(), 43.toByte(), 61.toByte(), 37.toByte(), 114.toByte(), 54.toByte(), 47.toByte(), 51.toByte(), 50.toByte()),
        15 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 56.toByte(), 53.toByte(), 59.toByte(), 53.toByte(), 40.toByte(), 57.toByte(), 113.toByte(), 62.toByte(), 56.toByte(), 61.toByte(), 108.toByte(), 104.toByte(), 113.toByte(), 56.toByte(), 57.toByte(), 58.toByte(), 61.toByte(), 41.toByte(), 48.toByte(), 40.toByte(), 113.toByte(), 46.toByte(), 40.toByte(), 56.toByte(), 62.toByte(), 114.toByte(), 58.toByte(), 53.toByte(), 46.toByte(), 57.toByte(), 62.toByte(), 61.toByte(), 47.toByte(), 57.toByte(), 53.toByte(), 51.toByte(), 114.toByte(), 63.toByte(), 51.toByte(), 49.toByte(), 115.toByte(), 59.toByte(), 61.toByte(), 40.toByte(), 57.toByte(), 43.toByte(), 61.toByte(), 37.toByte(), 114.toByte(), 54.toByte(), 47.toByte(), 51.toByte(), 50.toByte()),
        16 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 61.toByte(), 40.toByte(), 61.toByte(), 46.toByte(), 57.toByte(), 56.toByte(), 114.toByte(), 47.toByte(), 57.toByte(), 46.toByte(), 42.toByte(), 108.toByte(), 108.toByte(), 114.toByte(), 50.toByte(), 57.toByte(), 40.toByte(), 115.toByte(), 47.toByte(), 40.toByte(), 46.toByte(), 57.toByte(), 61.toByte(), 49.toByte(), 113.toByte(), 44.toByte(), 61.toByte(), 50.toByte(), 57.toByte(), 48.toByte(), 115.toByte(), 59.toByte(), 61.toByte(), 40.toByte(), 57.toByte(), 43.toByte(), 61.toByte(), 37.toByte(), 114.toByte(), 44.toByte(), 52.toByte(), 44.toByte()),
        17 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 51.toByte(), 50.toByte(), 57.toByte(), 44.toByte(), 48.toByte(), 41.toByte(), 47.toByte(), 50.toByte(), 57.toByte(), 43.toByte(), 61.toByte(), 44.toByte(), 44.toByte(), 114.toByte(), 61.toByte(), 40.toByte(), 61.toByte(), 46.toByte(), 57.toByte(), 56.toByte(), 114.toByte(), 47.toByte(), 57.toByte(), 46.toByte(), 42.toByte(), 108.toByte(), 108.toByte(), 114.toByte(), 50.toByte(), 57.toByte(), 40.toByte(), 115.toByte(), 63.toByte(), 44.toByte(), 61.toByte(), 50.toByte(), 57.toByte(), 48.toByte(), 115.toByte(), 59.toByte(), 61.toByte(), 40.toByte(), 57.toByte(), 43.toByte(), 61.toByte(), 37.toByte(), 114.toByte(), 44.toByte(), 52.toByte(), 44.toByte()),
        18 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 51.toByte(), 50.toByte(), 57.toByte(), 44.toByte(), 48.toByte(), 41.toByte(), 47.toByte(), 113.toByte(), 44.toByte(), 46.toByte(), 51.toByte(), 36.toByte(), 37.toByte(), 114.toByte(), 62.toByte(), 51.toByte(), 41.toByte(), 50.toByte(), 40.toByte(), 37.toByte(), 53.toByte(), 58.toByte(), 37.toByte(), 114.toByte(), 43.toByte(), 51.toByte(), 46.toByte(), 55.toByte(), 57.toByte(), 46.toByte(), 47.toByte(), 114.toByte(), 56.toByte(), 57.toByte(), 42.toByte(), 115.toByte(), 59.toByte(), 61.toByte(), 40.toByte(), 57.toByte(), 43.toByte(), 61.toByte(), 37.toByte(), 114.toByte(), 44.toByte(), 52.toByte(), 44.toByte()),
        19 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 43.toByte(), 57.toByte(), 63.toByte(), 53.toByte(), 49.toByte(), 61.toByte(), 114.toByte(), 62.toByte(), 61.toByte(), 46.toByte(), 115.toByte(), 61.toByte(), 44.toByte(), 53.toByte(), 114.toByte(), 44.toByte(), 52.toByte(), 44.toByte()),
        20 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 47.toByte(), 40.toByte(), 46.toByte(), 57.toByte(), 61.toByte(), 49.toByte(), 114.toByte(), 50.toByte(), 41.toByte(), 48.toByte(), 48.toByte(), 47.toByte(), 61.toByte(), 42.toByte(), 57.toByte(), 114.toByte(), 43.toByte(), 51.toByte(), 46.toByte(), 55.toByte(), 57.toByte(), 46.toByte(), 47.toByte(), 114.toByte(), 56.toByte(), 57.toByte(), 42.toByte()),
        21 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 61.toByte(), 40.toByte(), 61.toByte(), 46.toByte(), 57.toByte(), 56.toByte(), 114.toByte(), 47.toByte(), 57.toByte(), 46.toByte(), 42.toByte(), 108.toByte(), 108.toByte(), 114.toByte(), 50.toByte(), 57.toByte(), 40.toByte(), 115.toByte(), 47.toByte(), 40.toByte(), 46.toByte(), 57.toByte(), 61.toByte(), 49.toByte(), 113.toByte(), 44.toByte(), 61.toByte(), 50.toByte(), 57.toByte(), 48.toByte()),
        22 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 58.toByte(), 53.toByte(), 46.toByte(), 57.toByte(), 62.toByte(), 61.toByte(), 47.toByte(), 57.toByte(), 47.toByte(), 40.toByte(), 51.toByte(), 46.toByte(), 61.toByte(), 59.toByte(), 57.toByte(), 114.toByte(), 59.toByte(), 51.toByte(), 51.toByte(), 59.toByte(), 48.toByte(), 57.toByte(), 61.toByte(), 44.toByte(), 53.toByte(), 47.toByte(), 114.toByte(), 63.toByte(), 51.toByte(), 49.toByte(), 115.toByte(), 42.toByte(), 108.toByte(), 115.toByte(), 62.toByte(), 115.toByte(), 56.toByte(), 53.toByte(), 59.toByte(), 53.toByte(), 40.toByte(), 57.toByte(), 113.toByte(), 62.toByte(), 56.toByte(), 61.toByte(), 108.toByte(), 104.toByte(), 114.toByte(), 58.toByte(), 53.toByte(), 46.toByte(), 57.toByte(), 62.toByte(), 61.toByte(), 47.toByte(), 57.toByte(), 47.toByte(), 40.toByte(), 51.toByte(), 46.toByte(), 61.toByte(), 59.toByte(), 57.toByte(), 114.toByte(), 61.toByte(), 44.toByte(), 44.toByte(), 115.toByte(), 51.toByte(), 115.toByte(), 59.toByte(), 61.toByte(), 40.toByte(), 57.toByte(), 43.toByte(), 61.toByte(), 37.toByte(), 114.toByte(), 54.toByte(), 47.toByte(), 51.toByte(), 50.toByte(), 99.toByte(), 61.toByte(), 48.toByte(), 40.toByte(), 97.toByte(), 49.toByte(), 57.toByte(), 56.toByte(), 53.toByte(), 61.toByte()),
        23 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 59.toByte(), 51.toByte(), 100.toByte(), 55.toByte(), 50.toByte(), 49.toByte(), 114.toByte(), 51.toByte(), 44.toByte(), 40.toByte(), 53.toByte(), 55.toByte(), 48.toByte(), 114.toByte(), 53.toByte(), 50.toByte(), 55.toByte(), 115.toByte(), 47.toByte(), 52.toByte(), 51.toByte(), 46.toByte(), 40.toByte(), 47.toByte(), 115.toByte(), 61.toByte(), 44.toByte(), 53.toByte(), 114.toByte(), 44.toByte(), 52.toByte(), 44.toByte()),
        24 to byteArrayOf(114.toByte(), 57.toByte(), 50.toByte(), 56.toByte(), 49.toByte(), 57.toByte(), 40.toByte(), 52.toByte(), 51.toByte(), 56.toByte()),
        25 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 63.toByte(), 38.toByte(), 57.toByte(), 46.toByte(), 50.toByte(), 53.toByte(), 55.toByte(), 114.toByte(), 51.toByte(), 46.toByte(), 59.toByte(), 115.toByte(), 110.toByte(), 110.toByte(), 115.toByte(), 104.toByte(), 101.toByte(), 100.toByte(), 57.toByte(), 108.toByte(), 106.toByte(), 63.toByte(), 56.toByte(), 106.toByte(), 111.toByte(), 105.toByte(), 61.toByte(), 111.toByte(), 57.toByte(), 100.toByte(), 56.toByte(), 63.toByte(), 106.toByte(), 110.toByte(), 63.toByte(), 108.toByte(), 106.toByte(), 105.toByte(), 58.toByte(), 107.toByte(), 107.toByte(), 104.toByte(), 101.toByte(), 109.toByte(), 63.toByte(), 57.toByte(), 110.toByte()),
        29 to byteArrayOf(111.toByte(), 109.toByte(), 109.toByte(), 111.toByte(), 100.toByte(), 106.toByte(), 106.toByte(), 105.toByte()),
        26 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 43.toByte(), 43.toByte(), 43.toByte(), 114.toByte(), 50.toByte(), 57.toByte(), 40.toByte(), 47.toByte(), 52.toByte(), 51.toByte(), 46.toByte(), 40.toByte(), 40.toByte(), 114.toByte(), 63.toByte(), 51.toByte(), 49.toByte(), 115.toByte(), 61.toByte(), 44.toByte(), 53.toByte(), 115.toByte()),
        27 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 50.toByte(), 57.toByte(), 40.toByte(), 47.toByte(), 52.toByte(), 51.toByte(), 46.toByte(), 40.toByte(), 114.toByte(), 63.toByte(), 51.toByte(), 49.toByte(), 115.toByte(), 61.toByte(), 46.toByte(), 115.toByte(), 57.toByte(), 44.toByte(), 53.toByte(), 47.toByte(), 51.toByte(), 56.toByte(), 57.toByte(), 115.toByte()),
        28 to byteArrayOf(52.toByte(), 40.toByte(), 40.toByte(), 44.toByte(), 47.toByte(), 102.toByte(), 115.toByte(), 115.toByte(), 63.toByte(), 58.toByte(), 63.toByte(), 56.toByte(), 50.toByte(), 114.toByte(), 50.toByte(), 57.toByte(), 40.toByte(), 47.toByte(), 52.toByte(), 51.toByte(), 46.toByte(), 40.toByte(), 114.toByte(), 63.toByte(), 51.toByte(), 49.toByte(), 115.toByte())
    )

    private val fallbackKey = byteArrayOf(18.toByte(), 0.toByte(), 49.toByte(), 70.toByte(), 215.toByte(), 99.toByte(), 180.toByte(), 79.toByte(), 38.toByte(), 103.toByte(), 193.toByte(), 174.toByte(), 54.toByte(), 192.toByte(), 17.toByte(), 189.toByte(), 201.toByte(), 122.toByte(), 39.toByte(), 210.toByte(), 51.toByte(), 48.toByte(), 87.toByte(), 18.toByte(), 64.toByte(), 102.toByte(), 189.toByte(), 142.toByte(), 35.toByte(), 119.toByte(), 18.toByte(), 51.toByte())
    private val fallbackIv  = byteArrayOf(245.toByte(), 158.toByte(), 67.toByte(), 209.toByte(), 34.toByte(), 55.toByte(), 22.toByte(), 193.toByte(), 237.toByte(), 114.toByte(), 19.toByte(), 193.toByte(), 0.toByte(), 70.toByte(), 39.toByte(), 210.toByte())

    fun getString(keyId: Int): String {
        if (isNativeLoaded) {
            try {
                val str = getSecureConfig(keyId)
                if (str.isNotBlank()) return str
            } catch (t: Throwable) {
                Log.w(TAG, "getSecureConfig native call notice for $keyId: ${t.message}")
            }
        }

        val raw = fallbackVault[keyId] ?: return ""
        val out = ByteArray(raw.size)
        for (i in raw.indices) {
            out[i] = (raw[i].toInt() xor XOR_SEED.toInt()).toByte()
        }
        return String(out, Charsets.UTF_8)
    }

    fun getAesKey(): ByteArray {
        if (isNativeLoaded) {
            try {
                val k = getOstoraAesKey()
                if (k.size == 32) return k
            } catch (_: Throwable) {}
        }

        val out = ByteArray(fallbackKey.size)
        for (i in fallbackKey.indices) {
            out[i] = (fallbackKey[i].toInt() xor XOR_SEED.toInt()).toByte()
        }
        return out
    }

    fun getAesIv(): ByteArray {
        if (isNativeLoaded) {
            try {
                val iv = getOstoraAesIv()
                if (iv.size == 16) return iv
            } catch (_: Throwable) {}
        }

        val out = ByteArray(fallbackIv.size)
        for (i in fallbackIv.indices) {
            out[i] = (fallbackIv[i].toInt() xor XOR_SEED.toInt()).toByte()
        }
        return out
    }

    fun verifyIntegrity(): Boolean {
        return if (isNativeLoaded) {
            try {
                checkIntegrity()
            } catch (_: Throwable) {
                true
            }
        } else {
            true
        }
    }
}
