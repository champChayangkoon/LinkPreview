package com.chayangkoon.champ.linkpreview

import android.net.Uri
import androidx.arch.core.executor.ArchTaskExecutor
import com.chayangkoon.champ.linkpreview.common.*
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.nodes.Document
import java.util.concurrent.Executor


class LinkPreview private constructor(builder: Builder) {
    private val mimeImageTypeRegex: Regex by lazy { MIME_IMAGE_TYPE_PATTERN.toRegex() }
    private val mimeVideoTypeRegex: Regex by lazy { MIME_VIDEO_TYPE_PATTERN.toRegex() }
    private val lock = Any()
    private var linkPreviewExecutor: LinkPreviewExecutor? = null
    private var _jsoupUtils: JsoupUtils
    private var _descriptionLength = 20
    private var _loadExecutor: Executor? = null

    val jsoupUtils: JsoupUtils
        get() = _jsoupUtils

    val descriptionLength: Int
        get() = _descriptionLength

    constructor() : this(Builder())

    init {
        _jsoupUtils = builder.jsoupUtils ?: JsoupUtils()
        _descriptionLength = builder.descriptionLength
        _loadExecutor = builder.loadExecutor
    }

    inline fun loadPreview(
            previewUrl: String,
            crossinline onLoadLinkContentSuccess: (LinkContent) -> Unit,
            crossinline onLoadLinkContentFailed: (Throwable) -> Unit
    ) {
        val onLoadLinkContentListener = object : OnLoadLinkContentListener {
            override fun onLoadLinkContentSuccess(linkContent: LinkContent) {
                onLoadLinkContentSuccess(linkContent)
            }

            override fun onLoadLinkContentFailed(throwable: Throwable) {
                onLoadLinkContentFailed(throwable)
            }
        }
        loadPreview(previewUrl, onLoadLinkContentListener)
    }

    fun loadPreview(previewUrl: String, onLoadLinkContentListener: OnLoadLinkContentListener) {
        if (linkPreviewExecutor == null) {
            synchronized(lock) {
                if (linkPreviewExecutor == null) {
                    linkPreviewExecutor = DefaultLinkPreviewExecutor(_loadExecutor)
                }
            }
        }

        linkPreviewExecutor?.executeOnBackgroundThread(Runnable {
            try {
                val linkContent = loadPreview(previewUrl)
                linkPreviewExecutor?.executeOnMainThread(Runnable {
                    onLoadLinkContentListener.onLoadLinkContentSuccess(linkContent)
                })
            } catch (e: Exception) {
                linkPreviewExecutor?.executeOnMainThread(Runnable {
                    onLoadLinkContentListener.onLoadLinkContentFailed(e)
                })
            }
        })
    }

    fun loadPreview(previewUrl: String): LinkContent {
        val linkContent = LinkContent()
        val fullUrl = getFullUrl(previewUrl)
        if (fullUrl.isNotBlank()) {
            try {
                val document = _jsoupUtils.connectAndGetDocument(fullUrl)
                linkContent.fullUrl = if (document.location().isNotEmpty()) document.location() else fullUrl
                linkContent.canonicalUrl = getCanonicalUrl(linkContent.fullUrl)
                linkContent.htmlCode = document.toString()
                linkContent.metaTags = document.getMetaTags()
                linkContent.title = document.getTitle(linkContent.metaTags, linkContent.canonicalUrl)
                linkContent.description = document.getDescription(linkContent.metaTags)
                linkContent.imageUrl = document.getImageUrl(linkContent.metaTags, linkContent.canonicalUrl)
            } catch (e: UnsupportedMimeTypeException) {
                handleUnsupportedMimeTypeException(e, linkContent)
            }
        }
        return linkContent
    }

    fun cancel() {
        linkPreviewExecutor?.cancelOnBackgroundThread()
        linkPreviewExecutor?.cancelOnMainThread()
    }

    private fun getFullUrl(previewUrl: String): String {
        var fullUrl: String = previewUrl
        val lowerCasePreviewUrl = previewUrl.toLowerCase()
        if (!lowerCasePreviewUrl.startsWith(HTTP) && !lowerCasePreviewUrl.startsWith(HTTPS)) {
            fullUrl = HTTPS + previewUrl
        }
        return fullUrl
    }

    private fun getCanonicalUrl(fullUrl: String): String {
        var canonicalUrl = Uri.parse(fullUrl).host.orEmpty()
        if (canonicalUrl.toLowerCase().startsWith(WORLD_WIDE_WEB, true)) {
            canonicalUrl = canonicalUrl.substring(WORLD_WIDE_WEB.length)
        }
        return canonicalUrl
    }

    private fun handleUnsupportedMimeTypeException(e: UnsupportedMimeTypeException, linkContent: LinkContent) {
        linkContent.fullUrl = e.url
        linkContent.canonicalUrl = getCanonicalUrl(linkContent.fullUrl)
        linkContent.title = linkContent.canonicalUrl
        if (e.mimeType.matches(mimeImageTypeRegex) || e.mimeType.matches(mimeVideoTypeRegex)) {
            linkContent.imageUrl = linkContent.fullUrl
        }
    }

