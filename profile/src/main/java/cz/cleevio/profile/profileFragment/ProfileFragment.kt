package cz.cleevio.profile.profileFragment

import android.widget.Toast
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cz.cleevio.core.base.BaseGraphFragment
import cz.cleevio.core.utils.repeatScopeOnStart
import cz.cleevio.core.utils.viewBinding
import cz.cleevio.core.widget.BlockUserBottomSheetDialog
import cz.cleevio.core.widget.CurrencyPriceChartWidget
import cz.cleevio.core.widget.DeleteAccountBottomSheetDialog
import cz.cleevio.profile.R
import cz.cleevio.profile.databinding.FragmentProfileBinding
import lightbase.core.extensions.listenForInsets
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ProfileFragment : BaseGraphFragment(R.layout.fragment_profile) {

	private val profileViewModel by viewModel<ProfileViewModel>()
	private val binding by viewBinding(FragmentProfileBinding::bind)

	override var priceChartWidget: CurrencyPriceChartWidget? = null

	override fun bindObservers() {
		super.bindObservers()
		repeatScopeOnStart {
			profileViewModel.userFlow.collect {
				it?.let { user ->
					binding.profileUserName.text = user.username
					//todo: this should convert from base64 to bitmap
					binding.profileUserPhoto.load(user.avatar) {
						crossfade(true)
						fallback(R.drawable.ic_baseline_person_128)
						error(R.drawable.ic_baseline_person_128)
						placeholder(R.drawable.ic_baseline_person_128)
					}
				}
			}
		}

		repeatScopeOnStart {
			profileViewModel.contactsNumber.collect {
				binding.profileContacts.setSubtitle(
					getString(R.string.profile_import_contacts_subtitle, it.toString())
				)
			}
		}
	}

	override fun initView() {
		priceChartWidget = binding.priceChart

		super.initView()
		listenForInsets(binding.container) { insets ->
			binding.container.updatePadding(
				top = insets.top,
				bottom = insets.bottom
			)
		}

		binding.profileGroups.setOnClickListener {
			findNavController().navigate(
				ProfileFragmentDirections.actionProfileFragmentToGroupFragment()
			)
		}

		binding.profileChangePicture.setOnClickListener {
			Toast.makeText(requireContext(), "Profile picture change not implemented", Toast.LENGTH_SHORT)
				.show()
		}

		binding.profileEditName.setOnClickListener {
			findNavController().navigate(
				ProfileFragmentDirections.actionProfileFragmentToEditNameFragment()
			)
		}

		binding.profileSetPin.setOnClickListener {
			Toast.makeText(requireContext(), "Pin not implemented", Toast.LENGTH_SHORT)
				.show()
		}

		binding.profileContacts.setOnClickListener {
			Toast.makeText(requireContext(), "Contact import not implemented", Toast.LENGTH_SHORT)
				.show()
		}

		binding.profileFacebook.setOnClickListener {
			Toast.makeText(requireContext(), "Facebook import not implemented", Toast.LENGTH_SHORT)
				.show()
		}

		binding.profileRequestData.setOnClickListener {
			Toast.makeText(requireContext(), "Data request not implemented", Toast.LENGTH_SHORT)
				.show()
		}

		binding.profileLogout.setOnClickListener {
			showBottomDialog(
				DeleteAccountBottomSheetDialog {
					if (it) {
						profileViewModel.logout(
							{
								profileViewModel.navigateToOnboarding()
							},
							{
								profileViewModel.navigateToOnboarding()
							}
						)
					}
				}
			)
		}
	}

	private fun showBottomDialog(dialog: BottomSheetDialogFragment) {
		dialog.show(childFragmentManager, dialog.javaClass.simpleName)
	}
}