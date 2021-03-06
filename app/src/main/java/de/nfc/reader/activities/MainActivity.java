package de.nfc.reader.activities;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.nfc.reader.R;
import de.nfc.reader.util.AppUtility;
import de.nfc.reader.util.Constant;
import de.nfc.reader.util.CustomJsonRequest;
import de.nfc.reader.util.CustomVolleyRequestQueue;

import static de.nfc.reader.util.Constant.BEEP_START_TIME;
import static de.nfc.reader.util.Constant.BEEP_VOLUME_LEVEL;
import static de.nfc.reader.util.Constant.PERMISSIONS_STORAGE;
import static de.nfc.reader.util.Constant.REQUEST_EXTERNAL_STORAGE;

/**
 *
 *  Main activity for reading the NFC card and showing the student data.
 *  @author Teguh Santoso
 *  @since  version 1.0 2016
 *
 */
public class MainActivity extends AppCompatActivity implements Response.Listener, Response.ErrorListener {
    private TextView            textViewInfo;
    private TextView            textViewTagId;
    private TextView            textViewTimestamp;
    private ImageView           imageViewWarning;
    private ImageView           imageViewSuccess;
    private ProgressBar         progressBarSendData;
    private long                back_pressed_time;
    private NfcAdapter          nfcAdapter;
    private RequestQueue        mQueue;
    private int                 volleyOperationMode;

