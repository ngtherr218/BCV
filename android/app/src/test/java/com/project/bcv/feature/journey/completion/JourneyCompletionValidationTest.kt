package com.project.bcv.feature.journey.completion
import org.junit.Assert.*
import org.junit.Test
class JourneyCompletionValidationTest{
 private val receipt=JourneyCompletion(12,30,"Hồ Hoàn Kiếm",null,null,1234,1234,150,"2026-11-13T10:00:00",null)
 @Test fun completedReceiptIsValid(){assertTrue(receipt.validFor(12))}
 @Test fun progressAloneIsNotACompletion(){assertFalse(receipt.copy(status="IN_PROGRESS").validFor(12))}
 @Test fun wrongJourneyIsRejected(){assertFalse(receipt.validFor(13))}
 @Test fun invalidIdsAreRejected(){assertFalse(receipt.copy(placeId=0).validFor(12));assertFalse(receipt.validFor(0))}
 @Test fun incompleteStepsAreRejected(){assertFalse(receipt.copy(recordedSteps=100).validFor(12))}
 @Test fun zeroPointsAreValid(){assertTrue(receipt.copy(rankingPoints=0).validFor(12))}
 @Test fun negativePointsAreRejected(){assertFalse(receipt.copy(rankingPoints=-1).validFor(12))}
 @Test fun completedTimeIsRequired(){assertFalse(receipt.copy(completedAt="").validFor(12))}
}
