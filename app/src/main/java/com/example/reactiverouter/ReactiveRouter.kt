package com.example.reactiverouter

import androidx.fragment.app.FragmentManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlin.math.max

class ReactiveRouter(private val fragmentManager: FragmentManager) : FragmentManager.OnBackStackChangedListener {
	private val scopeProvider = ScopeProvider(NavigatorImpl())
	private val backStackSubject = BehaviorSubject.createDefault(backStack)

	fun attach() {
		fragmentManager.addOnBackStackChangedListener(this)
	}

	fun detach() {
		fragmentManager.removeOnBackStackChangedListener(this)
	}

	val backStackStream: Observable<List<FragmentManager.BackStackEntry>> = backStackSubject.hide()

	override fun onBackStackChanged() {
		backStackSubject.onNext(backStack)
	}

	private val backStack: List<FragmentManager.BackStackEntry>
		get() = mutableListOf<FragmentManager.BackStackEntry>().apply {
			val backStackSize = fragmentManager.backStackEntryCount
			for (i in 0 until backStackSize) {
				add(
					fragmentManager.getBackStackEntryAt(i)
				)
			}
		}

	fun call(provideScope: ScopeProvider.() -> Scope) = Completable.defer {
		val scope = scopeProvider.provideScope()
		scope.deferredActions.forEach { it() }
		backStackStream.onlyNew().skip(max(0, scope.deferredActions.size - 1).toLong()).ignoreElements()
	}

	private inner class NavigatorImpl : Navigator {
		override fun show() {
			TODO("Not yet implemented")
		}

		override fun close() {
			TODO("Not yet implemented")
		}
	}
}