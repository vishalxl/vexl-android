package cz.cleevio.lightspeedskeleton.di

import cz.cleevio.lightspeedskeleton.ui.mainActivity.MainViewModel
import cz.cleevio.lightspeedskeleton.ui.splashFragment.SplashViewModel
import org.koin.dsl.module

val viewModelsModule = module {

	single {
		MainViewModel(
			navMainGraphModel = get(),
		)
	}

	single {
		SplashViewModel(
			encryptedPreferences = get(),
			userRepository = get(),
			offerRepository = get(),
			chatRepository = get(),
			navMainGraphModel = get()
		)
	}
}