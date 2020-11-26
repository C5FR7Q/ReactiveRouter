package com.example.reactiverouter.example

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.reactiverouter.R
import com.example.reactiverouter.base.ReactiveRouter
import com.example.reactiverouter.base.extractor.SimpleTagExtractor
import com.example.reactiverouter.base.navigator.SimpleNavigator
import com.example.reactiverouter.base.scopeprovider.SimpleScopeProvider

class DemoReactiveRouter(fragmentManager: FragmentManager) :
	ReactiveRouter<SimpleNavigator, DemoReactiveRouter.DemoScopeProvider>(fragmentManager) {

	override fun createTagExtractor() = SimpleTagExtractor()
	override fun createNavigator() = SimpleNavigator(fragmentManager, R.id.main_container)
	override fun createScopeProvider() = DemoScopeProvider()

	inner class DemoScopeProvider : SimpleScopeProvider<SimpleNavigator>() {
		fun replace(fragment: Fragment) = scope {
			close()
			show(fragment)
		}

		fun show(fragment: Fragment) = scope { show(fragment) }
	}
}