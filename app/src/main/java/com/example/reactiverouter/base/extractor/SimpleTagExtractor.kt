package com.example.reactiverouter.base.extractor

import androidx.fragment.app.Fragment

/**
 * Simplest tag extractor, that uses fragments name as tag
 * */
open class SimpleTagExtractor : TagExtractor {
	override fun extractTagFrom(fragment: Fragment) = fragment.javaClass.name
}