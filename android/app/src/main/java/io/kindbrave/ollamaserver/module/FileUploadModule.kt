package io.kindbrave.ollamaserver.module

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.net.toUri
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.kindbrave.ollamaserver.utils.Api.OLLAMA_SERVICE_URL
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.roundToInt

class FileUploadModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(
        reactContext
    ) {
    override fun getName(): String {
        return "FileUploadModule"
    }

    /**
     * 读取图片并压缩为 JPEG base64（用于视觉模型对话）
     * maxDimension: 最长边像素上限；quality: JPEG 质量 10~100
     */
    @ReactMethod
    fun readImageAsBase64(uriString: String?, maxDimension: Int, quality: Int, promise: Promise) {
        if (uriString == null) {
            promise.reject("INVALID_PARAMETERS", "uriString must be provided")
            return
        }
        try {
            val uri = uriString.toUri()
            val resolver = reactContext.contentResolver

            // 先读取图片尺寸（不加载像素）
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
                promise.reject("READ_IMAGE_ERROR", "Failed to decode image bounds")
                return
            }

            // 计算采样率，避免加载超大原图
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDimension * 2
                || boundsOpts.outHeight / sampleSize > maxDimension * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            var bitmap: Bitmap? = null
            resolver.openInputStream(uri)?.use { bitmap = BitmapFactory.decodeStream(it, null, decodeOpts) }
                ?: run {
                    promise.reject("READ_IMAGE_ERROR", "Cannot open image stream")
                    return
                }
            if (bitmap == null) {
                promise.reject("READ_IMAGE_ERROR", "Cannot decode image")
                return
            }

            // 精确缩放到 maxDimension 内
            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
                val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
                bitmap.recycle()
                bitmap = scaled
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            bitmap.recycle()
            promise.resolve(base64)
        } catch (e: Exception) {
            promise.reject("READ_IMAGE_ERROR", e)
        }
    }

    @ReactMethod
    fun uploadFile(uriString: String?, sha256: String?, promise: Promise) {
        if (uriString == null || sha256 == null) {
            promise.reject("INVALID_PARAMETERS", "uriString and sha256 must be provided")
            return
        }
        if (currentActivity == null) {
            promise.reject("INVALID_PARAMETERS", "currentActivity is null")
            return
        }

        val progressIndicator = LinearProgressIndicator(currentActivity!!)
        progressIndicator.apply {
            isIndeterminate = false
            max = 100
        }

        currentActivity!!.runOnUiThread {
            val dialogView = ConstraintLayout(currentActivity!!)
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            progressIndicator.id = View.generateViewId()
            dialogView.addView(progressIndicator, params)

            val constraintSet = ConstraintSet()
            constraintSet.clone(dialogView)
            constraintSet.connect(
                progressIndicator.id,
                ConstraintSet.START,
                ConstraintLayout.LayoutParams.PARENT_ID,
                ConstraintSet.START,
                16 // 左边距为16dp
            )
            constraintSet.connect(
                progressIndicator.id,
                ConstraintSet.END,
                ConstraintLayout.LayoutParams.PARENT_ID,
                ConstraintSet.END,
                16 // 右边距为16dp
            )
            constraintSet.applyTo(dialogView)
            
            val builder = MaterialAlertDialogBuilder(currentActivity!!).apply {
                setTitle("Uploading File")
                setMessage("Please wait...")
                setView(dialogView)
                setCancelable(false)
            }
            val dialog = builder.create()
            dialog.show()

            Thread(Runnable {
                try {
                    val uri = uriString.toUri()
                    val resolver = reactContext.contentResolver

                    // 创建OkHttp客户端
                    val client = OkHttpClient.Builder().build()

                    // 获取文件长度
                    val parcelFileDescriptor = resolver.openFileDescriptor(uri, "r")
                    val fileLength = parcelFileDescriptor!!.statSize
                    parcelFileDescriptor.close()

                    // 构建请求体
                    val requestBody: RequestBody = object : RequestBody() {
                        override fun contentType(): MediaType {
                            return "application/octet-stream".toMediaType()
                        }

                        @Throws(IOException::class)
                        override fun contentLength(): Long {
                            return fileLength
                        }

                        @Throws(IOException::class)
                        override fun writeTo(sink: BufferedSink) {
                            resolver.openInputStream(uri).use { input ->
                                val buffer = ByteArray(4096)
                                var read: Int
                                var uploaded = 0L
                                while ((input!!.read(buffer).also { read = it }) != -1) {
                                    sink.write(buffer, 0, read)
                                    uploaded += read.toLong()
                                    val progress = uploaded * 100.0 / fileLength
                                    currentActivity!!.runOnUiThread {
                                        progressIndicator.progress = progress.roundToInt()
                                    }
                                }
                            }
                        }
                    }

                    // 构建请求
                    val request = Request.Builder()
                        .url("$OLLAMA_SERVICE_URL/api/blobs/sha256:$sha256")
                        .post(requestBody)
                        .build()

                    // 执行请求
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        currentActivity!!.runOnUiThread {
                            dialog.dismiss()
                            promise.resolve(true)
                        }
                    } else {
                        currentActivity!!.runOnUiThread {
                            dialog.dismiss()
                            promise.reject("UPLOAD_FAILED", "Code: " + response.code)
                        }
                    }
                } catch (e: Exception) {
                    currentActivity!!.runOnUiThread {
                        dialog.dismiss()
                        promise.reject("UPLOAD_ERROR", e)
                    }
                }
            }).start()
        }
    }
}
