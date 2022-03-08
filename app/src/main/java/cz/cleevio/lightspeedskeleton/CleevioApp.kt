package cz.cleevio.lightspeedskeleton

import android.app.Application
import cz.cleeevio.onboarding.di.onboardingModule
import cz.cleevio.cache.di.cacheModule
import cz.cleevio.core.di.coreModule
import cz.cleevio.core.utils.CustomDebugTree
import cz.cleevio.lightspeedskeleton.di.appModule
import cz.cleevio.lightspeedskeleton.di.viewModelsModule
import cz.cleevio.network.di.networkModule
import cz.cleevio.pin.di.pinModule
import cz.cleevio.repository.di.repoModule
import lightbase.countrypicker.di.countryPickerModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class CleevioApp : Application() {

	override fun onCreate() {
		super.onCreate()

		if (BuildConfig.DEBUG) {
			Timber.plant(CustomDebugTree())
		}

		startKoin {
			//androidLogger()
			androidContext(this@CleevioApp)
			modules(
				listOf(
					appModule,
					viewModelsModule,
					pinModule,
					countryPickerModule,
					onboardingModule,
					coreModule,
					cacheModule,
					networkModule,
					repoModule
				)
			)
		}
	}
}