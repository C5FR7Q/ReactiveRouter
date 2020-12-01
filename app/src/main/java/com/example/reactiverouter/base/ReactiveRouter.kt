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
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.SingleSubject
import java.util.*
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
	private val scopesQueue: Queue<Pair<Scope<*, N>, SingleSubject<Boolean>>> = LinkedList()
	private val scopesQueueSubject = BehaviorSubject.createDefault(scopesQueue)
	private val scopesSubscriptions = mutableMapOf<Scope<*, N>, Disposable>()
	private val simpleScopesQueue: Queue<Pair<Scope.Simple<N>, SingleSubject<Boolean>>> = LinkedList()
	private val simpleScopesQueueSubject = BehaviorSubject.createDefault(simpleScopesQueue)
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
		return callResponsive(provideScope).ignoreElement()
	}

	/**
	 * Entry point of any navigational action. Provide [Scope] that should be processed.
	 * @return true or false on whether [Scope] is processed or not.
	 * */
	fun <T> callResponsive(provideScope: SP.() -> Scope<T, N>): Single<Boolean> {
		return deferScope(scopeProvider.provideScope())
	}

	final override fun onBackStackChanged() {
		_backStackChangeEvent.onNext(true)
	}

	private fun loopDeferredScopes() {
		scopesQueueSubject.filter { it.isNotEmpty() }
			.map { it.peek()!! }
			.distinctUntilChanged()
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe { (scope, subject) ->
				when (scope) {
					is Scope.Simple -> {
						deferSimpleScope(scope, subject).subscribe { processed ->
							scopesSubscriptions.remove(scope)
							scopesQueue.poll()
							notifyScopesChanged()
						}.also { scopesSubscriptions[scope] = it }
					}
					is Scope.Reactive.Blocking -> {
						deferReactiveScope(scope, subject).subscribe { processed ->
							scopesSubscriptions.remove(scope)
							scopesQueue.poll()
							notifyScopesChanged()
						}.also { scopesSubscriptions[scope] = it }
					}
					is Scope.Chain.Blocking -> {
						deferChainScope(scope, subject).subscribe { processed ->
							scopesSubscriptions.remove(scope)
							scopesQueue.poll()
							notifyScopesChanged()
						}.also { scopesSubscriptions[scope] = it }
					}
					is Scope.Reactive.NonBlocking -> {
						deferReactiveScope(scope, subject, false).subscribe { processed ->
							scopesSubscriptions.remove(scope)
						}.also { scopesSubscriptions[scope] = it }
						scopesQueue.poll()
						notifyScopesChanged()
					}
					is Scope.Chain.NonBlocking -> {
						deferChainScope(scope, subject, false).subscribe { processed ->
							scopesSubscriptions.remove(scope)
						}.also { scopesSubscriptions[scope] = it }
						scopesQueue.poll()
						notifyScopesChanged()
					}
				}
			}
			.also { subscriptions.add(it) }

		simpleScopesQueueSubject.let { subject ->
			if (stateLossStrategy == StateLossStrategy.POSTPONE) {
				isResumedSubject.distinctUntilChanged().switchMap { isResumed ->
					if (isResumed) {
						subject
					} else {
						Observable.empty<Queue<Pair<Scope.Simple<N>, SingleSubject<Boolean>>>>()
					}
				}
			} else {
				subject
			}
		}
			.filter { it.isNotEmpty() }
			.map { it.peek()!! }
			.distinctUntilChanged()
			.observeOn(AndroidSchedulers.mainThread())
			.switchMapMaybe { (scope, subject) ->
				Maybe.defer {
					scopeInExecution = scope
					navigator.startSession()
					scope.invoke(navigator)
					val stackChangeActionsCount = navigator.finishSession()
					if (stackChangeActionsCount == 0)
						Maybe.just(subject to true) else
						backStackChangeEvent
							.skip(max(0, stackChangeActionsCount - 1).toLong())
							.map { subject to true }
							.firstElement()
				}.let { maybe ->
					when (stateLossStrategy) {
						StateLossStrategy.POSTPONE -> {
							scopeInExecution = null
							maybe.onErrorComplete()
						}
						StateLossStrategy.IGNORE -> maybe.onErrorReturnItem(subject to false)
						StateLossStrategy.ERROR -> maybe
					}
				}
			}
			.doOnDispose {
				scopeInExecution = null
				simpleScopesQueue.forEach { (_, subject) ->
					subject.onSuccess(false)
				}
				simpleScopesQueue.clear()
				notifySimpleScopesChanged()
			}
			.subscribe { (completeSubject, isProcessed) ->
				scopeInExecution = null
				completeSubject.onSuccess(isProcessed)
				simpleScopesQueue.poll()
				notifySimpleScopesChanged()
			}
			.also { subscriptions.add(it) }
	}

	private fun notifySimpleScopesChanged() {
		simpleScopesQueueSubject.onNext(simpleScopesQueue)
	}

	private fun notifyScopesChanged() {
		scopesQueueSubject.onNext(scopesQueue)
	}

	private fun <T> deferScope(scope: Scope<T, N>): Single<Boolean> {
		return Single.defer {
			val isIgnoring = scopesQueue.map { (scope, _) ->
				when (scope) {
					is Scope.Simple -> scope.isIgnoring
					is Scope.Reactive.Blocking -> scope.isIgnoring
					is Scope.Chain.Blocking -> scope.isIgnoring
					else -> false
				}
			}.firstOrNull { it } ?: false
			if (isIgnoring) {
				Single.just(false)
			}
			val subject = SingleSubject.create<Boolean>()
			val deferredScope = scope to subject
			scopesQueue.add(deferredScope)
			notifyScopesChanged()
			subject.doOnDispose {
				scopesSubscriptions.remove(scope)?.dispose()
				if (scopesQueue.contains(deferredScope)) {
					scopesQueue.remove(deferredScope)
					notifyScopesChanged()
				}
			}
		}
	}

	private fun <T> deferInnerScope(scope: Scope<T, N>, subject: SingleSubject<Boolean> = SingleSubject.create()): Single<Boolean> {
		return Single.defer {
			when (scope) {
				is Scope.Simple -> deferSimpleScope(scope, subject)
				is Scope.Reactive -> deferReactiveScope(scope, subject)
				is Scope.Chain -> deferChainScope(scope, subject)
			}
		}
	}

	private fun deferSimpleScope(simpleScope: Scope.Simple<N>, subject: SingleSubject<Boolean>): Single<Boolean> {
		val deferredSimpleScope = simpleScope to subject
		simpleScopesQueue.add(deferredSimpleScope)
		notifySimpleScopesChanged()
		return subject.doOnDispose {
			if (scopeInExecution != simpleScope) {
				simpleScopesQueue.remove(deferredSimpleScope)
				notifySimpleScopesChanged()
			}
		}
	}

	private fun <T> deferReactiveScope(
		reactiveScope: Scope.Reactive<T, N>,
		subject: SingleSubject<Boolean>,
		isBlocking: Boolean = true
	): Single<Boolean> {
		return reactiveScope.stream.flatMap {
			val scope: Scope<*, N>? = reactiveScope.scopeProvider.invoke(it)
			if (scope != null) {
				deferInnerScope(scope, subject)
			} else {
				subject.onSuccess(true)
				Single.just(true)
			}
		}
	}

	private fun deferChainScope(
		chainScope: Scope.Chain<N>,
		subject: SingleSubject<Boolean>,
		isBlocking: Boolean = true
	): Single<Boolean> {
		val scopes = chainScope.scopes
		if (scopes.isEmpty()) {
			subject.onSuccess(true)
			return Single.just(true)
		}
		var completable: Single<Boolean>? = null
		for (element in scopes) {
			val nextCompletable = deferInnerScope(element)
			completable = completable?.flatMap { success ->
				if (success) {
					nextCompletable
				} else {
					Single.just(false)
				}
			} ?: nextCompletable
		}
		return completable!!.doOnSuccess { subject.onSuccess(it) }
	}
}