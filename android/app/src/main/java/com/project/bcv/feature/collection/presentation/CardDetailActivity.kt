package com.project.bcv.feature.collection.presentation

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.project.bcv.R
import com.project.bcv.core.ui.RemoteImages
import com.project.bcv.core.ui.ScreenMotion
import com.project.bcv.databinding.ActivityCardDetailBinding
import com.project.bcv.feature.discovery.presentation.PlaceDetailActivity
import kotlinx.coroutines.launch

/** "Thẻ địa danh": one collectible card, shown from what the collection screen already loaded. */
class CardDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCardDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        binding = ActivityCardDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val placeId = intent.getLongExtra(EXTRA_PLACE_ID, 0L)
        val name = intent.getStringExtra(EXTRA_CARD_NAME).orEmpty()
        val owned = intent.getBooleanExtra(EXTRA_OWNED, false)
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
        if (placeId <= 0) { finish(); return }

        binding.backButton.setOnClickListener { finish() }
        show(name, owned, imageUrl)
        if (owned && intent.getBooleanExtra(EXTRA_IS_NEW, false)) {
            binding.headline.text = "Thẻ địa danh mới của bạn!"
            binding.ownedChip.text = "MỚI · Vừa nhận"
            binding.cardFrame.scaleX = .88f
            binding.cardFrame.scaleY = .88f
            binding.cardFrame.animate().scaleX(1f).scaleY(1f).setDuration(500)
                .setInterpolator(android.view.animation.OvershootInterpolator()).start()
            val cardId = intent.getLongExtra(EXTRA_CARD_ID, 0L)
            if (cardId > 0) lifecycleScope.launch {
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        com.project.bcv.core.di.CollectionModule.repository(this@CardDetailActivity).markSeen(cardId)
                    }
                } catch (error: kotlinx.coroutines.CancellationException) { throw error }
                catch (_: Exception) {
                    com.google.android.material.snackbar.Snackbar.make(binding.root,
                        "Chưa lưu được trạng thái đã xem. Thẻ sẽ vẫn được đánh dấu mới.",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                }
            }
        }
        binding.placeButton.setOnClickListener {
            startActivity(Intent(this, PlaceDetailActivity::class.java)
                .putExtra(PlaceDetailActivity.EXTRA_PLACE_ID, placeId)
                .putExtra(PlaceDetailActivity.EXTRA_PLACE_NAME, name)
                .putExtra(PlaceDetailActivity.EXTRA_DISCOVERED, owned))
        }
        if (savedInstanceState == null) ScreenMotion.reveal(binding.cardFrame)
    }

    private fun show(name: String, owned: Boolean, imageUrl: String?) = with(binding) {
        val teal = ContextCompat.getColor(this@CardDetailActivity, R.color.teal)
        val muted = 0xFF8A7E66.toInt()
        collectionEyebrow.text = intent.getStringExtra(EXTRA_COLLECTION_NAME).orEmpty()
        headline.text = if (owned) "Một dấu chân đáng nhớ." else "Một dấu chân đang chờ."
        cardName.text = name
        cardFace.contentDescription = "Thẻ địa danh $name, " + if (owned) "đã sở hữu" else "chưa sở hữu"

        cardLock.visibility = if (owned) View.GONE else View.VISIBLE
        cardImageFrame.setBackgroundResource(if (owned) R.drawable.card_image_bg else R.drawable.collection_lock_placeholder)
        imageUrl?.takeIf { owned }?.let { url ->
            RemoteImages.cached(url, sample = 2)?.let(cardImage::setImageBitmap)
                ?: lifecycleScope.launch { RemoteImages.load(url, sample = 2)?.let(cardImage::setImageBitmap) }
        }

        ownedChip.text = if (owned) "Đã sở hữu" else "Chưa sở hữu"
        ownedChip.setTextColor(if (owned) teal else muted)
        ownedChip.setBackgroundResource(if (owned) R.drawable.card_owned_chip_bg else R.drawable.card_locked_chip_bg)
        ownedChip.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (owned) R.drawable.ic_check_circle else R.drawable.ic_lock, 0, 0, 0)
        ownedChip.compoundDrawablesRelative.firstOrNull()?.let { icon ->
            val size = (20 * resources.displayMetrics.density).toInt()
            icon.setBounds(0, 0, size, size)
            ownedChip.setCompoundDrawablesRelative(icon, null, null, null)
        }
        ownedChip.compoundDrawableTintList = ColorStateList.valueOf(if (owned) teal else muted)

        ownedNote.text = if (owned) "Được thêm sau lần đầu khám phá địa danh.\nThẻ đã có trong bộ sưu tập của bạn."
            else "Hoàn thành hành trình tới địa danh này\nđể nhận thẻ vào bộ sưu tập."
        placeButton.text = if (owned) "Xem nội dung địa danh" else "Xem địa danh"
    }

    companion object {
        const val EXTRA_CARD_ID = "card_id"
        const val EXTRA_IS_NEW = "card_is_new"
        const val EXTRA_CARD_NAME = "card_name"
        const val EXTRA_PLACE_ID = "place_id"
        const val EXTRA_IMAGE_URL = "image_url"
        const val EXTRA_OWNED = "owned"
        const val EXTRA_COLLECTION_NAME = "collection_name"
    }
}
