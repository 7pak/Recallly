package com.at.recallly.data.whisper

import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.concurrent.Executors

private const val LOG_TAG = "WhisperContext"

class WhisperContext private constructor(private var ptr: Long) {

    private val scope: CoroutineScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    suspend fun transcribeData(data: FloatArray): String = withContext(scope.coroutineContext) {
        require(ptr != 0L) { "WhisperContext has been released" }
        val numThreads = preferredThreadCount()
        Log.d(LOG_TAG, "Transcribing ${data.size} samples with $numThreads threads")
        WhisperLib.fullTranscribe(ptr, numThreads, data)
        val textCount = WhisperLib.getTextSegmentCount(ptr)
        buildString {
            for (i in 0 until textCount) {
                append(WhisperLib.getTextSegment(ptr, i))
            }
        }.trim()
    }

    suspend fun release() = withContext(scope.coroutineContext) {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0
        }
    }

    companion object {
        fun createFromFile(filePath: String): WhisperContext {
            val ptr = WhisperLib.initContext(filePath)
            if (ptr == 0L) {
                throw RuntimeException("Failed to create WhisperContext from $filePath")
            }
            return WhisperContext(ptr)
        }

        fun getSystemInfo(): String = WhisperLib.getSystemInfo()
    }
}

private class WhisperLib {
    companion object {
        init {
            Log.d(LOG_TAG, "Primary ABI: ${Build.SUPPORTED_ABIS[0]}")
            var loadVfpv4 = false
            var loadV8fp16 = false
            if (Build.SUPPORTED_ABIS[0] == "armeabi-v7a") {
                val cpuInfo = readCpuInfo()
                if (cpuInfo?.contains("vfpv4") == true) {
                    loadVfpv4 = true
                }
            } else if (Build.SUPPORTED_ABIS[0] == "arm64-v8a") {
                val cpuInfo = readCpuInfo()
                if (cpuInfo?.contains("fphp") == true) {
                    loadV8fp16 = true
                }
            }

            when {
                loadVfpv4 -> {
                    Log.d(LOG_TAG, "Loading libwhisper_vfpv4.so")
                    System.loadLibrary("whisper_vfpv4")
                }
                loadV8fp16 -> {
                    Log.d(LOG_TAG, "Loading libwhisper_v8fp16_va.so")
                    System.loadLibrary("whisper_v8fp16_va")
                }
                else -> {
                    Log.d(LOG_TAG, "Loading libwhisper.so")
                    System.loadLibrary("whisper")
                }
            }
        }

        private fun readCpuInfo(): String? = try {
            File("/proc/cpuinfo").inputStream().bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Couldn't read /proc/cpuinfo", e)
            null
        }

        external fun initContext(modelPath: String): Long
        external fun freeContext(contextPtr: Long)
        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray)
        external fun getTextSegmentCount(contextPtr: Long): Int
        external fun getTextSegment(contextPtr: Long, index: Int): String
        external fun getSystemInfo(): String
    }
}

private fun preferredThreadCount(): Int = try {
    val lines = BufferedReader(FileReader("/proc/cpuinfo")).useLines { it.toList() }
    val cpuFreqs = lines.asSequence()
        .filter { it.startsWith("processor") }
        .map { it.substringAfter(':').trim().toInt() }
        .map { idx ->
            try {
                BufferedReader(FileReader("/sys/devices/system/cpu/cpu$idx/cpufreq/cpuinfo_max_freq"))
                    .use { it.readLine().toInt() }
            } catch (_: Exception) { 0 }
        }
        .sorted()
        .toList()
    val min = cpuFreqs.minOrNull() ?: 0
    cpuFreqs.count { it > min }.coerceAtLeast(2)
} catch (_: Exception) {
    (Runtime.getRuntime().availableProcessors() - 4).coerceAtLeast(2)
}
