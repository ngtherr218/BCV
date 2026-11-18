package com.project.bcv.feature.journey.completion

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.project.bcv.R
import com.project.bcv.core.presentation.Remote
import com.project.bcv.core.presentation.viewModelFactory
import com.project.bcv.core.ui.*
import com.project.bcv.databinding.ActivityJourneyCompletionBinding
import com.project.bcv.feature.collection.data.CardInbox
import com.project.bcv.feature.discovery.presentation.ExploreActivity
import com.project.bcv.feature.discovery.presentation.PlaceDetailActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class JourneyCompletionActivity:AppCompatActivity(){
 private lateinit var binding:ActivityJourneyCompletionBinding
 private lateinit var model:JourneyCompletionViewModel
 private val id by lazy{intent.getLongExtra(EXTRA_JOURNEY_ID,0)}
 private var result:JourneyCompletion?=null
 private var animated=false
 private var imagesBound=false
 private var leaving=false
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  binding=ActivityJourneyCompletionBinding.inflate(layoutInflater)
  setUpDetailScreen(binding.root)
  binding.content.addOnLayoutChangeListener{_,left,_,right,_,_,_,_,_->
   val width=minOf(right-left,(640*resources.displayMetrics.density).toInt())
   if(binding.completionBody.layoutParams.width!=width) binding.completionBody.layoutParams=
    (binding.completionBody.layoutParams as android.widget.FrameLayout.LayoutParams).apply{this.width=width;gravity=android.view.Gravity.CENTER_HORIZONTAL}
  }
  BottomNav.skipTransition(this,true)
  animated=savedInstanceState?.getBoolean("entrancePlayed")?:false
  binding.topBar.screenTitle.text="Thành quả hành trình"
  binding.topBar.backButton.setOnClickListener{finish()}
  binding.state.stateRetry.setOnClickListener{model.load()}
  binding.viewPlaceButton.setOnClickListener{navigate(true)}
  binding.continueButton.setOnClickListener{navigate(false)}
  model=ViewModelProvider(this,viewModelFactory{JourneyCompletionViewModel(id,JourneyCompletionModule.repository(this))})[JourneyCompletionViewModel::class.java]
  model.state.observe(this){state->when(state){
   Remote.Loading->binding.state.showLoading(binding.content,"Đang xác minh thành quả hành trình…")
   is Remote.Failed->binding.state.showError(binding.content,state.message)
   Remote.SessionExpired->openSignIn()
   is Remote.Ready->show(state.value)
  }}
 }
 private fun show(value:JourneyCompletion)=with(binding){
  result=value
  state.showContent(content)
  successMessage.text="Bạn đã hoàn thành hành trình khám phá ${value.placeName}."
  placeName.text=value.placeName
  regionName.text=value.regionName.orEmpty()
  regionName.visibility=if(value.regionName.isNullOrBlank())View.GONE else View.VISIBLE
  val format=NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"))
  stepsValue.text=format.format(value.recordedSteps)
  stepsValue.contentDescription="${stepsValue.text} bước hành trình được máy chủ ghi nhận"
  pointsValue.text="+${format.format(value.totalPoints)}"
  pointsValue.contentDescription="Tổng cộng ${format.format(value.totalPoints)} điểm đã nhận trong hành trình"
  val rewards=mutableListOf("Khám phá địa danh   +${format.format(value.rankingPoints)} điểm")
  if(value.quizAnswered>0) rewards.add("Trả lời đúng ${value.quizCorrect}/${value.quizAnswered} câu   +${format.format(value.quizPoints)} điểm")
  if(value.masteryPoints>0) rewards.add("Thưởng Am hiểu   +${format.format(value.masteryPoints)} điểm")
  rewardBreakdown.text=rewards.joinToString("\n")
  val before=value.rankBefore;val after=value.rankAfter
  rankValue.text=when{after==null->"Chưa có thứ hạng";before==null->"#${format.format(after)}";else->"#${format.format(before)} → #${format.format(after)}"}
  rankChange.text=when{
   after==null->"Chưa ghi nhận vị trí trong tuần hoàn thành."
   before==null->"Bạn đã có tên trên bảng xếp hạng!"
   after<before->"↑ Tăng ${format.format(before-after)} bậc khi hoàn thành"
   after>before->"↓ Giảm ${format.format(after-before)} bậc khi hoàn thành"
   else->"Giữ vững vị trí #${format.format(after)}"
  }
  rankChange.setTextColor(getColor(if(after!=null&&before!=null&&after>before) R.color.forest else R.color.teal))
  completedTime.text=runCatching{"Hoàn thành lúc "+LocalDateTime.parse(value.completedAt).format(DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy"))}.getOrDefault("")
  completedTime.visibility=if(completedTime.text.isEmpty())View.GONE else View.VISIBLE
  cardSection.visibility=if(value.card==null)View.GONE else View.VISIBLE
  value.card?.let{card->
   cardTitle.text=if(card.newlyAwarded)"ĐÃ MỞ KHÓA THẺ MỚI" else "THẺ TRONG BỘ SƯU TẬP"
   cardName.text=card.name
   cardSection.contentDescription=if(card.newlyAwarded)"Đã mở khóa thẻ ${card.name}. Thẻ đã được lưu vào bộ sưu tập." else "Thẻ ${card.name} đã có trong bộ sưu tập."
  }
  if(!imagesBound){
   imagesBound=true
   loadImage(placeImage,value.imageUrl,"Ảnh ${value.placeName}")
   value.card?.let{loadImage(cardImage,it.imageUrl,"Thẻ ${it.name}")}
  }
  // Acknowledges presentation only. Rewards and ownership are never written by this screen.
  CardInbox(this@JourneyCompletionActivity).markDialogShown(value.journeyId)
  if(!animated){
   animated=true
   if(ValueAnimator.areAnimatorsEnabled()){
    completionBody.alpha=0f;completionBody.animate().alpha(1f).setDuration(240).start()
    successIcon.scaleX=.95f;successIcon.scaleY=.95f
    successIcon.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
   }
  }
 }
 private fun loadImage(view:ImageView,url:String?,description:String){
  view.contentDescription=if(url.isNullOrBlank())"$description · Chưa có ảnh" else description
  if(url==null || !RemoteImages.supports(url))return
  lifecycleScope.launch{RemoteImages.load(url,2)?.let{
   view.setPadding(0,0,0,0);view.imageTintList=null
   view.scaleType=if(view.id==R.id.placeImage)ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
   view.setImageBitmap(it)
  }}
 }
 private fun navigate(place:Boolean){
  val receipt=result?:return
  if(leaving)return
  leaving=true;binding.viewPlaceButton.isEnabled=false;binding.continueButton.isEnabled=false
  val target=if(place)Intent(this,PlaceDetailActivity::class.java)
   .putExtra(PlaceDetailActivity.EXTRA_PLACE_ID,receipt.placeId)
   .putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME,receipt.placeName)
   .putExtra(PlaceDetailActivity.EXTRA_DISCOVERED,true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
  else Intent(this,ExploreActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
  startActivity(target);finish()
 }
 override fun onSaveInstanceState(outState:Bundle){outState.putBoolean("entrancePlayed",animated);super.onSaveInstanceState(outState)}
 override fun onPause(){
  binding.completionBody.animate().cancel();binding.completionBody.alpha=1f
  binding.successIcon.animate().cancel();binding.successIcon.scaleX=1f;binding.successIcon.scaleY=1f
  super.onPause()
 }
 override fun finish(){if(id>0)CardInbox(this).markDialogShown(id);super.finish();BottomNav.skipTransition(this,false)}
 companion object{const val EXTRA_JOURNEY_ID="completion_journey_id"}
}
