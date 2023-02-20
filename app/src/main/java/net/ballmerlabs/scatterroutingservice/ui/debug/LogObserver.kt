package net.ballmerlabs.scatterroutingservice.ui.debug

import android.os.FileObserver
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.ballmerlabs.uscatterbrain.util.logsDir
import net.ballmerlabs.uscatterbrain.util.scatterLog
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class LogStruct(
    val scope: String,
    val text: String
) {
    fun shortScope(): String {
        return SHORT_HEAD_REGEX.find(scope)?.value?:"invalid"
    }
}
const val HEAD_PATTERN = "^\\[.*\\]:"
val SHORT_HEAD_REGEX = "\\w+$".toRegex()
val HEAD_REGEX = HEAD_PATTERN.toRegex()
val TAIL_REGEX = "]:.*".toRegex()

fun getLogStruct(text: String): LogStruct {
    val scope = HEAD_REGEX.find(text)?.value?:"invalid"
    val tail = TAIL_REGEX.find(text)?.value?:"invalid"
    return LogStruct(
        scope = scope.slice(1..scope.length-3),
        text = tail.slice(2 until tail.length)
    )
}
@Singleton
class LogObserver @Inject constructor(
) {
    val logScope = CoroutineScope(SupervisorJob())
    val mappedLogs = ConcurrentHashMap<String, Pair<Long, SnapshotStateList<LogStruct>>>()
    val logger by scatterLog()
    val refreshLock = AtomicBoolean()
    private val observer by lazy {
        getLogObserver()!!
    }

    fun enableObserver() {
        observer.startWatching()
    }

    fun observeLogs(): LiveData<SnapshotStateList<LogStruct>> {
        val path = logger.getCurrentLog()
        return if (path != null) {
            postValue(path.name)
            logLiveData
        } else {
            logLiveData
        }
    }

    private fun postValue(path: String?) {
        logScope.launch(Dispatchers.IO) {
            val lock = refreshLock.getAndSet(true)
            if (!lock) {
                try {
                    if (path != null && path.isNotEmpty() && logLiveData.hasObservers()) {
                        val buf = mappedLogs.putIfAbsent(path, Pair(0, SnapshotStateList()))
                        val file = File(logsDir, path)
                        val reader = file.inputStream()
                        val channel = reader.channel
                        val buffered = reader.bufferedReader()
                        if (file.exists()) {
                            if (buf == null) {
                                val list = SnapshotStateList<LogStruct>()
                                for (x in buffered.lines()) {
                                    list.add(getLogStruct(x))
                                }
                                logLiveData.postValue(list)
                                mappedLogs[path] = Pair(channel.position(), list)

                            } else {
                                reader.skip(buf.first)
                                for (x in buffered.lines()) {
                                    buf.second.add(getLogStruct(x))
                                }
                                logLiveData.postValue(buf.second!!)
                                mappedLogs[path] = Pair(channel.position(), buf.second)
                            }
                        }
                        reader.close()
                        channel.close()
                    }
                } catch (exc: Exception) {
                    logger.e("exception in file logger refresh $exc")
                } finally {
                    refreshLock.set(false)
                }
            }
        }

    }

    private fun getLogObserver(): FileObserver? {
        val cache = logsDir
        if (cache != null) {
            return object : FileObserver(cache) {
                override fun onEvent(event: Int, path: String?) {
                    when (event) {
                        CLOSE_WRITE -> {
                            postValue(path)
                        }
                        OPEN -> {
                            postValue(path)
                        }
                        DELETE -> {
                            mappedLogs.remove(path)
                        }
                    }
                }
            }
        } else {
            return null
        }
    }

    val logLiveData = MutableLiveData<SnapshotStateList<LogStruct>>()
}