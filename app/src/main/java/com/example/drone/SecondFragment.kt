package com.example.drone

import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.drone.databinding.FragmentSecondBinding
import com.example.drone.utils.Util
import dji.common.flightcontroller.FlightControllerState
import dji.sdk.camera.VideoFeeder
import java.util.logging.Logger
import android.view.TextureView
import dji.sdk.codec.DJICodecManager

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */

class SecondFragment : Fragment(), TextureView.SurfaceTextureListener  {

    private var _binding: FragmentSecondBinding? = null
    private var ultrasonicFailed=false;
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    companion object{
        val LOG: Logger = Logger.getLogger(SecondFragment::class.java.name)
    }
    //CAMERA
    private var mCodecManager:DJICodecManager? =null;
    private val receivedVideoDataListener: VideoFeeder.VideoDataListener =
        VideoFeeder.VideoDataListener { videoBuffer, size -> mCodecManager?.sendDataToDecoder(videoBuffer,size) }
    private var videostreamPreview:TextureView? = null
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }
    fun navigateBack(){
        findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            navigateBack()
        }
        val receiver = object: BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) {
                navigateBack()
            }
        }
        activity?.registerReceiver(receiver, IntentFilter("Product Disconnected"))

        initPreview()

        tryGetUltrasonic(view)

        binding.retry.setOnClickListener{
            if(ultrasonicFailed){
                tryGetUltrasonic(view)

            }
        }


    }

    fun initPreview(){
        videostreamPreview= view?.findViewById<TextureView>(R.id.video)

        Util.getProductInstance().camera?.let{
            if(videostreamPreview!=null){
                videostreamPreview!!.surfaceTextureListener=this
            }
            if (receivedVideoDataListener != null) {
                VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(receivedVideoDataListener)
            }
        }
    }
    fun uninitPreview() {
        if(Util.getProductInstance().camera!=null){
            VideoFeeder.getInstance().primaryVideoFeed.removeVideoDataListener(receivedVideoDataListener)
        }
    }

    fun tryGetUltrasonic(view:View){
        if(Util.isFlightControllerAvaliable()
        ){
            ultrasonicFailed=false

            Util.getAircraftInstance().flightController.setStateCallback(object: FlightControllerState.Callback{
                override fun onUpdate(flightControllerState:FlightControllerState){
                    val alt = flightControllerState.ultrasonicHeightInMeters;
                    val update= "Ultrasonic Altitude: %.2f".format(alt);
                    val ultrasonicTextView = view.findViewById<TextView>(R.id.textview_ultrasonic)
                    activity?.runOnUiThread(Runnable {
                        ultrasonicTextView.text=update;
                    })

                    LOG.warning(update);
                }
            })}else{
            val myToast = Toast.makeText(context, "Flight Controller Unavailable..", Toast.LENGTH_SHORT);
            myToast.show();
            ultrasonicFailed=true;
        }
    }

    override fun onDestroyView() {
        if(mCodecManager!=null){
            mCodecManager!!.cleanSurface()
            mCodecManager!!.destroyCodec()
            mCodecManager=null
        }
        super.onDestroyView()
        _binding = null
        //setStateCallback null for both
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if(mCodecManager==null){
            mCodecManager= DJICodecManager(context,surface,width,height)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.e(TAG,"onSurfaceTextureSizeChanged")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.e(TAG,"onSurfaceTextureDestroyed")
        if(mCodecManager!=null){
            mCodecManager!!.cleanSurface()
            mCodecManager=null
        }
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

}