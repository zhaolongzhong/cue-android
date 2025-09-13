package ai.plusonelabs.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

@Singleton
class FileLogger @Inject constructor(
    private val context: Context,
) {
    private val logScope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getLogFile(): File {
        // Use Android's internal storage directory
        val logsDir = File(context.filesDir, "logs")
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }

        val fileName = "cue-${fileNameFormat.format(Date())}.log"
        return File(logsDir, fileName)
    }

    fun getLogFilePath(): String = getLogFile().absolutePath

    private fun writeToFile(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        logScope.launch {
            try {
                val logFile = getLogFile()
                val timestamp = dateFormat.format(Date())
                val logMessage = buildString {
                    append("[$timestamp] [${level.name}] [$tag] $message")
                    throwable?.let {
                        append("\n")
                        append(it.stackTraceToString())
                    }
                    append("\n")
                }

                FileWriter(logFile, true).use { writer ->
                    writer.append(logMessage)
                }
            } catch (e: Exception) {
                Log.e("FileLogger", "Failed to write to log file", e)
            }
        }
    }

    fun debug(tag: String, message: String) {
        val prefixedTag = "${LOG_TAG_PREFIX}$tag"
        Log.d(prefixedTag, message)
        writeToFile(LogLevel.DEBUG, prefixedTag, message)
    }

    fun info(tag: String, message: String) {
        val prefixedTag = "${LOG_TAG_PREFIX}$tag"
        Log.i(prefixedTag, message)
        writeToFile(LogLevel.INFO, prefixedTag, message)
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        val prefixedTag = "${LOG_TAG_PREFIX}$tag"
        Log.w(prefixedTag, message, throwable)
        writeToFile(LogLevel.WARN, prefixedTag, message, throwable)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val prefixedTag = "${LOG_TAG_PREFIX}$tag"
        Log.e(prefixedTag, message, throwable)
        writeToFile(LogLevel.ERROR, prefixedTag, message, throwable)
    }

    companion object {
        private const val TAG = "FileLogger"
        private const val LOG_TAG_PREFIX = "[AppLog]"

        // Convenience methods for when dependency injection is not available
        @Volatile
        private var instance: FileLogger? = null

        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = FileLogger(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): FileLogger = instance ?: throw IllegalStateException("FileLogger not initialized. Call initialize() first.")
    }
}

object AppLog {
    @JvmStatic
    fun d(tag: String?, msg: String): Int {
        val safeTag = tag ?: ""
        try {
            FileLogger.getInstance().debug(safeTag, msg)
        } catch (e: Exception) {
            Log.d(safeTag, msg)
        }
        return msg.length
    }

    @JvmStatic
    fun i(tag: String?, msg: String): Int {
        val safeTag = tag ?: ""
        try {
            FileLogger.getInstance().info(safeTag, msg)
        } catch (e: Exception) {
            Log.i(safeTag, msg)
        }
        return msg.length
    }

    @JvmStatic
    fun w(tag: String?, msg: String): Int {
        val safeTag = tag ?: ""
        try {
            FileLogger.getInstance().warn(safeTag, msg)
        } catch (e: Exception) {
            Log.w(safeTag, msg)
        }
        return msg.length
    }

    @JvmStatic
    fun w(tag: String?, msg: String, tr: Throwable?): Int {
        val safeTag = tag ?: ""
        try {
            FileLogger.getInstance().warn(safeTag, msg, tr)
        } catch (e: Exception) {
            Log.w(safeTag, msg, tr)
        }
        return msg.length
    }

    @JvmStatic
    fun e(tag: String?, msg: String): Int {
        val safeTag = tag ?: ""
        try {
            FileLogger.getInstance().error(safeTag, msg)
        } catch (e: Exception) {
            Log.e(safeTag, msg)
        }
        return msg.length
    }

    @JvmStatic
    fun e(tag: String?, msg: String, tr: Throwable?): Int {
        val safeTag = tag ?: ""
        try {
            FileLogger.getInstance().error(safeTag, msg, tr)
        } catch (e: Exception) {
            Log.e(safeTag, msg, tr)
        }
        return msg.length
    }
}
