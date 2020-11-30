package com.example.reactiverouter.base.scopeprovider

import com.example.reactiverouter.base.navigator.Navigator
import com.example.reactiverouter.base.scope.Scope
import io.reactivex.Single

/**
 * Should provide all available ready-to-use [Scope]s, that can be used inside of [ReactiveRouter.call]
 * */
abstract class ScopeProvider<N : Navigator> {

	//region Simple
	protected fun simple(isIgnoring: Boolean, scopeBody: N.() -> Unit) = Scope.Simple<N>(isIgnoring) { it.scopeBody() }
	protected fun simple(scopeBody: N.() -> Unit) = Scope.Simple<N>(false) { it.scopeBody() }
	protected fun simple(isIgnoring: Boolean, simple: Scope.Simple<N>) = Scope.Simple(isIgnoring, simple)
	//endregion

	//region Reactive
	protected fun <T> reactive(
		terminateOnObsolete: Boolean,
		stream: Single<T>,
		scopeProvider: (T) -> Scope.Simple<N>?
	) = Scope.Reactive.NonBlocking(terminateOnObsolete, stream, scopeProvider)

	protected fun <T> reactive(
		stream: Single<T>,
		scopeProvider: (T) -> Scope.Simple<N>?
	) = Scope.Reactive.NonBlocking(false, stream, scopeProvider)

	protected fun <T> reactive(
		terminateOnObsolete: Boolean,
		reactive: Scope.Reactive<T, N>
	) = Scope.Reactive.NonBlocking(terminateOnObsolete, reactive.stream, reactive.scopeProvider)

	protected fun <T> reactive(
		reactive: Scope.Reactive<T, N>
	) = Scope.Reactive.NonBlocking(false, reactive.stream, reactive.scopeProvider)

	protected fun <T> reactiveBlocking(
		isIgnoring: Boolean,
		stream: Single<T>,
		scopeProvider: (T) -> Scope.Simple<N>?
	) = Scope.Reactive.Blocking(isIgnoring, stream, scopeProvider)

	protected fun <T> reactiveBlocking(
		stream: Single<T>,
		scopeProvider: (T) -> Scope.Simple<N>?
	) = Scope.Reactive.Blocking(false, stream, scopeProvider)

	protected fun <T> reactiveBlocking(
		isIgnoring: Boolean,
		reactive: Scope.Reactive<T, N>
	) = Scope.Reactive.Blocking(isIgnoring, reactive.stream, reactive.scopeProvider)

	protected fun <T> reactiveBlocking(
		reactive: Scope.Reactive<T, N>
	) = Scope.Reactive.Blocking(false, reactive.stream, reactive.scopeProvider)
	//endregion


	//region Chain
	protected fun chain(
		terminateOnObsolete: Boolean,
		terminateOnInterfere: Boolean,
		vararg scopes: Scope<*, N>
	) = Scope.Chain.NonBlocking(terminateOnObsolete, terminateOnInterfere, scopes.toList())

	protected fun chain(
		vararg scopes: Scope<*, N>
	) = Scope.Chain.NonBlocking(false, false, scopes.toList())

	protected fun chain(
		terminateOnObsolete: Boolean,
		terminateOnInterfere: Boolean,
		chain: Scope.Chain<N>
	) = Scope.Chain.NonBlocking(terminateOnObsolete, terminateOnInterfere, chain.scopes)

	protected fun chain(
		chain: Scope.Chain<N>
	) = Scope.Chain.NonBlocking(false, false, chain.scopes)

	protected fun chainBlocking(
		isIgnoring: Boolean,
		vararg scopes: Scope<*, N>
	) = Scope.Chain.Blocking(isIgnoring, scopes.toList())

	protected fun chainBlocking(
		vararg scopes: Scope<*, N>
	) = Scope.Chain.Blocking(false, scopes.toList())

	protected fun chainBlocking(
		isIgnoring: Boolean,
		chain: Scope.Chain<N>
	) = Scope.Chain.Blocking(isIgnoring, chain.scopes)

	protected fun chainBlocking(
		chain: Scope.Chain<N>
	) = Scope.Chain.Blocking(false, chain.scopes)
	//endregion
}
