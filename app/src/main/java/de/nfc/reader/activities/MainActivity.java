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
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import de.nfc.reader.R;
import de.nfc.reader.entities.NFCData;
import de.nfc.reader.util.AppUtility;
import de.nfc.reader.util.Constant;
import de.nfc.reader.util.CustomJsonRequest;
import de.nfc.reader.util.CustomVolleyRequestQueue;

import static de.nfc.reader.util.Constant.BEEP_START_TIME;
import static de.nfc.reader.util.Constant.BEEP_VOLUME_LEVEL;
import static de.nfc.reader.util.Constant.DATA_FILE_NAME;
import static de.nfc.reader.util.Constant.PERMISSIONS_STORAGE;
import static de.nfc.reader.util.Constant.REQUEST_EXTERNAL_STORAGE;
import static de.nfc.reader.util.Constant.ROOT_DIR_NAME;

/**
 *
 *  Main activity for reading the NFC card and showing the student data.
 *  @author Teguh Santoso
 *  @since  version 1.0 2016
 *
 */
public class MainActivity extends AppCompatActivity implements Response.Listener, Response.ErrorListener {
    public static final String  REQUEST_TAG = "MainVolleyActivity";
    private Context             cTxt;
    private TextView            textViewInfo;
    private TextView            textViewAppVersionNumber;
    private TextView            textViewTagId;
    private TextView            textViewTimetamp;
    private ImageView           imageViewWarning;
    private ImageView           imageViewSuccess;
    private long                back_pressed_time;
    private NfcAdapter          nfcAdapter;
    private RequestQueue        mQueue;

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
        cTxt = this;

