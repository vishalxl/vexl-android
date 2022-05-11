package cz.cleevio.vexl.chat.di

import cz.cleevio.repository.model.user.User
import cz.cleevio.vexl.chat.chatContactList.ChatContactListViewModel
import cz.cleevio.vexl.chat.chatFragment.ChatViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


val chatModule = module {

	viewModel {
		ChatContactListViewModel(
			chatRepository = get()
		)
	}

	viewModel { (user: User) ->
		ChatViewModel(
			user = user,
			chatRepository = get()
		)
	}
}