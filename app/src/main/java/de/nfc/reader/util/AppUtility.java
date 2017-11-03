package de.nfc.reader.util;

import android.content.Context;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 *  Singleton class for storing class member and also utilities method.
 *  @author Teguh Santoso
 *  @since  version 1.0 2016
 *
 */
public class AppUtility {

    /**
     * Initialize the self singleton class object.
     */
    private static AppUtility instance;

    public static AppUtility getInstance() {
        if (instance == null) {
            instance = new AppUtility();
        }
        return instance;
    }

    public String getAppVersionNumber(Context cTxt){
        String retVal = null;
        try{
            retVal = cTxt.getPackageManager().getPackageInfo(cTxt.getPackageName(), 0).versionName;
        }catch(Throwable throwable){
            Log.e(Constant.LOGGER, throwable.getLocalizedMessage());
        }
        return retVal;
    }

    public String convertByteArrayToHexString(byte[] inByteArray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        StringBuilder out= new StringBuilder();
        for(j = 0 ; j < inByteArray.length ; ++j)
        {
            in = (int) inByteArray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out.append(hex[i]);
            i = in & 0x0f;
            out.append(hex[i]);
        }
        return out.toString();
    }

    public boolean isInternetConnectionAvailable() {
        InetAddress inetAddress = null;
        try {
            Future<InetAddress> future = Executors.newSingleThreadExecutor().submit(new Callable<InetAddress>() {
                @Override
                public InetAddress call() {
                    try {
                        return InetAddress.getByName("google.com");
                    } catch (UnknownHostException e) {
                        return null;
                    }
                }
            });
            inetAddress = future.get(5000, TimeUnit.MILLISECONDS);
            future.cancel(true);
        } catch (InterruptedException e) {
            Log.e(Constant.LOGGER, e.getLocalizedMessage());
        } catch (ExecutionException e) {
            Log.e(Constant.LOGGER, e.getLocalizedMessage());
        } catch (TimeoutException e) {
            Log.e(Constant.LOGGER, e.getLocalizedMessage());
        }
        return inetAddress!=null && !inetAddress.equals("");
    }

}
