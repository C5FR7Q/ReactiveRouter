package com.example.reactiverouter.base

/**
 * Defines behavior on "Can not perform this action after onSaveInstanceState"
 * */
enum class StateLossStrategy {
	/**
	 * Postpones all [Scope]s until [androidx.lifecycle.Lifecycle.Event.ON_RESUME] is called.
	 * */
	POSTPONE,

	/**
	 * Ignores all [Scope]s, that tries to throw exception
	 * */
	IGNORE,

	/**
	 * Crashes on exception.
	 * Use it if your [com.example.reactiverouter.base.navigator.Navigator] is based on allowingStateLoss methods only
	 * */
	ERROR
}