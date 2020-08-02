package com.chayangkoon.champ.linkpreview.common

data class LinkContent(
        var fullUrl: String = "",
        var canonicalUrl: String = "",
        var htmlCode: String = "",
        var metaTags: Map<String, String> = mutableMapOf(),
        var title: String = "",
        var description: String = "",
        var imageUrl: String = ""
)