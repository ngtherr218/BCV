package com.project.bcv.feature.discovery.domain

import com.project.bcv.feature.location.startingpoint.domain.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Test

class JourneyMilestonesTest {
    private fun journey(recorded: Int, required: Int=100)=ExploreJourney(1,"Đích",Coordinates(21.0,105.0),required,recorded,"IN_PROGRESS",0)
    @Test fun milestonesRequireActualThresholds() {
        assertEquals(emptyList<Int>(),journey(24).reachedMilestones)
        assertEquals(listOf(25),journey(25).reachedMilestones)
        assertEquals(listOf(25,50),journey(50).reachedMilestones)
        assertEquals(listOf(25,50,75),journey(75).reachedMilestones)
        assertEquals(listOf(25,50,75,100),journey(100).reachedMilestones)
    }
    @Test fun invalidAndFractionalProgressDoNotInventMilestones() {
        assertEquals(emptyList<Int>(),journey(25,101).reachedMilestones)
        assertEquals(emptyList<Int>(),journey(100,0).reachedMilestones)
        assertEquals(listOf(25,50,75,100),journey(Int.MAX_VALUE).reachedMilestones)
    }
}
