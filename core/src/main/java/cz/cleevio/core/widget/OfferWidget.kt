package cz.cleevio.core.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import cz.cleevio.core.R
import cz.cleevio.core.databinding.WidgetOfferBinding
import cz.cleevio.core.utils.BuySellColorizer.colorizeTransactionType
import cz.cleevio.core.utils.formatAsPercentage
import cz.cleevio.repository.model.offer.Offer
import cz.cleevio.vexl.lightbase.core.extensions.layoutInflater
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

class OfferWidget @JvmOverloads constructor(
	context: Context,
	val attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

	private lateinit var binding: WidgetOfferBinding

	init {
		setupUI()
	}

	fun bind(item: Offer, requestOffer: ((String) -> Unit)? = null, mode: Mode? = null) {
		binding.card.offerDescription.text = item.offerDescription
		binding.card.priceLimit.text = "${(item.amountTopLimit / BigDecimal(THOUSAND)).toInt()}k"
		binding.card.priceCurrency.text = item.currency
		binding.card.offerType.text = if (item.offerType == "SELL") {
			resources.getString(R.string.offer_to_sell)
		} else {
			resources.getString(R.string.offer_to_buy)
		}
		if (mode == Mode.MY_OFFER) {
			binding.userName.text = context.getString(R.string.offer_my_offer)
		} else {
			if (item.offerType == "SELL") {
				colorizeTransactionType(
					resources.getString(R.string.marketplace_detail_user_sell, "Unknown friend "),
					"Unknown friend",
					binding.userName,
					R.color.pink_100
				)
			} else {
				colorizeTransactionType(
					resources.getString(R.string.marketplace_detail_user_buy, "Unknown friend "),
					"Unknown friend",
					binding.userName,
					R.color.green_100
				)
			}
		}

		binding.card.location.text = item.location.joinToString(", ") { it.city }

		binding.userType.text = if (mode == Mode.MY_OFFER) {
			context.getString(R.string.offer_added, myOfferFormat.format(item.createdAt))
		} else {
			if (item.friendLevel == "FIRST") {
				resources.getString(R.string.marketplace_detail_friend_first)
			} else {
				resources.getString(R.string.marketplace_detail_friend_second)
			}
		}

		binding.card.feeDescription.isVisible = item.feeState == "WITH_FEE"
		binding.card.feeDescription.text =
			resources.getString(R.string.marketplace_detail_fee, item.feeAmount.formatAsPercentage())

		binding.card.paymentMethod.text = item.paymentMethod.joinToString(", ") {
			it.lowercase().replaceFirstChar { a -> a.uppercase() }
		}
		binding.card.paymentMethodIcons.bind(item.paymentMethod)

		if (mode == Mode.MY_OFFER) {
			binding.requestBtn.isVisible = false
			binding.editBtn.isVisible = true
		} else {
			binding.requestBtn.isVisible = true
			binding.editBtn.isVisible = false

			binding.requestBtn.isVisible = requestOffer != null
			binding.requestBtn.text = if (item.isRequested) {
				context.getString(R.string.offer_requested)
			} else {
				context.getString(R.string.offer_request)
			}

			setUiColor(item.isRequested)
		}
		binding.requestBtn.setOnClickListener {
			requestOffer?.invoke(item.offerId)
		}
		binding.editBtn.setOnClickListener {
			requestOffer?.invoke(item.offerId)
		}
	}

	private fun setupUI() {
		binding = WidgetOfferBinding.inflate(layoutInflater, this)

		val widgetMode = getMode()
		binding.userInformationGroup.isVisible = widgetMode == Mode.MARKETPLACE
		if (widgetMode == Mode.CHAT) {
			binding.card.offerWrapper.run {
				setCardBackgroundColor(resources.getColor(R.color.gray_6, null))
				cardElevation = 0.0f
			}
		}
	}

	private fun getMode(): Mode {
		val a = context.theme.obtainStyledAttributes(
			attrs,
			R.styleable.OfferWidget,
			0, 0
		)
		val value = a.getInt(R.styleable.OfferWidget_widget_mode, 0)
		return Mode.values()[value]
	}

	private fun setUiColor(isRequested: Boolean) {
		binding.requestBtn.setTextColor(
			if (isRequested) {
				context.getColor(R.color.white)
			} else {
				context.getColor(R.color.black)
			}
		)
		binding.requestBtn.icon = if (isRequested) {
			context.getDrawable(R.drawable.ic_succesfull_blue)
		} else {
			context.getDrawable(R.drawable.ic_eye)
		}

		binding.card.offerWrapper.run {
			setCardBackgroundColor(
				if (isRequested) resources.getColor(R.color.gray_1, null) else resources.getColor(R.color.gray_6, null)
			)
			cardElevation = 0.0f
		}

		binding.card.offerDescription.setTextColor(
			if (isRequested) resources.getColor(R.color.gray_3, null) else resources.getColor(R.color.black, null)
		)

		binding.card.feeDescription.setTextColor(
			if (isRequested) resources.getColor(R.color.gray_3, null) else resources.getColor(R.color.black, null)
		)

		binding.arrowImage.setColorFilter(
			ContextCompat.getColor(
				context,
				if (isRequested) R.color.gray_1 else R.color.white
			)
		)

		binding.requestBtn.isEnabled = !isRequested
	}

	enum class Mode {
		MARKETPLACE, CHAT, MY_OFFER
	}

	companion object {
		const val THOUSAND = 1000
		val myOfferFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd. MM. yyyy")
	}
}
