package com.example.reactiverouter.base

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.example.reactiverouter.base.navigator.Navigator
import com.example.reactiverouter.base.scope.IdentifiableSimpleScope
import com.example.reactiverouter.base.scope.Scope
import com.example.reactiverouter.base.scopeprovider.ScopeProvider
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
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
	private val simpleScopesQueue: Queue<Pair<IdentifiableSimpleScope<N>, SingleSubject<Boolean>>> = LinkedList()
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
	private var nextId = 0

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
		return deferScope(scopeProvider.provideScope(), getNextId())
	}

	final override fun onBackStackChanged() {
		_backStackChangeEvent.onNext(true)
	}

	private fun loopDeferredScopes() {
		simpleScopesQueueSubject.let { subject ->
			if (stateLossStrategy == StateLossStrategy.POSTPONE) {
				isResumedSubject.distinctUntilChanged().switchMap { isResumed ->
					if (isResumed) {
						subject
					} else {
						Observable.empty<Queue<Pair<IdentifiableSimpleScope<N>, SingleSubject<Boolean>>>>()
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
			.map { (scope, subject) -> scope.scope to subject }
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

	private fun <T> deferScope(scope: Scope<T, N>, id: Int): Single<Boolean> {
		return Single.defer {
			when (scope) {
				is Scope.Simple -> deferSimpleScope(scope, id)
				is Scope.Reactive -> deferReactiveScope(scope, id)
				is Scope.Chain -> deferChainScope(scope, id)
			}
		}
	}

	private fun deferSimpleScope(simpleScope: Scope.Simple<N>, id: Int): Single<Boolean> {
		val subject = SingleSubject.create<Boolean>()
		val deferredSimpleScope = IdentifiableSimpleScope(id, simpleScope) to subject
		simpleScopesQueue.add(deferredSimpleScope)
		notifySimpleScopesChanged()
		return subject.doOnDispose {
			if (scopeInExecution != simpleScope) {
				simpleScopesQueue.remove(deferredSimpleScope)
				notifySimpleScopesChanged()
			}
		}
	}

	private fun <T> deferReactiveScope(reactiveScope: Scope.Reactive<T, N>, id: Int): Single<Boolean> {
		return wrapWithInterruption(id, simpleScopesQueue.toList(), reactiveScope.stream.flatMap {
			val scope: Scope<*, N>? = reactiveScope.scopeProvider.invoke(it)
			if (scope != null) {
				deferScope(scope, id)
			} else {
				Single.just(true)
			}
		})
	}

	private fun deferChainScope(chainScope: Scope.Chain<N>, id: Int): Single<Boolean> {
		val scopes = chainScope.scopes
		if (scopes.isEmpty()) {
			return Single.just(true)
		}
		val initialScopes = simpleScopesQueue.toMutableList()
		var single: Single<Boolean>? = null
		for (element in scopes) {
			val nextSingle = wrapWithInterruption(id, initialScopes, deferScope(element, id))
			single = single?.flatMap { success ->
				if (success) {
					nextSingle
				} else {
					Single.just(false)
				}
			} ?: nextSingle
		}
		return single!!
	}

	private fun getNextId() = nextId++

	private fun wrapWithInterruption(
		id: Int,
		initialScopes: List<Pair<IdentifiableSimpleScope<N>, SingleSubject<Boolean>>>,
		wrappedSingle: Single<Boolean>
	): Single<Boolean> {
		return Single.ambArray(
			simpleScopesQueueSubject.filter { currentScopes ->
				currentScopes.toMutableList().run {
					removeAll(initialScopes)
					lastOrNull()?.let { (identifiableSimpleScope, _) ->
						identifiableSimpleScope.scope.isInterrupting && identifiableSimpleScope.id != id
					} ?: false
				}
			}
				.firstOrError()
				.map { false },
			wrappedSingle
		)
	}
}