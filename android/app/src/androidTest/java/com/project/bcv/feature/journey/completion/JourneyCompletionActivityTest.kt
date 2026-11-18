package com.project.bcv.feature.journey.completion

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.res.Configuration
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.project.bcv.R
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.collection.data.CardInbox
import com.project.bcv.feature.collection.presentation.JourneyRewardController
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.*
import org.junit.Assert.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** UI-only receipts are deterministic fixtures. A separate LocalExplorer test exercises the real API. */
class JourneyCompletionActivityTest{
 private val inst=InstrumentationRegistry.getInstrumentation()
 private val context=inst.targetContext
 private val id=1_900_000_001L
 private val calls=AtomicInteger()
 private var value=JourneyCompletion(id,3,"Địa danh Việt Nam với tên dài để kiểm tra giao diện", "Khánh Hòa",null,12345,12345,375,"2026-11-13T10:30:00",CompletionCard(99,"Thẻ di sản Việt Nam",null,true))
 private var fail=false
 private lateinit var session:SessionPreferences
 private var token:String?=null;private var user:Long?=null;private var ready=false
 private val activities=mutableListOf<Activity>()
 @Before fun before(){
  session=SessionPreferences(context);token=session.token;user=session.userId;ready=session.readyForHome
  session.token="completion-ui-${UUID.randomUUID()}";session.userId=(UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).coerceAtLeast(1)
  JourneyCompletionModule.overrideRepository=JourneyCompletionRepository{requestId->calls.incrementAndGet();if(fail)throw java.io.IOException("fixture network unavailable");value.copy(journeyId=requestId)}
 }
 @After fun after(){
  inst.runOnMainSync{activities.filter{!it.isDestroyed}.forEach{it.finish()}}
  JourneyCompletionModule.overrideRepository=null
  session.token=token;session.userId=user;session.readyForHome=ready
 }
 private fun open(journeyId:Long=id):JourneyCompletionActivity{
  val a=inst.startActivitySync(Intent(context,JourneyCompletionActivity::class.java).putExtra(JourneyCompletionActivity.EXTRA_JOURNEY_ID,journeyId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as JourneyCompletionActivity
  activities.add(a);return a
 }
 private fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
 private fun waitFor(a:Activity,text:String){val deadline=SystemClock.uptimeMillis()+10000;do{var found=false;inst.runOnMainSync{found=views(a.window.decorView).filterIsInstance<TextView>().any{it.isShown&&it.text.contains(text)}};if(found)return;SystemClock.sleep(50)}while(SystemClock.uptimeMillis()<deadline);throw AssertionError("Missing UI: $text")}
 private fun capture(name:String){inst.waitForIdleSync();SystemClock.sleep(300);inst.uiAutomation.takeScreenshot()?.let{bitmap->java.io.File(context.getExternalFilesDir(null),"$name.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}}
 @Test fun jc01_03_05_07_successUsesReceiptAndPlaceholder(){
  val a=open();waitFor(a,"Chinh phục thành công!")
  inst.runOnMainSync{assertEquals("+375",a.findViewById<TextView>(R.id.pointsValue).text.toString());assertEquals("12.345",a.findViewById<TextView>(R.id.stepsValue).text.toString());assertEquals(View.VISIBLE,a.findViewById<View>(R.id.cardSection).visibility);assertNotNull(a.findViewById<android.widget.ImageView>(R.id.placeImage).drawable)}
  capture("journey-completion-fixture-full")
 }
 @Test fun completionShowsQuizBreakdownAndRankGain(){
  value=value.copy(quizAnswered=3,quizCorrect=2,quizPoints=100,masteryPoints=50,rankBefore=32,rankAfter=25)
  val a=open();waitFor(a,"Chinh phục thành công!")
  inst.runOnMainSync{
   assertEquals("+525",a.findViewById<TextView>(R.id.pointsValue).text.toString())
   assertTrue(a.findViewById<TextView>(R.id.rewardBreakdown).text.contains("2/3 câu   +100 điểm"))
   assertTrue(a.findViewById<TextView>(R.id.rewardBreakdown).text.contains("Am hiểu   +50 điểm"))
   assertEquals("#32 → #25",a.findViewById<TextView>(R.id.rankValue).text.toString())
   assertTrue(a.findViewById<TextView>(R.id.rankChange).text.contains("Tăng 7 bậc"))
  }
  capture("journey-completion-rewards-rank")
 }
 @Test fun jc04_06_noCardAndZeroPoints(){value=value.copy(card=null,rankingPoints=0);val a=open();waitFor(a,"Chinh phục thành công!");inst.runOnMainSync{assertEquals(View.GONE,a.findViewById<View>(R.id.cardSection).visibility);assertEquals("+0",a.findViewById<TextView>(R.id.pointsValue).text.toString())};capture("journey-completion-zero")}
 @Test fun jc02_unconfirmedNeverShowsSuccess(){value=value.copy(status="IN_PROGRESS");val a=open();waitFor(a,"Chưa xác minh");inst.runOnMainSync{assertEquals(View.GONE,a.findViewById<View>(R.id.content).visibility)}}
 @Test fun jc16_networkErrorCanRetry(){fail=true;val a=open();waitFor(a,"Không kết nối");inst.runOnMainSync{assertEquals(View.GONE,a.findViewById<View>(R.id.content).visibility)};capture("journey-completion-error");fail=false;onView(withId(R.id.stateRetry)).perform(click());waitFor(a,"Chinh phục thành công!");assertEquals(2,calls.get())}
 @Test fun jc08_10_placeNavigationIsSingle(){val a=open();waitFor(a,"Chinh phục thành công!");val monitor=inst.addMonitor(com.project.bcv.feature.discovery.presentation.PlaceDetailActivity::class.java.name,Instrumentation.ActivityResult(Activity.RESULT_CANCELED,Intent()),true);try{inst.runOnMainSync{a.findViewById<View>(R.id.viewPlaceButton).performClick();a.findViewById<View>(R.id.viewPlaceButton).performClick()};assertEquals(1,monitor.hits)}finally{inst.removeMonitor(monitor)}}
 @Test fun jc13_presentationSurvivesTokenRenewal(){val a=open();waitFor(a,"Chinh phục thành công!");val owner=session.userId;session.token="renewed-"+UUID.randomUUID();session.userId=owner;assertTrue(CardInbox(context).dialogShown(id))}
 @Test fun jc09_continueNavigation(){val a=open();waitFor(a,"Chinh phục thành công!");val monitor=inst.addMonitor(com.project.bcv.feature.discovery.presentation.ExploreActivity::class.java.name,Instrumentation.ActivityResult(Activity.RESULT_CANCELED,Intent()),true);try{inst.runOnMainSync{a.findViewById<View>(R.id.continueButton).performClick()};assertEquals(1,monitor.hits)}finally{inst.removeMonitor(monitor)}}
 @Test fun jc11_12_recreationAndReopenOnlyReadReceipt(){
  val a=open();waitFor(a,"Chinh phục thành công!");inst.runOnMainSync{a.recreate()}
  var recreated:JourneyCompletionActivity?=null;val deadline=SystemClock.uptimeMillis()+10000
  do{inst.runOnMainSync{recreated=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<JourneyCompletionActivity>().firstOrNull{it!==a}};if(recreated!=null)break;SystemClock.sleep(50)}while(SystemClock.uptimeMillis()<deadline)
  val next=recreated?:throw AssertionError("Activity not recreated");activities.add(next);waitFor(next,"Chinh phục thành công!");assertEquals(1,calls.get());assertTrue(CardInbox(context).dialogShown(id));inst.runOnMainSync{next.finish()};val reopened=open();waitFor(reopened,"Chinh phục thành công!");assertEquals(2,calls.get())
 }
 @Test fun jc01_02_10_13_controllerDoesNotNavigateTwice(){
  val parent=open();waitFor(parent,"Chinh phục thành công!");val nextId=id+1
  val monitor=inst.addMonitor(JourneyCompletionActivity::class.java.name,null,false)
  try{
   val trip=ExploreJourney(nextId,"Điểm đến",Coordinates(0.0,0.0),100,100,"IN_PROGRESS",0,destinationPlaceId=3)
   inst.runOnMainSync{val controller=JourneyRewardController(parent){};controller.completed(trip);assertEquals(0,monitor.hits);controller.completed(trip.copy(status="COMPLETED"));controller.completed(trip.copy(status="COMPLETED"))}
   val child=inst.waitForMonitorWithTimeout(monitor,5000) as? JourneyCompletionActivity?:throw AssertionError("Completion not opened")
   activities.add(child);waitFor(child,"Chinh phục thành công!");assertEquals(1,monitor.hits);inst.runOnMainSync{child.finish()};SystemClock.sleep(400);assertEquals(1,monitor.hits)
  }finally{inst.removeMonitor(monitor)}
 }
 private fun shell(command:String)=inst.uiAutomation.executeShellCommand(command).use{p->java.io.FileInputStream(p.fileDescriptor).bufferedReader().readText().trim()}
 @Test fun jc14_animationsDisabled(){val old=shell("settings get global animator_duration_scale");try{shell("settings put global animator_duration_scale 0");val a=open();waitFor(a,"Chinh phục thành công!");inst.runOnMainSync{assertEquals(1f,a.findViewById<View>(R.id.completionBody).alpha);assertTrue(a.findViewById<View>(R.id.viewPlaceButton).isEnabled)}}finally{if(old=="null")shell("settings delete global animator_duration_scale")else shell("settings put global animator_duration_scale $old")}}
 @Test fun jc15_largeFontScrollable(){val old=shell("settings get system font_scale");try{shell("settings put system font_scale 1.6");SystemClock.sleep(1000);val a=open();waitFor(a,"Chinh phục thành công!");inst.runOnMainSync{assertTrue("Font scale must actually change",a.resources.configuration.fontScale>=1.5f)};onView(withId(R.id.continueButton)).perform(scrollTo()).check(matches(isDisplayed()));capture("journey-completion-large-font")}finally{if(old=="null")shell("settings delete system font_scale")else shell("settings put system font_scale "+old);SystemClock.sleep(500)}}
}
