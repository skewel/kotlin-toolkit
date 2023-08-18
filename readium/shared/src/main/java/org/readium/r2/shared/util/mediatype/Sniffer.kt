/*
 * Copyright 2020 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.shared.util.mediatype

@Deprecated(message = "Use MediaTypeSniffer instead", level = DeprecationLevel.ERROR)
public typealias Sniffer = MediaTypeSniffer

/*

import android.webkit.MimeTypeMap
import java.io.File
import java.net.URLConnection
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.extensions.tryOrNull
import org.readium.r2.shared.publication.*
/**
 * Determines if the provided content matches a known media type.
 *
 * The context holds the file metadata and cached content, which are shared among the sniffers.
 */
public typealias Sniffer = suspend (context: SnifferContext) -> MediaType?

/**
 * Default media type sniffers provided by Readium.
 */
public object Sniffers {

    /**
     * The default sniffers provided by Readium 2 to resolve a [MediaType].
     * The sniffers order is important, because some formats are subsets of other formats.
     */
    public val all: List<Sniffer> = listOf(
        ::xhtml, ::html, ::opds, ::lcpLicense, ::bitmap, ::webpubManifest, ::webpub, ::w3cWPUB,
        ::epub, ::lpf, ::archive, ::pdf, ::json
    )

    /**
     * Sniffs an XHTML document.
     *
     * Must precede the HTML sniffer.
     */
    public suspend fun xhtml(context: SnifferContext): MediaType? {
        if (context.hasFileExtension("xht", "xhtml") || context.hasMediaType(
                "application/xhtml+xml"
            )
        ) {
            return MediaType.XHTML
        }

        if (context !is ResourceSnifferContext) {
            return null
        }

        context.contentAsXml()?.let {
            if (it.name.lowercase(Locale.ROOT) == "html" && it.namespace.lowercase(Locale.ROOT).contains(
                    "xhtml"
                )
            ) {
                return MediaType.XHTML
            }
        }
        return null
    }

    /** Sniffs an HTML document. */
    public suspend fun html(context: SnifferContext): MediaType? {
        if (context.hasFileExtension("htm", "html") || context.hasMediaType("text/html")) {
            return MediaType.HTML
        }

        if (context !is ResourceSnifferContext) {
            return null
        }

        // [contentAsXml] will fail if the HTML is not a proper XML document, hence the doctype check.
        if (
            context.contentAsXml()?.name?.lowercase(Locale.ROOT) == "html" ||
            context.contentAsString()?.trimStart()?.take(15)?.lowercase() == "<!doctype html>"
        ) {
            return MediaType.HTML
        }
        return null
    }

    /** Sniffs an OPDS document. */
    public suspend fun opds(context: SnifferContext): MediaType? {
        // OPDS 1
        if (context.hasMediaType("application/atom+xml;type=entry;profile=opds-catalog")) {
            return MediaType.OPDS1_ENTRY
        }
        if (context.hasMediaType("application/atom+xml;profile=opds-catalog")) {
            return MediaType.OPDS1
        }

        // OPDS 2
        if (context.hasMediaType("application/opds+json")) {
            return MediaType.OPDS2
        }
        if (context.hasMediaType("application/opds-publication+json")) {
            return MediaType.OPDS2_PUBLICATION
        }

        // OPDS Authentication Document.
        if (context.hasMediaType("application/opds-authentication+json") || context.hasMediaType(
                "application/vnd.opds.authentication.v1.0+json"
            )
        ) {
            return MediaType.OPDS_AUTHENTICATION
        }

        if (context !is ResourceSnifferContext) {
            return null
        }

        // OPDS 1
        context.contentAsXml()?.let { xml ->
            if (xml.namespace == "http://www.w3.org/2005/Atom") {
                if (xml.name == "feed") {
                    return MediaType.OPDS1
                } else if (xml.name == "entry") {
                    return MediaType.OPDS1_ENTRY
                }
            }
        }

        // OPDS 2
        context.contentAsRwpm()?.let { rwpm ->
            if (rwpm.linkWithRel("self")?.mediaType?.matches("application/opds+json") == true) {
                return MediaType.OPDS2
            }
            if (rwpm.links.firstWithRelMatching { it.startsWith("http://opds-spec.org/acquisition") } != null) {
                return MediaType.OPDS2_PUBLICATION
            }
        }

        // OPDS Authentication Document.
        if (context.containsJsonKeys("id", "title", "authentication")) {
            return MediaType.OPDS_AUTHENTICATION
        }

        return null
    }

