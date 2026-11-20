package com.example.unifiedcinema.network

import com.example.unifiedcinema.data.repository.ContentType
import java.net.URL
import java.net.URLDecoder
import java.util.regex.Pattern

class HlsPlaylistParser {

    companion object {
        private const val EXT_M3U = "#EXTM3U"
        private const val EXT_X_STREAM_INF = "#EXT-X-STREAM-INF"
        private const val EXT_X_KEY = "#EXT-X-KEY"
        private const val EXTINF = "#EXTINF"
        private const val EXT_X_ENDLIST = "#EXT-X-ENDLIST"
        
        private val URI_PATTERN = Pattern.compile("URI=\"([^\"]+)\"")
        private val SEGMENT_PATTERN = Pattern.compile("^[^#].+\\.ts.*$", Pattern.MULTILINE)
    }

    data class ParsedPlaylist(
        val isMaster: Boolean,
        val variants: List<Variant>,
        val segments: List<Segment>,
        val keys: List<KeyInfo>,
        val rawContent: String
    )

    data class Variant(
        val bandwidth: Long,
        val resolution: String?,
        val codecs: String?,
        val uri: String
    )

    data class Segment(
        val duration: Double,
        val title: String?,
        val uri: String,
        val byteRange: String?,
        val key: KeyInfo?
    )

    data class KeyInfo(
        val method: String,
        val uri: String?,
        val iv: String?,
        val keyFormat: String?,
        val keyFormatVersions: String?
    )

    fun parsePlaylist(content: String, baseUrl: String): ParsedPlaylist {
        val lines = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val variants = mutableListOf<Variant>()
        val segments = mutableListOf<Segment>()
        val keys = mutableListOf<KeyInfo>()
        
        var currentKey: KeyInfo? = null
        var currentExtInf: ExtInfInfo? = null
        var isMaster = false
        
        for (line in lines) {
            when {
                line == EXT_M3U -> continue
                line.startsWith(EXT_X_STREAM_INF) -> {
                    isMaster = true
                    currentExtInf = parseExtXStreamInf(line)
                }
                line.startsWith(EXT_X_KEY) -> {
                    currentKey = parseExtXKey(line, baseUrl)
                    keys.add(currentKey!!)
                }
                line.startsWith(EXTINF) -> {
                    currentExtInf = parseExtInf(line)
                }
                line.startsWith("#EXT-X-BYTERANGE") -> {
                    currentExtInf?.let { it.byteRange = line.substringAfter(":") }
                }
                line.startsWith("#") -> continue
                else -> {
                    if (isMaster && currentExtInf != null) {
                        val absoluteUri = resolveUrl(baseUrl, line)
                        variants.add(Variant(
                            bandwidth = currentExtInf.bandwidth ?: 0,
                            resolution = currentExtInf.resolution,
                            codecs = currentExtInf.codecs,
                            uri = absoluteUri
                        ))
                        currentExtInf = null
                    } else if (!isMaster) {
                        val absoluteUri = resolveUrl(baseUrl, line)
                        segments.add(Segment(
                            duration = currentExtInf?.duration ?: 0.0,
                            title = currentExtInf?.title,
                            uri = absoluteUri,
                            byteRange = currentExtInf?.byteRange,
                            key = currentKey
                        ))
                        currentExtInf = null
                    }
                }
            }
        }

        return ParsedPlaylist(
            isMaster = isMaster,
            variants = variants,
            segments = segments,
            keys = keys,
            rawContent = content
        )
    }

    private data class ExtInfInfo(
        var duration: Double? = null,
        var title: String? = null,
        var bandwidth: Long? = null,
        var resolution: String? = null,
        var codecs: String? = null,
        var byteRange: String? = null
    )

    private fun parseExtXStreamInf(line: String): ExtInfInfo {
        val info = ExtInfInfo()
        val attrs = line.substringAfter(":")
        parseAttributes(attrs) { key, value ->
            when (key) {
                "BANDWIDTH" -> info.bandwidth = value.toLongOrNull()
                "RESOLUTION" -> info.resolution = value
                "CODECS" -> info.codecs = value.replace("\"", "")
            }
        }
        return info
    }

