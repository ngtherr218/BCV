package com.project.bcv.feature.collection.data

import com.project.bcv.core.network.JsonApiClient
import org.json.JSONObject

interface CollectionApi {
    fun get(path: String, token: String): JSONObject
    fun post(path: String, body: JSONObject, token: String): JSONObject
}

class JsonCollectionApi(private val client: JsonApiClient) : CollectionApi {
    override fun get(path: String, token: String) = client.get(path, token)
    override fun post(path: String, body: JSONObject, token: String) = client.post(path, body, token)
}
