package com.smartagent.app.data

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

object ProductLinkResolver {
    fun resolve(rawUrl: String): Result<String> = runCatching {
        val trimmed = rawUrl.trim()
        val uri = URI(trimmed)
        require(uri.scheme == "https" || uri.scheme == "http") {
            "Enter a complete Shopee or TikTok link beginning with https://"
        }

        var currentUrl = trimmed
        repeat(MAX_REDIRECTS) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36"
                )
                setRequestProperty("Accept-Language", "en-MY,ms-MY;q=0.9,en;q=0.8")
            }

            try {
                val status = connection.responseCode
                val location = connection.getHeaderField("Location")
                if (status in 300..399 && !location.isNullOrBlank()) {
                    currentUrl = URL(URL(currentUrl), location).toString()
                } else {
                    return@runCatching connection.url.toString()
                }
            } finally {
                connection.disconnect()
            }
        }
        currentUrl
    }

    private const val MAX_REDIRECTS = 8
}
