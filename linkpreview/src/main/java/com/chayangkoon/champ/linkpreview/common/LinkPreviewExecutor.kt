package com.chayangkoon.champ.linkpreview.common

internal abstract class LinkPreviewExecutor {
    abstract fun executeOnBackgroundThread(runnable: Runnable)
    abstract fun cancelOnBackgroundThread()
    abstract fun postToMainThread(runnable: Runnable)
    open fun executeOnMainThread(runnable: Runnable) {
        if (isMainThread()) {
            runnable.run()
        } else {
            postToMainThread(runnable)
        }
    }
    abstract fun cancelOnMainThread()
    abstract fun isMainThread(): Boolean
}