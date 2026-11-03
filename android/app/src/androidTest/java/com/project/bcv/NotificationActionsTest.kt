package com.project.bcv

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.statsnotify.notifications.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class NotificationActionsTest {
    private class Api : NotificationApi {
        var newest = 2
        var bulkMissing = false
        val posts = mutableListOf<String>()
        override fun get(path: String, token: String): JSONObject {
            val items = org.json.JSONArray()
            for (id in newest downTo 1) items.put(JSONObject().put("notificationId", id)
                .put("type", "JOURNEY_COMPLETED").put("title", "Notice $id")
                .put("content", "Content").put("read", false))
            return JSONObject().put("unreadCount", newest).put("items", items)
        }
        override fun post(path: String, body: JSONObject, token: String): JSONObject {
            posts.add(path)
            if (bulkMissing) throw ApiFailure("NOT_FOUND", "Không tìm thấy dữ liệu")
            return JSONObject() // Also accept a successful response with no list/body.
        }
    }

    @Test fun markAllReadKeepsRowsAndSurvivesRefreshWithOlderApi() = fixture { repo, api, _ ->
        repo.notifications()
        api.bulkMissing = true
        val read = repo.markAllRead()
        assertEquals(2, read.items.size)
        assertEquals(0, read.unread)
        assertTrue(read.items.all { it.read })
        assertEquals(0, repo.notifications().unread)
        api.newest = 3
        assertEquals(1, repo.notifications().unread)
        assertFalse(repo.notifications().items.first().read)
    }

    @Test fun markAllReadAcceptsEmptySuccessfulResponse() = fixture { repo, _, _ ->
        repo.notifications()
        assertEquals(2, repo.markAllRead().items.size)
        assertEquals(0, repo.markAllRead().unread)
    }

    @Test fun hideAllNeverDeletesOnServerAndDoesNotHideFutureNotices() = fixture { repo, api, _ ->
        repo.notifications()
        assertTrue(repo.deleteAll().items.isEmpty())
        assertTrue(api.posts.isEmpty())
        assertTrue(repo.notifications().items.isEmpty())
        api.newest = 3
        assertEquals(listOf(3L), repo.notifications().items.map { it.id })
    }

    @Test fun displayStateIsScopedToAccount() = fixture { repo, _, session ->
        repo.notifications()
        repo.deleteAll()
        session.token = "another-session"
        session.userId = 2
        assertEquals(2, repo.notifications().items.size)
    }

    private fun fixture(block: (NotificationRepositoryImpl, Api, SessionPreferences) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "notice-test-${UUID.randomUUID()}"
        val session = SessionPreferences(context, "$name-session").apply { token = "fixture"; userId = 1 }
        val api = Api()
        try { block(NotificationRepositoryImpl(api, session, NotificationLocalState(context, session, name),
            java.util.concurrent.Executor { it.run() }), api, session) }
        finally {
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences("$name-session", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
