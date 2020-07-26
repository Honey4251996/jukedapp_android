package audiosdk.test.utilities;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.zip.Deflater;
import java.util.zip.InflaterInputStream;

public class Utils {

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    // Calculate the module of the complex number
    public static double absComplex(double real, double imag) {
        return Math.sqrt(Math.pow(real, 2) + Math.pow(imag, 2));
    }


    public static String intToHexString(int num, int digits) {


        String ret = String.format("%0"+digits+"X", num);

        return ret;
    }

    public static double[] convertShortArrayAsDoubleArray(short[] in, int size) {
        double[] result = new double[size];
        for (int i = 0; i < size; i++) {
            result[i] = in[i] / 32768.0;
        }
        return result;
    }

    public static short[] byteArrayToShortArray(byte[] input) {
        short[] shorts = new short[input.length / 2];
        // to turn bytes to shorts as either big endian or little endian.
        ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }


    public static double[] byteArrayToDouble(byte[] bytes) {
        double[] doubles = new double[bytes.length / 3];
        for (int i = 0, j = 0; i != doubles.length; ++i, j += 3) {
            doubles[i] = (double)( (bytes[j] & 0xff) |
                    ((bytes[j+1] & 0xff) <<  8) |
                    ( bytes[j+2]         << 16));
        }
        return doubles;
    }

    /****************************************/
    /************ STRINGS ****************/
    /****************************************/



    public static String byteArrayToBinaryString(byte[] bytes){
        String ret = "";
        for (byte b : bytes){
            ret+= String.format("%8s",  Integer.toBinaryString(b & 0xFF)).replace(' ','0');
        }
        return ret;
    }


    /****************************************/
    /************ BYTE ARRAY ****************/
    /**
     * ************************************
     */

    public static String byteArrayToString(byte[] input) {

        String output = "";

        for (int i = 0; i < input.length; i++) {
            output += (int) input[i];
        }

        return output;
    }


    public static byte[] compressByteArray(byte[] originalBytes) {

        Deflater deflater = new Deflater();
        deflater.setInput(originalBytes);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!deflater.finished()) {
            int byteCount = deflater.deflate(buf);
            baos.write(buf, 0, byteCount);
        }
        deflater.end();