    /** Sniffs an LCP License Document. */
    public suspend fun lcpLicense(context: SnifferContext): MediaType? {
        if (context.hasFileExtension("lcpl") || context.hasMediaType(
                "application/vnd.readium.lcp.license.v1.0+json"
            )
        ) {
            return MediaType.LCP_LICENSE_DOCUMENT
        }

        if (context !is ResourceSnifferContext) {
            return null
        }

        if (context.containsJsonKeys("id", "issued", "provider", "encryption")) {
            return MediaType.LCP_LICENSE_DOCUMENT
        }
        return null
    }

    /** Sniffs a bitmap image. */
    @Suppress("RedundantSuspendModifier")
    public suspend fun bitmap(context: SnifferContext): MediaType? {
        if (context.hasFileExtension("avif") || context.hasMediaType("image/avif")) {
            return MediaType.AVIF
        }
        if (context.hasFileExtension("bmp", "dib") || context.hasMediaType(
                "image/bmp",
                "image/x-bmp"
            )
        ) {
            return MediaType.BMP
        }
        if (context.hasFileExtension("gif") || context.hasMediaType("image/gif")) {
            return MediaType.GIF
        }
        if (context.hasFileExtension("jpg", "jpeg", "jpe", "jif", "jfif", "jfi") || context.hasMediaType(
                "image/jpeg"
            )
        ) {
            return MediaType.JPEG
        }
        if (context.hasFileExtension("jxl") || context.hasMediaType("image/jxl")) {
            return MediaType.JXL
        }
        if (context.hasFileExtension("png") || context.hasMediaType("image/png")) {
            return MediaType.PNG
        }
        if (context.hasFileExtension("tiff", "tif") || context.hasMediaType(
                "image/tiff",
                "image/tiff-fx"
            )
        ) {
            return MediaType.TIFF
        }
        if (context.hasFileExtension("webp") || context.hasMediaType("image/webp")) {
            return MediaType.WEBP
        }
        return null
    }

    /** Sniffs a Readium Web Manifest. */
    public suspend fun webpubManifest(context: SnifferContext): MediaType? {
        if (context.hasMediaType("application/audiobook+json")) {
            return MediaType.READIUM_AUDIOBOOK_MANIFEST
        }

        if (context.hasMediaType("application/divina+json")) {
            return MediaType.DIVINA_MANIFEST
        }

        if (context.hasMediaType("application/webpub+json")) {
            return MediaType.READIUM_WEBPUB_MANIFEST
        }

        if (context !is ResourceSnifferContext) {
            return null
        }

        val manifest: Manifest =
            context.contentAsRwpm() ?: return null

        if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
            return MediaType.READIUM_AUDIOBOOK_MANIFEST
        }

