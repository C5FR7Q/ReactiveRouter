package com.example.reactiverouter.base.extractor

import androidx.fragment.app.Fragment

interface KeyExtractor {
	fun extractKeyFrom(fragment: Fragment): String
}