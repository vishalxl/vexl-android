package cz.cleeevio.vexl.contacts.finishImportFragment

import androidx.lifecycle.lifecycleScope
import cz.cleeevio.vexl.contacts.R
import cz.cleevio.core.utils.viewBinding
import kotlinx.coroutines.launch
import lightbase.core.baseClasses.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class FinishImportFragment : BaseFragment(R.layout.fragment_finish_import) {

	private val binding by viewBinding(cz.cleeevio.vexl.contacts.databinding.FragmentFinishImportBinding::bind)
	override val viewModel by viewModel<FinishImportViewModel>()

	override fun bindObservers() {
		viewLifecycleOwner.lifecycleScope.launch {
			viewModel.user.collect { user ->
			}
		}
	}

	override fun initView() {
		viewModel.loadMyContactsKeys()


//		viewModel.navMainGraphModel.navigateToGraph(
//			NavMainGraphModel.NavGraph.Main
//		)
	}
}