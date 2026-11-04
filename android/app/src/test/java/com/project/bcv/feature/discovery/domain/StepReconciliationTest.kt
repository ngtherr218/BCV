package com.project.bcv.feature.discovery.domain

import org.junit.Assert.*
import org.junit.Test

class StepReconciliationTest {
    private fun healthy(primary: Long = 100, sensor: Long = 50) =
        StepReconciliation(StepProvider.HEALTH_CONNECT).observe(primary, sensor, 1000)

    @Test fun healthyPrimaryNeverAddsItsShadowSensor() {
        val next = healthy().observe(140, 90, 2000)
        assertEquals(140, next.total)
        assertEquals(StepProvider.HEALTH_CONNECT, next.provider)
    }
    @Test fun idleUserDoesNotTriggerFallbackEvenAfterHours() {
        val next = healthy().observe(100, 50, 7_200_000)
        assertEquals(100, next.total)
        assertEquals(StepProvider.HEALTH_CONNECT, next.provider)
    }
    @Test fun shortProviderDelayDoesNotSwitchTheSource() {
        val next = healthy().observe(100, 90, 120_999)
        assertEquals(StepProvider.HEALTH_CONNECT, next.provider)
        assertEquals(100, next.total)
    }
    @Test fun stalledPrimaryRecoversExactlyTheNewShadowSteps() {
        val next = healthy().observe(100, 70, 121_000)
        assertEquals(StepProvider.SENSOR, next.provider)
        assertEquals(120, next.total)
        assertEquals(125, next.observe(null, 75, 122_000).total)
    }
    @Test fun smallSensorNoiseDoesNotTriggerFallback() {
        assertEquals(StepProvider.HEALTH_CONNECT, healthy().observe(100, 69, 300_000).provider)
    }
    @Test fun healthyProgressResetsTheStallWindow() {
        val next = healthy().observe(130, 80, 120_000).observe(130, 110, 121_000)
        assertEquals(StepProvider.HEALTH_CONNECT, next.provider)
        assertEquals(130, next.total)
    }
    @Test fun providerErrorImmediatelyUsesOnlyUncoveredShadowSteps() {
        val next = healthy().observe(null, 60, 2000, failed = true)
        assertEquals(StepProvider.SENSOR, next.provider)
        assertEquals(110, next.total)
    }
    @Test fun firstProviderErrorDoesNotLoseStepsRecordedSinceCapture() {
        val next = StepReconciliation(StepProvider.HEALTH_CONNECT, total = 29, sensorAnchor = 29)
            .observe(null, 45, 2000, failed = true)
        assertEquals(45, next.total)
    }
    @Test fun lateHealthConnectBackfillNeverCountsStepsTwice() {
        val switched = healthy().observe(100, 80, 121_000)
        val later = switched.observe(130, 80, 122_000)
        assertEquals(130, later.total)
        assertEquals(StepProvider.SENSOR, later.provider)
        assertEquals(140, later.observe(140, 90, 123_000).total)
    }
    @Test fun restoredCheckpointContinuesWithoutDuplicatingPriorSteps() {
        val restored = StepReconciliation(StepProvider.SENSOR, total = 600, sensorAnchor = 250)
        assertEquals(600, restored.observe(null, 250, 1000).total)
        assertEquals(612, restored.observe(null, 262, 1000).total)
    }
    @Test fun rebootResetsTheObservationWindowInsteadOfDeclaringAStall() {
        val next = healthy().observe(100, 90, 1)
        assertEquals(StepProvider.HEALTH_CONNECT, next.provider)
        assertEquals(100, next.total)
    }
    @Test fun primaryReclassificationCannotSubtractConfirmedSteps() {
        val next = healthy().observe(90, 80, 2000)
        assertEquals(100, next.total)
        assertEquals(130, next.observe(90, 80, 121_000).total)
    }
    @Test fun noHardwareMeansNoSpeculativeFallback() {
        val next = healthy().observe(100, null, 900_000)
        assertEquals(StepProvider.HEALTH_CONNECT, next.provider)
        assertEquals(100, next.total)
    }
    @Test fun sensorResetNeverMakesTheJourneyGoBackwards() {
        val next = StepReconciliation(StepProvider.SENSOR, total = 600, sensorAnchor = 250)
            .observe(null, 0, 1000)
        assertEquals(600, next.total)
        assertEquals(605, next.observe(null, 5, 2000).total)
    }
    @Test fun oldRecordingCheckpointMovesToSensorWithoutRecountingPreviousSteps() {
        val restored = StepReconciliation.restore("RECORDING", 600, 600, 250, 1000, true)!!
        val next = restored.observe(null, 262, 2000)
        assertEquals(StepProvider.SENSOR, next.provider)
        assertEquals(612, next.total)
        assertEquals(612, next.observe(900, 262, 3000).total)
    }
    @Test fun oldRecordingCheckpointWithoutSensorUsesHealthConnectWithoutAddingTotals() {
        val restored = StepReconciliation.restore("RECORDING", 600, 600, 250, 1000, false)!!
        assertEquals(StepProvider.HEALTH_CONNECT, restored.provider)
        assertNull(restored.primaryRaw)
        assertNull(restored.sensorAnchor)
        val next = restored.observe(500, null, 2000)
        assertEquals(600, next.total)
        assertEquals(650, next.observe(650, null, 3000).total)
    }
    @Test fun existingHealthConnectAndSensorCheckpointsKeepTheirBaselines() {
        for (provider in StepProvider.entries) {
            val old = StepReconciliation(provider, 600, 500, 250, 1000)
            assertEquals(old, StepReconciliation.restore(provider.name, 600, 500, 250, 1000, true))
        }
        assertNull(StepReconciliation.restore("UNKNOWN", 600, 500, 250, 1000, true))
    }
    @Test fun partialCatchUpAfterBackgroundDoesNotDiscardTheUncoveredSensorWindow() {
        val next = healthy(primary = 100, sensor = 50).observe(120, 150, 600_000)
        assertEquals(StepProvider.SENSOR, next.provider)
        assertEquals(200, next.total)
        assertEquals(200, next.observe(200, 150, 601_000).total)
    }
    @Test fun fullCatchUpAfterBackgroundKeepsTheHealthyPrimaryWithoutAddingBothSources() {
        val next = healthy(primary = 100, sensor = 50).observe(200, 150, 600_000)
        assertEquals(StepProvider.HEALTH_CONNECT, next.provider)
        assertEquals(200, next.total)
    }
}
