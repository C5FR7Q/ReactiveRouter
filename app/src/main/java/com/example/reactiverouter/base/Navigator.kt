package com.example.reactiverouter.base

import com.example.reactiverouter.base.extractor.KeyExtractor

abstract class Navigator {
	lateinit var keyExtractor: KeyExtractor
		internal set
}