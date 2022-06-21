package com.example.drone.utils;

import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class Util {
    public static synchronized BaseProduct getProductInstance(){
        return DJISDKManager.getInstance().getProduct();
    }
    public static boolean isAircraft(){
        return getProductInstance() instanceof Aircraft;
    }
    public static synchronized Aircraft getAircraftInstance(){
        return (Aircraft) getProductInstance();
    }
    public static boolean isProductModuleAvaliable(){
        return getProductInstance()!=null;
    }
    public static boolean isRTKAvaliable(){
        return isProductModuleAvaliable() && isAircraft() && getAircraftInstance().getFlightController().getRTK()!=null;
    }
    public static boolean isFlightControllerAvaliable(){
        return isProductModuleAvaliable()&&isAircraft()&&
                null != Util.getAircraftInstance().getFlightController();
    }

}
