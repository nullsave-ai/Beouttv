package com.beouttv.app.data.source.stream

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class SmartStreamDataSource(
    private val upstream: DataSource,
    private val customHeaders: Map<String, String> = emptyMap()
) : DataSource {

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        return upstream.open(dataSpec)
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        return upstream.read(buffer, offset, length)
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    @Throws(IOException::class)
    override fun close() {
        upstream.close()
    }

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val customHeaders: Map<String, String> = emptyMap()
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource {
            return SmartStreamDataSource(
                upstream = upstreamFactory.createDataSource(),
                customHeaders = customHeaders
            )
        }
    }
}
