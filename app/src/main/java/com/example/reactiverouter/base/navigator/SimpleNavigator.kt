package com.example.reactiverouter.base.navigator

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.reactiverouter.R

open class SimpleNavigator(protected val fragmentManager: FragmentManager) : Navigator() {
	open fun close() {
		if (fragmentManager.backStackEntryCount > 0) {
			fragmentManager.popBackStack()
			increaseStackChangeActionsCount()
		}
	}

	open fun closeUntil(fragment: Fragment, inclusive: Boolean) {
		if (isShown(fragment)) {
			fragmentManager.popBackStack(
				tagExtractor.extractTagFrom(fragment),
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
			.replace(R.id.main_container, fragment, fragment.javaClass.simpleName)
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

	open fun isShown(fragment: Fragment) = fragmentManager.findFragmentByTag(tagExtractor.extractTagFrom(fragment)) != null

	open fun isVisible(fragment: Fragment) = fragmentManager.findFragmentByTag(tagExtractor.extractTagFrom(fragment))?.isVisible ?: false
}