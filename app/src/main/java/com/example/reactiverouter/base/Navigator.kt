package com.example.reactiverouter.base

import com.example.reactiverouter.base.extractor.TagExtractor

/**
 * Should provide navigational functions, that deals with [androidx.fragment.app.FragmentManager]
 * */
abstract class Navigator {
	lateinit var tagExtractor: TagExtractor
		internal set
}