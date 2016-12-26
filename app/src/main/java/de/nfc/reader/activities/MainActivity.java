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
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.nfc.reader.R;
import de.nfc.reader.entities.NFCData;
import de.nfc.reader.util.Constant;

public class MainActivity extends AppCompatActivity {

    private static final        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final        SimpleDateFormat dayFormatter = new SimpleDateFormat("dd.MM.yyyy");
    private static final int    REQUEST_EXTERNAL_STORAGE = 1;
    private static final int    BEEP_VOLUME_LEVEL = 100;
    private static final int    BEEP_START_TIME = 200;
    private static String[]     PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String ROOT_DIR_NAME = "/NFC-App";
    private static final String DATA_FILE_NAME = "data.txt";

    // List of NFC technologies avalable for this app:
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

    private Context     cTxt;
    private NfcAdapter  nfcAdapter;
    private TextView    textViewAppVersionNumber;
    private TextView    textViewTagId;
    private TextView    textViewTimetamp;
    private ImageView   imageViewWarning;
    private ImageView   imageViewSuccess;
    private TextView    textViewInfo;
    private long        back_pressed_time;

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
        this.textViewAppVersionNumber.setText("v" + getAppVersionNumber(cTxt));
        this.textViewTagId = (TextView)findViewById(R.id.textViewTagId);
        this.textViewTagId.setVisibility(View.GONE);
        this.textViewTimetamp = (TextView)findViewById(R.id.textViewTimestamp);
        this.textViewTimetamp.setVisibility(View.GONE);
        this.imageViewWarning = (ImageView)findViewById(R.id.imageViewWarning);
        this.imageViewWarning.setVisibility(View.GONE);
        this.imageViewSuccess = (ImageView)findViewById(R.id.imageViewSuccess);
        this.imageViewSuccess.setVisibility(View.GONE);
        this.textViewInfo = (TextView)findViewById(R.id.textViewReport);
        this.textViewInfo.setVisibility(View.GONE);

        // Initialize the NFC adapter for reading the tag UID.
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(this.nfcAdapter == null){
            Toast.makeText(this,
                    "NFC NOT supported on this devices!",
                    Toast.LENGTH_LONG).show();
        }else if(!this.nfcAdapter.isEnabled()){
            Toast.makeText(this,
                    "NFC NOT Enabled!",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Verifiy the write external storage permission.
        verifyStoragePermissions(this);

        // Initialize the NFC adapter for reading the tag UID.
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(this.nfcAdapter == null){
            Toast.makeText(this,
                    "NFC NOT supported on this devices!",
                    Toast.LENGTH_LONG).show();
        }else if(!this.nfcAdapter.isEnabled()){
            Toast.makeText(this,
                    "NFC NOT Enabled!",
                    Toast.LENGTH_LONG).show();
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
    public void onBackPressed() {
        if (back_pressed_time + Constant.TIME_MILIS_PERIOD_BACKPRESSED > System.currentTimeMillis()) {
            closeApp();
        } else {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.text_press_once_again), Toast.LENGTH_SHORT).show();
        }
        back_pressed_time = System.currentTimeMillis();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {

            // Set beep sound if new tag discovered.
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, BEEP_VOLUME_LEVEL);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, BEEP_START_TIME);

            // Retrieve the tag UID from intent, it contains 7 bytes.
            String tagID = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            String strTimestamp = getCurrentTimestamp();

            this.textViewTagId.setVisibility(View.VISIBLE);
            this.textViewTimetamp.setVisibility(View.VISIBLE);

            if(tagID == null){
                this.textViewTagId.setText("Error, no NFC Tag-ID found.");
            }else{
                this.textViewTagId.setText("Tag-ID: " + tagID);
                this.textViewTimetamp.setText("Timestamp: " + strTimestamp);
                storeNFCData(new NFCData(tagID, strTimestamp));
            }
        }
    }

    public void openSetting(View view){
        Toast.makeText(getBaseContext(), "Coming soon", Toast.LENGTH_SHORT).show();
    }

    private String getCurrentTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return dateFormatter.format(timestamp);
    }

    private String getCurrentDate(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return dayFormatter.format(timestamp);
    }

    private void closeApp(){
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private String ByteArrayToHexString(byte[] inByteArray) {
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

        int tappedSum = 1;
        for (NFCData data : storageData) {
            String strDateData = data.getTimestamp();
            String[] parts = strDateData.trim().split(" ");
            String tapDate = parts[0].trim();
            Log.d(Constant.LOGGER, ">>> Current date: " + getCurrentDate());
            Log.d(Constant.LOGGER, ">>> Date data: " + tapDate);
            if(tapDate.equals(getCurrentDate()) && data.getTagId().equals(userId)){
                tappedSum++;
            }
        }

        Log.d(Constant.LOGGER, ">>> Tapped sum for " + userId + " -> #" + tappedSum);
        if(tappedSum > 2){
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
            Toast.makeText(getBaseContext(), "Data.txt was not found!", Toast.LENGTH_SHORT).show();
            return false;
        }else{
            if(isUserAlreadyTappedTwiceInSameDay(file, data.getTagId())){
                this.imageViewWarning.setVisibility(View.VISIBLE);
                this.imageViewSuccess.setVisibility(View.INVISIBLE);
                this.textViewInfo.setVisibility(View.VISIBLE);
                this.textViewInfo.setText("You already tapped twice today!");
            }else{
                this.imageViewWarning.setVisibility(View.INVISIBLE);
                this.imageViewSuccess.setVisibility(View.VISIBLE);
                this.textViewInfo.setVisibility(View.VISIBLE);
                this.textViewInfo.setText("Your presence was recorded successfully!");
            }

            FileOutputStream fos = null;
            OutputStreamWriter outStreamWriter = null;
            try {
                fos = new FileOutputStream(file, true);
                outStreamWriter = new OutputStreamWriter(fos);
                outStreamWriter.append(data.getTagId() + "," + data.getTimestamp()).append("\n");
                outStreamWriter.flush();
                fos.close();
                Log.d(Constant.LOGGER, ">>> Data for tag-ID " + data.getTagId() + " was stored successfully.");
                return true;
            } catch (Throwable throwable) {
                Log.e(Constant.LOGGER, throwable.getLocalizedMessage().toString());
            }

        }

        return false;
    }

    private String getAppVersionNumber(Context cTxt){
        String retVal = null;
        try{
            retVal = cTxt.getPackageManager().getPackageInfo(cTxt.getPackageName(), 0).versionName;
        }catch(Throwable throwable){
            Log.e(Constant.LOGGER, throwable.getLocalizedMessage().toString());
        }
        return retVal;
    }

}
