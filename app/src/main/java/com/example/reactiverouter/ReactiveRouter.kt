package com.example.reactiverouter

import androidx.fragment.app.FragmentManager
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

class ReactiveRouter(private val fragmentManager: FragmentManager) : FragmentManager.OnBackStackChangedListener {
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

}