package com.example.reactiverouter.base

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
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
	private val fragmentManager: FragmentManager,
	private val stateLossStrategy: StateLossStrategy = StateLossStrategy.ERROR
) : FragmentManager.OnBackStackChangedListener {

	private val backStackSubject = BehaviorSubject.createDefault(backStack)
	private val deferredScopes = mutableListOf<Pair<Scope<N>, BehaviorSubject<Boolean>>>()
	private val deferredScopesSubject = BehaviorSubject.createDefault(deferredScopes)
	private val isResumedSubject = BehaviorSubject.createDefault(false)

	private val subscriptions = CompositeDisposable()

	private val recycleObserver = object : LifecycleObserver {
		@OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
		fun onResume() {
			isResumedSubject.onNext(true)
		}

		@OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
		fun onPause() {
			isResumedSubject.onNext(false)
		}
	}

	/**
	 * Attaches [FragmentManager.OnBackStackChangedListener], starts processing [call]
	 * */
	fun attach(lifecycleOwner: LifecycleOwner) {
		lifecycleOwner.lifecycle.addObserver(recycleObserver)
		fragmentManager.addOnBackStackChangedListener(this)
		loopDeferredScopes()
	}

	/**
	 * Detaches [FragmentManager.OnBackStackChangedListener], stops processing [call]
	 * */
	fun detach(lifecycleOwner: LifecycleOwner) {
		lifecycleOwner.lifecycle.removeObserver(recycleObserver)
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

	final override fun onBackStackChanged() {
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
		deferredScopesSubject.let { subject ->
			if (stateLossStrategy == StateLossStrategy.POSTPONE) {
				isResumedSubject.distinctUntilChanged().switchMap { isResumed ->
					if (isResumed) {
						subject
					} else {
						Observable.empty<List<Pair<Scope<N>, BehaviorSubject<Boolean>>>>()
					}
				}
			} else {
				subject
			}
		}
			.filter { it.isNotEmpty() }
			.map { it.first() }
			.distinctUntilChanged()
			.observeOn(AndroidSchedulers.mainThread())
			.switchMapMaybe { (scope, subject) ->
				Maybe.defer {
					navigator.startSession()
					scope.invoke(navigator)
					val stackChangeActionsCount = navigator.finishSession()
					if (stackChangeActionsCount == 0)
						Maybe.just(subject) else
						backStackStream.onlyNew()
							.skip(max(0, stackChangeActionsCount - 1).toLong())
							.map { subject }
							.firstElement()
				}.let { maybe ->
					when (stateLossStrategy) {
						StateLossStrategy.POSTPONE -> maybe.onErrorComplete()
						StateLossStrategy.IGNORE -> maybe.onErrorReturnItem(subject)
						StateLossStrategy.ERROR -> maybe
					}
				}
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