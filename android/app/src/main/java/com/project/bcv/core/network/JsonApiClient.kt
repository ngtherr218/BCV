package com.project.bcv.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.project.bcv.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException

/** Shared HTTP transport. Feature repositories own endpoint and JSON mapping. */
class ApiFailure(val code: String, override val message: String, val field: String? = null) : Exception(message)

class JsonApiClient(context: Context, private val baseUrl: String) {
    private val connectivityManager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val useWifiForLocalApi = isPrivateIpv4(URL(baseUrl).host)

    fun get(path: String, token: String) = request("GET", path, null, token)
    fun post(path: String, body: JSONObject, token: String? = null) = request("POST", path, body, token)
    fun put(path: String, body: JSONObject, token: String) = request("PUT", path, body, token)

    private fun request(method: String, path: String, body: JSONObject?, token: String?): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + "/" + path)
        val wifi = if (useWifiForLocalApi) connectivityManager.allNetworks.firstOrNull { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else null
        val connection = (wifi?.openConnection(url) ?: url.openConnection()) as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            // Render Free can take around a minute to wake after inactivity.
            connection.readTimeout = 90_000
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = if (content.isBlank()) JSONObject() else runCatching { JSONObject(content) }.getOrNull()
            if (status in 200..299) {
                return json ?: throw ApiFailure("BAD_RESPONSE", "Máy chủ trả về dữ liệu không hợp lệ. Vui lòng thử lại.")
            }
            val code = json?.optString("code").orEmpty()
            // A bare 401 means the token was rejected; a 401 with a code (e.g. INVALID_CREDENTIALS) is a domain error.
            if (code.isEmpty()) throw statusFailure(status)
            throw ApiFailure(
                code,
                json?.optString("message")?.takeIf(String::isNotBlank) ?: statusFailure(status).message,
                json?.takeUnless { it.isNull("field") }?.optString("field")?.takeIf(String::isNotBlank)
            )
        } catch (error: SocketTimeoutException) {
            throw ApiFailure("TIMEOUT", "Máy chủ phản hồi quá lâu. Lần truy cập đầu sau thời gian không sử dụng có thể cần khoảng một phút; vui lòng thử lại.")
        } finally {
            connection.disconnect()
        }
    }

    private fun statusFailure(status: Int) = when (status) {
        HttpURLConnection.HTTP_UNAUTHORIZED ->
            ApiFailure("UNAUTHORIZED", "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
        HttpURLConnection.HTTP_FORBIDDEN -> ApiFailure("FORBIDDEN", "Bạn không có quyền thực hiện thao tác này.")
        HttpURLConnection.HTTP_NOT_FOUND -> ApiFailure("NOT_FOUND", "Không tìm thấy dữ liệu yêu cầu.")
        in 500..599 -> ApiFailure("SERVER_ERROR", "Máy chủ đang gặp lỗi. Vui lòng thử lại sau.")
        else -> ApiFailure("SERVER_ERROR", "Không thể hoàn tất yêu cầu. Vui lòng thử lại.")
    }

    private fun isPrivateIpv4(host: String): Boolean {
        val parts = host.split('.').mapNotNull(String::toIntOrNull)
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        return parts[0] == 10 ||
            (parts[0] == 172 && parts[1] in 16..31) ||
            (parts[0] == 192 && parts[1] == 168)
    }
}
