package com.example.reactiverouter.base

/**
 * Should provide all available ready-to-use [Scope]s, that can be used inside of [ReactiveRouter.call]
 * */
abstract class ScopeProvider<N : Navigator> {
	protected fun scope(scopeBody: N.() -> Unit) = Scope<N> { it.scopeBody() }
}
