package net.ballmerlabs.scatterroutingservice.ui.debug

import android.os.FileObserver
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ballmerlabs.uscatterbrain.util.LoggerImpl.Companion.LOGS_SIZE
import net.ballmerlabs.uscatterbrain.util.logsDir
import net.ballmerlabs.uscatterbrain.util.scatterLog
import java.io.File
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogObserver @Inject constructor() {
    val mappedLogs = ConcurrentHashMap<String, Pair<Long, SnapshotStateList<String>>>()
    val logger by scatterLog()
    private val observer by lazy {
        getLogObserver()!!
    }

    fun enableObserver() {
        observer.startWatching()
    }

    fun observeLogs(): LiveData<SnapshotStateList<String>> {
       val path = logger.getCurrentLog()
        return if (path != null) {
            postValue(path.name)
            logLiveData
        } else {
            logLiveData
        }
    }

    private fun postValue(path: String?) {
        if (path != null && path.isNotEmpty() && logLiveData.hasObservers() ) {
            val buf = mappedLogs[path]
            val file = File(logsDir, path)
            if (file.exists()) {
                val f = FileChannel.open(file.toPath(), StandardOpenOption.READ)
                if (buf == null) {
                    val readbuf = ByteBuffer.allocateDirect(f.size().toInt())
                    val read = f.read(readbuf)
                    readbuf.rewind()
                    Log.e("debug", "read new $read")
                    val s = StandardCharsets.UTF_8.decode(readbuf)
                    val list = SnapshotStateList<String>()
                    for(x in s.split("\n")) {
                        list.add(x)
                    }
                    logLiveData.postValue(list)
                    mappedLogs[path] = Pair(read.toLong(), list)

                } else {
                    val size = f.size() - buf.first
                    if (size > 0) {
                        val readbuf = ByteBuffer.allocateDirect(size.toInt())
                        val read = f.read(readbuf, buf.first)
                        readbuf.rewind()
                        Log.e("debug", "read old ${buf.first}")
                        for (x in readbuf.asCharBuffer().split("\n")) {
                            buf.second.add(x)
                        }
                        logLiveData.postValue(buf.second!!)
                        mappedLogs[path] = Pair(buf.first + read, buf.second)
                    }
                }
                f.close()
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

                        }
                    }
                }
            } } else {
            Log.e("debug", "observer null")
            return null
        }
    }

    val logLiveData = MutableLiveData<SnapshotStateList<String>>()
}