package com.example.reactiverouter.example

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.reactiverouter.R
import com.example.reactiverouter.base.ReactiveRouter
import com.example.reactiverouter.base.StateLossStrategy
import com.example.reactiverouter.base.extractor.SimpleTagExtractor
import com.example.reactiverouter.base.navigator.SimpleNavigator
import io.reactivex.Observable

class DemoReactiveRouter(
	fragmentManager: FragmentManager,
	demoScopeProvider: DemoScopeProvider
) :
	ReactiveRouter<SimpleNavigator, DemoScopeProvider>(
		SimpleNavigator(R.id.main_container, SimpleTagExtractor(), fragmentManager),
		demoScopeProvider,
		fragmentManager,
		StateLossStrategy.POSTPONE
	) {

	fun isShown(fragment: Fragment): Observable<Boolean> {
		return backStackChangeEvent.map { navigator.isShown(fragment) }.distinctUntilChanged()
	}

	fun isVisible(fragment: Fragment): Observable<Boolean> {
		return backStackChangeEvent.map { navigator.isVisible(fragment) }.distinctUntilChanged()
	}
}