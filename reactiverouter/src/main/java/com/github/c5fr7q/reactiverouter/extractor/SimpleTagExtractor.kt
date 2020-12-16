package com.github.c5fr7q.reactiverouter.extractor

import androidx.fragment.app.Fragment

/**
 * Simplest tag [TagExtractor], that uses [Fragment]'s class name as tag.
 * Supports either [Fragment] or its [Class]
 * */
open class SimpleTagExtractor : TagExtractor() {
	override fun extractTag(source: Any): String {
		return when {
			source is Fragment -> getTagFromFragment(source)
			source is Class<*> && source.isAssignableFrom(Fragment::class.java) -> getTagFromClass(source)
			else -> super.extractTag(source)
		}
	}

	protected open fun getTagFromClass(clazz: Class<*>) = clazz.name
	protected open fun getTagFromFragment(fragment: Fragment) = fragment.javaClass.name
}