package com.example.nfcdog;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static NfcAdapter mNfcAdapter;
    static MifareClassic currentMF1 = null;

    Button readBtn;
    Button writeBtn;
    EditText editText;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        readBtn = findViewById(R.id.readm1);
        writeBtn = findViewById(R.id.writem1);
        editText = findViewById(R.id.context);
        textView = findViewById(R.id.textView);

        boolean isNFC = initNFC();

        if (!isNFC) {
            readBtn.setEnabled(false);
            writeBtn.setEnabled(false);
            editText.setEnabled(false);
        }


        readBtn.setOnClickListener(v -> {
            String sector1block0 = log(readMF1(1, 0, MifareClassic.KEY_DEFAULT));
            textView.setText(sector1block0);
        });


        writeBtn.setOnClickListener(v -> {

            byte[] test = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
            writeMF1(1, 0, MifareClassic.KEY_DEFAULT, test);
//            textView.setText(editText.getText());
        });


    }

    boolean initNFC() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "对不起，您的设备不支持NFC，无法正常使用本软件",
                    Toast.LENGTH_LONG).show();
        } else if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "请到系统设置，开启NFC功能", Toast.LENGTH_LONG).show();
        } else {
            return true;
        }

        return false;
    }

    private boolean writeMF1(int sectorIndex, int blockIndex, byte[] key, byte[] data) {
        MifareClassic mfc = currentMF1;
        try {
            mfc.connect();
            boolean isOpen = mfc.authenticateSectorWithKeyA(sectorIndex, key);
            if (isOpen) {
                mfc.writeBlock(mfc.sectorToBlock(sectorIndex) + blockIndex, data);
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                mfc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private byte[] readMF1(int sectorIndex, int blockIndex, byte[] key) {
        MifareClassic mfc = currentMF1;
        try {
            mfc.connect();
            boolean isOpen = mfc.authenticateSectorWithKeyA(sectorIndex, key);
            log("is OPen" + isOpen);
            if (isOpen) {
                return mfc.readBlock(mfc.sectorToBlock(sectorIndex) + blockIndex);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                mfc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            String action = intent.getAction();

            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                    || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                    || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

                Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);


                byte[] MF1_UID = tagFromIntent.getId();

                textView.setText(log(MF1_UID));

                MifareClassic mfc = MifareClassic.get(tagFromIntent);

                if (mfc == null) return;

                currentMF1 = mfc;

                mfc.connect();
                boolean isWhite = mfc.authenticateSectorWithKeyA(2, MifareClassic.KEY_DEFAULT);
                int blockCount = mfc.getBlockCount();
                int sectorCount = mfc.getSectorCount();

                HashMap map = new HashMap();
                map.put("uid", MF1_UID);
                map.put("isWhite", isWhite);
                map.put("sectorCount", sectorCount);
                map.put("unitBlockCount", blockCount / sectorCount);


                mfc.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void log(String str) {
        Log.e("nfcdog", str);
    }

    private static String log(byte[] bytes) {
        String str = bytesToHexString(bytes);
        log(str);
        return str;
    }

    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }
}