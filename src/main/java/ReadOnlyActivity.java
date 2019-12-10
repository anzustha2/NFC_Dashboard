package app.nfc_test;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ReadOnlyActivity extends AppCompatActivity{

        NfcAdapter nfcAdapter;
        PendingIntent pendingIntent;
        IntentFilter writeTagFilters[];
        boolean writeMode;
        Tag myTag;
        EditText text;
        /*
        The onCreate method is called when Activity class is first created. savedInstanceState is a reference to
        a Bundle object that is passed into the onCreate method of every Android Activity.
        */
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_hashvalue);
            getSupportActionBar().setTitle("NFC Dashboard");
            text = findViewById(R.id.text);
            Button doneBtn = findViewById(R.id.save);
            /*
            When we click the URL button then do something or go to page WriteUrlActivity Class
            */
            doneBtn.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        //stop here if no NFC tag is detected.
                        if (myTag == null) {
                            Toast.makeText(app.nfc_test.ReadOnlyActivity.this, "No NFC tag detected!", Toast.LENGTH_LONG).show();
                        } else {
                            write(text.getText().toString() + "#" + hashResult(), myTag);
                            Toast.makeText(app.nfc_test.ReadOnlyActivity.this, "Text written to the NFC tag successfully!", Toast.LENGTH_LONG ).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(app.nfc_test.ReadOnlyActivity.this, "Error during writing, is the NFC tag close enough to your device?", Toast.LENGTH_LONG ).show();
                        e.printStackTrace();
                    }
                }
            });

            nfcAdapter = NfcAdapter.getDefaultAdapter(this);
            //Helper to get the default NFC Adapter.
            if (nfcAdapter == null) {
                Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
                finish();
            }
            /*In order to implement the foreground dispatch system, you first need to create a PendingIntent
             so that Android can get the details of the tag
             */
            pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
            //If the tag is detected, then we make it default
            writeTagFilters = new IntentFilter[] { tagDetected };

        }
        /*
        Creates the hash value of the input string using the algorithm SHA-256
         */
        public String hashResult() throws NoSuchAlgorithmException, UnsupportedEncodingException
        {
            EditText text = (EditText) findViewById(R.id.text);
            byte[] input = text.getText().toString().getBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.reset();
            digest.update(input);
            byte[] output = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for(int i = 0; i < output.length; i++){
                hexString.append(Integer.toHexString(0xFF & output[i]));
            }
            return hexString.toString();
        }
        /*
        Writes to the NFC tag creating NDEF record.
         */
        private void write(String text, Tag tag) throws IOException, FormatException {

            NdefRecord[] records = { createRecord(text) };
            NdefMessage message = new NdefMessage(records);
            // Get an instance of Ndef for the tag.
            Ndef ndef = Ndef.get(tag);
            // Enable I/O
            ndef.connect();
            // Write the message
            ndef.writeNdefMessage(message);
            // Close the connection
            ndef.close();
            sendNotification("fo", "fi", "bo", "done writing operation !", "done writing the info to the tag");

        }
        /*
       Creates the Ndef record depending on the type of record(text, url, smartPoster etc)
        */
        private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
            String lang       = "en";
            byte[] textBytes  = text.getBytes();
            byte[] langBytes  = lang.getBytes("US-ASCII");
            int    langLength = langBytes.length;
            int    textLength = textBytes.length;
            byte[] payload    = new byte[1 + langLength + textLength];
            // set status byte (see NDEF spec for actual bits)
            payload[0] = (byte) langLength;
            // copy langbytes and textbytes into payload
            System.arraycopy(langBytes, 0, payload, 1,              langLength);
            System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

            NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

            return recordNFC;
        }

        public void sendNotification(String bigTextx, String setBigContentTitle, String SummaryText, String ContentTitle, String ContentText){
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this.getApplicationContext(), "notify_001");
            Intent ii = new Intent(this.getApplicationContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, ii, 0);
            NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
            bigText.bigText(bigTextx);
            bigText.setBigContentTitle(setBigContentTitle);
            bigText.setSummaryText(SummaryText);
            mBuilder.setContentIntent(pendingIntent);
            mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
            mBuilder.setContentTitle(ContentTitle);
            mBuilder.setContentText(ContentText);
            mBuilder.setPriority(Notification.PRIORITY_MAX);
            mBuilder.setStyle(bigText);
            NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("notify_001", "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT);
                mNotificationManager.createNotificationChannel(channel);
            }
            mNotificationManager.notify(0, mBuilder.build());
        }

        /*
         * First, the intent is checked if it was launched from an NFC interaction of TAG_DISCOVERED.
         * The getParcelableExtra method is used to retrieve the extended data from the intent.
         * Moreover, NfcAdapter.EXTRA_TAG is used as a parameter to get the tag information.
         */
        @Override
        protected void onNewIntent(Intent intent) {
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
                myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            }
        }
        /*
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */

        @Override
        public void onPause(){
            super.onPause();
            WriteModeOff();
        }
        //called when the activity will start interacting with the user.
        /*
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        @Override
        public void onResume(){
            super.onResume();
            WriteModeOn();
        }

        /******************************************************************************
         **********************************Enable Write********************************
         ******************************************************************************/
        private void WriteModeOn(){
            writeMode = true;
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
        }

        /******************************************************************************
         **********************************Disable Write*******************************
         ******************************************************************************/
        private void WriteModeOff(){
            writeMode = false;
            nfcAdapter.disableForegroundDispatch(this);
        }
    }


