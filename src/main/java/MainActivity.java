package app.nfc_test;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/*
Activity is a java class that creates default window on the screen where we can place different
 components such as Button, EditText, TextView, Spinner etc.
It provides life cycle methods for activity such as onCreate, onStop, OnResume etc.
 */
public class MainActivity extends Activity
{
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
    //NFC variables
    boolean writeMode;
    /*
    NFC tag to read or write information
     */
    Tag myTag;
    TextView tvNFCContent;
    Button btnWrite;
     /*
        The onCreate method is called when Activity class is first created. savedInstanceState is a reference to
        a Bundle object that is passed into the onCreate method of every Android Activity.
        */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*
        By calling super.onCreate(savedInstanceState) you tell to run your code in addition to the existing code
        in the onCreate() of the parent class.
         */
        super.onCreate(savedInstanceState);
          /*
        The setContentView(R.layout.activity_main) gives information about our layout resource. Here, our layout
        resources are defined in activity_main.xml file. The letter R in the round brackets is short for res.
         */
        setContentView(R.layout.activity_main);
        /*
        tvNFCContent is a textView which is found by its resource id nfc_contents
         */
        tvNFCContent = findViewById(R.id.nfc_contents);
        /*
        btnWrite is a button which is found by its resource id button.
         */
        btnWrite = findViewById(R.id.button);
        /*
        When we click the write button then do something or go to page WriteActivity Class
         */
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, WriteEncryptText.class));

            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        //Helper to get the default NFC Adapter.
        /*
        if Nfc Adapter is not found, display this device does not support NFC
         */
        if (nfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }

        readFromIntent(getIntent());
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
        writeTagFilters = new IntentFilter[]{tagDetected};
    }

    /******************************************************************************
     **********************************Read From NFC Tag***************************
     ******************************************************************************/
    /*
    readFromIntent is a method that takes Intent as a parameter and it does not return anything.
     The getParcelableExtra method is used to retrieve the extended data from the intent. EXTRA_NDEF_MESSAGES to get the raw
     data in parcels. assuming the raw data is not null, you can create an NdefMessage object by iterating over the raw Parcelable object.
     */
    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        /*
        Checks whether the action is one of the intent filter.
         */
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }

            }
            //set up the views
           buildTagViewsUsingSecretKey(msgs);
        }
    }
    /******************************************************************************
     **********************************Read Text with hashCode***************************
     ******************************************************************************/
    //Parsing the NFC tag and figuring out the MIME type or a URI that identifies the data payload in the tag.
    private void buildTagwithHashValue(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;
        String text = "";
        String tagId = new String(msgs[0].getRecords()[0].getType());
        final byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        String languageCode = "";
        try {
            languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1,  textEncoding);

        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());

        }
        /*
        Sets the text written to the textView tvNFCContent
         */
        tvNFCContent.setText("NFC Content: " + text);

        try {
            if (compareresult(text)) {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
                AlertDialog.Builder adb = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);

                adb.setTitle("Reading the Content of the nearby NFC tag !");

                adb.setMessage("NFC Content: " + tagId + " --- " + text + " --- " + languageCode + " - which is Webpage text!");

                adb.setIcon(android.R.drawable.ic_dialog_info);
                adb.setPositiveButton("View the Page", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ////do something
                    }
                });
                adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //finish();
                    }
                });
                adb.show();
            } else
                {
                Toast.makeText(MainActivity.this, "Cannot read, Somebody might have changed the content", Toast.LENGTH_LONG).show();
                AlertDialog.Builder adb = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);

                adb.setTitle("Reading the Content of the nearby NFC tag !");

                adb.setMessage("NFC Content: Content is not same, somebody has changed it");

                adb.show();
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }
    /*
      Compares the result from the text file and if it is the same returns true else returns false
     */
        public boolean compareresult(String text) throws  NoSuchAlgorithmException {
            String[] arrOfStr = text.split("#", 2);
            String first, second;
            String str = "";
            first = arrOfStr[0];
            byte[] input = first.getBytes();
            MessageDigest md1 = MessageDigest.getInstance("SHA-256");
            md1.reset();
            md1.update(input);
            byte[] output = md1.digest();
            StringBuffer hexString1 = new StringBuffer();

            for (int i = 0; i < output.length; i++)
                hexString1.append(Integer.toHexString(0xFF & output[i]));

            String compare = "Hello";
            byte[] input1 = compare.getBytes();
            MessageDigest md2 = MessageDigest.getInstance("SHA-256");
            md2.reset();
            md2.update(input1);
            byte[] output1 = md2.digest();
            StringBuffer hexString2 = new StringBuffer();

            for (int i = 0; i < output1.length; i++)
                hexString2.append(Integer.toHexString(0xFF & output1[i]));

            boolean bln = hexString1.toString().equals(hexString2.toString());
            return bln;
        }

    /******************************************************************************
     **********************************Read Text with secret Key***************************
     ******************************************************************************/

    /*
    convert the hexadecimal value to byte.
     */
    public static byte[] hex2byte(byte[] value) {
        if ((value.length % 2) != 0)
            throw new IllegalArgumentException("hello");
        byte[] value2 = new byte[value.length / 2];
        for (int n = 0; n < value.length; n += 2) {
            String item = new String(value, n, 2);
            value2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        return value2;
    }
    /*
    Convert hexadecimal value to byte format
     */
    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int i = 0; i < b.length; i++) {
            stmp = Integer.toHexString(b[i] & 0xFF);
            if (stmp.length() == 1) {
                hs += ("0" + stmp);
            } else {
                hs += stmp;
            }
        }
        return hs.toUpperCase();
    }
    /*
    Generate the key from String using "AES" algorithm
     */
    private static Key generateKey(String secretKeyString) throws Exception {
        // generate secret key from string
        Key key = new SecretKeySpec(secretKeyString.getBytes(), "AES");
        return key;
    }
    /*
    Parsing the NFC tag and figuring out the MIME type or a URI that identifies the data payload in the tag.
     */
    String text;
    private void buildTagViewsUsingSecretKey(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;
        text = "";
        String tagId = new String(msgs[0].getRecords()[0].getType());
        final byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);

        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        //tvNFCContent.setText("NFC Content: " + text);
        String first = "", second;
        AlertDialog.Builder adb = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
        try {
            String secretKey = read_file();
            byte[] message = hex2byte(text.getBytes());
            byte[] result = decryptMessage(secretKey, message);
            String decryptedMessage = new String(result);
            String[] arrOfStr = decryptedMessage.split("#", 2);
            String str = "";
            first = arrOfStr[0];
            second = arrOfStr[1];
            adb = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
            adb.setMessage("NFC Content: " + tagId + " --- " + decryptedMessage + " --- ");

            if (compareresultWithEncryptedFile(decryptedMessage)) {
                //Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
               adb = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
               adb.setTitle("Reading the Content of the nearby NFC tag !");
               //tvNFCContent.setText("NFC Content: " + first);
               adb.setMessage("NFC Content: " + tagId + " --- " +  first + " --- ");
               tvNFCContent.setText("NFC Content: " + first);
                adb.setIcon(android.R.drawable.ic_dialog_info);

                try {
                    List<String> list = extractUrls(first);
                    String url = list.get(0);
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("" +  url)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    System.out.println("");
                }

            } else if(!compareresultWithEncryptedFile(decryptedMessage)){
                //Toast.makeText(MainActivity.this, second, Toast.LENGTH_LONG).show();
               adb = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
               adb.setTitle("Reading the Content of the nearby NFC tag !");
               adb.setMessage("NFC Content: " + tagId + " --- " + text + " --- ");

            }
            else{
                adb.setMessage("Somebody has overwritten the information, please make sure data is written securely using this application");
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e("No such Algorithm", e.toString());
        }
        catch (Exception e){
            System.out.println("cannot decrypt");
        }
        final String url = first;
        adb.setPositiveButton("View the Page", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                try {
                    List<String> listOfUrl = extractUrls(url);
                    String firstUrl = listOfUrl.get(0);
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("" + firstUrl)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    System.out.println("");
                }

            }

        });
        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //finish();
            }
        });
        adb.show();
    }

    /*
   Decrypt the messages using the public key that is used to encrypt the text.
    */
    public static byte[] decryptMessage(String secretKeyString, byte[] encryptedMsg)  {
        // generate AES secret key from the user input string
        try {
            Key key = generateKey(secretKeyString);

            // get the cipher algorithm for AES
            Cipher c = Cipher.getInstance("AES");
            // specify the decryption mode
            c.init(Cipher.DECRYPT_MODE, key);
            // decrypt the message
            byte[] decryptValue = c.doFinal(encryptedMsg);
            return decryptValue;
        } catch (Exception e) {
            e.printStackTrace();
            byte[] de = null;
            return de;
        }

    }
    /*
    Reading the file from the phone.
     */
    public String read_file() {
        try {
            File myDir = new File(getFilesDir().getAbsolutePath());
            String key = "";
            BufferedReader br = new BufferedReader(new FileReader(myDir + "/hello.txt"));
            key = br.readLine();
            return key;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "FileNotFound";
        } catch (IOException e) {
            e.printStackTrace();
            return "IOException";
        }
    }

    /*
    Compares the result from the text file and if it is the same returns true or returns false
     */
    public boolean compareresultWithEncryptedFile(String text) throws NoSuchAlgorithmException {

        String[] arrOfStr = text.split("#", 2);
        String first, second;
        first = arrOfStr[0];
        byte[] input = first.getBytes();
        second = arrOfStr[1];
        byte[] output1 = second.getBytes();

        MessageDigest md1 = MessageDigest.getInstance("SHA-256");
        md1.reset();
        md1.update(input);

        byte[] output = md1.digest();

        StringBuffer hexString1 = new StringBuffer();

        for (int i = 0; i < output.length; i++)

            hexString1.append(Integer.toHexString(0xFF & output[i]));
        boolean bln = hexString1.toString().equals(second);
        return bln;

    }

    /*
   extractUrls method extracts the url from the text information.
   It takes the String as a parameter that can contain string with text and url.
   It will find the URL from that text information and returns back only URL.
    */
    public List<String> extractUrls(String input) {

        List<String> result = new ArrayList<String>();
        String[] words = input.split("\\s+");
        Pattern pattern = Patterns.WEB_URL;
        for (String word : words) {
            if (pattern.matcher(word).find()) {
                if (!word.toLowerCase().contains("http://") && !word.toLowerCase().contains("https://")) {
                    word = "http://" + word;
                    tvNFCContent.setLinksClickable(true);
                    Linkify.addLinks(tvNFCContent, Linkify.WEB_URLS);
                    //tvNFCContent.setLinksClickable(true);
                    //Linkify.addLinks(tvNFCContent, Linkify.PHONE_NUMBERS);
                }
                result.add(word + "\n");
            }
        }
        StringBuilder sb = new StringBuilder();
        for(String ch : result){
            sb.append(ch);

        }
        String string = sb.toString();

        return result;
    }

     /*
        First, the intent is checked if it was launched from an NFC interaction of TAG_DISCOVERED.
        The getParcelableExtra method is used to retrieve the extended data from the intent.
        Moreover, NfcAdapter.EXTRA_TAG is used as a parameter to get the tag information.
     */

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
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
     * called when the activity will start interacting with the user
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
    enables the write mode
     */
    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    /******************************************************************************
     **********************************Disable Write*******************************
     ******************************************************************************/
    /*
    disables the write mode
     */
    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }


}