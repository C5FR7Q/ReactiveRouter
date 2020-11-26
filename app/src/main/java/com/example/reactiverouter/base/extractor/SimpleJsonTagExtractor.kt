package com.example.reactiverouter.base.extractor

import android.os.Bundle
import androidx.fragment.app.Fragment
import org.json.JSONException
import org.json.JSONObject

/**
 * [SimpleTagExtractor], that adds converted to json arguments to the end of the tag.
 * Doesn't support collections inside of arguments. Write your own one in case of such need.
 * */
class SimpleJsonTagExtractor : SimpleTagExtractor() {

	override fun extractTag(source: Any): String {
		return if (source is TagModel) {
			getTagFromClassAndBundle(source.clazz, source.bundle)
		} else super.extractTag(source)
	}

	override fun getTagFromClass(clazz: Class<*>): String {
		return getTagFromClassAndBundle(clazz, null)
	}

	override fun getTagFromFragment(fragment: Fragment): String {
		return getTagFromClassAndBundle(fragment::class.java, fragment.arguments)
	}

	private fun getTagFromClassAndBundle(clazz: Class<*>, bundle: Bundle?): String {
		return "${clazz.name}_${bundle.toJson()}"
	}

	/* https://stackoverflow.com/a/21859000/7745890 */
	private fun Bundle?.toJson(): String {
		if (this == null) return ""
		val json = JSONObject()
		val keys: Set<String> = keySet()
		for (key in keys) {
			try {
				json.put(key, JSONObject.wrap(get(key)))
			} catch (e: JSONException) {
				return ""
			}
		}
		return json.toString()
	}

	data class TagModel(
		val clazz: Class<out Fragment>,
		val bundle: Bundle?
	)
}