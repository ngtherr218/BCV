package com.project.bcv.feature.collection.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.project.bcv.core.di.CollectionModule
import com.project.bcv.feature.collection.data.CardInbox
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.journey.completion.JourneyCompletionActivity

/** Backend completion state and one-shot navigation are intentionally separate. */
class JourneyRewardController(private val activity:AppCompatActivity,private val changed:()->Unit):DefaultLifecycleObserver{
 private val inbox=CardInbox(activity)
 private var journey:ExploreJourney?=null
 private var active=false
 private var opening=false
 init{activity.lifecycle.addObserver(this)}
 fun completed(value:ExploreJourney){
  if(value.status!="COMPLETED" || value.requiredSteps<=0 || value.recordedSteps<value.requiredSteps || (value.destinationPlaceId?:0)<=0)return
  journey=value
  if(inbox.recordJourney(value.id,value.destinationPlaceId!!)){CollectionModule.repository(activity).invalidate();changed()}
  show()
 }
 override fun onResume(owner:LifecycleOwner){active=true;opening=false;show()}
 override fun onPause(owner:LifecycleOwner){active=false}
 private fun show(){
  val trip=journey?:return
  if(!active||opening||activity.isFinishing||inbox.dialogShown(trip.id))return
  opening=true
  activity.startActivity(Intent(activity,JourneyCompletionActivity::class.java)
   .putExtra(JourneyCompletionActivity.EXTRA_JOURNEY_ID,trip.id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
 }
}
