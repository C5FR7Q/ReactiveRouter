package com.example.reactiverouter.base.extractor

import androidx.fragment.app.Fragment

/**
 * Simplest key extractor, that uses fragments name as tag
 * */
open class SimpleKeyExtractor : KeyExtractor {
	override fun extractKeyFrom(fragment: Fragment) = fragment.javaClass.name
}