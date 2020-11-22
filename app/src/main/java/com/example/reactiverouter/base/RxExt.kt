package com.example.reactiverouter.base

import io.reactivex.Observable

fun <T> Observable<T>.onlyNew() = publish().autoConnect(0)