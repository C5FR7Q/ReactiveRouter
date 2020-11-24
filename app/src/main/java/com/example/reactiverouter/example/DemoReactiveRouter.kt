package com.example.reactiverouter.example

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.reactiverouter.R
import com.example.reactiverouter.base.Navigator
import com.example.reactiverouter.base.ReactiveRouter
import com.example.reactiverouter.base.Scope
import com.example.reactiverouter.base.ScopeProvider
import com.example.reactiverouter.base.extractor.SimpleKeyExtractor

class DemoReactiveRouter(fragmentManager: FragmentManager) :
	ReactiveRouter<DemoReactiveRouter.DemoNavigator, DemoReactiveRouter.DemoScopeProvider>(fragmentManager) {

	override fun createKeyExtractor() = SimpleKeyExtractor()
	override fun createNavigator() = DemoNavigator()
	override fun createScopeProvider() = DemoScopeProvider()

	inner class DemoScopeProvider : ScopeProvider<DemoNavigator>() {
		fun close() = scope { close() }

		fun replace(fragment: Fragment) = scope {
			close()
			show(fragment)
		}

		fun show(fragment: Fragment) = scope { show(fragment) }

		//region Scope extensions
		private fun Scope<DemoNavigator>.close() {
			defer { navigator.close() }
		}

		private fun Scope<DemoNavigator>.show(fragment: Fragment) {
			defer { navigator.show(fragment) }
		}
		//endregion
	}

	inner class DemoNavigator : Navigator() {
		fun close() {
			fragmentManager.popBackStack()
		}

		fun show(fragment: Fragment) {
			fragmentManager.beginTransaction()
				.replace(R.id.main_container, fragment, fragment.javaClass.simpleName)
				.addToBackStack(fragment.javaClass.simpleName)
				.commit()
		}
	}
}