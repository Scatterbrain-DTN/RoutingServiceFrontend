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

@Singleton
class LogObserver @Inject constructor(
) {
    val logScope = CoroutineScope(SupervisorJob())
    val mappedLogs = ConcurrentHashMap<String, Pair<Long, SnapshotStateList<String>>>()
    val logger by scatterLog()
    val refreshLock = AtomicBoolean()
    private val observer by lazy {
        getLogObserver()!!
    }

    fun enableObserver() {
        observer.startWatching()
    }

    fun observeLogs(): LiveData<SnapshotStateList<String>> {
        val path = logger.getCurrentLog()
        return if (path != null) {
            Log.e("debug", "force reload ${path.name}")
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
                                val list = SnapshotStateList<String>()
                                for (x in buffered.lines()) {
                                    list.add(x)
                                }
                                Log.e("debug", "read new ${list.size}")
                                logLiveData.postValue(list)
                                mappedLogs[path] = Pair(channel.position(), list)

                            } else {
                                reader.skip(buf.first)
                                for (x in buffered.lines()) {
                                    buf.second.add(x)
                                }
                                Log.e("debug", "read old ${buf.second.size}")
                                logLiveData.postValue(buf.second)
                                mappedLogs[path] = Pair(channel.position(), buf.second)
                            }
                            reader.close()
                        }
                    }
                } catch (exc: Exception) {
                    Log.e("debug", "exception $exc in refresh ")
                } finally {
                    refreshLock.set(false)
                }
            }
        }

    }

    private fun getLogObserver(): FileObserver? {
        val cache = logsDir
        if (cache != null) {
            Log.e("debug", "observer initialized on ${cache.canonicalPath}")
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
            Log.e("debug", "observer null")
            return null
        }
    }

    val logLiveData = MutableLiveData<SnapshotStateList<String>>()
}