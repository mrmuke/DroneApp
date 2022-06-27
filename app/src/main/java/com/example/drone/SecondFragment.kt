package com.example.drone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.text.method.KeyListener
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.drone.databinding.FragmentSecondBinding
import com.example.drone.utils.Util
import com.serenegiant.usb.Size
import dji.common.flightcontroller.FlightControllerState
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import dji.common.error.DJIError
import dji.common.flightcontroller.IOStateOnBoard
import dji.common.remotecontroller.ProfessionalRC
import dji.common.util.CommonCallbacks
import dji.keysdk.KeyManager
import dji.keysdk.RemoteControllerKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */

class SecondFragment : Fragment()/*, USBMonitor.OnDeviceConnectListener*/ {

    private var _binding: FragmentSecondBinding? = null
    private var ultrasonicFailed=false;
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
/*

    private var mUSBMonitor: USBMonitor? = null
    private var currentCamera: UVCCamera? = null
    private val cameraMutex = Mutex()
    private var surface: Surface? = null


    companion object {
        const val TAG = "UvcCameraExample"
        const val RTAG = "Remote Controller"
    }
    override fun onStart() {
        super.onStart()
        mUSBMonitor?.register()
    }

    override fun onStop() {
        mUSBMonitor?.unregister()
        releaseCameraAsync()
        super.onStop()
    }

    /**
     * Called when the camera is attached to the Android device
     */
    override fun onAttach(device: UsbDevice?) {
        Log.i(TAG, "Device has been attached")
        releaseCameraAsync()
        // When the camera is attached, we need to ask the user for permission to access it.
        mUSBMonitor?.requestPermission(device)
    }
    /**
     * Called when the camera connects
     *
     * Initialize camera properties for the preview stream
     */
    override fun onConnect(
        device: UsbDevice?,
        controlBlock: USBMonitor.UsbControlBlock?,
        createNew: Boolean
    ) {

        Log.i(TAG, "Device has been connected")

        // Try to open the camera that was connected
        val camera = UVCCamera()
        try {
            camera.open(controlBlock)
        } catch (e: UnsupportedOperationException) {
            Log.e(TAG, "Failed to open camera", e)
            return
        }
        val textureView = view?.findViewById<TextureView>(R.id.video)
        Log.d(TAG,textureView.toString())
        // Specify a surface to display camera feed
        if (textureView != null) {
            surface = Surface(textureView.surfaceTexture)
        }
        camera.setPreviewDisplay(surface)


        // This frame format is used for the thermal camera.
        // To use a different camera type, the format may have to be changed.
        camera.setPreviewSize(50,50, UVCCamera.FRAME_FORMAT_YUYV)

        camera.startPreview()

        // Store camera for later so it can be properly released
        storeCameraAsync(camera)
    }

    /**
     * Called when the camera connection is cancelled
     */
    override fun onCancel(device: UsbDevice?) {
        Log.i(TAG, "Device connection has been cancelled")
    }

    /**
     * Called when the camera disconnects
     */
    override fun onDisconnect(device: UsbDevice?, controlBlock: USBMonitor.UsbControlBlock?) {
        Log.i(TAG, "Device has disconnected")
        releaseCameraAsync()
    }

    /**
     * Called when the camera is detached
     */
    override fun onDettach(device: UsbDevice?) {
        Log.i(TAG, "Device has been detached")
    }

    /**
     * Save the currently connected [camera].
     */
    private fun storeCameraAsync(camera: UVCCamera) = GlobalScope.async {
        cameraMutex.withLock {
            currentCamera = camera
        }
    }

    /**
     * Disconnect from the current camera.
     */
    private fun releaseCameraAsync() = GlobalScope.async {
        cameraMutex.withLock {
            currentCamera?.stopPreview()
            currentCamera?.setStatusCallback(null)
            currentCamera?.setButtonCallback(null)
            currentCamera?.close()
            currentCamera = null

            surface?.release()
            surface = null
        }
    }

*/
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)


        return binding.root


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
        //mUSBMonitor = USBMonitor(context, this)


        //Extended IO Port testing
        Util.getAircraftInstance().flightController.setPowerSupplyPortEnabled(true, object:CommonCallbacks.CompletionCallback<DJIError> {
            override fun onResult(p0: DJIError) {
                Log.d(RTAG,p0.toString())
            }
        })
        Util.getAircraftInstance().flightController.initOnBoardIO(0,IOStateOnBoard.Builder.createInitialParams(1),object:CommonCallbacks.CompletionCallback<DJIError> {
            override fun onResult(p0: DJIError) {
                Log.d(RTAG,p0.toString())
            }
        })
        Util.getAircraftInstance().flightController.setOnBoardIO(0,IOStateOnBoard9,object:CommonCallbacks.CompletionCallback<DJIError> {
            override fun onResult(p0: DJIError) {
                Log.d(RTAG,p0.toString())
            }
        })
        //method 1
        //Util.getAircraftInstance().remoteController.customizeButton(ProfessionalRC.CustomizableButton.C1,ProfessionalRC.ButtonAction)
        //method 2
        val keyC1:RemoteControllerKey = RemoteControllerKey.create(RemoteControllerKey.CUSTOM_BUTTON_1)
        KeyManager.getInstance().addListener(keyC1,object:dji.keysdk.callback.KeyListener{
            override fun onValueChange(p0: Any?, p1: Any?) {
                Log.d(RTAG,"Old Value" + p0.toString())
                Log.d(RTAG,"New Value: "+p1.toString())
                Log.d(RTAG,"Pressed")
            }
        })


        tryGetUltrasonic(view)

        binding.retry.setOnClickListener{
            if(ultrasonicFailed){
                tryGetUltrasonic(view)
            }
        }


    }
    //error??
    fun navigateBack(){
        findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)

    }

    fun tryGetUltrasonic(view:View){
        if(Util.isFlightControllerAvaliable()
        ){
            ultrasonicFailed=false

            Util.getAircraftInstance().flightController.setStateCallback(object: FlightControllerState.Callback{
                override fun onUpdate(flightControllerState:FlightControllerState){
                    val distance = flightControllerState.ultrasonicHeightInMeters;
                    val barometricAltitude=flightControllerState.aircraftLocation.altitude
                    val ultrasonicUpdate= "Ultrasonic Distance: %.2f".format(distance);
                    val ultrasonicTextView = view.findViewById<TextView>(R.id.textview_ultrasonic)
                    val barometricUpdate= "Barometric Altitude: %.2f".format(barometricAltitude);
                    val barometricTextView = view.findViewById<TextView>(R.id.textview_barometric)
                    //get distance between each using barometric
                    //set package 0 value for altitude at beginning of adventure and subtract
                    activity?.runOnUiThread(Runnable {
                        ultrasonicTextView.text=ultrasonicUpdate;
                        barometricTextView.text=barometricUpdate
                    })


                   ;
                }
            })
            Util.getAircraftInstance().flightController

        }else{
            val myToast = Toast.makeText(context, "Flight Controller Unavailable..", Toast.LENGTH_SHORT);
            myToast.show();
            ultrasonicFailed=true;
        }
    }

}