    private fun Document.getMetaTags(): Map<String, String> {
        val metaTagsMap = mutableMapOf<String, String>()
        val metaTagsElements = getElementsByTag(TAG_META)

        metaTagsElements.forEach {
            val property = it.attr(ATTRIBUTE_PROPERTY)
            val name = it.attr(ATTRIBUTE_NAME)
            val content = it.attr(ATTRIBUTE_CONTENT)

            if (property == KEY_OG_URL || name == KEY_URL) {
                metaTagsMap[KEY_URL] = content

            } else if (property == KEY_OG_TITLE || name == KEY_TITLE) {
                metaTagsMap[KEY_TITLE] = content

            } else if (property == KEY_OG_DESCRIPTION || name == KEY_DESCRIPTION) {
                metaTagsMap[KEY_DESCRIPTION] = content

            } else if (property == KEY_OG_IMAGE || name == KEY_IMAGE) {
                metaTagsMap[KEY_IMAGE] = content
            }
        }

        return metaTagsMap
    }

    private fun Document.getTitle(metaTags: Map<String, String>, canonicalUrl: String): String {
        var title = getMetaTagContent(metaTags, KEY_TITLE, TAG_TITLE)
        if (title.isEmpty()) title = canonicalUrl
        return title
    }

    private fun Document.getDescription(metaTags: Map<String, String>): String {
        var description = metaTags[KEY_DESCRIPTION].orEmpty()
        if (description.isEmpty()) {
            val paragraphContent = getFirstTagContent(TAG_PARAGRAPH, _descriptionLength)
            val spanContent = getFirstTagContent(TAG_SPAN, _descriptionLength)
            val divContent = getFirstTagContent(TAG_DIV, _descriptionLength)

            description = if (paragraphContent > spanContent && paragraphContent > divContent) {
                paragraphContent
            } else if (spanContent > paragraphContent && spanContent > divContent) {
                spanContent
            } else {
                divContent
            }
        }
        return description
    }

    private fun Document.getImageUrl(metaTags: Map<String, String>, canonicalUrl: String): String {
        var imageUrl = getMetaTagContent(metaTags, KEY_IMAGE, TAG_IMAGE)
        val lowerCaseImageUrl = imageUrl.toLowerCase()
        if (lowerCaseImageUrl.startsWith(DOUBLE_SEPARATOR)) {
            imageUrl = HTTPS + imageUrl.substring(DOUBLE_SEPARATOR.length)

        } else if (lowerCaseImageUrl.startsWith(SEPARATOR)) {
            imageUrl = HTTPS + WORLD_WIDE_WEB + canonicalUrl + imageUrl

        } else if (!lowerCaseImageUrl.startsWith(HTTP)
                && !lowerCaseImageUrl.startsWith(HTTPS)
                && !lowerCaseImageUrl.startsWith(WORLD_WIDE_WEB)) {
            imageUrl = HTTPS + WORLD_WIDE_WEB + canonicalUrl + SEPARATOR + imageUrl
        }
        return imageUrl
    }

    private fun Document.getMetaTagContent(metaTags: Map<String, String>, key: String, tag: String): String {
        var meta = metaTags[key].orEmpty()
        if (meta.isEmpty()) meta = getFirstTagContent(tag)
        return meta
    }

    private fun Document.getFirstTagContent(tag: String, contentLength: Int = 0): String {
        var tagContent = ""
        val tagElement = getElementsByTag(tag)
        if (tagElement.any()) tagElement.forEach {
            tagContent = if (tag != TAG_IMAGE) it.text() else it.attr(ATTRIBUTE_SRC)
            if (tagContent.isNotEmpty() && tagContent.length > contentLength) return tagContent
        }
        return tagContent
    }

    class Builder {
        var descriptionLength: Int = 30
            private set
        var jsoupUtils: JsoupUtils? = null
            private set
        var loadExecutor: Executor? = null
            private set

        fun descriptionLength(descriptionLength: Int): Builder {
            this.descriptionLength = descriptionLength
            return this
        }

        fun loadExecutor(loadExecutor: Executor): Builder {
            this.loadExecutor = loadExecutor
            return this
        }

        fun jSoupUtils(jsoupUtils: JsoupUtils): Builder {
            this.jsoupUtils = jsoupUtils
            return this
        }

        fun build(): LinkPreview {
            return LinkPreview(this)
        }
    }

    companion object {
        private const val SEPARATOR = "/"
        private const val DOUBLE_SEPARATOR = "//"
        private const val HTTPS = "https://"
        private const val HTTP = "http://"
        private const val WORLD_WIDE_WEB = "www."
        private const val MIME_IMAGE_TYPE_PATTERN = "^image\\/(jpg|jpeg|png|gif|webp|svg|bmp|tiff).{0}\$"
        private const val MIME_VIDEO_TYPE_PATTERN = "^video\\/(x-flv|mp4|MP2T|3gpp|quicktime|ogg|webm|x-msvideo|x-ms-wmv|x-m4v|ms-asf).{0}\$"
        private const val TAG_TITLE = "title"
        private const val TAG_META = "meta"
        private const val TAG_SPAN = "span"
        private const val TAG_DIV = "div"
        private const val TAG_PARAGRAPH = "p"
        private const val TAG_IMAGE = "img"
        private const val ATTRIBUTE_PROPERTY = "property"
        private const val ATTRIBUTE_NAME = "name"
        private const val ATTRIBUTE_CONTENT = "content"
        private const val ATTRIBUTE_SRC = "src"
        private const val KEY_OG_URL = "og:url"
        private const val KEY_OG_TITLE = "og:title"
        private const val KEY_OG_DESCRIPTION = "og:description"
        private const val KEY_OG_IMAGE = "og:image"
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_IMAGE = "image"
    }
}