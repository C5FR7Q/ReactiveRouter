package com.example.reactiverouter.base.scope

import com.example.reactiverouter.base.navigator.Navigator
import io.reactivex.Single

/**
 * Stores, what navigation should be performed above above [Navigator]
 * */
sealed class Scope<T, N : Navigator> {
	class Simple<N : Navigator>(body: (N) -> Unit) : (N) -> Unit, Scope<Nothing, N>() {
		private var bodies = mutableListOf<(N) -> Unit>().apply { add(body) }

		/**
		 * Contacts [Simple]
		 * */
		infix operator fun plus(scope: Simple<N>): Simple<N> {
			bodies.addAll(scope.bodies)
			return this
		}

		override fun invoke(p1: N) {
			bodies.forEach { it.invoke(p1) }
		}
	}

	class Reactive<T, N : Navigator>(
		val stream: Single<T>,
		val scopeProvider: (T) -> Scope<*, N>?
	) : Scope<T, N>()

	class Chain<N : Navigator>(
		scopes: List<Scope<*, N>>
	) : Scope<Any, N>() {
		var scopes = scopes
			private set

		/**
		 * Contacts [Chain]
		 * */
		infix operator fun plus(chain: Chain<N>): Chain<N> {
			scopes = scopes.toMutableList().apply { addAll(chain.scopes) }
			return this
		}
	}
}