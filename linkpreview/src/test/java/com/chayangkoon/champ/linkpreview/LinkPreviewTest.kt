package com.chayangkoon.champ.linkpreview

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chayangkoon.champ.linkpreview.LinkPreview.Companion.KEY_DESCRIPTION
import com.chayangkoon.champ.linkpreview.LinkPreview.Companion.KEY_IMAGE
import com.chayangkoon.champ.linkpreview.LinkPreview.Companion.KEY_TITLE
import com.chayangkoon.champ.linkpreview.LinkPreview.Companion.KEY_URL
import com.chayangkoon.champ.linkpreview.common.JsoupUtils
import com.chayangkoon.champ.linkpreview.common.LinkContent
import com.chayangkoon.champ.linkpreview.common.OnLoadLinkContentListener
import com.chayangkoon.champ.linkpreview.utils.MainCoroutineRule
import com.chayangkoon.champ.linkpreview.utils.ResourcesUtils
import com.chayangkoon.champ.linkpreview.utils.ResourcesUtils.WEB_TEST_1
import com.chayangkoon.champ.linkpreview.utils.ResourcesUtils.WEB_TEST_2
import com.chayangkoon.champ.linkpreview.utils.ResourcesUtils.WEB_TEST_3
import com.google.common.truth.Truth
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.UnknownHostException
import java.util.concurrent.Executor
import kotlin.test.assertFailsWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.P])
internal class LinkPreviewTest {
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @MockK
    private lateinit var jsoupUtils: JsoupUtils

