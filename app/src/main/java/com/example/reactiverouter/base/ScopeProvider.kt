package com.example.reactiverouter.base

abstract class ScopeProvider<N : Navigator> {
	lateinit var navigator: N
		internal set

	protected fun scope(scopeBody: Scope<N>.() -> Unit) = Scope(navigator).apply { scopeBody() }
}
