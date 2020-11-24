package com.example.reactiverouter.example

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.reactiverouter.R
import com.example.reactiverouter.base.Scope
import com.example.reactiverouter.base.navigator.SimpleNavigator

class MainActivity : AppCompatActivity() {
	private var router: DemoReactiveRouter? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		router = DemoReactiveRouter(supportFragmentManager)
		router?.attach()
	}

	override fun onDestroy() {
		super.onDestroy()
		router?.detach()
	}

	override fun onResume() {
		super.onResume()
		router?.run {
			callWithMessage("show DemoFragment1") {
				show(DemoFragment1())
			}
			callWithMessage("replace with DemoFragment2; show DemoFragment3") {
				replace(DemoFragment2()) + show(DemoFragment3())
			}
			call { closeCurrent() }
				.andThen(call { replace(DemoFragment3()) })
				.andThen(call { show(DemoFragment2()) })
				.subscribe { Log.v("ReactiveRouter", "Close. THEN replace with DemoFragment3. THEN show DemoFragment2") }
		}
	}

	private fun DemoReactiveRouter.callWithMessage(
		message: String,
		provideScope: DemoReactiveRouter.DemoScopeProvider.() -> Scope<SimpleNavigator>
	) {
		call(provideScope).subscribe { Log.v("ReactiveRouter", message) }
	}
}