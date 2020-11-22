package com.example.reactiverouter

open class Scope<N : Navigator>(private val navigator: N) {
	val deferredActions = mutableListOf<() -> Unit>()

	fun defer(action: () -> Unit) {
		deferredActions.add(action)
	}

	operator fun plus(scope: Scope<N>) {
		deferredActions.addAll(scope.deferredActions)
	}
}