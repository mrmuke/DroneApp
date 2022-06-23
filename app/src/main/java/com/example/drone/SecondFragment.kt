package com.example.drone

import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.SurfaceTexture
import android.media.MediaFormat
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
import androidx.constraintlayout.widget.ConstraintLayout
import dji.common.product.Model
import dji.sdk.codec.DJICodecManager
import java.nio.ByteBuffer

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */

class SecondFragment : Fragment(), TextureView.SurfaceTextureListener {

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
    private var receivedVideoDataListener: VideoFeeder.VideoDataListener?=null;
    private var videostreamPreview:TextureView? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        initUI(binding.root)


        return binding.root

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
                initPreview()
            }
        }


    }
    //error??
    fun navigateBack(){
        findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)

    }

    fun initUI(view:View){
        videostreamPreview= view?.findViewById(R.id.video)
        LOG.warning(videostreamPreview.toString())
        if(videostreamPreview!=null){
            LOG.warning("set surface texture listener ui")
            videostreamPreview!!.surfaceTextureListener=this
            receivedVideoDataListener =object: VideoFeeder.VideoDataListener {
                override fun onReceive(videoBuffer: ByteArray?, size: Int) {
                    Log.d("msg","new frame")
                    LOG.warning(videoBuffer.toString())
                    mCodecManager?.sendDataToDecoder(
                        videoBuffer,
                        size
                    )
                }



            }
        }
    }

    fun initPreview(){
        if (!Util.getProductInstance().model.equals(Model.UNKNOWN_AIRCRAFT)) {
            receivedVideoDataListener?.let {
                LOG.warning("Added listener..")
                VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(
                    it
                )
            }

        }


    }
    fun uninitPreview() {
        LOG.warning("uninit")
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

                   ;
                }
            })}else{
            val myToast = Toast.makeText(context, "Flight Controller Unavailable..", Toast.LENGTH_SHORT);
            myToast.show();
            ultrasonicFailed=true;
        }
    }


    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if(mCodecManager==null){
            mCodecManager= DJICodecManager(context,surface,width,height)
//            mCodecManager!!.enabledYuvData(true)
//            mCodecManager!!.yuvDataCallback = object:DJICodecManager.YuvDataCallback{
//                override fun onYuvDataReceived(
//                    mediaFormat: MediaFormat?,
//                    byteBuffer: ByteBuffer?,
//                    i: Int,
//                    i1: Int,
//                    i2: Int
//                ) {
//                    Log.d("msg","new yuv frame")
//                    LOG.warning("Got new yuv frame "+i+" "+i1+" "+i2)
//                }
//            }
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.e(TAG,"onSurfaceTextureSizeChanged")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.e(TAG,"onSurfaceTextureDestroyed")
        LOG.warning("surface texture destroyed")

        if(mCodecManager!=null){
            mCodecManager!!.cleanSurface()
            mCodecManager=null
        }
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
    }

    override fun onResume() {
        super.onResume()
        initPreview()
    }

    override fun onPause() {
        super.onPause()
        uninitPreview()
    }
    override fun onDestroyView() {
        LOG.warning("view destroyed")
        if(mCodecManager!=null){
            mCodecManager!!.cleanSurface()
            mCodecManager!!.destroyCodec()
            mCodecManager=null
        }
        uninitPreview()

        super.onDestroyView()
        _binding = null
    }

}