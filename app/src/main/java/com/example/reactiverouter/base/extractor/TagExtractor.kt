package com.example.reactiverouter.base.extractor

import androidx.fragment.app.Fragment

/**
 * Represents [Fragment] as [String] tag
 * */
interface TagExtractor {
	fun extractTagFrom(fragment: Fragment): String
}