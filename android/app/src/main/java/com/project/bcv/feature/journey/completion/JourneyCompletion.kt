package com.project.bcv.feature.journey.completion

import android.content.Context
import com.project.bcv.BuildConfig
import com.project.bcv.core.network.ApiFailure
import com.project.bcv.core.network.JsonApiClient
import com.project.bcv.feature.account.auth.data.local.SessionPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.loadInto

data class CompletionCard(val id:Long,val name:String,val imageUrl:String?,val newlyAwarded:Boolean)
data class JourneyCompletion(val journeyId:Long,val placeId:Long,val placeName:String,val regionName:String?,val imageUrl:String?,
 val recordedSteps:Int,val requiredSteps:Int,val rankingPoints:Int,val completedAt:String,val card:CompletionCard?,val status:String="COMPLETED") {
 fun validFor(id:Long)=id>0 && journeyId==id && placeId>0 && status=="COMPLETED" && requiredSteps>0 && recordedSteps>=requiredSteps && rankingPoints>=0 && completedAt.isNotBlank()
}
fun interface JourneyCompletionRepository { fun load(id:Long):JourneyCompletion }
class RemoteJourneyCompletionRepository(context:Context):JourneyCompletionRepository {
 private val api=JsonApiClient(context,BuildConfig.API_BASE_URL)
 private val session=SessionPreferences(context)
 override fun load(id:Long):JourneyCompletion {
  val token=session.token?:throw ApiFailure("UNAUTHORIZED","Phiên đăng nhập đã hết hạn.")
  val j=api.get("api/journeys/$id/completion",token)
  fun org.json.JSONObject.text(key:String)=optString(key).takeIf{it.isNotBlank()&&it!="null"}
  val c=j.optJSONObject("card")?.let{CompletionCard(it.getLong("cardId"),it.getString("name"),it.text("imageUrl"),it.optBoolean("newlyAwarded"))}
  return JourneyCompletion(j.getLong("journeyId"),j.getLong("placeId"),j.getString("placeName"),j.text("regionName"),j.text("imageUrl"),
   j.getInt("recordedSteps"),j.getInt("requiredSteps"),j.getInt("rankingPoints"),j.optString("completedAt"),c,j.getString("status"))
 }
}
/** Same lightweight DI style as the other modules; overrides are internal to instrumentation. */
object JourneyCompletionModule {
 internal var overrideRepository:JourneyCompletionRepository?=null
 fun repository(context:Context)=overrideRepository?:RemoteJourneyCompletionRepository(context)
}
class JourneyCompletionViewModel(private val id:Long,private val repository:JourneyCompletionRepository):ViewModel(){
 private val mutable=MutableLiveData<Remote<JourneyCompletion>>()
 val state:LiveData<Remote<JourneyCompletion>> = mutable
 init{load()}
 fun load(){if(mutable.value==Remote.Loading)return;loadInto(mutable){
  if(id<=0)throw ApiFailure("INVALID_JOURNEY","Không tìm thấy hành trình cần xem.")
  repository.load(id).also{if(!it.validFor(id))throw ApiFailure("INVALID_RESULT","Chưa xác minh được kết quả hành trình. Vui lòng tải lại.")}
 }}
}
