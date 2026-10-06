package com.beouttv.app.data.storage

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Secure App-Internal Sandbox Streams Catalog Storage Loader.
 * Keeps `libbeout_streams.dat` securely inside the private package data directory:
 * `/data/data/<package_name>/files/libbeout_streams.dat`
 *
 * Features:
 * - Hidden from assets directory for security and protection against APK scrapers.
 * - Zero fallback to external storage.
 * - Instant initialization on first run via embedded secure in-memory vault.
 * - High-speed reading (< 1ms) directly from the isolated app data sandbox.
 */
object StreamsCatalogStorage {

    private const val TAG = "StreamsCatalogStorage"
    const val CATALOG_FILENAME = "libbeout_streams.dat"

    fun loadCatalogBytes(context: Context): ByteArray? {
        val internalFile = File(context.filesDir, CATALOG_FILENAME)

        // 1. Primary Source: App-internal secure data sandbox (/data/data/<package>/files/libbeout_streams.dat)
        try {
            if (internalFile.exists() && internalFile.length() == EmbeddedStreamsVault.EXPECTED_PAYLOAD_SIZE.toLong()) {
                val bytes = internalFile.readBytes()
                Log.d(TAG, "Loaded catalog from internal package sandbox: ${internalFile.absolutePath} (${bytes.size} bytes)")
                return bytes
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading internal sandbox file: ${e.message}")
        }

        // 2. First Run or Update: Deploy directly from embedded vault to internal package sandbox
        try {
            val vaultBytes = EmbeddedStreamsVault.getPayloadBytes()
            if (vaultBytes.isNotEmpty()) {
                internalFile.writeBytes(vaultBytes)
                Log.i(TAG, "Securely deployed catalog into package data sandbox: ${internalFile.absolutePath} (${vaultBytes.size} bytes)")
                return vaultBytes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed deploying from embedded vault: ${e.message}")
        }

        // 3. Fallback: Secondary internal storage locations (noBackupFilesDir or dataDir)
        try {
            val noBackupFile = File(context.noBackupFilesDir, CATALOG_FILENAME)
            if (noBackupFile.exists() && noBackupFile.length() == EmbeddedStreamsVault.EXPECTED_PAYLOAD_SIZE.toLong()) {
                return noBackupFile.readBytes()
            }
        } catch (_: Exception) {}

        try {
            val dataDir = context.applicationInfo.dataDir
            if (!dataDir.isNullOrBlank()) {
                val dataDirFile = File(dataDir, CATALOG_FILENAME)
                if (dataDirFile.exists() && dataDirFile.length() == EmbeddedStreamsVault.EXPECTED_PAYLOAD_SIZE.toLong()) {
                    return dataDirFile.readBytes()
                }
            }
        } catch (_: Exception) {}

        // 4. Return any non-empty existing sandbox file if present
        if (internalFile.exists() && internalFile.length() > 100) {
            return internalFile.readBytes()
        }

        return null
    }

    fun syncToInternalStorage(context: Context, bytes: ByteArray) {
        try {
            val target = File(context.filesDir, CATALOG_FILENAME)
            if (!target.exists() || target.length() != bytes.size.toLong()) {
                target.writeBytes(bytes)
                Log.d(TAG, "Synchronized catalog to internal filesDir: ${target.absolutePath}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync catalog to internal storage: ${e.message}")
        }
    }
}
