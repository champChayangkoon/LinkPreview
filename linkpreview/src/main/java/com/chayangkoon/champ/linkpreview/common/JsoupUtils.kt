package com.chayangkoon.champ.linkpreview.common

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class JsoupUtils private constructor(builder: Builder) {
    private var _connectTimeOut = DEFAULT_CONNECT_TIMEOUT
    val connectTimeOut: Int
        get() = _connectTimeOut

    private var _userAgent = DEFAULT_USER_AGENT
    val userAgent: String
        get() = _userAgent

    constructor() : this(Builder())

    init {
        _connectTimeOut = builder.connectTimeOut
        _userAgent = builder.userAgent
    }

    fun connect(url: String): Connection {
        return Jsoup.connect(url)
                .timeout(_connectTimeOut)
                .userAgent(_userAgent)
    }

    fun connectAndGetDocument(url: String): Document {
        return connect(url).get()
    }

    class Builder {
        var connectTimeOut = DEFAULT_CONNECT_TIMEOUT
            private set
        var userAgent = DEFAULT_USER_AGENT
            private set

        fun connectTimeOut(connectTimeOut: Int): Builder {
            this.connectTimeOut = connectTimeOut
            return this
        }

        fun userAgent(userAgent: String): Builder {
            this.userAgent = userAgent
            return this
        }

        fun build(): JsoupUtils {
            return JsoupUtils(this)
        }
    }

    companion object {
        const val DEFAULT_CONNECT_TIMEOUT = 30000
        const val DEFAULT_USER_AGENT = "Mozilla"
    }
}