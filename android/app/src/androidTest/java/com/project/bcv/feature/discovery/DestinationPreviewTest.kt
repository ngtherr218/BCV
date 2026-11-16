package com.project.bcv.feature.discovery
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.BuildConfig
import com.project.bcv.R
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import com.project.bcv.feature.discovery.domain.RecommendedPlace
import com.project.bcv.feature.discovery.presentation.RecommendationActivity
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID
class DestinationPreviewTest {
 @Test fun lockedPhotoIsVisibleAndDiscoveredPlaceIsNotSelectable(){
  val inst=InstrumentationRegistry.getInstrumentation();val context=inst.targetContext;val api=JsonApiClient(context,BuildConfig.API_BASE_URL)
  assertEquals("vietnam_explorer_dev_v2",api.get("health","").getString("database"))
  val password="Preview-${UUID.randomUUID()}!";val auth=api.post("api/account/register",JSONObject().put("email","preview-${UUID.randomUUID()}@example.invalid").put("password",password).put("confirmPassword",password));val token=auth.getString("token")
  api.put("api/interests",JSONObject().put("placeTypeIds",org.json.JSONArray().put(101)),token);api.put("api/activity-level",JSONObject().put("activityLevel","HIGH"),token);api.put("api/starting-point",JSONObject().put("source","SUGGESTED"),token)
  val session=SessionPreferences(context);val previous=session.token;val previousUser=session.userId;val previousReady=session.readyForHome
  session.token=token;session.userId=auth.getJSONObject("user").getLong("userId")
  val activity=inst.startActivitySync(Intent(context,RecommendationActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  fun views(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{views(v.getChildAt(it))}else emptyList()
  fun waitUntil(predicate:()->Boolean){val deadline=SystemClock.uptimeMillis()+20000;do{var found=false;inst.runOnMainSync{found=predicate()};if(found)return;SystemClock.sleep(100)}while(SystemClock.uptimeMillis()<deadline);throw AssertionError("Preview UI not ready")}
  try{
   waitUntil{views(activity.window.decorView).filterIsInstance<TextView>().any{it.text.contains("địa danh · Trang 1")}}
   val imageUrl=InstrumentationRegistry.getArguments().getString("imageProbeUrl")?:throw AssertionError("Photo probe URL required")
   // Render controlled status fixtures after the real initial request, using a real DB image URL.
   val places=listOf(RecommendedPlace(1,999001,"Ảnh xem trước chưa mở khóa","Hà Nội",imageUrl,false,1.0,1500,emptyList(),emptyList()),RecommendedPlace(2,999002,"Địa danh đã mở khóa","Hà Nội",imageUrl,true,1.0,1500,emptyList(),emptyList()))
   inst.runOnMainSync{activity.javaClass.getDeclaredMethod("showCards",List::class.java).apply{isAccessible=true}.invoke(activity,places)}
   waitUntil{views(activity.window.decorView).filterIsInstance<ImageView>().filter{it.id==R.id.image}.all{it.drawable is BitmapDrawable}}
   inst.runOnMainSync{
    val all=views(activity.window.decorView);val images=all.filterIsInstance<ImageView>().filter{it.id==R.id.image};assertEquals(2,images.size);assertEquals(.65f,images[0].alpha);assertEquals(1f,images[1].alpha)
    val locks=all.filter{it.id==R.id.rewardLock};assertEquals(View.VISIBLE,locks[0].visibility);assertEquals(View.GONE,locks[1].visibility)
    val buttons=all.filterIsInstance<TextView>().filter{it.id==R.id.selectButton};assertEquals("Chọn điểm đến",buttons[0].text.toString());assertEquals("Xem địa danh",buttons[1].text.toString())
   }
   inst.waitForIdleSync();SystemClock.sleep(300);inst.uiAutomation.takeScreenshot()?.let{b->java.io.File(context.getExternalFilesDir(null),"destination-photo-lock.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
  }finally{inst.runOnMainSync{activity.finish()};session.token=previous;session.userId=previousUser;session.readyForHome=previousReady}
 }
}
