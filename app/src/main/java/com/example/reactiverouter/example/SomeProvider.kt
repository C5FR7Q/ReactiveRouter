package com.example.reactiverouter.example

import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SomeProvider {
	val shouldDoSomething: Single<Boolean> =
		Observable.interval(8, 5, TimeUnit.SECONDS)
			.firstElement()
			.flatMapSingle { Single.just(Random.nextInt(10) > 3) }
}