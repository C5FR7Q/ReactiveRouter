package com.example.reactiverouter

import android.util.Log
import androidx.fragment.app.Fragment
import com.github.c5fr7q.reactiverouter.navigator.SimpleNavigator
import com.github.c5fr7q.reactiverouter.scopeprovider.SimpleScopeProvider

class DemoScopeProvider(
	private val someProvider: SomeProvider
) : SimpleScopeProvider<SimpleNavigator>() {
	fun replace(fragment: Fragment) = scope {
		close()
		show(fragment)
	}

	fun show(fragment: Fragment) = scope { show(fragment) }

	fun showDemo1IfNeed() = scope(someProvider.shouldDoSomething) { shouldShow ->
		Log.d("ReactiveRouter", "showDemo2:$shouldShow")
		if (shouldShow) {
			scope {
				show(DemoFragment1())
			}
		} else null
	}

	fun chainOfDemo2AndDemo3() = scope(showDemo1IfNeed(), show(DemoFragment3()))
}
