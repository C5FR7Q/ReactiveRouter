package com.example.reactiverouter.base.extractor

import androidx.fragment.app.Fragment

interface TagExtractor {
	fun extractTagFrom(fragment: Fragment): String
}