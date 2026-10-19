package com.example.campuslostfound

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID

/**
 * Item photo uploads on Supabase Storage (bucket "item-images") — replaces the
 * old ImgBB uploader. Returns the public URL of the uploaded image, or null on
 * failure (the caller then reports the item without a photo, same as before).
 */
class ImageRepository {

    private val supabase get() = SupabaseProvider.client

    suspend fun uploadImage(context: Context, uri: Uri): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes() }
                ?: return null

            // The resolver can return null (photo-picker / downloaded URIs), and
            // guessing the content type from the *file extension* produced
            // "image/jpg" — which is not a real media type. Resolve the MIME
            // first, then derive the extension from it.
            val resolvedMime = context.contentResolver.getType(uri)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
                )
            val mime = resolvedMime?.takeIf { it.startsWith("image/") } ?: "image/jpeg"

            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                ?: resolvedMime?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                ?: "jpg"

            val path = "items/${System.currentTimeMillis()}_${UUID.randomUUID()}.$ext"
            val bucket = supabase.storage.from("item-images")

            bucket.upload(path, bytes) {
                contentType = ContentType.parse(mime)
            }

            bucket.publicUrl(path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
