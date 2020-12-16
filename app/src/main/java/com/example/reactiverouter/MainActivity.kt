package com.example.reactiverouter

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.c5fr7q.reactiverouter.navigator.SimpleNavigator
import com.github.c5fr7q.reactiverouter.scope.Scope
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
	private var router: DemoReactiveRouter? = null
	private var shown = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		router = DemoReactiveRouter(supportFragmentManager, DemoScopeProvider(SomeProvider()))
		router?.attach(this)
	}

	override fun onDestroy() {
		super.onDestroy()
		router?.detach(this)
	}

	override fun onResume() {
		super.onResume()
		if (shown) return
		shown = true
		router?.run {
			call { show(DemoFragment1()) }.subscribe()
			callWithMessage("show DemoFragment1") {
				show(DemoFragment1())
			}
			callWithMessage("show DemoFragment1") {
				show(DemoFragment1())
			}
			callWithMessage("show DemoFragment1") {
				show(DemoFragment1())
			}
			callWithMessage("show DemoFragment1") {
				show(DemoFragment1())
			}
			callWithMessage("show DemoFragment1") {
				show(DemoFragment1())
			}
			callWithMessage("replace with DemoFragment2; show DemoFragment3") {
				replace(DemoFragment2()) + show(DemoFragment3())
			}
			call { closeCurrent() }
				.andThen(call { replace(DemoFragment3()) })
				.delay(5, TimeUnit.SECONDS)
				.andThen(call { show(DemoFragment2()) })
				.subscribe { Log.v("ReactiveRouter", "Close. THEN replace with DemoFragment3. THEN show DemoFragment2") }

			call { showDemo1IfNeed() }.subscribe {
				Log.v("ReactiveRouter", "showDemo1IfNeedCompleted")
			}
		}
	}

	private fun DemoReactiveRouter.callWithMessage(
		message: String,
		provideScope: DemoScopeProvider.() -> Scope.Simple<SimpleNavigator>
	) {
		val subscription = call(provideScope).subscribe { Log.v("ReactiveRouter", message) }
		subscription.dispose()
	}
}