package com.example.reactiverouter.base

/**
 * Set of navigational actions above [Navigator]
 * */
open class Scope<N : Navigator>(body: (N) -> Unit) : (N) -> Unit {
	var bodies = mutableListOf<(N) -> Unit>().apply { add(body) }

	/**
	 * Contacts [Scope]
	 * */
	infix operator fun plus(scope: Scope<N>): Scope<N> {
		bodies.addAll(scope.bodies)
		return this
	}

	override fun invoke(p1: N) {
		bodies.forEach { it.invoke(p1) }
	}
}