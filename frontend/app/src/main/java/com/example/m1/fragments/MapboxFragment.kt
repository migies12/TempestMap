package com.example.m1.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.m1.MapboxActivity
import com.example.m1.R

class MapboxFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // Start MapboxActivity when fragment is opened
        val intent = Intent(requireContext(), MapboxActivity::class.java)
        startActivity(intent)

        return view
    }
}