        if (manifest.conformsTo(Publication.Profile.DIVINA)) {
            return MediaType.DIVINA_MANIFEST
        }
        if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
            return MediaType.READIUM_WEBPUB_MANIFEST
        }

        return null
    }

    /** Sniffs a Readium Web Publication, protected or not by LCP. */
    public suspend fun webpub(context: SnifferContext): MediaType? {
        if (context.hasFileExtension("audiobook") || context.hasMediaType(
                "application/audiobook+zip"
            )
        ) {
            return MediaType.READIUM_AUDIOBOOK
        }

        if (context.hasFileExtension("divina") || context.hasMediaType("application/divina+zip")) {
            return MediaType.DIVINA
        }

        if (context.hasFileExtension("webpub") || context.hasMediaType("application/webpub+zip")) {
            return MediaType.READIUM_WEBPUB
        }

        if (context.hasFileExtension("lcpa") || context.hasMediaType("application/audiobook+lcp")) {
            return MediaType.LCP_PROTECTED_AUDIOBOOK
        }
        if (context.hasFileExtension("lcpdf") || context.hasMediaType("application/pdf+lcp")) {
            return MediaType.LCP_PROTECTED_PDF
        }

        if (context !is ContainerSnifferContext) {
            return null
        }

        // Reads a RWPM from a manifest.json archive entry.
        val manifest: Manifest? =
            try {
                context.readArchiveEntryAt("manifest.json")
                    ?.let { Manifest.fromJSON(JSONObject(String(it))) }
            } catch (e: Exception) {
                null
            }

        if (manifest != null) {
            val isLcpProtected = context.containsArchiveEntryAt("license.lcpl")

            if (manifest.conformsTo(Publication.Profile.AUDIOBOOK)) {
                return if (isLcpProtected) MediaType.LCP_PROTECTED_AUDIOBOOK else MediaType.READIUM_AUDIOBOOK
            }
            if (manifest.conformsTo(Publication.Profile.DIVINA)) {
                return MediaType.DIVINA
            }
            if (isLcpProtected && manifest.conformsTo(Publication.Profile.PDF)) {
                return MediaType.LCP_PROTECTED_PDF
            }
            if (manifest.linkWithRel("self")?.mediaType?.matches("application/webpub+json") == true) {
                return MediaType.READIUM_WEBPUB
            }
        }

        return null
    }

    /** Sniffs a W3C Web Publication Manifest. */
    public suspend fun w3cWPUB(context: SnifferContext): MediaType? {
        if (context !is ResourceSnifferContext) {
            return null
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@context`.
        val content = context.contentAsString() ?: ""
        if (content.contains("@context") && content.contains("https://www.w3.org/ns/wp-context")) {
            return MediaType.W3C_WPUB_MANIFEST
        }

        return null
    }

    /**
     * Sniffs an EPUB publication.
     *
     * Reference: https://www.w3.org/publishing/epub3/epub-ocf.html#sec-zip-container-mime
     */
    public suspend fun epub(context: SnifferContext): MediaType? {
        if (context.hasFileExtension("epub") || context.hasMediaType("application/epub+zip")) {
            return MediaType.EPUB
        }

        if (context !is ContainerSnifferContext) {
            return null
        }

        val mimetype = context.readArchiveEntryAt("mimetype")
            ?.let { String(it, charset = Charsets.US_ASCII).trim() }
        if (mimetype == "application/epub+zip") {
            return MediaType.EPUB
        }

        return null
    }

    /**
     * Sniffs a Lightweight Packaging Format (LPF).
     *
     * References:
     *  - https://www.w3.org/TR/lpf/
     *  - https://www.w3.org/TR/pub-manifest/
     */
    public suspend fun lpf(context: SnifferContext): MediaType? {
        if (context.hasFileExtension("lpf") || context.hasMediaType("application/lpf+zip")) {
            return MediaType.LPF
        }

        if (context !is ContainerSnifferContext) {
            return null
        }

        if (context.containsArchiveEntryAt("index.html")) {
            return MediaType.LPF
        }

        // Somehow, [JSONObject] can't access JSON-LD keys such as `@context`.
        context.readArchiveEntryAt("publication.json")
            ?.let { String(it) }
            ?.let { manifest ->
                if (manifest.contains("@context") && manifest.contains(
                        "https://www.w3.org/ns/pub-context"
                    )
                ) {
                    return MediaType.LPF
                }
            }

        return null
    }

    /**
     * Authorized extensions for resources in a CBZ archive.
     * Reference: https://wiki.mobileread.com/wiki/CBR_and_CBZ
     */
    private val CBZ_EXTENSIONS = listOf(
        // bitmap
        "bmp", "dib", "gif", "jif", "jfi", "jfif", "jpg", "jpeg", "png", "tif", "tiff", "webp",
        // metadata
        "acbf", "xml"
    )

    /**
     * Authorized extensions for resources in a ZAB archive (Zipped Audio Book).
     */
    private val ZAB_EXTENSIONS = listOf(
        // audio
        "aac", "aiff", "alac", "flac", "m4a", "m4b", "mp3", "ogg", "oga", "mogg", "opus", "wav", "webm",
        // playlist
        "asx", "bio", "m3u", "m3u8", "pla", "pls", "smil", "vlc", "wpl", "xspf", "zpl"
    )

    /**
     * Sniffs a simple Archive-based format, like Comic Book Archive or Zipped Audio Book.
     *
     * Reference: https://wiki.mobileread.com/wiki/CBR_and_CBZ
     */
    public suspend fun archive(context: SnifferContext): MediaType? {
        if (context.hasFileExtension("cbz") || context.hasMediaType(
                "application/vnd.comicbook+zip",
                "application/x-cbz",
                "application/x-cbr"
            )
        ) {
            return MediaType.CBZ
        }
        if (context.hasFileExtension("zab")) {
            return MediaType.ZAB
        }

        if (context !is ContainerSnifferContext) {
            return null
        }

        fun isIgnored(file: File): Boolean =
            file.name.startsWith(".") || file.name == "Thumbs.db"

        suspend fun archiveContainsOnlyExtensions(fileExtensions: List<String>): Boolean =
            context.archiveEntriesAllSatisfy { entry ->
                val file = File(entry.path)
                isIgnored(file) || fileExtensions.contains(file.extension.lowercase(Locale.ROOT))
            }

        if (archiveContainsOnlyExtensions(CBZ_EXTENSIONS)) {
            return MediaType.CBZ
        }
        if (archiveContainsOnlyExtensions(ZAB_EXTENSIONS)) {
            return MediaType.ZAB
        }

        return null
    }

    /**
     * Sniffs a PDF document.
     *
     * Reference: https://www.loc.gov/preservation/digital/formats/fdd/fdd000123.shtml
     */
    public suspend fun pdf(context: SnifferContext): MediaType? {
        if (context.hasFileExtension("pdf") || context.hasMediaType("application/pdf")) {
            return MediaType.PDF
        }

        if (context !is ResourceSnifferContext) {
            return null
        }

        if (context.read(0L until 5L)?.toString(Charsets.UTF_8) == "%PDF-") {
            return MediaType.PDF
        }

        return null
    }

    /** Sniffs a JSON document. */
    public suspend fun json(context: SnifferContext): MediaType? {
        if (context.hasMediaType("application/problem+json")) {
            return MediaType.JSON_PROBLEM_DETAILS
        }

        if (context !is ResourceSnifferContext) {
            return null
        }

        if (context.contentAsJson() != null) {
            return MediaType.JSON
        }
        return null
    }

    /**
     * Sniffs the system-wide registered media types using [MimeTypeMap] and
     * [URLConnection.guessContentTypeFromStream].
     */
    public suspend fun system(context: SnifferContext): MediaType? {
        val mimetypes = tryOrNull { MimeTypeMap.getSingleton() }
            ?: return null

        fun sniffExtension(extension: String): MediaType? {
            val type = mimetypes.getMimeTypeFromExtension(extension)
                ?: return null
            val preferredExtension = mimetypes.getExtensionFromMimeType(type)
                ?: return null
            return MediaType.parse(type, fileExtension = preferredExtension)
        }

        fun sniffType(type: String): MediaType? {
            val extension = mimetypes.getExtensionFromMimeType(type)
                ?: return null
            val preferredType = mimetypes.getMimeTypeFromExtension(extension)
                ?: return null
            return MediaType.parse(preferredType, fileExtension = extension)
        }

        for (mediaType in context.mediaTypes) {
            return sniffType(mediaType.toString()) ?: continue
        }

        for (extension in context.fileExtensions) {
            return sniffExtension(extension) ?: continue
        }

        if (context !is ResourceSnifferContext) {
            return null
        }

        return withContext(Dispatchers.IO) {
            context.contentAsStream()
                .let { URLConnection.guessContentTypeFromStream(it) }
                ?.let { sniffType(it) }
        }
    }
}

/**
 * Finds the first [Link] having a relation matching the given [predicate].
 */
private fun List<Link>.firstWithRelMatching(predicate: (String) -> Boolean): Link? =
    firstOrNull { it.rels.any(predicate) }


 */
