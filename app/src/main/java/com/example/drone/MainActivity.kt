package com.example.drone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.internal.ContextUtils.getActivity
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.keysdk.FlightControllerKey
import dji.keysdk.KeyManager
import dji.log.DJILog
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.base.BaseProduct.ComponentKey
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.DJISDKManager.SDKManagerCallback
import dji.thirdparty.afinal.core.AsyncTask
import dji.thirdparty.io.reactivex.Observable
import dji.thirdparty.io.reactivex.ObservableEmitter
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {
    private var mHandler: Handler? = null
    private var prevRunnable:Runnable? = null

    private val missingPermission: MutableList<String> = ArrayList()
    private val isRegistrationInProgress: AtomicBoolean = AtomicBoolean(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()
        setContentView(R.layout.activity_main)

        //Initialize DJI SDK Manager
        mHandler = Handler(Looper.getMainLooper())

    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private fun checkAndRequestPermissions() {
        // Check for permissions
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    eachPermission
                ) !== PackageManager.PERMISSION_GRANTED
            ) {
                missingPermission.add(eachPermission)
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!")
            ActivityCompat.requestPermissions(
                this,
                missingPermission.toTypedArray(),
                REQUEST_PERMISSION_CODE
            )
        }
    }

    /**
     * Result of runtime permission request
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration()
        } else {
            showToast("Missing permissions!!!")
        }
    }

    companion object {


        private val TAG = MainActivity::class.java.name
        const val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
        const val PRODUCT_CONNECTED="Product Connected"
        const val PRODUCT_DISCONNECTED="Product Disconnected"
        private val mProduct: BaseProduct? = null
        private val REQUIRED_PERMISSION_LIST = arrayOf<String>(
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
        )

        private const val REQUEST_PERMISSION_CODE = 12345
    }

    private fun startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(Runnable {
                showToast("registering, pls wait...")
                DJISDKManager.getInstance()
                    .registerApp(this@MainActivity.applicationContext, object : SDKManagerCallback {
                        override fun onRegister(djiError: DJIError) {

                            if (djiError === DJISDKError.REGISTRATION_SUCCESS) {
                                showToast(
                                    "App registration: "+
                                    DJISDKError.REGISTRATION_SUCCESS.description
                                )

                                DJISDKManager.getInstance().startConnectionToProduct()


                            } else {
                                showToast("Register sdk failed: "+ djiError.description)

                            }

                        }

                        override fun onProductDisconnect() {
                            Log.d(TAG, "onProductDisconnect")
                            showToast(PRODUCT_DISCONNECTED)
                            notifyStatusChange(PRODUCT_DISCONNECTED)
                        }

                        override fun onProductConnect(baseProduct: BaseProduct) {
                            Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct))
                            showToast(PRODUCT_CONNECTED)
                            notifyStatusChange(PRODUCT_CONNECTED)
                        }

                        override fun onProductChanged(baseProduct:BaseProduct) {
                            Log.d(TAG, String.format("onProductChanged newProduct:%s", baseProduct))
                            showToast("Product Changed")
                        }

                        override fun onComponentChange(
                            componentKey: ComponentKey?, oldComponent: BaseComponent?,
                            newComponent: BaseComponent?
                        ) {
                            newComponent?.setComponentListener { isConnected ->
                                Log.d(TAG, "onComponentConnectivityChanged: $isConnected")
                            }
                            if(newComponent!=null&&componentKey!=null&&oldComponent!=null){
                                Log.d(
                                    TAG, String.format(
                                        "onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                        componentKey,
                                        oldComponent,
                                        newComponent
                                    )
                                )
                            }

                        }

                        override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, i: Int) {}
                        override fun onDatabaseDownloadProgress(l: Long, l1: Long) {}
                    })
            })
        }
    }
    private fun notifyStatusChange(msg:String) {
        val updateRunnable = Runnable {
            val intent = Intent(msg)
            sendBroadcast(intent)
        }
        prevRunnable?.let { mHandler!!.removeCallbacks(it) }
        prevRunnable=updateRunnable
        mHandler!!.postDelayed(updateRunnable, 500)
    }

    private fun showToast(toastMsg: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post { Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_LONG).show() }
    }
}