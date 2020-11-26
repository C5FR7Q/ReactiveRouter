package com.example.reactiverouter.base.navigator

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

open class SimpleNavigator(
	@IdRes
	private val containerId: Int,
	protected val fragmentManager: FragmentManager
) : Navigator() {
	open fun close() {
		if (fragmentManager.backStackEntryCount > 0) {
			fragmentManager.popBackStack()
			increaseStackChangeActionsCount()
		}
	}

	open fun closeUntil(tagSource: Any, inclusive: Boolean) {
		if (isShown(tagSource)) {
			fragmentManager.popBackStack(
				tagExtractor.extractTag(tagSource),
				if (inclusive) FragmentManager.POP_BACK_STACK_INCLUSIVE else 0
			)
			increaseStackChangeActionsCount()
		}
	}

	open fun clear() {
		if (fragmentManager.backStackEntryCount > 0) {
			fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
			increaseStackChangeActionsCount()
		}
	}

	open fun show(fragment: Fragment) {
		fragmentManager.beginTransaction()
			.replace(containerId, fragment, fragment.javaClass.simpleName)
			.addToBackStack(fragment.javaClass.simpleName)
			.commit()
		increaseStackChangeActionsCount()
	}

	open fun changeRoot(fragment: Fragment) {
		clear()
		show(fragment)
	}

	open fun replace(fragment: Fragment) {
		close()
		show(fragment)
	}

	open fun isShown(tagSource: Any) = fragmentManager.findFragmentByTag(tagExtractor.extractTag(tagSource)) != null

	open fun isVisible(tagSource: Any) = fragmentManager.findFragmentByTag(tagExtractor.extractTag(tagSource))?.isVisible ?: false
}