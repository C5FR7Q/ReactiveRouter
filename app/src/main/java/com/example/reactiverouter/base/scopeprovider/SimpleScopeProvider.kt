package com.example.reactiverouter.base.scopeprovider

import com.example.reactiverouter.base.navigator.SimpleNavigator

/**
 * [ScopeProvider] with the minimum set of common scopes
 * */
open class SimpleScopeProvider<N : SimpleNavigator> : ScopeProvider<N>() {
	open fun closeCurrent() = simple { close() }
}