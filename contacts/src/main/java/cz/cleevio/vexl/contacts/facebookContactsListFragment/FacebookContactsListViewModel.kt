package cz.cleevio.vexl.contacts.facebookContactsListFragment

import cz.cleevio.core.base.BaseFacebookContactsListViewModel
import cz.cleevio.core.utils.NavMainGraphModel
import cz.cleevio.repository.repository.contact.ContactRepository


class FacebookContactsListViewModel constructor(
	private val contactRepository: ContactRepository,
	navMainGraphModel: NavMainGraphModel
) : BaseFacebookContactsListViewModel(contactRepository, navMainGraphModel) {

}