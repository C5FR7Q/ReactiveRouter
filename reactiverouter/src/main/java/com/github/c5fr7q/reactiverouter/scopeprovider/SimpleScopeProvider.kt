package com.github.c5fr7q.reactiverouter.scopeprovider

import com.github.c5fr7q.reactiverouter.navigator.SimpleNavigator

/**
 * [ScopeProvider] with the minimum set of common scopes
 * */
open class SimpleScopeProvider<N : SimpleNavigator> : ScopeProvider<N>() {
	open fun closeCurrent() = scope { close() }
}