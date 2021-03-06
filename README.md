# ReactiveRouter
[ ![Download](https://api.bintray.com/packages/c5fr7q/ReactiveRouter/reactiverouter/images/download.svg) ](https://bintray.com/c5fr7q/ReactiveRouter/reactiverouter/_latestVersion)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)


`ReactiveRouter` is a navigational component (and a library), that builds navigation in accordance with reactive approach. Generally designed for `Fragment`s usage. It provides something like `FragmentManager`'s "window", where **the only** navigational action is acceptable at the same time.

### First sight

With `ReactiveRouter` you can do something like that:

```kotlin
router.run {
	call { closeCurrentScreen() }
		.andThen(call { openFooScreen() })
		.delay(5, TimeUnit.SECONDS)
		.andThen(call { replaceWithBarScreen() })
		.subscribe()
}
```

 ### General principles

1. Every single navigational action is called **Scope**;
2. `ReactiveRouter` **executes** scopes and has **the only** entry point for navigational actions (However, you can define separate method for each scope);
3. Scopes queue is used under the hood;
4. Scopes are being processed one by one;
5. If scope is not in execution right now, it can be easily canceled;
6. Complex scopes are interruptable;
7. Provide and use base navigation (such as manipulations above FragmentManager, Intents (if need) e.t.c.) yourself. Or use / extend existing simple one.

## Getting started

### Download
Gradle:
```groovy
implementation 'com.github.c5fr7q:reactiverouter:1.0.0'
```
Maven:
```xml
<dependency>
  <groupId>com.github.c5fr7q</groupId>
  <artifactId>reactiverouter</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```

### Base classes

#### Navigator

The place for playing with `FragmentManager`. All the base navigation should be defined there: open, close, replace e.t.c.

There's the only accessible method:
```kotlin
increaseStackChangeActionsCount(byCount: Int = 1)
```

Call it each time when changing back stack. For instance:
```kotlin
fun show(fragment: Fragment) {
	fragmentManager.beginTransaction()
		.replace(containerId, fragment, fragment.javaClass.simpleName)
		.addToBackStack(fragment.javaClass.simpleName)
		.commit()
	increaseStackChangeActionsCount()
}
```

#### Scope
Defines a single navigational action to be done above `Navigator`. There're two types of scopes: simple (`Simple`) and complex (`Reactive`, `Chain`)

##### Simple
A simple navigational action. 

Contains flag `isInterrupting`. If `true`, than stops all complex scopes, that are in the execution right now.

Supports concatenation. It might be useful, when several already existed `Simple`s should be executed simultaneously.

Example:
```kotlin
Simple<SimpleNavigator>(false) { show(CandyShopFragment()) }
```

##### Reactive
Sometimes it's necessary to do some navigation in accordance with some data. The source of that data is always the same (e.g. some Repository), so you want to reuse it. `Reactive` scope might be useful here: it takes a `Single` (that provides such a data) and a `Scope?`, that possibly should be executed (according to that data).

Let's say you want to show something only in case of A/B test says you should to. You will also need to reuse it. Example:
```kotlin
Reactive(someProvider.shouldShow()) { shouldShow ->
	if (shouldShow) {
		Simple<SimpleNavigator>(false) { show(CandyShopFragment()) }
	} else {
		null
	}
}
```

##### Chain
`Simple` supports concatenation only with another `Simple`. What if you want to concat `Simple` with `Reactive` (different scopes)? You can concat `Simple` with `Reactive`'s `Simple?` inside of `Reactive` (and they will be executed simultaneously). That's ok, if you're ready to wait `Reactive`'s `Single` completion. If not, then use `Chain`. It woun't execute scopes simultaneously. Instead, they will be executed one by one. It's like if you're trying to execute them separately, but with the difference, that each next `Scope` will be executed **only** in case of previous one was executed successfully.

An example. You can open Candy Info screen from anywhere by its name. It should always be above Candy Shop in back stack. To do it you first should check, whether that name is about valid Candy. That check takes ~ 2.5 seconds, that's why the next requirement appeared: show Candy Shop first, an then show Candy Info on rediness.
```kotlin
Chain(
	listOf(
		Simple<SimpleNavigator>(true) { show(CandyShopFragment()) },
		Reactive(someProvider.getCandyInfo(candyName)) { candyInfo ->
			if (candyInfo.candyID != CandyInfo.INVALID_ID) {
				Simple<SimpleNavigator>(true) {
					show(CandyInfoFragment().apply {
						arguments = bundleOf("candy_id" to candyInfo.candyID)
					})
				}
			} else {
				Simple<SimpleNavigator>(true) { show(InvalidCandyFragment()) }
			}
		}
	)
)
```

#### ScopeProvider
That's where you should define all of your scopes. It's simply like the place where only scopes you can use are defined.

Contains convenient `scope` functions, that allows you do something like that (instead of what we have in the example above):
```kotlin
private fun showCandyShop() = scope { show(CandyShopFragment()) }

private fun showCandyInfo(candyID: Int) = scope {
	show(CandyInfoFragment().apply {
		arguments = bundleOf("candy_id" to candyID)
	})
}

private fun showInvalidCandy() = scope { show(InvalidCandyFragment()) }

private fun tryToShowCandyInfo(candyName: String) = scope(someProvider.getCandyInfo(candyName)) { candyInfo ->
	if (candyInfo.candyID != CandyInfo.INVALID_ID != 0) {
		showCandyInfo(candyInfo.candyID)
	} else {
		showInvalidCandy()
	}
}

fun showCandyInfo(candyName: String) {
	scope(showCandyShop(), tryToShowCandyInfo(candyName))
}
```

#### ReactiveRouter

Adds scopes to the queue and then executes them.

```kotlin
fun attach(lifecycleOwner: LifecycleOwner)
fun detach(lifecycleOwner: LifecycleOwner)
```
Starts and stops execution. Call it from `onCreate` / `onDestroy` of your `Activity`. Define, where to call it from `Fragment` (and do it) if you need nested navigation inside of that `Fragment`

```kotlin
fun <T> call(provideScope: ScopeProvider.() -> Scope<T, N>): Completable
fun <T> callResponsive(provideScope: ScopeProvider.() -> Scope<T, N>): Single<Boolean>
```
An entry point of your `Scope`. Provide, which `Scope` of `ScopeProvider` should be executed.

Both returns reactive result of execution:
1. `callResponsive` - whether `Scope` was executed or not;
2. `call` - like `callResponsive`, but with ingore of returned value.

#### StateLossStrategy
Defines, what should `ReactiveRouter` do in case of `java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState`:
1. `POSTPONE` - executes scopes only in `RESUMED` state of `LifecycleOwner`;
2. `IGNORE` - ignores scopes, that catches such an error;
3. `ERROR` - throws an exception.

### Additional classes

1. **SimpleNavigator** is a `Navigator`, that provides base set of navigational, such as `show`, `showDialog`, `close`, different checks e.t.c.
2. **SimpleScopeProvide** is a `ScopeProvider`, that contains `closeCurrent` method, which closes current screen.
3. **TagExtractor** is an utility abstract class, that helps to extract String tag from different kinds of data. Is used in `SimpleNavigator`
4. **SimpleTagExtractor** is an implementation of `TagExtractor`, that extracts tags from `Fragment` or `Class<out Fragment>`. The structure of resulting tag is `class.name`
5. **SimpleJsonTagExtractor** is an implementation of `TagExtractor`, that supports `Fragment`, `Class<out Fragment>`, `SimpleJsonTagExtractor.TagModel` (Which is actually a pair of `Class<out Fragment>` and `Bundle?`). In brief, it helps you to separate tags of the same Fragments with different `Fragment.arguments`. Tag structure is like `${class.name}_${jsonBundle}`, where jsonBundle is a Json representation of `Bundle`. Doesn't work with collections inside of `Bundle`. 
---

### Timeline with interruption examples
![alt text](timeline1.png)

Interruption doesn't cancel simple of complex scope, that was already added to execution queue (the last example).

### Back / Up button behavior
Execution of scopes queue is based on back stack changes. So, you should completely control what is happening in the back stack, what can change it. By that reason override of default back / up button click behavior is needed. Just define default behavior with `ReactiveRouter` (e.g. with calling `closeCurrent` scope or something similar, what is calling `FragmentManager.popBackStack` under the hood).
