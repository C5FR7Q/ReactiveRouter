package com.example.reactiverouter

class ScopeProvider(private val navigator: Navigator) {
	protected fun scope(scopeBody: Scope.() -> Unit) = Scope(navigator).apply { scopeBody() }
}
