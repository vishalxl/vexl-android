package cz.cleevio.vexl.marketplace.newOfferFragment

import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cz.cleevio.core.model.OfferType
import cz.cleevio.core.model.toUnixTimestamp
import cz.cleevio.core.utils.OfferUtils
import cz.cleevio.core.utils.repeatScopeOnStart
import cz.cleevio.core.utils.viewBinding
import cz.cleevio.network.data.Status
import cz.cleevio.vexl.lightbase.core.baseClasses.BaseFragment
import cz.cleevio.vexl.lightbase.core.extensions.dpValueToPx
import cz.cleevio.vexl.lightbase.core.extensions.listenForInsets
import cz.cleevio.vexl.marketplace.LocationSuggestionAdapter
import cz.cleevio.vexl.marketplace.R
import cz.cleevio.vexl.marketplace.databinding.FragmentNewOfferBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

//TODO: !!! DULEZITE !!!
//TODO: vsechny zmeny ktere se budou delat tady v NewOfferFragment a taky v NewOfferViewModel je treba udelat
//TODO: i uvnitr EditOfferFragment a EditOfferViewModel.
//TODO: UI zmeny je treba udelat i uvnitr FiltersFramgent (funkcionalita filtru jeste neni udelana, takze tam staci UI)
//TODO: !!! DULEZITE !!!


//TODO: upravit fragment_new_offer, dat tam recycler s Grid managerem podle figmy
//TODO: https://www.figma.com/file/xDQNDwtoq8R0Aj1s4cKZTt/Vexl-App-V1.0-UI?node-id=2562%3A16719
class NewOfferFragment : BaseFragment(R.layout.fragment_new_offer) {

	private val binding by viewBinding(FragmentNewOfferBinding::bind)
	override val viewModel by viewModel<NewOfferViewModel>()

	private val args by navArgs<NewOfferFragmentArgs>()

	//TODO: vytvorit tady novy adapter ktery bude zobrazovat skupiny v gridu podle figmy
	//TODO: https://www.figma.com/file/xDQNDwtoq8R0Aj1s4cKZTt/Vexl-App-V1.0-UI?node-id=2562%3A16719

	override fun bindObservers() {
		repeatScopeOnStart {
			viewModel.userFlow.collect {
				it?.let { user ->
					binding.newOfferFriendLevel.setUserAvatar(user.avatar, user.anonymousAvatarImageIndex)
				}
			}
		}

		repeatScopeOnStart {
			viewModel.newOfferRequest.collect { resource ->
				when (resource.status) {
					is Status.Success -> {
						findNavController().popBackStack()
					}
					is Status.Error -> {
						binding.newOfferBtn.isVisible = true
						binding.progress.isVisible = false
					}
				}
			}
		}

		repeatScopeOnStart {
			viewModel.groups.collect { groups ->
				//TODO: tady je treba submitnout skupiny do adapteru
				//TODO: napojeni flow do DB je hotove
			}
		}

		repeatScopeOnStart {
			viewModel.suggestions.collect { (offerLocationItem, queries) ->
				if (queries.isEmpty()) return@collect
				if (queries.map { it.city }.contains(offerLocationItem?.getEditText()?.text.toString())) {
					offerLocationItem?.setLocation(queries.first())
					return@collect
				}

				offerLocationItem?.getEditText()?.let {
					it.setAdapter(null)
					val adapter = LocationSuggestionAdapter(queries, requireActivity())

					it.dropDownVerticalOffset = requireContext().dpValueToPx(SUGGESTION_PADDING).toInt()
					it.setDropDownBackgroundResource(R.drawable.background_rounded)
					it.setAdapter(adapter)
					it.showDropDown()
					it.setOnItemClickListener { _, _, position, _ ->
						offerLocationItem.setLocation(queries[position])
					}
				}
			}
		}

		repeatScopeOnStart {
			viewModel.queryForSuggestions.collect { (view, query) ->
				view?.let {
					viewModel.getDebouncedSuggestions(query, it)
				}
			}
		}
	}

