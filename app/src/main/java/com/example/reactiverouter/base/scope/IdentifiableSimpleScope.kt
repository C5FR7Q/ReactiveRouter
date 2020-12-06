package com.example.reactiverouter.base.scope

import com.example.reactiverouter.base.navigator.Navigator

internal data class IdentifiableSimpleScope<N : Navigator>(
	val id: Int,
	val scope: Scope.Simple<N>
)