    private fun parseExtInf(line: String): ExtInfInfo {
        val info = ExtInfInfo()
        val attrs = line.substringAfter(":")
        val parts = attrs.split(",", limit = 2)
        info.duration = parts[0].toDoubleOrNull() ?: 0.0
        if (parts.size > 1) {
            info.title = parts[1]
        }
        return info
    }

    private fun parseExtXKey(line: String, baseUrl: String): KeyInfo {
        val attrs = line.substringAfter(":")
        var method = "NONE"
        var uri: String? = null
        var iv: String? = null
        var keyFormat: String? = null
        var keyFormatVersions: String? = null
        
        parseAttributes(attrs) { key, value ->
            when (key) {
                "METHOD" -> method = value
                "URI" -> uri = resolveUrl(baseUrl, value.replace("\"", ""))
                "IV" -> iv = value
                "KEYFORMAT" -> keyFormat = value
                "KEYFORMATVERSIONS" -> keyFormatVersions = value
            }
        }
        
        return KeyInfo(method, uri, iv, keyFormat, keyFormatVersions)
    }

    private fun parseAttributes(attrs: String, onAttr: (String, String) -> Unit) {
        val pattern = Pattern.compile("([A-Z-]+)=(\"[^\"]*\"|[^,]+)")
        val matcher = pattern.matcher(attrs)
        while (matcher.find()) {
            val key = matcher.group(1)
            var value = matcher.group(2)
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length - 1)
            }
            onAttr(key, value)
        }
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl
        }
        try {
            val base = URL(baseUrl)
            val resolved = URL(base, relativeUrl)
            return resolved.toString()
        } catch (e: Exception) {
            return relativeUrl
        }
    }

    fun rewritePlaylistForProxy(
        content: String,
        baseUrl: String,
        host: String,
        contentType: ContentType,
        proxyRoute: String = "proxy"
    ): String {
        val parsed = parsePlaylist(content, baseUrl)
        val lines = content.split("\n")
        val output = mutableListOf<String>()
        
        var segmentIndex = 0
        var variantIndex = 0
        
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() || trimmed.startsWith("#") && !trimmed.startsWith(EXTINF) && !trimmed.startsWith(EXT_X_STREAM_INF) -> {
                    output.add(line)
                }
                trimmed.startsWith(EXT_X_STREAM_INF) -> {
                    output.add(line)
                    // Next line will be the URI
                }
                trimmed.startsWith(EXTINF) -> {
                    output.add(line)
                    // Next line will be the segment URI
                }
                !trimmed.startsWith("#") -> {
                    val absoluteUrl = resolveUrl(baseUrl, trimmed)
                    val encodedUrl = absoluteUrl.encodeUrl()
                    val route = if (absoluteUrl.contains(".m3u8")) "sub" else "chunk"
                    output.add("http://$host/$proxyRoute/$route?url=$encodedUrl&type=${contentType.name.lowercase()}")
                }
                else -> output.add(line)
            }
        }
        
        // Clean up duplicate EXT-M3U
        val result = output.joinToString("\n")
        val parts = result.split(EXT_M3U).filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) "$EXT_M3U\n${parts[0]}" else result
    }

    private fun String.encodeUrl(): String {
        return java.net.URLEncoder.encode(this, "UTF-8")
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("~", "%7E")
    }

    fun extractVideoIdFromM3u8(url: String): String? {
        val patterns = listOf(
            "newplaylist/([^/]+)/".toRegex(),
            "newplaylist_g/([^/]+)/".toRegex(),
            "([a-f0-9]{24,})".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun isMasterPlaylist(content: String): Boolean {
        return content.contains(EXT_X_STREAM_INF)
    }

    fun cleanMasterPlaylist(content: String): String {
        val parts = content.split(EXT_M3U).filter { it.isNotBlank() }
        return if (parts.isNotEmpty()) "$EXT_M3U\n${parts[0]}" else content
    }
}