    // List of NFC technologies available for this app:
    private final String[][] techList = new String[][] {
            new String[] {
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcF.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(), Ndef.class.getName()
            }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the current context on this activity.
        Context cTxt = this;

        // Prepare data for absence inside external storage device.
        //prepareDataAbsenceInStorage();

        // Initialize all UI elements.
        TextView textViewAppVersionNumber = findViewById(R.id.textViewVersionNumber);
        textViewAppVersionNumber.setText(AppUtility.getInstance().getAppVersionNumber(cTxt));
        this.textViewTagId = findViewById(R.id.textViewTagId);
        this.textViewTagId.setVisibility(View.GONE);
        this.textViewTimestamp = findViewById(R.id.textViewTimestamp);
        this.textViewTimestamp.setVisibility(View.GONE);
        this.imageViewWarning = findViewById(R.id.imageViewWarning);
        this.imageViewWarning.setVisibility(View.GONE);
        this.imageViewSuccess = findViewById(R.id.imageViewSuccess);
        this.imageViewSuccess.setVisibility(View.GONE);
        this.textViewInfo = findViewById(R.id.textViewReport);
        this.progressBarSendData = findViewById(R.id.progressBarConnecting);
        this.progressBarSendData.setVisibility(View.INVISIBLE);

        // Initialize the NFC adapter for reading the tag UID.
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(this.nfcAdapter == null){
            Toast.makeText(this, getResources().getString(R.string.text_nfc_is_not_supported), Toast.LENGTH_LONG).show();
        }else if(!this.nfcAdapter.isEnabled()){
            Toast.makeText(this, getResources().getString(R.string.text_nfc_is_not_enabled), Toast.LENGTH_LONG).show();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Instantiate our volley request queue.
        this.mQueue = CustomVolleyRequestQueue.getInstance(this.getApplicationContext()).getRequestQueue();

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Verify the write external storage permission.
        verifyStoragePermissions(this);

        // Initialize the NFC adapter for reading the tag UID.
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(this.nfcAdapter == null){
            Toast.makeText(this, getResources().getString(R.string.text_nfc_is_not_supported), Toast.LENGTH_LONG).show();
        }else if(!this.nfcAdapter.isEnabled()){
            Toast.makeText(this, getResources().getString(R.string.text_nfc_is_not_enabled), Toast.LENGTH_LONG).show();
        }

        if(this.nfcAdapter != null && this.nfcAdapter.isEnabled()){
            // Creating pending intent for reading NFC tag.
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            // Creating intent receiver for NFC events.
            IntentFilter filter = new IntentFilter();
            filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
            filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

            // Enabling foreground dispatch for getting intent from NFC event.
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Disabling foreground dispatch:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mQueue != null) {
            mQueue.cancelAll(Constant.REQUEST_TAG);
        }
    }

    @Override
    public void onBackPressed() {
        if (this.back_pressed_time + Constant.TIME_MILIS_PERIOD_BACKPRESSED > System.currentTimeMillis()) {
            closeApp();
        } else {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.text_press_once_again), Toast.LENGTH_SHORT).show();
        }
        this.back_pressed_time = System.currentTimeMillis();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Objects.equals(intent.getAction(), NfcAdapter.ACTION_TAG_DISCOVERED)) {

                // Set beep sound if new tag discovered.
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, BEEP_VOLUME_LEVEL);
                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_START_TIME);

                // Retrieve the tag UID from intent, it contains 7 bytes.
                String tagID = AppUtility.getInstance().convertByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));

                // Refresh the UI elements contents and states.
                this.textViewTagId.setVisibility(View.VISIBLE);
                if(tagID == null){
                    this.textViewTagId.setText(getResources().getString(R.string.text_no_tag_id_found));
                }else{
                    this.imageViewSuccess.setVisibility(View.INVISIBLE);
                    this.imageViewWarning.setVisibility(View.INVISIBLE);
                    this.textViewTimestamp.setVisibility(View.INVISIBLE);
                    this.textViewInfo.setText("");
                    this.textViewTagId.setText(getResources().getString(R.string.text_tag_id, tagID));

                    // Check if internet connection is available.
                    if(!AppUtility.getInstance().isInternetConnectionAvailable()){
                        this.imageViewWarning.setVisibility(View.VISIBLE);
                        this.textViewInfo.setText(getResources().getString(R.string.text_no_internet_connection));
                        return;
                    }

                    // Send request to volley queue based on webservice address.
                    String urlWS = Constant.WEBSERVICE_URL_ADDRESS_GET + tagID;
                    //noinspection unchecked
                    final CustomJsonRequest jsonRequest = new CustomJsonRequest(Request.Method.GET, urlWS, new JSONObject(), this, this);
                    jsonRequest.setTag(Constant.REQUEST_TAG);
                    volleyOperationMode = Constant.VOLLEY_GET_OPERATION;
                    mQueue.add(jsonRequest);

                }
            }
        }
    }

    private String getCurrentTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return Constant.dateFormatter.format(timestamp);
    }

    private void closeApp(){
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void verifyStoragePermissions(Activity activity) {
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        this.imageViewWarning.setVisibility(View.VISIBLE);
        this.textViewTimestamp.setVisibility(View.INVISIBLE);
        this.textViewInfo.setText(getResources().getString(R.string.text_unknown_card));
    }

    @Override
    public void onResponse(Object response) {
        try {

            // Show progress bar for sending data to server.
            progressBarSendData.setVisibility(View.VISIBLE);

            // Validation for the volley operation mode, 0 = GET, 1 = POST.
            if(volleyOperationMode == Constant.VOLLEY_GET_OPERATION){
                final JSONObject mData = (JSONObject) response;
                switch(Integer.valueOf(mData.getString(Constant.JSON_PARAM_STATUS))){
                    case 0:
                        this.imageViewWarning.setVisibility(View.VISIBLE);
                        this.textViewTimestamp.setVisibility(View.INVISIBLE);
                        this.textViewInfo.setText(getResources().getString(R.string.text_unknown_card));
                        break;
                    case 1:
                        this.textViewTimestamp.setVisibility(View.VISIBLE);
                        final String strName = ((JSONObject) response).getString(Constant.JSON_PARAM_NAME);
                        final String strTimestamp = getCurrentTimestamp();
                        //this.textViewTimestamp.setText(getResources().getString(R.string.text_name) + ": " + strName + "\n" + getResources().getString(R.string.text_timestamp) + ": " + strTimestamp);
                        this.textViewTimestamp.setText(getResources().getString(R.string.text_name, strName, strTimestamp));
                        this.textViewInfo.setText(getResources().getString(R.string.text_data_sent_to_system));
                        sendDataAbsenceToServer(mData.getString(Constant.JSON_PARAM_TAG_ID), strTimestamp);
                        break;
                    default:
                        this.imageViewWarning.setVisibility(View.VISIBLE);
                        this.textViewTimestamp.setVisibility(View.INVISIBLE);
                        this.textViewInfo.setText(getResources().getString(R.string.text_unknown_card));
                }
            }else if(volleyOperationMode == Constant.VOLLEY_POST_OPERATION){
                JSONObject mData = (JSONObject) response;
                String mString = mData.getString(Constant.JSON_PARAM_MESSAGE);
                if(!mString.trim().equals(Constant.PARAM_OK)){
                    progressBarSendData.setVisibility(View.INVISIBLE);
                    this.imageViewWarning.setVisibility(View.VISIBLE);
                    this.textViewInfo.setText(getResources().getString(R.string.text_fail_sent_data_to_system));
                    progressBarSendData.setVisibility(View.INVISIBLE);
                }else{
                    progressBarSendData.setVisibility(View.INVISIBLE);
                    this.imageViewSuccess.setVisibility(View.VISIBLE);
                    this.textViewInfo.setText(getResources().getString(R.string.text_success_sent_data_to_system));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void sendDataAbsenceToServer(String strTagId, String strTimestamp) {
        // Prepare the post method parameters for volley.
        Map<String, String> params = new HashMap<>();
        params.put(Constant.JSON_PARAM_TAG_ID, strTagId);
        params.put(Constant.JSON_PARAM_TIMESTAMP, strTimestamp);

        // Send post request using volley queue.
        @SuppressWarnings("unchecked")
        final CustomJsonRequest jsonRequest = new CustomJsonRequest(Request.Method.POST, Constant.WEBSERVICE_URL_ADDRESS_POST, new JSONObject(params), this, this);
        jsonRequest.setTag(Constant.REQUEST_TAG);
        volleyOperationMode = Constant.VOLLEY_POST_OPERATION;
        mQueue.add(jsonRequest);
    }
}
