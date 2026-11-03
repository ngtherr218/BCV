package com.project.bcv

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.collection.data.CardInbox
import com.project.bcv.feature.collection.data.CollectionApi
import com.project.bcv.feature.collection.data.CollectionRepositoryImpl
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class CollectionPerformanceTest {
    private class FakeApi : CollectionApi {
        val paths = mutableListOf<String>()
        var legacy = false
        override fun get(path: String, token: String): JSONObject {
            paths.add(path)
            return if (path == "api/collections") {
                JSONObject().put("ownedCards", 1).put("totalCards", 1).put("collections", org.json.JSONArray())
                    .apply { if (!legacy) put("newCards", 3) }
            } else JSONObject("""{"collectionId":1,"name":"Test","ownedCards":1,"totalCards":1,
                "cards":[{"cardId":7,"placeId":11,"name":"Card","owned":true,"isNew":true}]}""")
        }
        override fun post(path: String, body: JSONObject, token: String) = JSONObject()
    }

    @Test fun badgeRefreshNeverLoadsCollectionDetails() = fixture { repo, api, _ ->
        assertEquals(3, repo.loadBadgeCount())
        assertEquals(listOf("api/collections"), api.paths)
    }

    @Test fun legacyBadgeUsesPendingJourneyWithoutScanningCollections() = fixture { repo, api, inbox ->
        api.legacy = true
        inbox.recordJourney(19, 11)
        assertEquals(1, repo.loadBadgeCount())
        assertEquals(listOf("api/collections"), api.paths)
    }

    @Test fun cachedDetailsRespectViewingAndInvalidateOnNewRewards() = fixture { repo, api, _ ->
        assertTrue(repo.loadDetail(1).cards.single().isNew)
        repo.loadDetail(1)
        assertEquals(1, api.paths.size)
        repo.markSeen(7)
        assertFalse(repo.loadDetail(1).cards.single().isNew)
        assertEquals(1, api.paths.size)
        repo.invalidate()
        repo.loadDetail(1)
        assertEquals(2, api.paths.size)
    }

    private fun fixture(block: (CollectionRepositoryImpl, FakeApi, CardInbox) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scope = "collection-perf-${UUID.randomUUID()}"
        val session = SessionPreferences(context, "$scope-session").apply { token = "fixture" }
        val inbox = CardInbox(context, session, "$scope-inbox")
        val api = FakeApi()
        try { block(CollectionRepositoryImpl(api, session, inbox), api, inbox) }
        finally {
            context.getSharedPreferences("$scope-session", Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("$scope-inbox", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
