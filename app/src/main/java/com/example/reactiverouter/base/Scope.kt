package com.example.reactiverouter.base

/**
 * Set of navigational actions above [Navigator]
 * */
open class Scope<N : Navigator>(private val navigator: N) {
	val deferredActions = mutableListOf<() -> Unit>()

	/**
	 * Defers action until [ReactiveRouter.call] with that [Scope]
	 * */
	fun defer(action: () -> Unit) {
		deferredActions.add(action)
	}

	/**
	 * Contacts [Scope]
	 * */
	infix operator fun plus(scope: Scope<N>): Scope<N> {
		deferredActions.addAll(scope.deferredActions)
		return this
	}
}