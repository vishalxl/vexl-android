package cz.cleevio.core.widget

import android.content.Context
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import cz.cleevio.core.R
import cz.cleevio.core.databinding.WidgetPriceRangeBinding
import cz.cleevio.core.model.PriceRangeValue
import cz.cleevio.repository.model.Currency
import cz.cleevio.repository.model.Currency.Companion.getCurrencySymbol
import cz.cleevio.vexl.lightbase.core.extensions.layoutInflater
import kotlinx.coroutines.*
import timber.log.Timber

class PriceRangeWidget @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private var binding: WidgetPriceRangeBinding = WidgetPriceRangeBinding.inflate(layoutInflater, this)
	private val coroutineScope = CoroutineScope(Dispatchers.Main)
	private var bottomLimitJob: Job? = null
	private var topLimitJob: Job? = null
	private var processLimitsJob: Job? = null

	private var currentCurrency = Currency.USD
	private var bottomLimit: Float = 0.0f
	private var topLimit: Float = 0.0f

	private var bottomLimitTextWatcher: TextWatcher? = null
	private var topLimitTextWatcher: TextWatcher? = null

	init {
		binding.priceRangeSlider.setCustomThumbDrawable(R.drawable.ic_picker)

		setupWithCurrency(currentCurrency)
		setupBottomLimitInputListener()
		setupTopLimitInputListener()
		setupPriceRangeListener()
	}

	fun setupWithCurrency(currency: Currency) {
		currentCurrency = currency
		val symbol = currency.getCurrencySymbol(context)
		binding.maxCurrency.text = symbol
		binding.minCurrency.text = symbol

		binding.priceRangeSlider.clearOnChangeListeners()
		when (currency) {
			Currency.CZK -> {
				binding.priceRangeSlider.valueFrom = BOTTOM_LIMIT_CZK.toFloat()
				binding.priceRangeSlider.valueTo = TOP_LIMIT_CZK.toFloat()
				setupPriceRangeListener()
				processLimits(BOTTOM_LIMIT_CZK.toFloat(), TOP_LIMIT_CZK.toFloat())
			}
			Currency.USD -> {
				binding.priceRangeSlider.valueFrom = BOTTOM_LIMIT_USD.toFloat()
				binding.priceRangeSlider.valueTo = TOP_LIMIT_USD.toFloat()
				setupPriceRangeListener()
				processLimits(BOTTOM_LIMIT_USD.toFloat(), TOP_LIMIT_USD.toFloat())
			}
			Currency.EUR -> {
				binding.priceRangeSlider.valueFrom = BOTTOM_LIMIT_EUR.toFloat()
				binding.priceRangeSlider.valueTo = TOP_LIMIT_EUR.toFloat()
				setupPriceRangeListener()
				processLimits(BOTTOM_LIMIT_EUR.toFloat(), TOP_LIMIT_EUR.toFloat())
			}
		}
	}

	fun getCurrencyName() = currentCurrency.name

	fun getPriceRangeValue(): PriceRangeValue = PriceRangeValue(
		topLimit = topLimit,
		bottomLimit = bottomLimit
	)

	fun reset() {
		val initValues = resources.getIntArray(R.array.initial_slider_values).map { it.toFloat() }
		binding.priceRangeSlider.values = initValues
		processLimits(initValues[0], initValues[1])
	}

	fun setValues(bottomLimit: Float, topLimit: Float) {
		processLimits(bottomLimit = bottomLimit, topLimit = topLimit)
	}

	private fun setupPriceRangeListener() {
		binding.priceRangeSlider.addOnChangeListener { slider, _, _ ->
			val currentValues = slider.values
			processLimits(currentValues[0], currentValues[1])
		}
	}

	private fun setupBottomLimitInputListener() {
		bottomLimitTextWatcher = binding.minInputValue.doAfterTextChanged { number ->
			try {
				val setValue = if (number.toString().toFloat() > topLimit) {
					topLimit
				} else {
					number.toString().toFloat()
				}

				bottomLimitJob?.cancel()
				bottomLimitJob = coroutineScope.launch {
					delay(DEBOUNCE_DELAY)
					processLimits(
						bottomLimit = setValue.coerceAtMost(binding.priceRangeSlider.valueTo),
						topLimit = topLimit
					)
				}
			} catch (e: NumberFormatException) {
				Timber.e(e)
			}
		}
	}

	private fun setupTopLimitInputListener() {
		topLimitTextWatcher = binding.maxInputValue.doAfterTextChanged { number ->
			try {
				val setValue = if (number.toString().toFloat() < bottomLimit) {
					bottomLimit
				} else {
					number.toString().toFloat()
				}
				topLimitJob?.cancel()
				topLimitJob = coroutineScope.launch {
					delay(DEBOUNCE_DELAY)
					processLimits(
						bottomLimit = bottomLimit,
						topLimit = setValue.coerceAtMost(binding.priceRangeSlider.valueTo)
					)
				}
			} catch (e: NumberFormatException) {
				Timber.e(e)
			}
		}
	}

	private fun processLimits(bottomLimit: Float, topLimit: Float) {
		processLimitsJob?.cancel()
		processLimitsJob = coroutineScope.launch {
			this@PriceRangeWidget.bottomLimit = bottomLimit
			this@PriceRangeWidget.topLimit = topLimit

			if (binding.minInputValue.text.toString() != bottomLimit.toInt().toString()) {
				bottomLimitTextWatcher?.let { binding.minInputValue.removeTextChangedListener(it) }
				binding.minInputValue.setText(bottomLimit.toInt().toString())
				binding.minInputValue.setSelection(binding.minInputValue.length())
				setupBottomLimitInputListener()
			}

			if (binding.maxInputValue.text.toString() != topLimit.toInt().toString()) {
				topLimitTextWatcher?.let { binding.maxInputValue.removeTextChangedListener(it) }
				binding.maxInputValue.setText(topLimit.toInt().toString())
				binding.maxInputValue.setSelection(binding.maxInputValue.length())
				setupTopLimitInputListener()
			}

			val currentSlider = binding.priceRangeSlider.values
			if (currentSlider[0] != bottomLimit || currentSlider[1] != topLimit) {
				binding.priceRangeSlider.clearOnChangeListeners()
				binding.priceRangeSlider.setValues(
					bottomLimit.coerceAtMost(binding.priceRangeSlider.valueTo),
					topLimit.coerceAtMost(binding.priceRangeSlider.valueTo)
				)
				setupPriceRangeListener()
			}
		}
	}

	private companion object {
		private const val BOTTOM_LIMIT_CZK = 0L
		private const val TOP_LIMIT_CZK = 250_000L
		private const val BOTTOM_LIMIT_EUR = 0L
		private const val TOP_LIMIT_EUR = 10_000L
		private const val BOTTOM_LIMIT_USD = 0L
		private const val TOP_LIMIT_USD = 10_000L
		private const val DEBOUNCE_DELAY = 800L
	}
}
