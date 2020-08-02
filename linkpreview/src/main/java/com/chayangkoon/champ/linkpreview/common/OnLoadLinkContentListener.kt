package com.chayangkoon.champ.linkpreview.common

interface OnLoadLinkContentListener {
    fun onLoadLinkContentSuccess(linkContent: LinkContent)
    fun onLoadLinkContentFailed(throwable: Throwable)
}