package com.example.reactiverouter.base.scopeprovider

import com.example.reactiverouter.base.navigator.Navigator
import com.example.reactiverouter.base.scope.ReactiveScope
import com.example.reactiverouter.base.scope.Scope
import io.reactivex.Single

/**
 * Should provide all available ready-to-use [Scope]s, that can be used inside of [ReactiveRouter.call]
 * */
abstract class ScopeProvider<N : Navigator> {
	protected fun scope(scopeBody: N.() -> Unit) = Scope<N> { it.scopeBody() }
	protected fun <T> reactiveScope(stream: Single<T>, scopeProvider: (T) -> Scope<N>?) = ReactiveScope(stream, scopeProvider)
}
