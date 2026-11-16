package com.project.bcv

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.discovery.presentation.LeaderboardActivity
import com.project.bcv.feature.discovery.presentation.PassportActivity
import com.project.bcv.feature.discovery.presentation.PlaceSearchActivity
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URI
import java.util.UUID

/** Real local API + real Activities; creates only an isolated example.invalid account. */
@RunWith(AndroidJUnit4::class)
class LocalExplorerScreensTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(400)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    private fun views(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()

    private fun waitFor(activity: Activity, predicate: (List<View>) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 20000
        do {
            var found = false
            instrumentation.runOnMainSync { found = predicate(views(activity.window.decorView)) }
            if (found) return
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("Activity did not render expected API result: ${activity.javaClass.simpleName}")
    }

    @Test fun rankingAndQuestPointRewardsRenderLocalApiResults() {
        val context=instrumentation.targetContext
        val api=JsonApiClient(context,BuildConfig.API_BASE_URL)
        assertEquals("vietnam_explorer_dev_v2",api.get("health","").getString("database"))
        val password="Ranking-${UUID.randomUUID()}!"
        val account=api.post("api/account/register",JSONObject().put("email","rank-${UUID.randomUUID()}@example.invalid").put("password",password).put("confirmPassword",password))
        val token=api.post("api/account/login",JSONObject().put("email",account.getJSONObject("user").getString("email")).put("password",password)).getString("token")
        api.put("api/interests",JSONObject().put("placeTypeIds",org.json.JSONArray().put(101)),token)
        api.put("api/activity-level",JSONObject().put("activityLevel","HIGH"),token)
        api.put("api/starting-point",JSONObject().put("source","SUGGESTED"),token)
        val places=api.get("api/explore/places?pageSize=50",token).getJSONArray("places")
        val album=api.get("api/collections/all-cards",token).getJSONArray("cards")
        val cardPlaces=(0 until album.length()).map{album.getJSONObject(it).getLong("placeId")}.toSet()
        val localArtPlaces=(0 until album.length()).map{album.getJSONObject(it)}.filter{it.optString("imageUrl").startsWith("data:")}.map{it.getLong("placeId")}.toSet()
        val destination=(0 until places.length()).map { places.getJSONObject(it) }.sortedByDescending{it.getLong("placeId") in localArtPlaces}.first { !it.getBoolean("isDiscovered") && it.getDouble("distanceKm")>0.001 && it.getLong("placeId") in cardPlaces }
        val journey=api.post("api/explore/journeys",JSONObject().put("placeId",destination.getLong("placeId")).put("replaceActive",false),token).getJSONObject("journey")
        val completed=api.post("api/explore/sync-steps",JSONObject().put("journeyId",journey.getLong("journeyId")).put("cumulativeSteps",journey.getInt("requiredSteps")),token)
        assertTrue(completed.getJSONObject("journey").getInt("rankingPoints")>0)
        assertTrue("Eligible discovery confirms card ownership",completed.getJSONObject("journey").has("awardedCard") && !completed.getJSONObject("journey").isNull("awardedCard"))
        val session=SessionPreferences(context);val previous=session.token;val previousUser=session.userId;val previousReady=session.readyForHome
        session.token=token;session.userId=account.getJSONObject("user").getLong("userId");session.readyForHome=true
        fun screen(type:Class<out Activity>,action:(Activity)->Unit){
            val activity=instrumentation.startActivitySync(Intent(context,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            try{action(activity)}finally{instrumentation.runOnMainSync{activity.finish()}}
        }
        try {
            screen(LeaderboardActivity::class.java){ activity ->
                waitFor(activity){ list -> list.filterIsInstance<TextView>().any{it.text.startsWith("Bạn: Hạng ")} && list.filterIsInstance<TextView>().any{it.text.startsWith("Bạn · ")} }
                capture("ranking-my-position")
            }
            screen(com.project.bcv.feature.questitem.quest.presentation.QuestActivity::class.java){ activity ->
                // Local catalog may have weekly quests only; select the populated cycle.
                val overview=api.get("api/quests",token)
                if(overview.getJSONArray("daily").length()==0) instrumentation.runOnMainSync {
                    activity.findViewById<View>(R.id.weeklyTab).performClick()
                }
                waitFor(activity){ list -> list.filterIsInstance<TextView>().any{it.text.contains("điểm xếp hạng")} }
                val daily=overview.getJSONArray("daily")
                val expected=(0 until daily.length()).map{daily.getJSONObject(it)}.map{if(it.getBoolean("completed"))it.getInt("awardedRankingPoints") else it.getInt("rankingPoints")}.filter{it>0}.distinct()
                assertTrue("Demo quests must have different point values",expected.size>=2)
                expected.forEach { points -> waitFor(activity){list -> list.filterIsInstance<TextView>().any{it.text.contains("+$points điểm xếp hạng")}} }
                capture("quest-ranking-points")
                instrumentation.runOnMainSync{activity.findViewById<View>(R.id.weeklyTab).performClick()}
                waitFor(activity){list -> list.filterIsInstance<TextView>().any{it.text.contains("150 điểm xếp hạng")}}
                capture("quest-weekly-points-v3")
            }
            val receiptActivity=instrumentation.startActivitySync(Intent(context,com.project.bcv.feature.journey.completion.JourneyCompletionActivity::class.java)
                .putExtra(com.project.bcv.feature.journey.completion.JourneyCompletionActivity.EXTRA_JOURNEY_ID,journey.getLong("journeyId")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            try {
                waitFor(receiptActivity){ list -> list.filterIsInstance<TextView>().any{it.text=="Chinh phục thành công!" && it.isShown} }
                val receipt=api.get("api/journeys/"+journey.getLong("journeyId")+"/completion",token)
                instrumentation.runOnMainSync {
                    assertEquals("+"+java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN")).format(receipt.getInt("rankingPoints")),receiptActivity.findViewById<TextView>(R.id.pointsValue).text.toString())
                    assertEquals(receipt.getJSONObject("card").getString("name"),receiptActivity.findViewById<TextView>(R.id.cardName).text.toString())
                }
                SystemClock.sleep(2000)
                capture("journey-completion-real")
            } finally {instrumentation.runOnMainSync{receiptActivity.finish()}}
            screen(com.project.bcv.feature.statsnotify.notifications.presentation.NotificationsActivity::class.java){ activity ->
                waitFor(activity){ list -> list.filterIsInstance<TextView>().any{it.text=="Hôm nay"} }
                capture("notifications-redesign-all")
                instrumentation.runOnMainSync { activity.findViewById<View>(R.id.unreadFilter).performClick() }
                waitFor(activity){ list -> list.filterIsInstance<TextView>().any{it.text=="Chưa đọc"} }
                instrumentation.runOnMainSync { activity.findViewById<View>(R.id.moreButton).performClick() }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(600) // Popup enter animation is outside Espresso's idling resources.
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Đánh dấu tất cả đã đọc"))
                    .perform(androidx.test.espresso.action.ViewActions.click())
                waitFor(activity){ list -> list.filterIsInstance<TextView>().any{it.text=="Bạn đã xem hết tin mới" && it.isShown} }
                capture("notifications-redesign-read")
                instrumentation.runOnMainSync { activity.findViewById<View>(R.id.showAllButton).performClick() }
                waitFor(activity){ list -> list.filterIsInstance<TextView>().any{it.text=="Đã đọc"} }
            }
        }finally{session.token=previous;session.userId=previousUser;session.readyForHome=previousReady}
    }

    @Test fun searchPassportAndLeaderboardRenderLocalApiResults() {
        val uri = URI(BuildConfig.API_BASE_URL)
        assumeTrue("Local v2 only", uri.port == 5082 && uri.host in listOf("127.0.0.1", "10.0.2.2", "localhost"))
        val context = instrumentation.targetContext
        val api = JsonApiClient(context, BuildConfig.API_BASE_URL)
        assertEquals("vietnam_explorer_dev_v2", api.get("health", "").getString("database"))
        val password = "Fixture-${UUID.randomUUID()}!"
        val account = api.post("api/account/register", JSONObject()
            .put("email", "screens-${UUID.randomUUID()}@example.invalid")
            .put("password", password).put("confirmPassword", password))
        val token = account.getString("token")
        api.put("api/interests", JSONObject().put("placeTypeIds", org.json.JSONArray().put(201)), token)
        api.put("api/activity-level", JSONObject().put("activityLevel", "HIGH"), token)
        api.put("api/starting-point", JSONObject().put("source", "SUGGESTED"), token)
        val session = SessionPreferences(context)
        val previousToken = session.token
        val previousUser = session.userId
        val previousReady = session.readyForHome
        session.token = token
        fun screen(type: Class<out Activity>, action: (Activity) -> Unit) {
            val activity = instrumentation.startActivitySync(Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            try { action(activity) } finally { instrumentation.runOnMainSync { activity.finish() } }
        }
        try {
            val interests = api.get("api/interests", token)
            org.junit.Assert.assertEquals(12, interests.getJSONArray("placeTypes").length())
            org.junit.Assert.assertTrue(interests.getBoolean("requiresReselection"))
            api.put("api/interests", JSONObject().put("placeTypeIds", org.json.JSONArray().put(101).put(108).put(112)), token)
            val saved = api.get("api/interests", token)
            org.junit.Assert.assertEquals(listOf(101L,108L,112L), (0 until 3).map { saved.getJSONArray("selectedPlaceTypeIds").getLong(it) })
            org.junit.Assert.assertFalse(saved.getBoolean("requiresReselection"))
            api.get("api/explore/recommendations", token)
            screen(com.project.bcv.feature.account.interests.presentation.InterestsActivity::class.java) { activity ->
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text == "Rừng và khu bảo tồn" } && list.filterIsInstance<TextView>().any { it.text == "Đã chọn 3/3 nhóm" } }
            }
            screen(com.project.bcv.feature.discovery.presentation.RecommendationActivity::class.java) { activity ->
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.contains("địa danh · Trang 1") } }
                capture("discovery-redesign-results")
                instrumentation.runOnMainSync {
                    val list=views(activity.window.decorView)
                    org.junit.Assert.assertTrue(list.filterIsInstance<android.widget.CheckBox>().first { it.text == "Ưu tiên sở thích của tôi" }.isChecked)
                    activity.findViewById<EditText>(R.id.destinationQuery).setText("zz-no-matching-destination")
                }
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.startsWith("Không có địa danh phù hợp") } }
                instrumentation.runOnMainSync {
                    val list=views(activity.window.decorView)
                    activity.findViewById<EditText>(R.id.destinationQuery).text.clear()
                    activity.findViewById<Button>(R.id.destinationFilterToggle).performClick()
                }
                capture("discovery-redesign-filters")
                for (name in listOf("Di tích và bảo tàng", "Biển và đảo", "Rừng và khu bảo tồn")) {
                    androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(name))
                        .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isChecked()))
                }
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Công trình kiến trúc"))
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(org.hamcrest.Matchers.not(androidx.test.espresso.matcher.ViewMatchers.isChecked())))
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Biển và đảo"))
                    .perform(androidx.test.espresso.action.ViewActions.scrollTo(), androidx.test.espresso.action.ViewActions.click())
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Xem kết quả"))
                    .perform(androidx.test.espresso.action.ViewActions.click())
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.contains("địa danh · Trang 1") } }
                instrumentation.runOnMainSync {
                    views(activity.window.decorView).filterIsInstance<android.widget.CheckBox>().first().isChecked=false
                }
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.contains("· Gần nhất") } }
                instrumentation.runOnMainSync { activity.findViewById<Button>(R.id.destinationFilterToggle).performClick() }
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Biển và đảo"))
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(org.hamcrest.Matchers.not(androidx.test.espresso.matcher.ViewMatchers.isChecked())))
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Đặt lại theo sở thích"))
                    .perform(androidx.test.espresso.action.ViewActions.click())
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Biển và đảo"))
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isChecked()))
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Xem kết quả"))
                    .perform(androidx.test.espresso.action.ViewActions.click())
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.contains("· Ưu tiên sở thích") } }
                assertEquals(listOf(101L,108L,112L), api.get("api/interests", token).getJSONArray("selectedPlaceTypeIds").let { rows -> (0 until rows.length()).map { rows.getLong(it) } })
            }
            screen(PlaceSearchActivity::class.java) { activity ->
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.contains("địa danh · Trang 1") } }
                instrumentation.runOnMainSync {
                    val list = views(activity.window.decorView)
                    list.filterIsInstance<EditText>().first { it.hint == "Tên địa danh" }.setText("zzzz-no-matching-place")
                    assertTrue(list.filterIsInstance<Button>().first { it.text == "Tìm địa danh" }.performClick())
                }
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.startsWith("Không có địa danh phù hợp") } }
            }
            screen(PassportActivity::class.java) { activity ->
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.contains("0/5 địa danh") } }
                capture("redesign-passport")
                instrumentation.runOnMainSync {
                    views(activity.window.decorView).filterIsInstance<EditText>().first().setText("zz-no-region")
                }
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text == "Không tìm thấy vùng phù hợp." && it.visibility==View.VISIBLE } }
            }
            screen(LeaderboardActivity::class.java) { activity ->
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.contains("Bạn: Chưa xếp hạng · 0 điểm") } }
                capture("redesign-leaderboard")
                instrumentation.runOnMainSync {
                    views(activity.window.decorView).filterIsInstance<Button>().first { it.text == "‹ Tuần trước" }.performClick()
                }
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.startsWith("Tuần đã chọn") } }
                instrumentation.runOnMainSync {
                    views(activity.window.decorView).filterIsInstance<Button>().first { it.text == "Tuần này" }.performClick()
                }
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.startsWith("Tuần này ·") } }
            }
            val catalog = api.get("api/collections", token)
            val collectionId = 0L
            val preview = api.get("api/collections/all-cards",token)
            val previewCards = preview.getJSONArray("cards")
            assertTrue("Unowned cards expose artwork previews", (0 until previewCards.length()).any {
                val card = previewCards.getJSONObject(it)
                !card.getBoolean("owned") && !card.isNull("imageUrl") && card.getString("imageUrl").isNotBlank()
            })
            screen(com.project.bcv.feature.collection.presentation.CollectionActivity::class.java) { activity ->
                waitFor(activity) { list -> list.filterIsInstance<TextView>().any { it.text.contains("thẻ đã sở hữu") } }
                instrumentation.runOnMainSync {
                    assertTrue("Collection home keeps card artwork inside the album",views(activity.window.decorView).none { it.id==R.id.cardImage })
                    assertTrue("No expanded unread-card banner",views(activity.window.decorView).filterIsInstance<TextView>().none { it.text.contains("thẻ mới đang chờ") })
                }
                capture("redesign-collections")
                val monitor=instrumentation.addMonitor(com.project.bcv.feature.collection.presentation.CollectionDetailActivity::class.java.name,null,false)
                try {
                    instrumentation.runOnMainSync { activity.findViewById<Button>(R.id.newCardsButton).performClick() }
                    val inbox=instrumentation.waitForMonitorWithTimeout(monitor,5000) ?: throw AssertionError("New-card section did not open")
                    try {
                        waitFor(inbox) { list -> list.filterIsInstance<TextView>().any { it.text.startsWith("Bạn đã xem hết thẻ mới") } }
                        capture("redesign-new-card-inbox")
                    } finally { instrumentation.runOnMainSync { inbox.finish() } }
                } finally { instrumentation.removeMonitor(monitor) }
            }
            val detailActivity = instrumentation.startActivitySync(Intent(context, com.project.bcv.feature.collection.presentation.CollectionDetailActivity::class.java)
                .putExtra("collection_id",collectionId).putExtra("all_cards",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            try {
                waitFor(detailActivity) { list -> list.filterIsInstance<TextView>().any { it.text == "Chưa sở hữu" } }
                waitFor(detailActivity) { list -> list.filterIsInstance<android.widget.ImageView>().any { it.id==R.id.cardImage && it.drawable!=null } }
                capture("redesign-collection-preview")
            } finally { instrumentation.runOnMainSync { detailActivity.finish() } }
            assertEquals("Preview does not grant cards",catalog.getInt("ownedCards"),api.get("api/collections",token).getInt("ownedCards"))
        } finally {
            session.token = previousToken
            session.userId = previousUser
            session.readyForHome = previousReady
        }
    }
}
