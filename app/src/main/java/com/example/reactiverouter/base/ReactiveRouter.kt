package com.example.reactiverouter.base

import android.util.Log
import androidx.fragment.app.FragmentManager
import com.example.reactiverouter.base.navigator.Navigator
import com.example.reactiverouter.base.scopeprovider.ScopeProvider
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlin.math.max

/**
 * Reactive based facade for any navigational actions.
 * */
abstract class ReactiveRouter<N : Navigator, SP : ScopeProvider<N>>(
	protected val navigator: N,
	private val scopeProvider: SP,
	private val fragmentManager: FragmentManager
) : FragmentManager.OnBackStackChangedListener {

	private val backStackSubject = BehaviorSubject.createDefault(backStack)
	private val deferredScopes = mutableListOf<Pair<Scope<N>, BehaviorSubject<Boolean>>>()
	private val deferredScopesSubject = BehaviorSubject.createDefault(deferredScopes)

	private val subscriptions = CompositeDisposable()

	/**
	 * Attaches [FragmentManager.OnBackStackChangedListener], starts processing [call]
	 * */
	fun attach() {
		fragmentManager.addOnBackStackChangedListener(this)
		loopDeferredScopes()
	}

	/**
	 * Detaches [FragmentManager.OnBackStackChangedListener], stops processing [call]
	 * */
	fun detach() {
		fragmentManager.removeOnBackStackChangedListener(this)
		subscriptions.dispose()
	}

	/**
	 * Entry point of any navigational action. Provide [Scope] that should be processed.
	 * */
	fun call(provideScope: SP.() -> Scope<N>) = Completable.defer {
		val scope = scopeProvider.provideScope()
		deferScope(scope)
	}

	val backStackStream: Observable<List<FragmentManager.BackStackEntry>> = backStackSubject.hide()

	init {
		backStackStream.subscribe { stack -> Log.d("ReactiveRouter", "stack=${stack.map { it.name }}") }
	}

	final override fun onBackStackChanged() {
		Log.e("ReactiveRouter", "onBackStackChanged")
		backStackSubject.onNext(backStack)
	}

	private val backStack: List<FragmentManager.BackStackEntry>
		get() = mutableListOf<FragmentManager.BackStackEntry>().apply {
			val backStackSize = fragmentManager.backStackEntryCount
			for (i in 0 until backStackSize) {
				add(fragmentManager.getBackStackEntryAt(i))
			}
		}

	private fun loopDeferredScopes() {
		deferredScopesSubject
			.filter { it.isNotEmpty() }
			.map { it.first() }
			.distinctUntilChanged()
			.observeOn(AndroidSchedulers.mainThread())
			.switchMapMaybe { (scope, subject) ->
				Log.i("ReactiveRouter", "-------")
				navigator.startSession()
				scope.invoke(navigator)
				val stackChangeActionsCount = navigator.finishSession()
				Log.w("ReactiveRouter", "scope.size = $stackChangeActionsCount")
				if (stackChangeActionsCount == 0)
					Maybe.just(subject) else
					backStackStream.onlyNew()
						.skip(max(0, stackChangeActionsCount - 1).toLong())
						.map { subject }
						.firstElement()
			}
			.subscribe { completeSubject ->
				completeSubject.onComplete()
				deferredScopes.removeAt(0)
				notifyScopesChanged()
			}
			.also { subscriptions.add(it) }
	}

	private fun deferScope(scope: Scope<N>): Completable {
		val subject = BehaviorSubject.create<Boolean>()
		deferredScopes.add(scope to subject)
		notifyScopesChanged()
		return subject.ignoreElements()
	}

	private fun notifyScopesChanged() {
		deferredScopesSubject.onNext(deferredScopes)
	}
}