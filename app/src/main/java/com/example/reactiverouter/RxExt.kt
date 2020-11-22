package com.example.reactiverouter

import io.reactivex.Observable

fun <T> Observable<T>.onlyNew() = publish().autoConnect(0)