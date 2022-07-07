package com.example.drone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.drone.databinding.FragmentSecondBinding
import com.example.drone.utils.Util
import dji.common.flightcontroller.FlightControllerState
import dji.common.error.DJIError
import dji.common.flightcontroller.GPIOWorkModeOnBoard
import dji.common.flightcontroller.IOStateOnBoard
import dji.common.remotecontroller.HardwareState
import dji.common.remotecontroller.ProfessionalRC
import dji.common.util.CommonCallbacks
import dji.keysdk.KeyManager
import dji.keysdk.RemoteControllerKey
import dji.midware.data.model.P3.DataOnBoardSdkSetIOState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */

class SecondFragment : Fragment()/*, USBMonitor.OnDeviceConnectListener*/ {
    private val PTAG="POWER PORT"
    private var _binding: FragmentSecondBinding? = null
    private var sensorsFailed=false;

    private var p5Status=true;
    private var p4Status=true;

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)


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


        //Extended IO Port testing
        Util.getAircraftInstance().flightController.setPowerSupplyPortEnabled(true, object:CommonCallbacks.CompletionCallback<DJIError> {
            override fun onResult(p0: DJIError?) {
                Log.d(PTAG,p0.toString())
            }
        })


        Util.getAircraftInstance().flightController.initOnBoardIO(4,IOStateOnBoard.Builder.createInitialParams(GPIOWorkModeOnBoard.PUSH_PULL_OUTPUT),object:CommonCallbacks.CompletionCallback<DJIError> {
            override fun onResult(p0: DJIError?) {
                Log.d(PTAG,p0.toString())
            }
        })
        /*Util.getAircraftInstance().flightController.getOnBoardIO(4,object:CommonCallbacks.CompletionCallbackWith<IOStateOnBoard> {
            override fun onSuccess(p0: IOStateOnBoard?) {
                if (p0 != null) {
                    IOStateOnBoard.Builder.createReturnValue(true,DataOnBoardSdkSetIOState.GPIOMode.PushPullOutput,true)
                    val turnedOn = p0.gpioWorkModeOnBoard.value()
                    Log.d(PTAG,turnedOn.toString())

                }
            }

            override fun onFailure(p0: DJIError?) {
                Log.d(PTAG,p0.toString())
            }
        })*/
        Util.getAircraftInstance().flightController.initOnBoardIO(3,IOStateOnBoard.Builder.createInitialParams(GPIOWorkModeOnBoard.PUSH_PULL_OUTPUT),object:CommonCallbacks.CompletionCallback<DJIError> {
            override fun onResult(p0: DJIError?) {
                Log.d(PTAG,p0.toString())
            }
        })
        Util.getAircraftInstance().remoteController.setHardwareStateCallback (object:HardwareState.HardwareStateCallback{
            override fun onUpdate(p0: HardwareState) {
                p0.c1Button?.let {it->
                    if(it.isClicked){
                        p5Status=!p5Status;
                        if(p5Status){
                            activity?.runOnUiThread(Runnable {
                                val controlLED = view.findViewById<TextView>(R.id.control_led)
                                controlLED.setBackgroundResource(R.color.purple_200)
                            })
                            Util.getAircraftInstance().flightController.setOnBoardIO(4,IOStateOnBoard.Builder.createSetParams(true),object:CommonCallbacks.CompletionCallback<DJIError> {
                                override fun onResult(p0: DJIError?) {
                                    Log.d(PTAG,p0.toString())
                                }
                            })
                        }else{
                            activity?.runOnUiThread(Runnable {
                                val controlLED = view.findViewById<TextView>(R.id.control_led)
                                controlLED.setBackgroundResource(R.color.white)
                            })
                            Util.getAircraftInstance().flightController.setOnBoardIO(4,IOStateOnBoard.Builder.createSetParams(false),object:CommonCallbacks.CompletionCallback<DJIError> {
                                override fun onResult(p0: DJIError?) {
                                    Log.d(PTAG,p0.toString())
                                }
                            })
                        }
                    }

                }
                /*p0.c2Button?.let {it->
                    if(it.isClicked){
                        p4Status=!p4Status;
                        if(p4Status){
                            activity?.runOnUiThread(Runnable {
                                val pp4 = view.findViewById<TextView>(R.id.pp4)
                                pp4.setBackgroundResource(R.color.purple_200)
                            })
                            Util.getAircraftInstance().flightController.setOnBoardIO(3,IOStateOnBoard.Builder.createSetParams(true),object:CommonCallbacks.CompletionCallback<DJIError> {
                                override fun onResult(p0: DJIError?) {
                                    Log.d(PTAG,p0.toString())
                                }
                            })
                        }else{
                            activity?.runOnUiThread(Runnable {
                                val pp4 = view.findViewById<TextView>(R.id.pp4)
                                pp4.setBackgroundResource(R.color.white)
                            })
                            binding.pp4.setBackgroundResource(R.color.white)
                            Util.getAircraftInstance().flightController.setOnBoardIO(3,IOStateOnBoard.Builder.createSetParams(false),object:CommonCallbacks.CompletionCallback<DJIError> {
                                override fun onResult(p0: DJIError?) {
                                    Log.d(PTAG,p0.toString())
                                }
                            })
                        }
                    }}*/

            }

        })



        tryGetUltrasonic(view)

        binding.retry.setOnClickListener{
            if(sensorsFailed){
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
            sensorsFailed=false

            Util.getAircraftInstance().flightController.setStateCallback(object: FlightControllerState.Callback{
                override fun onUpdate(flightControllerState:FlightControllerState){
                    val distance = flightControllerState.ultrasonicHeightInMeters;
                    val barometricAltitude=flightControllerState.aircraftLocation.altitude
                    val ultrasonicUpdate= "Ultrasonic Distance: %.2f".format(distance);
                    val ultrasonicTextView = view.findViewById<TextView>(R.id.textview_ultrasonic)
                    val barometricUpdate= "Pressure Altitude: %.2f (Test ultrasonic accuracy first -> use pressure to get relative altitude..)".format(barometricAltitude);
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
            sensorsFailed=true;
        }
    }

}