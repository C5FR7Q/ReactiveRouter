package com.github.c5fr7q.reactiverouter.scope

import com.github.c5fr7q.reactiverouter.navigator.Navigator

internal data class IdentifiableSimpleScope<N : Navigator>(
	val id: Int,
	val scope: Scope.Simple<N>
)