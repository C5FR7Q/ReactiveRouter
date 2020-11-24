package com.example.reactiverouter.base

/**
 * Should provide all available ready-to-use [Scope]s, that can be used inside of [ReactiveRouter.call]
 * */
abstract class ScopeProvider<N : Navigator> {
	lateinit var navigator: N
		internal set

	/**
	 * Defines [Scope]
	 * */
	protected fun scope(scopeBody: Scope<N>.() -> Unit) = Scope(navigator).apply { scopeBody() }
}