        byte[] compressedBytes = baos.toByteArray();
        return compressedBytes;
    }

    public static byte[] decompressByteArray(byte[] compressedBytes) throws Exception {

        InflaterInputStream ini = new InflaterInputStream(new ByteArrayInputStream(compressedBytes));
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int b;
        while ((b = ini.read()) != -1) {
            bout.write(b);
        }
        ini.close();
        bout.close();

        byte[] decompressed = bout.toByteArray();

        return decompressed;
    }


    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] shortArrayToByteArray(short[] input, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size * 2);
        for (int i = 0; i < size; i++) {
            buffer.putShort(input[i]);
        }
        return buffer.array();

    }


    /****************************************/
    /************ DATE **********************/
    /**
     * ************************************
     */


    public static Date localDateToGMT(Date dateIn) {
        Calendar c = Calendar.getInstance();
        c.setTime(dateIn);

        TimeZone z = c.getTimeZone();
        int offset = z.getRawOffset();
        if (z.inDaylightTime(new Date())) {
            offset = offset + z.getDSTSavings();
        }
        int offsetHrs = offset / 1000 / 60 / 60;
        int offsetMins = offset / 1000 / 60 % 60;


        c.add(Calendar.HOUR_OF_DAY, (-offsetHrs));
        c.add(Calendar.MINUTE, (-offsetMins));

        return c.getTime();
    }


    public static String concatString(String[] in, String separator) {

        String fileContent = "";
        int iter = 0;
        while (iter < in.length - 1){
            fileContent += in[iter] + separator;
            iter++;
        }

        fileContent += in[iter];

        return fileContent;

    }




    /****************************************/
    /************ FILE **********************/
    /****************************************/


    public static File createByteArrayFile(String fileName, byte[] bytes, File f) throws Exception {
        BufferedOutputStream bos;
        FileOutputStream fos;
        String encodedFileName;


        if (f == null) {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8");
            f = new File(getDefaultStorageFile(null), encodedFileName);
        }

        // create FileOutputStream from filename
        fos = new FileOutputStream(f);

        // create BufferedOutputStream for FileOutputStream
        bos = new BufferedOutputStream(fos);

        bos.write(bytes);

        bos.flush();
        bos.close();

        return f;

    }

    public static File createListByteArrayAsStringFile(String fileName, List<byte[]> bytes, File f) throws Exception {

        FileOutputStream fos = null;
        PrintWriter fOutputCompressedData = null;
        String encodedFileName;


        if (f == null) {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8");
            f = new File(getDefaultStorageFile(null), encodedFileName);
        }

        fOutputCompressedData = new PrintWriter(f);
        for (int i = 0; i < bytes.size(); i++) {
            fOutputCompressedData.write(byteArrayToBinaryString(bytes.get(i)));
            fOutputCompressedData.println();
        }
        fOutputCompressedData.close();


        return f;

    }


    public static File createStringFile(String fileName, String data, File f) throws Exception {

        String encodedFileName;
        if (f == null) {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8");
            f = new File(getDefaultStorageFile(null), encodedFileName);
        }

        PrintWriter out = new PrintWriter(new FileWriter(f));

        out.println(data);


        out.close();


        return f;

    }


    public static File createStringArrayFile(String fileName, String[] data, File f) throws Exception {

        String encodedFileName;
        if (f == null) {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8");
            f = new File(getDefaultStorageFile(null), encodedFileName);
        }

        PrintWriter out = new PrintWriter(new FileWriter(f));

        // Write each string in the array on a separate line
        for (String s : data) {
            out.println(s);
        }

        out.close();


        return f;

    }

    public static boolean deleteFile(String path){
        File file = new File(path);
        boolean deleted = file.delete();
        return deleted;
    }

    public static File getDefaultStorageFile(Context ctx){
        File f;
        String externalStorageState = Environment.getExternalStorageState();
        if (externalStorageState.compareTo(Environment.MEDIA_MOUNTED)==0) {
            f = Environment.getExternalStorageDirectory();
        }
        else{
            f = Environment.getDataDirectory();

        }

        File folder = new File(f.getPath() + "/AudioSDK");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
            if (success) {
                Log.d("AUDIO_SDK","Folder created");
            }
            else{
                Log.d("AUDIO_SDK","Couldn't create app folder");
            }
        }
        else{
            Log.d("AUDIO_SDK","App folder already created");
        }


        if (ctx != null) {
            // initiate media scan and put the new things into the path array to
            // make the scanner aware of the location and the files you want to see
            MediaScannerConnection.scanFile(ctx, new String[]{folder.getAbsolutePath()}, null, null);
        }
        return folder;
    }

    /****************************************/
    /************ PROPERTIES **********************/
    /****************************************/

    public static Properties loadProperties(Context ctx) {
        // Read from the /res/raw directory
        Resources resources = ctx.getResources();
        AssetManager assetManager = resources.getAssets();
        Properties properties = new Properties();

// Read from the /assets directory
        try {
            InputStream inputStream = assetManager.open("app.properties");
            Reader reader = new InputStreamReader(inputStream, "UTF-8");
            try {
                properties.load(reader);
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to open microlog property file");
            e.printStackTrace();
        }
        return properties;
    }

    /****************************************/
    /********* INTERNET CONNECTION ***********/
    /****************************************/

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static boolean hasActiveInternetConnection(Context context) {
        if (isNetworkAvailable(context)) {
            try {
                HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                urlc.setRequestProperty("User-Agent", "Test");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return (urlc.getResponseCode() == 200);
            } catch (IOException e) {
                Log.e("AUDIO_SDK", "Error checking internet connection", e);
            }
        } else {
            Log.d("AUDIO_SDK", "No network available!");
        }
        return false;
    }
}
