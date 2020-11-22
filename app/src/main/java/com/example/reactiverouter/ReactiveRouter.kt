package com.example.reactiverouter

import androidx.fragment.app.FragmentManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlin.math.max

class ReactiveRouter(private val fragmentManager: FragmentManager) : FragmentManager.OnBackStackChangedListener {
	private val scopeProvider = ScopeProvider(NavigatorImpl())
	private val backStackSubject = BehaviorSubject.createDefault(backStack)
	private val deferredScopes = mutableListOf<Pair<Scope, BehaviorSubject<Boolean>>>()
	private val deferredScopesSubject = BehaviorSubject.createDefault(deferredScopes)

	private val subscriptions = CompositeDisposable()

	fun attach() {
		fragmentManager.addOnBackStackChangedListener(this)
		deferredScopesSubject
			.filter { it.isNotEmpty() }
			.map { it.first() }
			// TODO[VVA]: 22.11.20 Possible bug in case of same scopes going one by one
			.distinctUntilChanged()
			.observeOn(AndroidSchedulers.mainThread())
			.switchMap { (scope, subject) ->
				scope.deferredActions.forEach { it() }
				backStackStream.onlyNew()
					.skip(max(0, scope.deferredActions.size - 1).toLong())
					.map { subject }
			}
			.subscribe { completeSubject ->
				completeSubject.onNext(true)
				deferredScopes.removeAt(0)
				notifyScopesChanged()
			}
			.also { subscriptions.add(it) }
	}

	fun detach() {
		fragmentManager.removeOnBackStackChangedListener(this)
		subscriptions.dispose()
	}

	val backStackStream: Observable<List<FragmentManager.BackStackEntry>> = backStackSubject.hide()

	override fun onBackStackChanged() {
		backStackSubject.onNext(backStack)
	}

	private val backStack: List<FragmentManager.BackStackEntry>
		get() = mutableListOf<FragmentManager.BackStackEntry>().apply {
			val backStackSize = fragmentManager.backStackEntryCount
			for (i in 0 until backStackSize) {
				add(fragmentManager.getBackStackEntryAt(i))
			}
		}

	fun call(provideScope: ScopeProvider.() -> Scope) = Completable.defer {
		val scope = scopeProvider.provideScope()
		deferScope(scope)
	}

	private fun deferScope(scope: Scope): Completable {
		val subject = BehaviorSubject.create<Boolean>()
		deferredScopes.add(scope to subject)
		notifyScopesChanged()
		return subject.ignoreElements()
	}

	private fun notifyScopesChanged() {
		deferredScopesSubject.onNext(deferredScopes)
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