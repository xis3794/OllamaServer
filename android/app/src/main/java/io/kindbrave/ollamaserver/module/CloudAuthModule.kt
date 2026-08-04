package io.kindbrave.ollamaserver.module

import android.content.Context
import android.util.Base64
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.interfaces.EdECPublicKey

/**
 * Ollama Cloud 设备认证：
 * 1. 生成 ed25519 密钥对（ollama CLI 的 initializeKeypair 逻辑，serve 不会自动生成）
 * 2. 私钥以 PKCS8 PEM 写入 <filesDir>/.ollama/id_ed25519（ollama 的 x/crypto/ssh 可解析）
 * 3. 公钥以 authorized_keys 格式写入 id_ed25519.pub
 * 4. 返回 base64url(公钥行)，用于拼接 ollama.com/connect 授权链接
 *    （https://ollama.com/connect?name=<设备名>&key=<base64url公钥>）
 * 授权后 ollama serve 转发 cloud 请求时携带设备签名（Authorization: <pubkey>:<sig>），
 * ollama.com 校验该设备已关联账号即可放行。
 */
class CloudAuthModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "CloudAuthModule"

    @ReactMethod
    fun ensureCloudKeypair(promise: Promise) {
        try {
            val encKey = ensureKeypair(reactContext)
            promise.resolve(encKey)
        } catch (e: Throwable) {
            promise.reject("KEYPAIR_ERROR", e)
        }
    }

    companion object {
        /** 确保密钥对存在，返回 base64url 编码的公钥行（用于 connect 链接） */
        fun ensureKeypair(context: Context): String {
            val ollamaDir = File(context.filesDir, ".ollama").apply { mkdirs() }
            val privFile = File(ollamaDir, "id_ed25519")
            val pubFile = File(ollamaDir, "id_ed25519.pub")
            if (privFile.exists() && pubFile.exists()) {
                // 已存在则复用（不能覆盖，否则 ollama.com 上已关联的公钥会失配）
                return rawUrlEncode(pubFile.readText().trim())
            }

            // 生成 Ed25519 密钥对（Android 8+ 支持）
            val kpg = KeyPairGenerator.getInstance("Ed25519")
            val kp = kpg.generateKeyPair()

            // 私钥：PKCS8 PEM（x/crypto/ssh 的 ParsePrivateKey 支持 "BEGIN PRIVATE KEY"）
            val pkcs8 = kp.private.encoded
            val pemBody = Base64.encodeToString(pkcs8, Base64.NO_WRAP)
                .chunked(64).joinToString("\n")
            val pem = "-----BEGIN PRIVATE KEY-----\n$pemBody\n-----END PRIVATE KEY-----\n"

            // 公钥：authorized_keys 格式 "ssh-ed25519 <base64>"
            val rawPub = rawEd25519PublicKey(kp.public as EdECPublicKey)
            val sshBlob = ByteArrayOutputStream().apply {
                writeLen("ssh-ed25519".toByteArray().size)
                write("ssh-ed25519".toByteArray())
                writeLen(rawPub.size)
                write(rawPub)
            }.toByteArray()
            val pubLine = "ssh-ed25519 " + Base64.encodeToString(sshBlob, Base64.NO_WRAP)

            privFile.writeText(pem)
            privFile.setReadable(true, true)  // 0600
            privFile.setWritable(true, true)
            pubFile.writeText(pubLine + "\n")
            return rawUrlEncode(pubLine)
        }

        private fun rawEd25519PublicKey(pub: EdECPublicKey): ByteArray {
            // y 坐标转 32 字节小端
            val yBytes = bigIntegerToLeBytes(pub.point.y, 32)
            // x 为奇偶位：置入小端最后一个字节的最高位
            if (pub.point.x) {
                yBytes[31] = (yBytes[31].toInt() or 0x80).toByte()
            }
            return yBytes
        }

        private fun bigIntegerToLeBytes(v: BigInteger, len: Int): ByteArray {
            val be = v.toByteArray() // big-endian（可能带符号位）
            val out = ByteArray(len)
            var src = be.size - 1
            var dst = 0
            while (src >= 0 && dst < len) {
                out[dst] = be[src]
                src--
                dst++
            }
            return out
        }

        private fun ByteArrayOutputStream.writeLen(n: Int) {
            write((n ushr 24) and 0xFF)
            write((n ushr 16) and 0xFF)
            write((n ushr 8) and 0xFF)
            write(n and 0xFF)
        }

        /** base64 URL 安全编码（对应 Go 的 base64.RawURLEncoding） */
        private fun rawUrlEncode(s: String): String =
            Base64.encodeToString(s.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
