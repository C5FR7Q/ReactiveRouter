package com.example.reactiverouter.base.extractor

import android.os.Bundle
import androidx.fragment.app.Fragment
import org.json.JSONException

import org.json.JSONObject

/**
 * SimpleKeyExtractor, that adds converted to json arguments to the end of the tag.
 * Doesn't support collections inside of arguments. Write your own one in case of such need.
 * */
class SimpleJsonKeyExtractor : SimpleKeyExtractor() {
	override fun extractKeyFrom(fragment: Fragment) = "${super.extractKeyFrom(fragment)}_${fragment.arguments.toJson()}"

	/* https://stackoverflow.com/a/21859000/7745890 */
	private fun Bundle?.toJson(): String? {
		if (this == null) return null
		val json = JSONObject()
		val keys: Set<String> = keySet()
		for (key in keys) {
			try {
				json.put(key, JSONObject.wrap(get(key)))
			} catch (e: JSONException) {
				return null
			}
		}
		return json.toString()
	}
}