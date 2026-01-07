package com.digswim.app.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.drawToBitmap
import com.digswim.app.ui.theme.DigSwimTheme
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

import android.content.Intent
import android.net.Uri

object ShareUtils {

    /**
     * Captures a bitmap from a Composable function using off-screen rendering.
     * This ensures the entire content is captured, even if it's not currently visible on screen.
     */
    suspend fun captureBitmapFromComposable(
        context: Context,
        content: @Composable () -> Unit
    ): Bitmap? {
        val activity = context as? Activity ?: return null
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return null

        return suspendCancellableCoroutine { continuation ->
            val view = ComposeView(context).apply {
                setContent {
                    DigSwimTheme {
                        content()
                    }
                }
            }

            // Wrap in a FrameLayout to control layout params more easily
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            
            // Add to root view but make it invisible or covered
            // We set alpha to 0 so it's not visible but still participates in layout/draw
            // visibility = View.INVISIBLE sometimes skips draw, so we use alpha
            view.alpha = 0f
            rootView.addView(view, layoutParams)

            val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (view.width > 0 && view.height > 0) {
                        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        
                        // Use post to ensure the frame is ready
                        view.post {
                            try {
                                // Measure with UNSPECIFIED height to allow expansion beyond screen bounds
                                val widthSpec = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY)
                                val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                                view.measure(widthSpec, heightSpec)
                                
                                // Layout the view with its full measured size
                                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                                
                                val bitmap = view.drawToBitmap()
                                rootView.removeView(view)
                                continuation.resume(bitmap)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                rootView.removeView(view)
                                continuation.resume(null)
                            }
                        }
                    }
                }
            }
            view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
        val filename = "${title}_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/DigSwim")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                return uri
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        } else {
            return null
        }
    }

    fun shareImage(context: Context, uri: Uri, packageName: String? = null, className: String? = null) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (packageName != null) {
                if (className != null) {
                    setClassName(packageName, className)
                } else {
                    setPackage(packageName)
                }
            }
        }

        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // Fallback to chooser if specific app not found or other error
            e.printStackTrace()
            val chooserIntent = Intent.createChooser(shareIntent, "分享到").apply {
                 // Create a clean intent for chooser without the specific package/component if it failed?
                 // Actually, if we set package/component on shareIntent and it failed, we should probably reset it before chooser.
                 // But createChooser takes a target intent.
            }
            // Better: recreate intent for chooser
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(genericIntent, "分享到"))
        }
    }
}
