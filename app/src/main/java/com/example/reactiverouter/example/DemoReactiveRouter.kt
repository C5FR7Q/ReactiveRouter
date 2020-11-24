package com.example.reactiverouter.example

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.reactiverouter.R
import com.example.reactiverouter.base.Navigator
import com.example.reactiverouter.base.ReactiveRouter
import com.example.reactiverouter.base.ScopeProvider
import com.example.reactiverouter.base.extractor.SimpleTagExtractor

class DemoReactiveRouter(fragmentManager: FragmentManager) :
	ReactiveRouter<DemoReactiveRouter.DemoNavigator, DemoReactiveRouter.DemoScopeProvider>(fragmentManager) {

	override fun createTagExtractor() = SimpleTagExtractor()
	override fun createNavigator() = DemoNavigator()
	override fun createScopeProvider() = DemoScopeProvider()

	inner class DemoScopeProvider : ScopeProvider<DemoNavigator>() {
		fun close() = scope { close() }
		fun replace(fragment: Fragment) = scope {
			close()
			show(fragment)
		}

		fun show(fragment: Fragment) = scope { show(fragment) }
	}

	inner class DemoNavigator : Navigator() {
		fun close() {
			fragmentManager.popBackStack()
			increaseStackChangeActionsCount()
		}

		fun show(fragment: Fragment) {
			fragmentManager.beginTransaction()
				.replace(R.id.main_container, fragment, fragment.javaClass.simpleName)
				.addToBackStack(fragment.javaClass.simpleName)
				.commit()
			increaseStackChangeActionsCount()
		}
	}
}