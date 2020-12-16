package com.github.c5fr7q.reactiverouter.scopeprovider

import com.github.c5fr7q.reactiverouter.navigator.Navigator
import com.github.c5fr7q.reactiverouter.scope.Scope
import io.reactivex.rxjava3.core.Single

/**
 * Should provide all available ready-to-use [Scope]s, that can be used inside of [com.github.c5fr7q.reactiverouter.ReactiveRouter.call]
 * */
abstract class ScopeProvider<N : Navigator> {

	//region Simple
	protected fun scope(
		isInterrupting: Boolean,
		scopeBody: N.() -> Unit
	) = Scope.Simple<N>(isInterrupting) { it.scopeBody() }

	protected fun scope(scopeBody: N.() -> Unit) = Scope.Simple<N>(true) { it.scopeBody() }
	//endregion

	//region Reactive
	protected fun <T> scope(
		stream: Single<T>,
		scopeProvider: (T) -> Scope.Simple<N>?
	) = Scope.Reactive(stream, scopeProvider)
	//endregion

	//region Chain
	protected fun scope(vararg scopes: Scope<*, N>) = Scope.Chain(scopes.toList())
	//endregion
}
