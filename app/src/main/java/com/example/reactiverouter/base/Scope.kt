package com.example.reactiverouter.base

open class Scope<N : Navigator>(private val navigator: N) {
	val deferredActions = mutableListOf<() -> Unit>()

	fun defer(action: () -> Unit) {
		deferredActions.add(action)
	}

	infix operator fun plus(scope: Scope<N>): Scope<N> {
		deferredActions.addAll(scope.deferredActions)
		return this
	}
}