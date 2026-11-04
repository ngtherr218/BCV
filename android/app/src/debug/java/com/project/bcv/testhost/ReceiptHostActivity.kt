package com.project.bcv.testhost

import com.project.bcv.core.network.ApiEnvironment

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.project.bcv.core.ui.JourneyCompletionDialog
import com.project.bcv.feature.discovery.domain.ExploreJourney
import com.project.bcv.feature.discovery.domain.JourneyCardReward
import com.project.bcv.feature.location.startingpoint.domain.Coordinates

/** Debug-only dialog host. No session restore, network requests or permission prompts. */
class ReceiptHostActivity : AppCompatActivity() {
    private val fixture = ExploreJourney(-2147483001, "Địa danh kiểm thử giao diện", Coordinates(21.0, 105.0),
        100, 100, "COMPLETED", 1, destinationPlaceId = -2147483001,
        awardedCard = JourneyCardReward(-2147483001, "Thẻ kiểm thử giao diện"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "Kiểm thử giao diện nhận thẻ" })
        if (intent.getBooleanExtra("reset_fixture", false) || intent.getBooleanExtra("clear_fixture", false)) {
            getSharedPreferences("bcv_card_receipts", MODE_PRIVATE).edit()
                .remove("${ApiEnvironment.currentUrl}:${fixture.id}:${fixture.awardedCard!!.id}").apply()
        }
        if (intent.getBooleanExtra("clear_fixture", false)) finish()
    }

    override fun onPostResume() {
        super.onPostResume()
        if (intent.getBooleanExtra("preview_receipt", false)) {
            window.decorView.post { JourneyCompletionDialog.showIfNeeded(this, fixture) }
        }
    }
}