        // Prepare the root directory and the data.txt file.
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + ROOT_DIR_NAME);
        if(!dir.exists()){
            dir.mkdirs();
        }
        File file = new File(dir, DATA_FILE_NAME);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Initialize all UI elements.
        this.textViewAppVersionNumber = (TextView)findViewById(R.id.textViewVersionNumber);
        this.textViewAppVersionNumber.setText("v" + AppUtility.getInstance().getAppVersionNumber(cTxt));
        this.textViewTagId = (TextView)findViewById(R.id.textViewTagId);
        this.textViewTagId.setVisibility(View.GONE);
        this.textViewTimetamp = (TextView)findViewById(R.id.textViewTimestamp);
        this.textViewTimetamp.setVisibility(View.GONE);
        this.imageViewWarning = (ImageView)findViewById(R.id.imageViewWarning);
        this.imageViewWarning.setVisibility(View.GONE);
        this.imageViewSuccess = (ImageView)findViewById(R.id.imageViewSuccess);
        this.imageViewSuccess.setVisibility(View.GONE);
        this.textViewInfo = (TextView)findViewById(R.id.textViewReport);

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
            mQueue.cancelAll(REQUEST_TAG);
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
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {

            // Set beep sound if new tag discovered.
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, BEEP_VOLUME_LEVEL);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_START_TIME);

            // Retrieve the tag UID from intent, it contains 7 bytes.
            String tagID = AppUtility.getInstance().convertByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            String strTimestamp = getCurrentTimestamp();

            // Refresh the UI elements contents and states.
            this.textViewTagId.setVisibility(View.VISIBLE);
            this.textViewTimetamp.setVisibility(View.VISIBLE);
            if(tagID == null){
                this.textViewTagId.setText(getResources().getString(R.string.text_no_tag_id_found));
            }else{
                this.textViewTagId.setText(getResources().getString(R.string.text_tag_id) + ":" + tagID);
                this.textViewTimetamp.setText(getResources().getString(R.string.text_timestamp) + ":" + strTimestamp);

                // Store NFC data in text file inside external storage device.
                //storeNFCData(new NFCData(tagID, strTimestamp));

                // Check if internet connection is available.
                if(!AppUtility.getInstance().isInternetConnectionAvailable(5000)){
                    Log.d(Constant.LOGGER, ">>>>>>> No internet connection.");
                    // TODO
                    // Show warning message if no internet connection.
                    return;
                };

                // Send request to volley queue based on webservice address.
                String urlWS = Constant.WEBSERVICE_URL_ADDRESS + tagID;
                final CustomJsonRequest jsonRequest = new CustomJsonRequest(Request.Method.GET, urlWS, new JSONObject(), this, this);
                jsonRequest.setTag(REQUEST_TAG);
                mQueue.add(jsonRequest);

            }
        }
    }

    public void openSetting(View view){
        Toast.makeText(getBaseContext(), "Coming soon", Toast.LENGTH_SHORT).show();
    }

    private String getCurrentTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return Constant.dateFormatter.format(timestamp);
    }

    private String getCurrentDate(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return Constant.dayFormatter.format(timestamp);
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

    private boolean isUserAlreadyTappedTwiceInSameDay(File file, String userId){
        boolean retVal = false;
        List<NFCData> storageData = new ArrayList<NFCData>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split(",");
                String tagId = parts[0];
                String timestamp = parts[1];
                NFCData newData = new NFCData(tagId, timestamp);
                storageData.add(newData);
            }
            br.close();
        }
        catch (IOException e) {
            Log.e(Constant.LOGGER, e.getLocalizedMessage().toString());
        }

        int tappedSum = Constant.INT_INITIAL_NFC_TAP;
        for (NFCData data : storageData) {
            String strDateData = data.getTimestamp();
            String[] parts = strDateData.trim().split(" ");
            String tapDate = parts[0].trim();
            if(tapDate.equals(getCurrentDate()) && data.getTagId().equals(userId)){
                tappedSum++;
            }
        }

        if(tappedSum > Constant.INT_MAX_NFC_TAP){
            return true;
        }

        return retVal;
    }

    private boolean storeNFCData(NFCData data){
        File root = android.os.Environment.getExternalStorageDirectory();

        File dir = new File (root.getAbsolutePath() + ROOT_DIR_NAME);
        if(!dir.exists()){
            dir.mkdirs();
        }

        File file = new File(dir, DATA_FILE_NAME);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(!file.exists()){
            Toast.makeText(getBaseContext(), getResources().getString(R.string.text_data_file_not_found), Toast.LENGTH_SHORT).show();
            return false;
        }else{
            if(isUserAlreadyTappedTwiceInSameDay(file, data.getTagId())){
                this.imageViewWarning.setVisibility(View.VISIBLE);
                this.imageViewSuccess.setVisibility(View.INVISIBLE);
                this.textViewInfo.setVisibility(View.VISIBLE);
                this.textViewInfo.setText(getResources().getString(R.string.text_user_tapped_twice_today));
            }else{
                this.imageViewWarning.setVisibility(View.INVISIBLE);
                this.imageViewSuccess.setVisibility(View.VISIBLE);
                this.textViewInfo.setVisibility(View.VISIBLE);
                this.textViewInfo.setText(getResources().getString(R.string.text_user_presence_recorded));
            }

            FileOutputStream fos = null;
            OutputStreamWriter outStreamWriter = null;
            try {
                fos = new FileOutputStream(file, true);
                outStreamWriter = new OutputStreamWriter(fos);
                outStreamWriter.append(data.getTagId() + "," + data.getTimestamp()).append("\n");
                outStreamWriter.flush();
                Log.d(Constant.LOGGER, ">>> Data for tag-ID " + data.getTagId() + " was stored successfully.");
                fos.close();
                return true;
            } catch (Throwable throwable) {
                Log.e(Constant.LOGGER, throwable.getLocalizedMessage().toString());
            }

        }

        return false;
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.d(Constant.LOGGER, ">>> Response error server: " + error.getLocalizedMessage());
    }

    @Override
    public void onResponse(Object response) {
        try {
            JSONObject mData = (JSONObject) response;
            Log.d(Constant.LOGGER, ">>> User name: " + mData.getString("nama"));
            Log.d(Constant.LOGGER, ">>> Tag-ID: " + mData.getString("tag_id"));
            Log.d(Constant.LOGGER, ">>> Status: " + mData.getString("status"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
