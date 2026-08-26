package com.smartagent.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.URL

data class ProductPageMetadata(
    val resolvedUrl: String,
    val text: String,
    val sourceLabel: String,
    val isProductSpecific: Boolean
)

object ProductPageMetadataClient {
    fun fetch(rawUrl: String): Result<ProductPageMetadata> = runCatching {
        val resolvedUrl = ProductLinkResolver.resolve(rawUrl).getOrDefault(rawUrl.trim())
        val host = URI(resolvedUrl).host.orEmpty().lowercase()

        if (host.endsWith("tiktok.com")) {
            fetchTikTokOEmbed(resolvedUrl)?.let { return@runCatching it }
        }

        fetchOpenGraph(resolvedUrl)?.let { return@runCatching it }
        error("The page did not expose public product metadata")
    }

    private fun fetchTikTokOEmbed(resolvedUrl: String): ProductPageMetadata? {
        val encodedUrl = URLEncoder.encode(resolvedUrl, Charsets.UTF_8.name())
        val endpoint = "https://www.tiktok.com/oembed?url=$encodedUrl"
        val response = readUrl(endpoint, acceptJson = true) ?: return null
        val json = runCatching { JSONObject(response) }.getOrNull() ?: return null
        val title = json.optString("title").trim()
        val author = json.optString("author_name").trim()
        if (title.isBlank() && author.isBlank()) return null

        val metadata = buildString {
            if (title.isNotBlank()) append("TikTok caption: ").append(title).append('\n')
            if (author.isNotBlank()) append("TikTok creator or seller: ").append(author).append('\n')
        }.trim()

        return ProductPageMetadata(
            resolvedUrl = resolvedUrl,
            text = metadata,
            sourceLabel = "TikTok public caption",
            isProductSpecific = false
        )
    }

    private fun fetchOpenGraph(resolvedUrl: String): ProductPageMetadata? {
        val html = readUrl(resolvedUrl, acceptJson = false) ?: return null
        val structuredProduct = findStructuredProduct(html)
        val title = findMeta(html, "og:title")
            .ifBlank { structuredProduct?.optString("name").orEmpty() }
            .ifBlank { findTitle(html) }
        val description = findMeta(html, "og:description")
            .ifBlank { structuredProduct?.optString("description").orEmpty() }
            .ifBlank { findMeta(html, "description") }
        val offers = structuredProduct?.let(::firstOffer)
        val price = findMeta(html, "product:price:amount")
            .ifBlank { offers?.optString("price").orEmpty() }
            .ifBlank { offers?.optString("lowPrice").orEmpty() }
        val currency = findMeta(html, "product:price:currency")
            .ifBlank { offers?.optString("priceCurrency").orEmpty() }
        val image = findMeta(html, "og:image")
            .ifBlank { structuredProduct?.let(::firstImage).orEmpty() }
        val brand = structuredProduct?.let(::brandName).orEmpty()
        val seller = offers?.optJSONObject("seller")?.optString("name").orEmpty()
        if (title.isBlank() && description.isBlank() && price.isBlank()) return null

        val metadata = buildString {
            if (title.isNotBlank()) append("Page title: ").append(title).append('\n')
            if (description.isNotBlank()) append("Page description: ").append(description).append('\n')
            if (price.isNotBlank()) {
                append("Page price: ").append(price)
                if (currency.isNotBlank()) append(' ').append(currency)
                append('\n')
            }
            if (brand.isNotBlank()) append("Brand: ").append(brand).append('\n')
            if (seller.isNotBlank()) append("Seller: ").append(seller).append('\n')
            if (image.isNotBlank()) append("Product image: ").append(image).append('\n')
        }.trim()

        return ProductPageMetadata(
            resolvedUrl = resolvedUrl,
            text = metadata,
            sourceLabel = if (structuredProduct != null) {
                "product structured data and public page metadata"
            } else {
                "public page metadata"
            },
            isProductSpecific = structuredProduct != null || price.isNotBlank()
        )
    }

    private fun findStructuredProduct(html: String): JSONObject? {
        for (match in JSON_LD.findAll(html)) {
            val raw = decodeHtml(match.groupValues[1]).trim()
            val value = runCatching { org.json.JSONTokener(raw).nextValue() }.getOrNull() ?: continue
            findProductObject(value)?.let { return it }
        }
        return null
    }

    private fun findProductObject(value: Any?): JSONObject? {
        return when (value) {
            is JSONObject -> {
                val type = value.opt("@type")
                val isProduct = when (type) {
                    is String -> type.equals("Product", ignoreCase = true)
                    is JSONArray -> (0 until type.length()).any {
                        type.optString(it).equals("Product", ignoreCase = true)
                    }
                    else -> false
                }
                if (isProduct) value else findProductObject(value.optJSONArray("@graph"))
            }
            is JSONArray -> {
                for (index in 0 until value.length()) {
                    val product = findProductObject(value.opt(index))
                    if (product != null) return product
                }
                null
            }
            else -> null
        }
    }

    private fun firstOffer(product: JSONObject): JSONObject? = when (val offers = product.opt("offers")) {
        is JSONObject -> offers
        is JSONArray -> offers.optJSONObject(0)
        else -> null
    }

    private fun firstImage(product: JSONObject): String = when (val image = product.opt("image")) {
        is String -> image
        is JSONArray -> image.optString(0)
        is JSONObject -> image.optString("url")
        else -> ""
    }

    private fun brandName(product: JSONObject): String = when (val brand = product.opt("brand")) {
        is String -> brand
        is JSONObject -> brand.optString("name")
        else -> ""
    }

    private fun readUrl(url: String, acceptJson: Boolean): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 20_000
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36"
            )
            setRequestProperty("Accept-Language", "en-MY,ms-MY;q=0.9,en;q=0.8")
            setRequestProperty("Accept", if (acceptJson) "application/json" else "text/html,application/xhtml+xml")
        }

        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { reader ->
                val buffer = CharArray(8192)
                val output = StringBuilder()
                while (output.length < MAX_RESPONSE_CHARS) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, MAX_RESPONSE_CHARS - output.length))
                    if (count < 0) break
                    output.append(buffer, 0, count)
                }
                output.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun findMeta(html: String, key: String): String {
        val tags = META_TAG.findAll(html)
        for (match in tags) {
            val tag = match.value
            val property = ATTRIBUTE.findAll(tag).associate {
                it.groupValues[1].lowercase() to it.groupValues[3]
            }
            val name = property["property"] ?: property["name"]
            if (name.equals(key, ignoreCase = true)) {
                return decodeHtml(property["content"].orEmpty())
            }
        }
        return ""
    }

    private fun findTitle(html: String): String = TITLE.find(html)
        ?.groupValues
        ?.getOrNull(1)
        ?.let(::decodeHtml)
        .orEmpty()

    private fun decodeHtml(value: String): String = value
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace("&lt;", "<", ignoreCase = true)
        .replace("&gt;", ">", ignoreCase = true)
        .replace(Regex("\\s+"), " ")
        .trim()

    private val META_TAG = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ATTRIBUTE = Regex("([a-zA-Z_:.-]+)\\s*=\\s*(['\"])(.*?)\\2", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val TITLE = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val JSON_LD = Regex(
        "<script[^>]*type\\s*=\\s*['\"]application/ld\\+json['\"][^>]*>(.*?)</script>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private const val MAX_RESPONSE_CHARS = 1_000_000
}
