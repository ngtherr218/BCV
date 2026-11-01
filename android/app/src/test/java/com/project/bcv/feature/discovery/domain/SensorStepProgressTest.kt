package com.project.bcv.feature.discovery.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorStepProgressTest {
    @Test fun initialHardwareTotalIsNotCreditedToTheJourney() {
        val baseline = SensorStepProgress().counter(80_000, 2)
        assertEquals(0L, baseline.total)
        assertEquals(123L, baseline.counter(80_123, 2).total)
    }
    @Test fun repeatedSamplesAreNotCountedTwice() {
        val steps = SensorStepProgress().counter(1000, 1).counter(1020, 1)
        assertEquals(20L, steps.counter(1020, 1).total)
    }
    @Test fun rebootKeepsEarnedStepsWithoutCreditingTheNewBootTotal() {
        val steps = SensorStepProgress(500, 12_000, 1).counter(14_000, 2)
        assertEquals(500L, steps.total)
        assertEquals(505L, steps.counter(14_005, 2).total)
    }
    @Test fun hardwareResetNeverSubtractsSteps() {
        val steps = SensorStepProgress(50, 1000, 1).counter(2, 1)
        assertEquals(50L, steps.total)
        assertEquals(53L, steps.counter(5, 1).total)
    }
    @Test fun invalidSamplesDoNotChangeTheBaseline() {
        val previous = SensorStepProgress(50, 1000, 1)
        assertEquals(previous, previous.counter(-1, 1))
    }
    @Test fun stepDetectorCountsOneStepPerEvent() {
        assertEquals(2L, SensorStepProgress().detected().detected().total)
    }
}
