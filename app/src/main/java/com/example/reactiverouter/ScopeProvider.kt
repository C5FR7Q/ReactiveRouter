package com.example.reactiverouter

abstract class ScopeProvider<N : Navigator>(private val navigator: N) {
	protected fun scope(scopeBody: Scope<N>.() -> Unit) = Scope(navigator).apply { scopeBody() }
}
