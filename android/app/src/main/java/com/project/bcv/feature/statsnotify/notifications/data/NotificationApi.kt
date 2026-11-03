package com.project.bcv.feature.statsnotify.notifications.data

import com.project.bcv.core.network.JsonApiClient
import org.json.JSONObject

interface NotificationApi {
    fun get(path: String, token: String): JSONObject
    fun post(path: String, body: JSONObject, token: String): JSONObject
}
class JsonNotificationApi(private val client: JsonApiClient) : NotificationApi {
    override fun get(path: String, token: String) = client.get(path, token)
    override fun post(path: String, body: JSONObject, token: String) = client.post(path, body, token)
}