	override fun initView() {
		viewModel.loadMyContactsKeys()

		binding.newOfferTitle.setTypeAndTitle(
			when (args.offerType) {
				OfferType.BUY -> getString(R.string.offer_create_buy_title)
				OfferType.SELL -> getString(R.string.offer_create_sell_title)
			}
		)
		binding.newOfferTitle.setListeners(
			onClose = {
				//close this screen
				findNavController().popBackStack()
			}
		)

		binding.descriptionCounter.text = getString(R.string.widget_offer_description_counter, 0, MAX_INPUT_LENGTH)
		binding.newOfferDescription.addTextChangedListener {
			binding.descriptionCounter.text = getString(
				R.string.widget_offer_description_counter, it?.length ?: 0, MAX_INPUT_LENGTH
			)
		}

		binding.newOfferLocation.setFragmentManager(parentFragmentManager)
		binding.newOfferDeleteTrigger.setFragmentManager(parentFragmentManager)

		binding.advancedBtn.setOnClickListener {
			viewModel.isAdvancedSectionShowed = !viewModel.isAdvancedSectionShowed
			if (viewModel.isAdvancedSectionShowed) {
				binding.advancedGroup.isVisible = true
				binding.advancedBtn.animate().setDuration(DURATION).rotation(START_ROTATION).start()
				Handler(Looper.getMainLooper()).postDelayed(DURATION) {
					binding.nestedScrollView.smoothScrollTo(
						binding.newOfferFriendLevel.x.toInt(),
						binding.newOfferFriendLevel.y.toInt() - Resources.getSystem().displayMetrics.heightPixels / 3
					)
				}
			} else {
				binding.advancedGroup.isVisible = false
				binding.advancedBtn.animate().setDuration(DURATION).rotation(MAX_ROTATION).start()
			}
		}

		binding.triggerBtn.setOnClickListener {
			viewModel.isTriggerSectionShowed = !viewModel.isTriggerSectionShowed
			if (viewModel.isTriggerSectionShowed) {
				binding.triggerGroup.isVisible = true
				binding.triggerBtn.animate().setDuration(DURATION).rotation(START_ROTATION).start()
				Handler(Looper.getMainLooper()).postDelayed(DURATION) {
					binding.nestedScrollView.smoothScrollTo(
						binding.newOfferDeleteTrigger.x.toInt(),
						binding.newOfferDeleteTrigger.y.toInt() - Resources.getSystem().displayMetrics.heightPixels / DISPLAY_THIRD
					)
				}
			} else {
				binding.triggerGroup.isVisible = false
				binding.triggerBtn.animate().setDuration(DURATION).rotation(MAX_ROTATION).start()
			}
		}

		binding.newOfferPriceTrigger.onFocusChangeListener = { hasFocus ->
			if (hasFocus) {
				binding.nestedScrollView.smoothScrollTo(
					binding.newOfferPriceTrigger.x.toInt(),
					binding.newOfferPriceTrigger.y.toInt() - Resources.getSystem().displayMetrics.heightPixels / DISPLAY_THIRD
				)
			}
		}

		binding.newOfferDeleteTrigger.onFocusChangeListener = { hasFocus ->
			if (hasFocus) {
				binding.nestedScrollView.smoothScrollTo(
					binding.newOfferDeleteTrigger.x.toInt(),
					binding.newOfferDeleteTrigger.y.toInt() - Resources.getSystem().displayMetrics.heightPixels / DISPLAY_THIRD
				)
			}
		}

		binding.newOfferLocation.setupFocusChangeListener { hasFocus, locationItem ->
			if (hasFocus) {
				binding.nestedScrollView.smoothScrollTo(
					binding.newOfferLocation.x.toInt(),
					locationItem.height *
						binding.newOfferLocation.getPositionOfItem(locationItem) +
						requireContext().dpValueToPx(OFFER_ITEM_PADDING).toInt() +
						binding.newOfferLocation.y.toInt() -
						Resources.getSystem().displayMetrics.heightPixels / DISPLAY_THIRD
				)
			}
		}

		binding.newOfferLocation.setupOnTextChanged { query, view ->
			viewModel.getSuggestions(query, view)
		}

		binding.newOfferCurrency.onCurrencyPicked = {
			binding.newOfferRange.setupWithCurrency(it)
			binding.newOfferPriceTrigger.setCurrency(it)
		}

		binding.newOfferBtn.text = when (args.offerType) {
			OfferType.BUY -> getString(R.string.offer_create_buy_btn)
			OfferType.SELL -> getString(R.string.offer_create_sell_btn)
		}
		binding.newOfferBtn.setOnClickListener {
			val params = OfferUtils.isOfferParamsValid(
				activity = requireActivity(),
				description = binding.newOfferDescription.text.toString(),
				location = binding.newOfferLocation.getLocationValue(),
				fee = binding.newOfferFee.getFeeValue(),
				priceRange = binding.newOfferRange.getPriceRangeValue(),
				friendLevel = binding.newOfferFriendLevel.getSingleChoiceFriendLevelValue(),
				priceTrigger = binding.newOfferPriceTrigger.getPriceTriggerValue(),
				btcNetwork = binding.newOfferBtcNetwork.getBtcNetworkValue(),
				paymentMethod = binding.newOfferPaymentMethod.getPaymentValue(),
				offerType = args.offerType.name,
				expiration = binding.newOfferDeleteTrigger.getValue().toUnixTimestamp(),
				active = true,
				currency = binding.newOfferRange.currentCurrency.name
			)

			if (params != null) {
				binding.newOfferBtn.isVisible = false
				binding.progress.isVisible = true
				viewModel.createOffer(params)
			}
		}

		listenForInsets(binding.container) { insets ->
			binding.container.updatePadding(
				top = insets.top,
				bottom = insets.bottomWithIME
			)
		}
	}

	companion object {
		const val MAX_INPUT_LENGTH = 140
		private const val DISPLAY_THIRD = 3
		private const val SUGGESTION_PADDING = 8
		private const val OFFER_ITEM_PADDING = 32
		private const val DURATION = 300L
		private const val MAX_ROTATION = 180f
		private const val START_ROTATION = 0f
	}
}
