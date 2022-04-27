package cz.cleevio.core.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import cz.cleevio.core.databinding.WidgetOfferPaymentMethodBinding
import cz.cleevio.core.model.PaymentMethodValue
import lightbase.core.extensions.layoutInflater

class OfferPaymentMethodWidget constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private lateinit var binding: WidgetOfferPaymentMethodBinding
	private var selectedButtons: MutableSet<PaymentButtonSelected> = mutableSetOf()

	init {
		setupUI()

		binding.paymentCash.setOnCheckedChangeListener { _, isChecked ->
			if (isChecked) {
				selectedButtons.add(PaymentButtonSelected.CASH)
			} else {
				selectedButtons.remove(PaymentButtonSelected.CASH)
			}
		}

		binding.paymentRevolut.setOnCheckedChangeListener { _, isChecked ->
			if (isChecked) {
				selectedButtons.add(PaymentButtonSelected.REVOLUT)
			} else {
				selectedButtons.remove(PaymentButtonSelected.REVOLUT)
			}
		}

		binding.paymentBank.setOnCheckedChangeListener { _, isChecked ->
			if (isChecked) {
				selectedButtons.add(PaymentButtonSelected.BANK)
			} else {
				selectedButtons.remove(PaymentButtonSelected.BANK)
			}
		}
	}

	private fun setupUI() {
		binding = WidgetOfferPaymentMethodBinding.inflate(layoutInflater, this)
	}

	fun getPaymentValue(): PaymentMethodValue = PaymentMethodValue(
		value = selectedButtons.toList()
	)
}

enum class PaymentButtonSelected {
	NONE, CASH, REVOLUT, BANK
}