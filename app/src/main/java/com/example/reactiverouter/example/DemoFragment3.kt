package com.example.reactiverouter.example

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.reactiverouter.R
import kotlin.random.Random

class DemoFragment3 : Fragment() {
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.demo_fragment_3, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.setBackgroundColor(
			Color.rgb(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))
		)
	}
}