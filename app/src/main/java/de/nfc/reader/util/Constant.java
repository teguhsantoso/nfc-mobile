package de.nfc.reader.util;

import android.Manifest;

import java.text.SimpleDateFormat;

/**
 *
 *  Class for storing all constant parameters used in this app.
 *  @author Teguh Santoso
 *  @since  version 1.0 2016
 *
 */
public class Constant {
    public static final String              LOGGER = "LOGGER";
    public static final long                TIME_MILIS_PERIOD_BACKPRESSED = 3000;
    public static final SimpleDateFormat    dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final SimpleDateFormat    dayFormatter = new SimpleDateFormat("yyyy-MM-dd");
    public static final int                 REQUEST_EXTERNAL_STORAGE = 1;
    public static final int                 BEEP_VOLUME_LEVEL = 100;
    public static final int                 BEEP_START_TIME = 200;
    public static final int                 INT_INITIAL_NFC_TAP = 1;
    public static final int                 INT_MAX_NFC_TAP = 2;
    public static final int                 VOLLEY_GET_OPERATION = 0;
    public static final int                 VOLLEY_POST_OPERATION = 1;
    public static final int                 PARAM_TIMER_DURATION_MILLIS = 1000;
    public static final int                 PARAM_TIMER_INTERVAL_MILLIS = 500;
    public static String[]                  PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static final String              ROOT_DIR_NAME = "/NFC-App";
    public static final String              DATA_FILE_NAME = "data.txt";
    public static final String              REQUEST_TAG = "MainVolleyActivity";
    public static final String              WEBSERVICE_URL_ADDRESS_GET = "http://api.jeni-us.xyz/api/absen/cek-status/";
    public static final String              WEBSERVICE_URL_ADDRESS_POST = "http://api.jeni-us.xyz/api/absen/push";
    public static final String              JSON_PARAM_TAG_ID = "tag_id";
    public static final String              JSON_PARAM_NAME = "nama";
    public static final String              JSON_PARAM_STATUS = "status";
    public static final String              JSON_PARAM_TIMESTAMP = "timestamp";
    public static final String              JSON_PARAM_MESSAGE = "message";
    public static final String              PARAM_OK = "Ok";
}
