package com.example.reactiverouter

class Scope(private val navigator: Navigator) : Navigator {
	val deferredActions = mutableListOf<() -> Unit>()
	override fun show() {
		deferredActions.add { navigator.show() }
	}

	override fun close() {
		deferredActions.add { navigator.close() }
	}

	operator fun plus(scope: Scope) {
		deferredActions.addAll(scope.deferredActions)
	}
}