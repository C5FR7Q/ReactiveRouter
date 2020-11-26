package com.example.reactiverouter.example

import androidx.fragment.app.Fragment
import com.example.reactiverouter.base.navigator.SimpleNavigator
import com.example.reactiverouter.base.scopeprovider.SimpleScopeProvider

class DemoScopeProvider : SimpleScopeProvider<SimpleNavigator>() {
	fun replace(fragment: Fragment) = scope {
		close()
		show(fragment)
	}

	fun show(fragment: Fragment) = scope { show(fragment) }
}
