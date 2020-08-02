package com.chayangkoon.champ.linkpreview.common

import com.google.common.truth.Truth
import io.mockk.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.After
import org.junit.Before
import org.junit.Test

class JsoupUtilsTest {

    @Before
    fun setup() {
        mockkStatic(Jsoup::class)
    }

    @After
    fun teardown() {
        unmockkStatic(Jsoup::class)
    }

    @Test
    fun `When set connect timeout and user agent should return from set value`() {
        val connectTimeOut = 10000
        val userAgent = "Edge/12.10136"
        val jsoupUtils = JsoupUtils.Builder()
                .connectTimeOut(connectTimeOut)
                .userAgent(userAgent)
                .build()


        Truth.assertThat(jsoupUtils.connectTimeOut).isEqualTo(connectTimeOut)
        Truth.assertThat(jsoupUtils.userAgent).isEqualTo(userAgent)
    }

    @Test
    fun `When connect with jsoup should return Connection`() {
        val jsoupUtils = JsoupUtils()
        val url = "www.test.com"
        val connectTimeOut = JsoupUtils.DEFAULT_CONNECT_TIMEOUT
        val userAgent = JsoupUtils.DEFAULT_USER_AGENT
        val connectionMock = mockk<Connection>()
        every { Jsoup.connect(url).timeout(connectTimeOut).userAgent(userAgent) } returns connectionMock

        val connection = jsoupUtils.connect("www.test.com")
        Truth.assertThat(connection).isSameAs(connectionMock)
    }

    @Test
    fun `When connect and get document with jsoup should return Document`() {
        val jsoupUtils = JsoupUtils()
        val url = "www.test.com"
        val connectTimeOut = JsoupUtils.DEFAULT_CONNECT_TIMEOUT
        val userAgent = JsoupUtils.DEFAULT_USER_AGENT
        val connectionMock = mockk<Connection>()
        val documentMock = mockk<Document>()
        every { Jsoup.connect(url).timeout(connectTimeOut).userAgent(userAgent) } returns connectionMock
        every { connectionMock.get() } returns documentMock

        val document = jsoupUtils.connectAndGetDocument(url)

        Truth.assertThat(document).isSameAs(documentMock)
    }
}