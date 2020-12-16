package com.example.reactiverouter

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SomeProvider {
	val shouldDoSomething: Single<Boolean> =
		Observable.interval(8, 5, TimeUnit.SECONDS)
			.firstOrError()
			.flatMap { Single.just(Random.nextInt(10) > 3) }
}