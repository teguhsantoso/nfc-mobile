package de.nfc.reader.util;

import android.content.Context;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

/**
 *
 *  Volley request queue singleton class.
 *  @author Teguh Santoso
 *  @since  version 1.0 2016
 *
 */
public class CustomVolleyRequestQueue {
    private static CustomVolleyRequestQueue mInstance;
    private final Context                   cTxt;
    private RequestQueue                    mRequestQueue;

    private CustomVolleyRequestQueue(Context context) {
        cTxt = context;
        mRequestQueue = getRequestQueue();
    }

    public static synchronized CustomVolleyRequestQueue getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new CustomVolleyRequestQueue(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            Cache cache = new DiskBasedCache(cTxt.getCacheDir(), 10 * 1024 * 1024);
            Network network = new BasicNetwork(new HurlStack());
            mRequestQueue = new RequestQueue(cache, network);

            // Start the volley request queue here.
            mRequestQueue.start();

        }
        return mRequestQueue;
    }

}
