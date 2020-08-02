package com.chayangkoon.champ.linkpreview.common

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.arch.core.executor.ArchTaskExecutor
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger


internal class DefaultLinkPreviewExecutor(loadExecutor: Executor? = null) : LinkPreviewExecutor() {
    private val lock = Any()
    private var backgroundThreadExecutor: Executor

    @Volatile
    private var mainHandler: Handler? = null
    private var runnable: Runnable? = null

    init {
        backgroundThreadExecutor = loadExecutor ?: Executors.newFixedThreadPool(2, object : ThreadFactory {
            private val mThreadId = AtomicInteger(0)

            override fun newThread(r: Runnable): Thread {
                return Thread(r).apply {
                    name = String.format(THREAD_NAME_STEM, mThreadId.getAndIncrement())
                }
            }
        })
    }

    override fun executeOnBackgroundThread(runnable: Runnable) {
        backgroundThreadExecutor.execute(runnable)
    }

    override fun cancelOnBackgroundThread() {
        if (backgroundThreadExecutor is ExecutorService) {
            val backgroundThreadExecutor = (backgroundThreadExecutor as ExecutorService)
            backgroundThreadExecutor.shutdown()
            try {
                if (!backgroundThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    backgroundThreadExecutor.shutdownNow()
                    if (!backgroundThreadExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        Log.e("cancelOnDiskIO", "Pool did not terminate")
                    }
                }
            } catch (ie: InterruptedException) {
                backgroundThreadExecutor.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
    }

    override fun postToMainThread(runnable: Runnable) {
        if (mainHandler == null) synchronized(lock) {
            if (mainHandler == null) {
                mainHandler = Handler(Looper.getMainLooper())
            }
        }
        this.runnable = runnable
        mainHandler?.post(runnable)
    }

    override fun cancelOnMainThread() {
        runnable?.let {
            mainHandler?.removeCallbacks(it)
            runnable = null
        }
    }

    override fun isMainThread(): Boolean {
        return Looper.getMainLooper().thread === Thread.currentThread()
    }

    companion object {
        private const val THREAD_NAME_STEM: String = "link_preview_disk_io_%d"
    }
}