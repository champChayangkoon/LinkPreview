package com.chayangkoon.champ.linkpreview.utils

internal object ResourcesUtils {
    const val WEB_TEST_1 = "web_test_1.html"
    const val WEB_TEST_2 = "web_test_2.html"
    const val WEB_TEST_3 = "web_test_3.html"

    fun getResources(fileName: String): String {
        return javaClass.classLoader?.let {
            it.getResourceAsStream(fileName)
                    .bufferedReader()
                    .use { reader ->
                        reader.readText()
                    }
        } ?: ""
    }
}