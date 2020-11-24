package com.example.reactiverouter.base

import io.reactivex.Observable

internal fun <T> Observable<T>.onlyNew() = publish().autoConnect(0)