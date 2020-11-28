package com.example.reactiverouter.example

import android.util.Log
import androidx.fragment.app.Fragment
import com.example.reactiverouter.base.navigator.SimpleNavigator
import com.example.reactiverouter.base.scopeprovider.SimpleScopeProvider

class DemoScopeProvider(
	private val someProvider: SomeProvider
) : SimpleScopeProvider<SimpleNavigator>() {
	fun replace(fragment: Fragment) = scope {
		close()
		show(fragment)
	}

	fun show(fragment: Fragment) = scope { show(fragment) }

	fun showDemo2IfNeed() = scope(someProvider.shouldDoSomething) { shouldShow ->
		Log.d("ReactiveRouter", "showDemo2:$shouldShow")
		if (shouldShow) {
			scope {
				show(DemoFragment2())
			}
		} else null
	}

	fun chainOfDemo2AndDemo3() = chain(showDemo2IfNeed(), show(DemoFragment3()))
}
