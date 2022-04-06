package cz.cleevio.lightspeedskeleton.ui.mainActivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import cz.cleeevio.vexl.marketplace.marketplaceFragment.MarketplaceFragment
import cz.cleevio.core.utils.NavMainGraphModel
import cz.cleevio.core.utils.safeNavigateWithTransition
import cz.cleevio.lightspeedskeleton.NavMainDirections
import cz.cleevio.lightspeedskeleton.R
import cz.cleevio.lightspeedskeleton.databinding.ActivityMainBinding
import cz.cleevio.network.NetworkError
import cz.cleevio.profile.profileFragment.ProfileFragment
import kotlinx.coroutines.launch
import lightbase.core.extensions.showSnackbar
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

	private lateinit var binding: ActivityMainBinding
	private val viewModel by inject<MainViewModel>()
	private val errorFlow: NetworkError by inject()

	private lateinit var navController: NavController

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		window.setBackgroundDrawableResource(R.color.background)
		// Going to Edge to Edge
		WindowCompat.setDecorFitsSystemWindows(window, false)

		bindObservers()

		val navHostFragment = supportFragmentManager.findFragmentById(
			R.id.navHostFragment
		) as NavHostFragment
		navController = navHostFragment.navController
		navController.addOnDestinationChangedListener(this)

		binding.bottomNavigation.setOnItemSelectedListener {
			NavigationUI.onNavDestinationSelected(it, navController)
			true
		}
	}

	private fun bindObservers() {
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				viewModel.navGraphFlow.collect {
					when (it) {
						NavMainGraphModel.NavGraph.EmptyState -> Unit
						NavMainGraphModel.NavGraph.Contacts -> navController.safeNavigateWithTransition(
							NavMainDirections.actionGlobalToContacts()
						)
						NavMainGraphModel.NavGraph.Onboarding -> navController.safeNavigateWithTransition(
							NavMainDirections.actionGlobalToOnboarding()
						)
						NavMainGraphModel.NavGraph.Marketplace -> navController.safeNavigateWithTransition(
							NavMainDirections.actionGlobalToMarketplace()
						)
						NavMainGraphModel.NavGraph.Chat -> TODO()
						NavMainGraphModel.NavGraph.Profile -> navController.safeNavigateWithTransition(
							NavMainDirections.actionGlobalToProfile()
						)
					}
				}
			}
		}

		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				errorFlow.error.collect { error ->
					error?.message?.let { messageCode ->
						if (messageCode != -1) {
							showSnackbar(
								view = binding.container,
								message = getString(messageCode)
							)
						}
					}
				}
			}
		}
	}

	override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
		when (destination.label) {
			MarketplaceFragment::class.java.simpleName,
			ProfileFragment::class.java.simpleName ->
				binding.bottomNavigation.isVisible = true
			else ->
				binding.bottomNavigation.isVisible = false
		}
	}
}