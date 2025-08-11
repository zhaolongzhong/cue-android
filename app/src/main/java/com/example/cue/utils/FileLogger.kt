package com.example.cue.utils

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
        Log.d(tag, message)
        writeToFile(LogLevel.DEBUG, tag, message)
    }

    fun info(tag: String, message: String) {
        Log.i(tag, message)
        writeToFile(LogLevel.INFO, tag, message)
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        writeToFile(LogLevel.WARN, tag, message, throwable)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeToFile(LogLevel.ERROR, tag, message, throwable)
    }

    companion object {
        private const val TAG = "FileLogger"

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

// AppLog object for simplified logging interface
object AppLog {
    /**
     * Log debug message. Automatically uses caller's class name as tag.
     */
    fun debug(message: String) {
        val tag = getCallerClassName()
        try {
            FileLogger.getInstance().debug(tag, message)
        } catch (e: Exception) {
            Log.d(tag, message)
        }
    }

    /**
     * Log info message. Automatically uses caller's class name as tag.
     */
    fun info(message: String) {
        val tag = getCallerClassName()
        try {
            FileLogger.getInstance().info(tag, message)
        } catch (e: Exception) {
            Log.i(tag, message)
        }
    }

    /**
     * Log warning message. Automatically uses caller's class name as tag.
     */
    fun warn(message: String, throwable: Throwable? = null) {
        val tag = getCallerClassName()
        try {
            FileLogger.getInstance().warn(tag, message, throwable)
        } catch (e: Exception) {
            Log.w(tag, message, throwable)
        }
    }

    /**
     * Log error message. Automatically uses caller's class name as tag.
     */
    fun error(message: String, throwable: Throwable? = null) {
        val tag = getCallerClassName()
        try {
            FileLogger.getInstance().error(tag, message, throwable)
        } catch (e: Exception) {
            Log.e(tag, message, throwable)
        }
    }

    /**
     * Get the class name of the caller for automatic tagging.
     */
    private fun getCallerClassName(): String = try {
        val stackTrace = Thread.currentThread().stackTrace
        // Find the first stack element that's not this class or Thread
        stackTrace.firstOrNull {
            !it.className.contains("FileLogger") &&
                !it.className.contains("Thread") &&
                !it.className.contains("VMStack")
        }?.let { element ->
            element.className.substringAfterLast('.')
        } ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}
