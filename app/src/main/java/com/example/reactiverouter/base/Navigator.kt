package com.example.reactiverouter.base

import com.example.reactiverouter.base.extractor.TagExtractor

abstract class Navigator {
	lateinit var tagExtractor: TagExtractor
		internal set
}