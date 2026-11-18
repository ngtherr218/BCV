package com.project.bcv.feature.discovery.presentation

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.project.bcv.databinding.ActivityExploreBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExploreMapToolLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun checkLayout(width: Int, height: Int, available: Int, collapsed: Boolean = false) {
        instrumentation.runOnMainSync {
            val app = instrumentation.targetContext
            val binding = ActivityExploreBinding.inflate(LayoutInflater.from(ContextThemeWrapper(app, app.applicationInfo.theme)))
            val density = app.resources.displayMetrics.density
            fun dp(value: Int) = (value * density).toInt()
            fun layout() {
                binding.root.measure(View.MeasureSpec.makeMeasureSpec(dp(width), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(height), View.MeasureSpec.EXACTLY))
                binding.root.layout(0,0,dp(width),dp(height))
            }
            binding.mapLoading.visibility=View.GONE
            binding.mapFailure.visibility=View.GONE
            binding.recenterButton.visibility=View.VISIBLE
            binding.quizMapButton.visibility=View.VISIBLE
            binding.addStepsMapButton.visibility=View.VISIBLE
            layout()
            binding.bottomSheet.layoutParams=binding.bottomSheet.layoutParams.apply { this.height=binding.mapContainer.height-dp(available) }
            layout()
            if(collapsed) binding.bottomSheet.translationY=(binding.bottomSheet.height-dp(86)).toFloat()
            ExploreMapToolLayout.apply(binding,true,collapsed,true)
            layout()
            fun rect(view: View)=Rect().also { view.getDrawingRect(it);binding.root.offsetDescendantRectToMyCoords(view,it) }
            if(binding.mapTools.visibility==View.VISIBLE) {
                val sheet=rect(binding.bottomSheet).apply { offset(0,binding.bottomSheet.translationY.toInt()) }
                val buttons=listOf(binding.recenterButton,binding.quizMapButton,binding.addStepsMapButton)
                buttons.forEach { button ->
                    assertTrue("48dp touch target",button.width>=dp(48) && button.height>=dp(48))
                    assertFalse("Map tool overlaps the journey sheet",Rect.intersects(rect(button),sheet))
                    assertTrue("Tool must remain inside the map",rect(binding.mapContainer).contains(rect(button)))
                    if(binding.locationCard.visibility==View.VISIBLE) assertFalse("Tool overlaps location card",Rect.intersects(rect(button),rect(binding.locationCard)))
                    if(binding.mapFilterBar.visibility==View.VISIBLE) assertFalse("Tool overlaps filter",Rect.intersects(rect(button),rect(binding.mapFilterBar)))
                }
                for(i in buttons.indices)for(j in i+1 until buttons.size)assertFalse("Buttons overlap",Rect.intersects(rect(buttons[i]),rect(buttons[j])))
            } else assertFalse("Only hide tools when the expanded sheet covers the map",collapsed)
            if(!collapsed && available==110) assertEquals(LinearLayout.HORIZONTAL,binding.mapTools.orientation)
        }
    }
    @Test fun portraitToolsDoNotOverlapCardsOrSheet()=checkLayout(360,740,250)
    @Test fun landscapeToolsReflowAboveSheet()=checkLayout(740,360,110)
    @Test fun narrowMapHidesLocationCardToKeepToolsClear()=checkLayout(320,480,110)
    @Test fun nearlyCoveredMapRestoresToolsAfterCollapse(){checkLayout(320,480,50);checkLayout(320,480,50,collapsed=true)}
}
