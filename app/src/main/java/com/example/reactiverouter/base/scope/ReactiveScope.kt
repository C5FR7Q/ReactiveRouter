package com.example.reactiverouter.base.scope

import com.example.reactiverouter.base.navigator.Navigator
import io.reactivex.Single

class ReactiveScope<T, N : Navigator>(
	val stream: Single<T>,
	val scopeProvider: (T) -> Scope<N>?
)

//
//class MyProfileInfoProvide {
//	val myId: Single<Int> = Single.just(123)
//}
//
// ReactiveScope(myProfileInfoProvider.myId) {id -> }