    private val loadExecutor = Executor { it.run() }
    private lateinit var linkPreview: LinkPreview

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        linkPreview = LinkPreview.Builder()
                .jSoupUtils(jsoupUtils)
                .loadExecutor(loadExecutor)
                .build()
    }

    @Test
    fun `When load preview asynchronous success should return link content`() {
        val url = "www.web_test_1.com"
        val fullUrl = "https://www.web_test_1.com"
        val htmlCode = ResourcesUtils.getResources(WEB_TEST_1)
        val document = Jsoup.parse(htmlCode)
        every { jsoupUtils.connectAndGetDocument(fullUrl) } returns document

        linkPreview.loadPreview(url, {
            Truth.assertThat(it.htmlCode).isEqualTo(document.toString())
            Truth.assertThat(it.fullUrl).isEqualTo("https://www.web_test_1.com")
            Truth.assertThat(it.canonicalUrl).isEqualTo("web_test_1.com")
            Truth.assertThat(it.title).isEqualTo("Web test")
            Truth.assertThat(it.description).isEqualTo("Welcome to first web test")
            Truth.assertThat(it.imageUrl).isEqualTo("https://www.web_test_1.com/test.jpg")

            val expectedMetaTagMap = mutableMapOf<String, String>()
            expectedMetaTagMap[KEY_URL] = "https://www.web_test_1.com"
            expectedMetaTagMap[KEY_TITLE] = "Web test"
            expectedMetaTagMap[KEY_IMAGE] = "https://www.web_test_1.com/test.jpg"
            expectedMetaTagMap[KEY_DESCRIPTION] = "Welcome to first web test"
            Truth.assertThat(it.metaTags).isEqualTo(expectedMetaTagMap)
        }, {

        })
    }

    @Test
    fun `When load preview synchronous success should return link content`() = mainCoroutineRule.runBlockingTest {
        val url = "www.web_test_1.com"
        val fullUrl = "https://www.web_test_1.com"
        val htmlCode = ResourcesUtils.getResources(WEB_TEST_1)
        val document = Jsoup.parse(htmlCode)
        every { jsoupUtils.connectAndGetDocument(fullUrl) } returns document

        val linkContent = linkPreview.loadPreview(url)

        Truth.assertThat(linkContent.htmlCode).isEqualTo(document.toString())
        Truth.assertThat(linkContent.fullUrl).isEqualTo("https://www.web_test_1.com")
        Truth.assertThat(linkContent.canonicalUrl).isEqualTo("web_test_1.com")
        Truth.assertThat(linkContent.title).isEqualTo("Web test")
        Truth.assertThat(linkContent.description).isEqualTo("Welcome to first web test")
        Truth.assertThat(linkContent.imageUrl).isEqualTo("https://www.web_test_1.com/test.jpg")

        val expectedMetaTagMap = mutableMapOf<String, String>()
        expectedMetaTagMap[KEY_URL] = "https://www.web_test_1.com"
        expectedMetaTagMap[KEY_TITLE] = "Web test"
        expectedMetaTagMap[KEY_IMAGE] = "https://www.web_test_1.com/test.jpg"
        expectedMetaTagMap[KEY_DESCRIPTION] = "Welcome to first web test"
        Truth.assertThat(linkContent.metaTags).isEqualTo(expectedMetaTagMap)
    }

    @Test
    fun `When load preview asynchronous success with case two should return link content`() {
        val url = "www.web_test_2.com"
        val fullUrl = "https://www.web_test_2.com"
        val htmlCode = ResourcesUtils.getResources(WEB_TEST_2)
        val document = Jsoup.parse(htmlCode)
        every { jsoupUtils.connectAndGetDocument(fullUrl) } returns document

        linkPreview.loadPreview(url, object : OnLoadLinkContentListener {
            override fun onLoadLinkContentSuccess(linkContent: LinkContent) {
                Truth.assertThat(linkContent.htmlCode).isEqualTo(document.toString())
                Truth.assertThat(linkContent.fullUrl).isEqualTo("https://www.web_test_2.com")
                Truth.assertThat(linkContent.canonicalUrl).isEqualTo("web_test_2.com")
                Truth.assertThat(linkContent.title).isEqualTo("Hi web test 2")
                Truth.assertThat(linkContent.description).isEqualTo("Welcome to second web test")
                Truth.assertThat(linkContent.imageUrl).isEqualTo("https://www.web_test_2.com/test2.jpg")

                val expectedMetaTagMap = mutableMapOf<String, String>()
                expectedMetaTagMap[KEY_URL] = "https://www.web_test_2.com"
                expectedMetaTagMap[KEY_IMAGE] = "/test2.jpg"
                expectedMetaTagMap[KEY_DESCRIPTION] = "Welcome to second web test"
                Truth.assertThat(linkContent.metaTags).isEqualTo(expectedMetaTagMap)
            }

            override fun onLoadLinkContentFailed(throwable: Throwable) {}
        })
    }

    @Test
    fun `When load preview synchronous success with case two should return link content`() = mainCoroutineRule.runBlockingTest {
        val url = "www.web_test_2.com"
        val fullUrl = "https://www.web_test_2.com"
        val htmlCode = ResourcesUtils.getResources(WEB_TEST_2)
        val document = Jsoup.parse(htmlCode)
        every { jsoupUtils.connectAndGetDocument(fullUrl) } returns document

        val linkContent = linkPreview.loadPreview(url)

        Truth.assertThat(linkContent.htmlCode).isEqualTo(document.toString())
        Truth.assertThat(linkContent.fullUrl).isEqualTo("https://www.web_test_2.com")
        Truth.assertThat(linkContent.canonicalUrl).isEqualTo("web_test_2.com")
        Truth.assertThat(linkContent.title).isEqualTo("Hi web test 2")
        Truth.assertThat(linkContent.description).isEqualTo("Welcome to second web test")
        Truth.assertThat(linkContent.imageUrl).isEqualTo("https://www.web_test_2.com/test2.jpg")

        val expectedMetaTagMap = mutableMapOf<String, String>()
        expectedMetaTagMap[KEY_URL] = "https://www.web_test_2.com"
        expectedMetaTagMap[KEY_IMAGE] = "/test2.jpg"
        expectedMetaTagMap[KEY_DESCRIPTION] = "Welcome to second web test"
        Truth.assertThat(linkContent.metaTags).isEqualTo(expectedMetaTagMap)
    }

    @Test
    fun `When load preview synchronous success with case three should return link content`() = mainCoroutineRule.runBlockingTest {
        val url = "www.web_test_3.com"
        val fullUrl = "https://www.web_test_3.com"
        val htmlCode = ResourcesUtils.getResources(WEB_TEST_3)
        val document = Jsoup.parse(htmlCode)
        every { jsoupUtils.connectAndGetDocument(fullUrl) } returns document

        val linkContent = linkPreview.loadPreview(url)

        Truth.assertThat(linkContent.htmlCode).isEqualTo(document.toString())
        Truth.assertThat(linkContent.fullUrl).isEqualTo("https://www.web_test_3.com")
        Truth.assertThat(linkContent.canonicalUrl).isEqualTo("web_test_3.com")
        Truth.assertThat(linkContent.title).isEqualTo("Hi web test 3")
        Truth.assertThat(linkContent.description).isEqualTo("Welcome to third web test, for test link preview only, I hope I would pass the test")
        Truth.assertThat(linkContent.imageUrl).isEqualTo("https://www.web_test_3.com/test3.jpg")

        val expectedMetaTagMap = mutableMapOf<String, String>()
        expectedMetaTagMap[KEY_URL] = "https://www.web_test_3.com"
        expectedMetaTagMap[KEY_IMAGE] = "//www.web_test_3.com/test3.jpg"
        Truth.assertThat(linkContent.metaTags).isEqualTo(expectedMetaTagMap)
    }

    @Test
    fun `When load preview synchronous with image link success should return link content`() = mainCoroutineRule.runBlockingTest {
        val url = "www.test.com/test.jpg"
        val fullUrl = "https://www.test.com/test.jpg"
        val unsupportedMimeTypeException = UnsupportedMimeTypeException(
                "Unhandled content type. Must be text/*, application/xml, or application/*+xml",
                "image/jpeg",
                "https://www.test.com/test.jpg"
        )
        every { jsoupUtils.connectAndGetDocument(fullUrl) } throws unsupportedMimeTypeException

        val linkContent = linkPreview.loadPreview(url)

        Truth.assertThat(linkContent.htmlCode).isEqualTo("")
        Truth.assertThat(linkContent.fullUrl).isEqualTo("https://www.test.com/test.jpg")
        Truth.assertThat(linkContent.canonicalUrl).isEqualTo("test.com")
        Truth.assertThat(linkContent.title).isEqualTo("test.com")
        Truth.assertThat(linkContent.description).isEqualTo("")
        Truth.assertThat(linkContent.imageUrl).isEqualTo("https://www.test.com/test.jpg")
        Truth.assertThat(linkContent.metaTags).isEqualTo(emptyMap<String, String>())

    }

    @Test
    fun `When load preview synchronous with video link success should return link content`() = mainCoroutineRule.runBlockingTest {
        val url = "www.test.com/test.mp4"
        val fullUrl = "https://www.test.com/test.mp4"
        val unsupportedMimeTypeException = UnsupportedMimeTypeException(
                "Unhandled content type. Must be text/*, application/xml, or application/*+xml",
                "video/mp4",
                "https://www.test.com/test.mp4"
        )
        every { jsoupUtils.connectAndGetDocument(fullUrl) } throws unsupportedMimeTypeException

        val linkContent = linkPreview.loadPreview(url)

        Truth.assertThat(linkContent.htmlCode).isEqualTo("")
        Truth.assertThat(linkContent.fullUrl).isEqualTo("https://www.test.com/test.mp4")
        Truth.assertThat(linkContent.canonicalUrl).isEqualTo("test.com")
        Truth.assertThat(linkContent.title).isEqualTo("test.com")
        Truth.assertThat(linkContent.description).isEqualTo("")
        Truth.assertThat(linkContent.imageUrl).isEqualTo("https://www.test.com/test.mp4")
        Truth.assertThat(linkContent.metaTags).isEqualTo(emptyMap<String, String>())
    }

    @Test
    fun `When load preview asynchronous failed should throw exception`()  {
        val url = "test"
        val fullUrl = "https://test"
        val errorMessage = "Unable to resolve host $url : No address associated with hostname"
        every { jsoupUtils.connectAndGetDocument(fullUrl) } throws UnknownHostException(errorMessage)

        linkPreview.loadPreview(url, {

        }, {
            Truth.assertThat(it).isInstanceOf(UnknownHostException::class.java)
            Truth.assertThat(it.message).isEqualTo(errorMessage)
        })
    }

    @Test
    fun `When load preview synchronous failed should throw exception`() = mainCoroutineRule.runBlockingTest {
        val url = "test"
        val fullUrl = "https://test"
        val errorMessage = "Unable to resolve host $url : No address associated with hostname"
        every { jsoupUtils.connectAndGetDocument(fullUrl) } throws UnknownHostException(errorMessage)

        val actualException = assertFailsWith<UnknownHostException> {
            linkPreview.loadPreview(url)
        }

        Truth.assertThat(actualException.message).isEqualTo(errorMessage)
    }
}