package com.example.reactiverouter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.github.c5fr7q.reactiverouter.StateLossStrategy
import com.github.c5fr7q.reactiverouter.extractor.SimpleTagExtractor
import com.github.c5fr7q.reactiverouter.navigator.SimpleNavigator
import io.reactivex.rxjava3.core.Observable

class DemoReactiveRouter(
	fragmentManager: FragmentManager,
	demoScopeProvider: DemoScopeProvider
) :
	com.github.c5fr7q.reactiverouter.ReactiveRouter<SimpleNavigator, DemoScopeProvider>(
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