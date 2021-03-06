package com.github.c5fr7q.reactiverouter.navigator

/**
 * Should provide navigational functions, that deals with [androidx.fragment.app.FragmentManager]
 * */
abstract class Navigator {
	private var stackChangeActionsCount = 0

	/**
	 * Should be called by each function, that changes back stack.
	 * */
	protected fun increaseStackChangeActionsCount(byCount: Int = 1) {
		stackChangeActionsCount++
	}

	internal fun startSession() {
		stackChangeActionsCount = 0
	}

	internal fun finishSession() = stackChangeActionsCount
}