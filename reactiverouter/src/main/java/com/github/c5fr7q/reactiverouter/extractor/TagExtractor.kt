package com.github.c5fr7q.reactiverouter.extractor

/**
 * Represents [Any] as [String] tag
 * */
abstract class TagExtractor {
	open fun extractTag(source: Any): String {
		if (source is String) {
			return source
		}
		throw RuntimeException("${javaClass.name} doesn't support ${source.javaClass.name}")
	}
}