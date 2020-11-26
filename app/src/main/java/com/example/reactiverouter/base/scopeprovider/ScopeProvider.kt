package com.example.reactiverouter.base.scopeprovider

import com.example.reactiverouter.base.scope.Scope
import com.example.reactiverouter.base.navigator.Navigator

/**
 * Should provide all available ready-to-use [Scope]s, that can be used inside of [ReactiveRouter.call]
 * */
abstract class ScopeProvider<N : Navigator> {
	protected fun scope(scopeBody: N.() -> Unit) = Scope<N> { it.scopeBody() }
}
