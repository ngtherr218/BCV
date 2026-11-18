package com.project.bcv

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Populated UI verification against the disposable fixture API, never an existing user's session. */
@RunWith(AndroidJUnit4::class)
class CollectionChallengeScreensTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun views(view: View): List<View> = listOf(view)+if(view is ViewGroup)(0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    private fun <T:Activity> screen(type:Class<T>,required:List<String>,name:String,after:((T)->Unit)?=null) {
        val args=InstrumentationRegistry.getArguments()
        val token=args.getString("fixtureToken")?:error("Use the disposable UI fixture.")
        val prefs=SessionPreferences(instrumentation.targetContext)
        val oldToken=prefs.token;val oldId=prefs.userId;val oldReady=prefs.readyForHome
        instrumentation.runOnMainSync { prefs.token=token;prefs.userId=args.getString("fixtureUserId")!!.toLong();prefs.readyForHome=true }
        try {
            ActivityScenario.launch(type).use { scenario ->
                val deadline=System.currentTimeMillis()+20000
                var ready=false
                while(!ready&&System.currentTimeMillis()<deadline){
                    scenario.onActivity { activity -> val text=views(activity.window.decorView).filterIsInstance<TextView>().map { it.text.toString() };ready=required.all { needle ->text.any { it.contains(needle) } } }
                    if(!ready)Thread.sleep(100)
                }
                assertTrue("Source UI content did not appear: $required",ready)
                Thread.sleep(300)
                val bitmap=instrumentation.uiAutomation.takeScreenshot()!!
                java.io.File(instrumentation.targetContext.getExternalFilesDir(null),"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
                after?.let { action ->
                    scenario.onActivity { action(it) };Thread.sleep(300)
                    val bottom=instrumentation.uiAutomation.takeScreenshot()!!
                    java.io.File(instrumentation.targetContext.getExternalFilesDir(null),"$name-bottom.png").outputStream().use { bottom.compress(Bitmap.CompressFormat.PNG,100,it) };bottom.recycle()
                }
            }
        } finally {instrumentation.runOnMainSync {prefs.token=oldToken;prefs.userId=oldId;prefs.readyForHome=oldReady}}
    }
    @Test fun collectionHasThreeSourceSectionsAndInlineNewCards() = screen(
        com.project.bcv.feature.collection.presentation.CollectionActivity::class.java,
        listOf("Thẻ","Hộ chiếu","Nhật ký","thẻ mới đang chờ bạn","Di sản văn hóa Việt Nam"),"collection-source-cards")
    @Test fun passportUsesTheSameCollectionHeaderAndStampCover() = screen(
        com.project.bcv.feature.engagement.PassportActivity::class.java,
        listOf("Bộ sưu tập","Thẻ","Nhật ký","Dấu chân Việt","Hà Nội"),"collection-source-passport")
    @Test fun journalShowsPrivateTimelineInTheCollectionTab() = screen(
        com.project.bcv.feature.engagement.JournalActivity::class.java,
        listOf("Bộ sưu tập","Hộ chiếu","Nhật ký hành trình","Văn Miếu"),"collection-source-journal")
    @Test fun challengeHasStreakWeekAndBadgeGrid() {
        screen(com.project.bcv.feature.questitem.quest.presentation.QuestActivity::class.java,
            listOf("Thử thách","ngày liên tiếp","Tạm nghỉ","Huy hiệu","Am hiểu văn hóa"),"challenge-source-top") { activity ->
            views(activity.window.decorView).filterIsInstance<ScrollView>().first().fullScroll(View.FOCUS_DOWN)
        }
    }
}
