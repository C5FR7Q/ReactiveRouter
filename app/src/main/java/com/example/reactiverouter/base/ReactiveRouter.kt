package com.example.reactiverouter.base

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.example.reactiverouter.base.navigator.Navigator
import com.example.reactiverouter.base.scope.Scope
import com.example.reactiverouter.base.scopeprovider.ScopeProvider
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
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

	private val _backStackChangeEvent = PublishSubject.create<Boolean>()
	private val deferredScopes = mutableListOf<Pair<Scope.Simple<N>, BehaviorSubject<Boolean>>>()
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

	protected val backStackChangeEvent: Observable<Boolean> = _backStackChangeEvent.hide()

	private var scopeInExecution: Scope.Simple<N>? = null

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
	fun <T> call(provideScope: SP.() -> Scope<T, N>): Completable {
		return deferScope(scopeProvider.provideScope())
	}

	final override fun onBackStackChanged() {
		_backStackChangeEvent.onNext(true)
	}

	private fun loopDeferredScopes() {
		deferredScopesSubject.let { subject ->
			if (stateLossStrategy == StateLossStrategy.POSTPONE) {
				isResumedSubject.distinctUntilChanged().switchMap { isResumed ->
					if (isResumed) {
						subject
					} else {
						Observable.empty<List<Pair<Scope.Simple<N>, BehaviorSubject<Boolean>>>>()
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
					scopeInExecution = scope
					navigator.startSession()
					scope.invoke(navigator)
					val stackChangeActionsCount = navigator.finishSession()
					if (stackChangeActionsCount == 0)
						Maybe.just(subject) else
						backStackChangeEvent
							.skip(max(0, stackChangeActionsCount - 1).toLong())
							.map { subject }
							.firstElement()
				}.let { maybe ->
					when (stateLossStrategy) {
						StateLossStrategy.POSTPONE -> {
							scopeInExecution = null
							maybe.onErrorComplete()
						}
						StateLossStrategy.IGNORE -> maybe.onErrorReturnItem(subject)
						StateLossStrategy.ERROR -> maybe
					}
				}
			}
			.doOnDispose {
				scopeInExecution = null
				deferredScopes.clear()
				notifyScopesChanged()
			}
			.subscribe { completeSubject ->
				scopeInExecution = null
				completeSubject.onComplete()
				deferredScopes.removeAt(0)
				notifyScopesChanged()
			}
			.also { subscriptions.add(it) }
	}

	private fun notifyScopesChanged() {
		deferredScopesSubject.onNext(deferredScopes)
	}

	private fun <T> deferScope(scope: Scope<T, N>): Completable {
		return Completable.defer {
			when (scope) {
				is Scope.Simple -> deferSimpleScope(scope)
				is Scope.Reactive -> deferReactiveScope(scope)
				is Scope.Chain -> deferChainScope(scope)
			}
		}
	}

	private fun deferSimpleScope(scope: Scope.Simple<N>): Completable {
		val subject = BehaviorSubject.create<Boolean>()
		val deferredScope = scope to subject
		deferredScopes.add(deferredScope)
		notifyScopesChanged()
		return subject.ignoreElements().doOnDispose {
			if (scopeInExecution != scope) {
				deferredScopes.remove(deferredScope)
				notifyScopesChanged()
			}
		}
	}

	private fun <T> deferReactiveScope(reactiveScope: Scope.Reactive<T, N>): Completable {
		return reactiveScope.stream.flatMapCompletable {
			val scope: Scope.Simple<N>? = reactiveScope.scopeProvider.invoke(it)
			if (scope != null) {
				deferSimpleScope(scope)
			} else {
				Completable.complete()
			}
		}
	}

	private fun deferChainScope(chain: Scope.Chain<N>): Completable {
		val scopes = chain.scopes
		if (scopes.isEmpty()) {
			return Completable.complete()
		}
		var completable: Completable? = null
		for (i in 0 until scopes.size) {
			val nextCompletable = deferScope(scopes[i])
			completable = if (completable == null) {
				nextCompletable
			} else {
				completable.andThen(nextCompletable)
			}
		}
		return completable!!
	}
}