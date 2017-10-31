package de.nfc.reader.util;

import android.content.Context;
import android.util.Log;

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
            Log.e(Constant.LOGGER, throwable.getLocalizedMessage().toString());
        }
        return retVal;
    }

    public String convertByteArrayToHexString(byte[] inByteArray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";
        for(j = 0 ; j < inByteArray.length ; ++j)
        {
            in = (int) inByteArray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

}
