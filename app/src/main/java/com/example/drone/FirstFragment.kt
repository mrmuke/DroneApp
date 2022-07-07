package com.example.drone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.drone.MainActivity.Companion.productConnectionState
import com.example.drone.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(productConnectionState){
            enableStartButton()
        }
        else{
            disableStartButton()
        }
        val receiver = object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                enableStartButton()
            }
        }

        activity?.registerReceiver(receiver, IntentFilter("Product Connected"))
        binding.startRetrieval.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

    }
    fun disableStartButton(){
        binding.startRetrieval.isEnabled=false
        binding.startRetrieval.isClickable=false
    }
    fun enableStartButton(){
        binding.startRetrieval.isEnabled=true;
        binding.startRetrieval.isClickable=true;
        binding.startRetrieval.backgroundTintList= ColorStateList.valueOf(Color.BLUE)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}