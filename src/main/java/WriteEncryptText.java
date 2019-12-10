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
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class WriteEncryptText extends AppCompatActivity {

    //NFC related variables
    NfcAdapter nfcAdapter;
    /*
    A PendingIntent is a token that you give to a foreign application which allows the foreign application
    to use your application's permissions to execute a predefined piece of code.
    */
    PendingIntent pendingIntent;
    /*
  Intent filters handles the intents that we want to intercept. By declaring an intent filter for an activity,
   we make it possible for other apps to directly start our activity with a certain kind of intent.
    */
    IntentFilter writeTagFilters[];
    boolean writeMode;
    /*
    NFC tag to write the information
     */
    Tag myTag;

    /*
     The onCreate method is called when Activity class is first created. savedInstanceState is a reference to
     a Bundle object that is passed into the onCreate method of every Android Activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        By calling super.onCreate(savedInstanceState) you tell to run your code in addition to the existing code
        in the onCreate() of the parent class.
         */
        super.onCreate(savedInstanceState);
        /*
        The setContentView(R.layout.activity_main) gives information about our layout resource. Here, our layout
        resources are defined in activity_main.xml file. The letter R in the round brackets is short for res.
         */
        setContentView(R.layout.activity_write);
        getSupportActionBar().setTitle("NFC Dashboard");
        /*
        urlBtn is a button which is found by its resource id urlBtn.
         */
        Button urlBtn = findViewById(R.id.urlBtn) ;
        /*
        When we click the URL button then do something or go to page WriteUrlActivity Class
         */
        urlBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    //stop here if no NFC tag is detected.
                    if (myTag == null) {
                        Toast.makeText(WriteEncryptText.this, "No NFC tag detected!", Toast.LENGTH_LONG).show();
                    } else {
                       write(encryptMessage(), myTag);
                        Toast.makeText(WriteEncryptText.this, "Text written to the NFC tag successfully!", Toast.LENGTH_LONG ).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(WriteEncryptText.this, "Error during writing, is the NFC tag close enough to your device?", Toast.LENGTH_LONG ).show();
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
        /*
        Handles foreground NFC scanning in this activity by creating a PendingIntent with FLAG_ACTIVITY_SINGLE_TOP flag
        so each new scan is not added to the Back Stack
         */
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        /*
        Create intent filter to handle NFC tags detected from inside our application when in "read mode"
         */
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };
    }
    /*
       Creates the hash value of the input string using the algorithm SHA-256
        */
    public String hashResult() throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        byte[] input = combination().getBytes();
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
    combines all the input EditText value
     */
    public String combination()
    {
        EditText name =  findViewById(R.id.name);
        EditText phone = findViewById(R.id.phone);
        EditText email =  findViewById(R.id.email);
        EditText web = findViewById(R.id.web);
        EditText description = findViewById(R.id.description);
        String combination = "\n\n***************" +  "\n\nName: " + name.getText().toString() + " ; "  + "\n\nPhone: "  + phone.getText().toString() + "; "  + "\n\nURL: " +  web.getText().toString() +  " ; "  + "\n\nEmail: " +  email.getText().toString() + " ; " + "\n\nDescription/Comments: " +  description.getText().toString() + "\n\n***************";
        return combination;
    }
    /*
    concatenates the input text and the hash value of that text separated by "#" sign
     */
    public String concatenate()
    {
        try {
            //EditText text = findViewById(R.id.text);
            //String inputText = text.getText().toString();
            return combination() + "#" + hashResult();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /*
    Read the file from the external storage
     */
    public String read_file(Context context, String filename) {
        try {
            File myDir = new File(getFilesDir().getAbsolutePath());
            String s = "";
            //FileWriter fw = new FileWriter(myDir + "/hello.txt");
            //fw.write("1111111111111111");
            //fw.close();
            //Toast.makeText(this, "saved to " + getFilesDir(), Toast.LENGTH_LONG).show();
            BufferedReader br = new BufferedReader(new FileReader(myDir + "/hello.txt"));
            s = br.readLine();
            return s;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "FileNotFound";
        } catch (IOException e) {
            e.printStackTrace();
            return "IOException";
        }
    }
    /*
    converts byte to hexadecimal value
     */
    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for(int i = 0; i <b.length; i++) {
            stmp = Integer.toHexString(b[i] & 0xFF);
            if (stmp.length() == 1) {
                hs += ("0" + stmp);
            }
            else {
                hs += stmp;
            }
        }
        return hs.toUpperCase();
    }
    /*
    encryptMessage() encrypts the string using "AES" algorithm and returns back the encrypted
    string that is generated using the input string and the public key. public key should be
    16 bytes of character.
     */
    public  String encryptMessage() throws Exception{
        try{
            String msgContentString = concatenate();
            Context context = getApplicationContext();
            String fileName = "file.txt";
            String secretKeyString= read_file(context, fileName);
            byte[] returnArray;

            // generate AES secret key from user input
            Key keys = generateKey(secretKeyString);

            // specify the cipher algorithm using AES
            Cipher c = Cipher.getInstance("AES");

            // specify encryption mode
            c.init(Cipher.ENCRYPT_MODE, keys);

            // encrypt
            returnArray = c.doFinal(msgContentString.getBytes());
            String encryptedString = byte2hex(returnArray);

            return encryptedString;

        } catch (Exception e) {
            System.out.println("Wrong key");
            String message = "error while decrypting" + e.toString();
            return message;
        }
    }
    /*
    Generate the key from String using "AES" algorithm
     */
    private  Key generateKey(String secretKeyString) throws NoSuchAlgorithmException, InvalidKeyException {
        // generate secret key from string
        Key keys = new SecretKeySpec(secretKeyString.getBytes(), "AES");
        return keys;
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

    /*
        First, the intent is checked if it was launched from an NFC interaction of TAG_DISCOVERED.
        The getParcelableExtra method is used to retrieve the extended data from the intent.
        Moreover, NfcAdapter.EXTRA_TAG is used as a parameter to get the tag information.
     */

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }
    /*
     * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
     * we need to disable foreground dispatch as soon as the app gets in the background
     * and the best place to do it is in onPause method
     */

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
    }

    /*
     * called when the activity will start interacting with the user.
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
    /*
    enables the foreground dispatch once the app is opened.
     */
    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    /******************************************************************************
     **********************************Disable Write*******************************
     ******************************************************************************/
    /*
    disables the foreground dispatch once the app gets in the background
     */
    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
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
}

