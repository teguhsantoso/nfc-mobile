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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import de.nfc.reader.R;
import de.nfc.reader.entities.NFCData;
import de.nfc.reader.util.Constant;

public class MainActivity extends AppCompatActivity {

    private static final        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final int    REQUEST_EXTERNAL_STORAGE = 1;
    private static String[]     PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

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
    private String      dataFileName;
    private TextView    textViewTagId;
    private TextView    textViewTimstamp;
    private long        back_pressed_time;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the current context on this activity.
        cTxt = this;

        // Initialize all UI elements.
        this.textViewTagId = (TextView)findViewById(R.id.textViewTagId);
        this.textViewTagId.setVisibility(View.GONE);
        this.textViewTimstamp = (TextView)findViewById(R.id.textViewTimestamp);
        this.textViewTimstamp.setVisibility(View.GONE);

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
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);

            // Retrieve the tag UID from intent.
            String tagID = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            String strTimestamp = getCurrentTimestamp();

            this.textViewTagId.setVisibility(View.VISIBLE);
            this.textViewTimstamp.setVisibility(View.VISIBLE);

            if(tagID == null){
                this.textViewTagId.setText("Error, no NFC Tag-ID found.");
            }else{
                this.textViewTagId.setText("Tag-ID: " + tagID);
                this.textViewTimstamp.setText("Timestamp: " + strTimestamp);
                storeNFCData(new NFCData(tagID, strTimestamp));
            }
        }
    }

    private String getCurrentTimestamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return dateFormatter.format(timestamp);
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

    private boolean storeNFCData(NFCData data){
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + "/NFC-App");
        if(!dir.exists()){
            dir.mkdirs();
        }

        File file = new File(dir, "data.txt");
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileOutputStream fos = null;
        OutputStreamWriter outStreamWriter = null;
        try {
            fos = new FileOutputStream(file, true);
            outStreamWriter = new OutputStreamWriter(fos);
            outStreamWriter.append("Tag-ID: " + data.getTagId() + ", " + "Timestamp: " + data.getTimestamp()).append("\n");
            outStreamWriter.flush();
            fos.close();
            Log.d(Constant.LOGGER, ">>> Write data for tag-ID: " + data.getTagId());
            return true;
        } catch (Throwable throwable) {
            Log.e(Constant.LOGGER, throwable.getLocalizedMessage().toString());
        }
        return false;
    }

}
