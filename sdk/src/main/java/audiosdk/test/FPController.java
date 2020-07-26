package audiosdk.test;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import audiosdk.test.communication.ServerComm;
import audiosdk.test.interfaces.InfoReceivedListener;
import audiosdk.test.utilities.Utils;


public class FPController {

    /**
     * ********************************CONSTANTS***************************************
     */
    private static final int MAX_ON_DURATION = 60;
    private static final int MIN_OFF_DURATION = 0;
    private static final int MAX_OFF_DURATION = 60;
    private static final int ONE_SECOND_IN_MILLISECOND = 1000;
    private static final int FP_PACKAGE_SIZE = 32;
    private static final int FP_BYTES_SIZE = 10;
    private static final int BYTES_TO_COMPRESS = FP_PACKAGE_SIZE * FP_BYTES_SIZE;
    private static final int SIXTY_SECONDS = 60;
    private static final int SIXTY_MINUTES = 60;
    private static final int MIN_HOUR_VALUE = 1;
    private static final int MAX_HOUR_VALUE = 24;
    private static final int MIN_MINUTE_VALUE = 1;
    private static final int MAX_MINUTE_VALUE = 24;


    /**
     * ********************************INNER STRUCTURES***************************************
     */
    public enum MatchRate {
        ONE_PER_SECOND,
        TWO_PER_SECOND,
        FOUR_PER_SECOND,
        EIGHT_PER_SECOND,
        SIXTEEN_PER_SECOND,
        THIRTY_TWO_PER_SECOND
    }

    public enum UploadInterval {
        IMMEDIATELY,
        HOUR_VALUE,
        MINUTE_VALUE
    }



    public class TimerTaskNotificationResponse extends TimerTask {

        @Override
        public void run() {
            try {
                instance.getServerResponse();
            } catch (Exception e) {
                errorReceived(e);
                e.printStackTrace();
            }
        }
    }

    public class TimerTaskOffDuration extends TimerTask {

        @Override
        public void run() {
            try {
                instance.start();
            } catch (Exception e) {
                errorReceived(e);
                e.printStackTrace();
            }
        }
    }

    public class TimerTaskUploadInterval extends TimerTask {

        @Override
        public void run() {
            // check that i have 32 FP to create a packet to send the server

            if (mReadyFP.size() > 0) {
                sendBundle();
            }
        }

    }


    /**
     * ********************************FIELDS***************************************
     */
    private static FPController instance = null;

    private MatchRate mMatchRate;

    private int mOnDuration;

    private int mOffDuration;

    private boolean mRepeatMode;

    private UploadInterval mUploadInterval;

    private int mUploadIntervalMinuteValue;

    private int mUploadIntervalHourValue;

    private int mNotificationResponse;

    private int mAudioSource;

    private ServerComm mServerComm;

    private AudioRecord mRecorder = null;

    private boolean mIsRecording = false;

    private int mRecorderSampleRate;

    private int mRecorderChannels = AudioFormat.CHANNEL_IN_MONO;

    private int mRecorderAudioEncoding;

    private int mFFTInputSizeInBytes; // want to play 2048 (2K) since 2 bytes we use only 1024

    private int mFFTPoints = 8192;
    //
    // 128 kbps = 16KBs = frecuency * encoding
    //
    private int mAudioSizePerSecond;

    // timer to control the off duration elapsed time
    private Timer mTimerOffDuration;

    // task to execute when the time is elapsed
    private TimerTaskOffDuration mTimerTaskOffDuration;

    // timer to control the upload interval elapsed time
    private Timer mTimerUploadInterval;

    // task to execute when the time is elapsed
    private TimerTaskUploadInterval mTimerTaskUploadInterval;

    // task to execute when the time is elapsed
    private TimerTaskNotificationResponse mTimerTaskNotificationResponseInterval;

    // timer to control the notificationResponse elapsed time
    private Timer mTimerNotificationResponseInterval;

    // all the FP saved to do the UPLOAD
    private List<Pair<String, String[]>> mReadyFP;

    //listeners
    private List<InfoReceivedListener> listeners;

    private boolean isDebugging;

    private boolean mStopped;

    private Context mContext;

    private CircularByteBuffer mCircularByteBuffer;

    /*
    Only to debbug. Printed on file's name
     */
    private int mNumberInFile;

    /*
        Use to save the previous data. USe to follow the read pattern (1-4000), (2001 a 6000), (4001, 8000)
     */
    private byte [] mBufferAux;

    private boolean generateFFTFiles;

    private int mTotalBytesToRecord;

    /**
     * offset There are 8000 samples in 1 second since its sampled @ 8 kHz.
     * So in the scenario where we have an interval of 4/sec we would take a fingerprint every 8000/4 = 2000 samples.
     * This is the start point of your fingerprint and NOT the amount of audio you use for the FFT.
     * So if you had 24000 samples you would have a start points at 1 2001,4001,6001 ...... 22001.
     */
    private int mOffsetForFFT;


    byte[] audioOfTotalBytesToRecord;
    byte[] audioOfTotalBytesToRecordRead;

    /**
     * ********************************CONSTRUCTORS***************************************
     */
    private FPController(Context context) {
        listeners = new ArrayList<InfoReceivedListener>();
        this.mServerComm = new ServerComm(context);
        this.mIsRecording = false;
        this.mReadyFP = new ArrayList<Pair<String, String[]>>();
        this.mContext = context;
        this.mNumberInFile = 0;
        this.mBufferAux = new byte[1]; // only for initialize, the real size is initialized in the initialized function

        Properties properties = Utils.loadProperties(context);
        isDebugging = Boolean.parseBoolean(properties.getProperty("isDebugging"));
        generateFFTFiles = Boolean.parseBoolean(properties.getProperty("generateFFTFiles"));



    }

    /**
     * @return
     */
    public static FPController getInstance(Context ctx) {
        if (instance == null) {
            instance = new FPController(ctx);
        }
        return instance;
    }


    /***********************************PUBLIC METHODS****************************************/
    /**
     * @param matchRate             - This dictates how many FP's we take per second. Our system works in packets of 32 FP's.
     *                              So if you want to match audio in 2 seconds we would need the FP rate to be 16/sec and if you
     *                              want to match in 8 seconds you would set it to 4/sec.
     *                              We would have a max of 16/sec and a min of 1/sec
     * @param onDuration            - This is how long they want to run the FP'ing for. 2 seconds, 4 seconds, 8 seconds, etc.
     *                              It can also be continuously meaning full time on mode.
     *                              Another example would be to continuously tag music in the BG while the app is running.
     * @param offDuration           - This is how long to stop fingerprinting for. An example is the app wants to FP for 32 seconds,
     *                              then shut off for 60 seconds
     * @param repeatMode            - If this flag is OFF then the OFF duration will do nothing
     * @param uploadInterval        - This is when the app should upload the fingerprints.
     *                              This can be set to immediately (when every a bundle of 32 is ready), never, or some time frame
     * @param uploadIntervalMinutes This parameter is used when the upload interval is specified as MINUTE_VALUE
     * @param uploadIntervalHours   This parameter is used when the upload interval is specified as HOUR_VALUE
     * @param notificationResponse  -  Number between 2-360 seconds or 0 (never).
     */
    public Pair<Boolean, String> initialize(MatchRate matchRate, int onDuration, int offDuration, boolean repeatMode,
                                            UploadInterval uploadInterval, int uploadIntervalHours,
                                            int uploadIntervalMinutes, int notificationResponse) {

        int minValueOnDuration;
        int matchRateValue;

        try {

            //TODO See if it works, this was in the constructor but when the app started without internet access
            // the variable was not initialized.
            this.mServerComm.initialize();

            this.mMatchRate = matchRate;
            this.mOnDuration = onDuration;
            this.mOffDuration = offDuration;
            this.mRepeatMode = repeatMode;
            this.mUploadInterval = uploadInterval;
            this.mNotificationResponse = notificationResponse;


            switch (uploadInterval) {
                case HOUR_VALUE:
                    mUploadIntervalHourValue = uploadIntervalHours;
                    break;
                case MINUTE_VALUE:
                    mUploadIntervalMinuteValue = uploadIntervalMinutes;
                    break;
                default:
                    mUploadIntervalMinuteValue = 0;
                    mUploadIntervalHourValue = 0;
                    break;
            }


            switch (matchRate) {
                case ONE_PER_SECOND:
                    minValueOnDuration = 32;
                    matchRateValue = 1;
                    break;
                case TWO_PER_SECOND:
                    minValueOnDuration = 16;
                    matchRateValue = 2;
                    break;
                case FOUR_PER_SECOND:
                    minValueOnDuration = 8;
                    matchRateValue = 4;
                    break;
                case EIGHT_PER_SECOND:
                    minValueOnDuration = 4;
                    matchRateValue = 8;
                    break;
                case SIXTEEN_PER_SECOND:
                    minValueOnDuration = 2;
                    matchRateValue = 16;
                    break;
                case THIRTY_TWO_PER_SECOND:
                    minValueOnDuration = 1;
                    matchRateValue = 32;
                    break;
                default:
                    // error case
                    minValueOnDuration = 32;
                    matchRateValue = 1;
                    break;
            }

            // onDuration = 0 is ALWAYS case
            if (onDuration != 0) {
                if (onDuration < minValueOnDuration || onDuration > MAX_ON_DURATION) {
                    throw new Exception("With the match rate selected, the On duration must be greater to " + minValueOnDuration + " and lower than 60 seconds");

                }
                if (offDuration < MIN_OFF_DURATION || offDuration > MAX_OFF_DURATION) {
                    throw new Exception("Off Duration must be an integer greater or equal to 0 and lower than 60 seconds");
                }
            }


            // default values of Recording Audio
            this.mAudioSource = MediaRecorder.AudioSource.MIC;
            this.mRecorderChannels = AudioFormat.CHANNEL_IN_MONO;
            this.mRecorderSampleRate = 8000;
            this.mRecorderAudioEncoding = AudioFormat.ENCODING_PCM_16BIT;
            // (8000 hz * 16 Bits) = 128.000 Bits/s = 16.000 Bytes/s
            this.mAudioSizePerSecond = 16000;

            // set the input size of each fft calculation in bytes
            //this.mFFTInputSizeInBytes = (this.mAudioSizePerSecond / matchRateValue);
            this.mFFTInputSizeInBytes = fftSizeInBytes(); // FIXME see if it is ok with samples instead of bytes

            this.mTotalBytesToRecord = maximumBytesToRead();

            // initialize the timer, it is done here because of the execution of the cancel method cancel
            // the timers and they have to be re-initialized
            this.mTimerUploadInterval = new Timer();
            this.mTimerTaskUploadInterval = new TimerTaskUploadInterval();

            this.mOffsetForFFT = this.mAudioSizePerSecond / matchRateValue;


            // check that the selected value is between the limits, obviously when hour_value upload interval is selected
            if (this.mUploadInterval == UploadInterval.HOUR_VALUE && (this.mUploadIntervalHourValue < MIN_HOUR_VALUE || this.mUploadIntervalHourValue > MAX_HOUR_VALUE)) {
                throw new Exception("Upload Interval Hour value must be greater to " + MIN_HOUR_VALUE + " and lower than " + MAX_HOUR_VALUE + " hours");
            }

            // check that the selected value is between the limits, obviously when minute_value upload interval is selected
            if (this.mUploadInterval == UploadInterval.MINUTE_VALUE && (this.mUploadIntervalMinuteValue < MIN_MINUTE_VALUE || this.mUploadIntervalMinuteValue > MAX_MINUTE_VALUE)) {
                throw new Exception("Upload Interval Minute value must be greater to " + MIN_MINUTE_VALUE + " and lower than " + MAX_MINUTE_VALUE + " minutes");
            }

            if (mTotalBytesToRecord <= 0) {
                throw new Exception("The combination of match rate and on duration parameters must generate al least 32 FP");
            }

            switch (this.mUploadInterval) {
                case MINUTE_VALUE:
                    // schedule the task of Upload
                    this.mTimerUploadInterval.schedule(mTimerTaskUploadInterval, mUploadIntervalMinuteValue * SIXTY_SECONDS * ONE_SECOND_IN_MILLISECOND, mUploadIntervalMinuteValue * SIXTY_SECONDS * ONE_SECOND_IN_MILLISECOND);
                    break;
                case HOUR_VALUE:
                    // schedule the task of Upload
                    this.mTimerUploadInterval.schedule(mTimerTaskUploadInterval, mUploadIntervalHourValue * SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_IN_MILLISECOND, mUploadIntervalHourValue * SIXTY_MINUTES * SIXTY_SECONDS * ONE_SECOND_IN_MILLISECOND);
                    break;
            }
/*

            // check that the NotificationResponse parameter is valid
            if ((mNotificationResponse < 0 || mNotificationResponse == 1 || mNotificationResponse > 360)) {
                throw new Exception("NotificationResponse parameter must be between 2 and 360, or 0 (never)");
            }
            if (mNotificationResponse > 0) { // schedule service
                this.mTimerNotificationResponseInterval = new Timer();
                this.mTimerTaskNotificationResponseInterval = new TimerTaskNotificationResponse();
                if (mNotificationResponse > 0) {
                    this.mTimerNotificationResponseInterval.schedule(mTimerTaskNotificationResponseInterval, mNotificationResponse * ONE_SECOND_IN_MILLISECOND, mNotificationResponse * ONE_SECOND_IN_MILLISECOND);
                }
            }

*/
            mBufferAux = new byte[this.mOffsetForFFT];
            if (isDebugging) {
                audioOfTotalBytesToRecord = new byte[mTotalBytesToRecord];
                audioOfTotalBytesToRecordRead = new byte[mTotalBytesToRecord];
            }

        } catch (Exception e) {
            if (isDebugging) {
                e.printStackTrace();
            }
            return new Pair<Boolean, String>(false, e.getMessage());
        }
        return new Pair<Boolean, String>(true, "OK");

    }

    /**
     * Set the recording Audio Configuration
     *
     * @param audioSource           - the recording source. See MediaRecorder.AudioSource for recording source definitions.
     *                              By default MIC
     * @param sampleRateInHz        - the sample rate expressed in Hertz. 44100Hz is currently the only rate that is guaranteed
     *                              to work on all devices, but other rates such as 22050, 16000, and 11025 may work on some devices.
     *                              By default 8000
     * @param channelConfig         - describes the configuration of the audio channels. See CHANNEL_IN_MONO
     *                              and CHANNEL_IN_STEREO. CHANNEL_IN_MONO is guaranteed to work on all devices.
     *                              By default CHANNEL_IN_MONO
     * @param recorderAudioEncoding - the format in which the audio data is represented. See ENCODING_PCM_16BIT and ENCODING_PCM_8BIT
     */
    public void setAudioRecordConfiguration(int audioSource, int sampleRateInHz, int channelConfig, int recorderAudioEncoding) {

        int matchRateValue;

        this.mAudioSource = audioSource;
        this.mRecorderSampleRate = sampleRateInHz;
        this.mRecorderChannels = channelConfig;
        this.mRecorderAudioEncoding = recorderAudioEncoding;
        if (recorderAudioEncoding == AudioFormat.ENCODING_PCM_16BIT) {
            this.mAudioSizePerSecond = 16000; //wikipedia 8,000 Hz 16 bit PCM   128 kbit/s
        } else if (recorderAudioEncoding == AudioFormat.ENCODING_PCM_8BIT) {
            this.mAudioSizePerSecond = 8000; //wikipedia 8,000 Hz 8 bit PCM 64 kbit/s
        }


        switch (this.mMatchRate) {
            case ONE_PER_SECOND:
                matchRateValue = 1;
                break;
            case TWO_PER_SECOND:
                matchRateValue = 2;
                break;
            case FOUR_PER_SECOND:
                matchRateValue = 4;
                break;
            case EIGHT_PER_SECOND:
                matchRateValue = 8;
                break;
            case SIXTEEN_PER_SECOND:
                matchRateValue = 16;
                break;
            case THIRTY_TWO_PER_SECOND:
                matchRateValue = 32;
                break;
            default:
                // error case
                matchRateValue = 1;
                break;
        }

        this.mFFTInputSizeInBytes = fftSizeInBytes(); // FIXME see if it is ok with samples instead of bytes

        this.mOffsetForFFT = this.mAudioSizePerSecond / matchRateValue;

        mBufferAux = new byte[this.mOffsetForFFT];

        if (isDebugging) {
            audioOfTotalBytesToRecord = new byte[mTotalBytesToRecord];
            audioOfTotalBytesToRecordRead = new byte[mTotalBytesToRecord];
        }


    }

    /**
     * returns if the app is running (recording)
     *
     * @return isRecording
     */
    public boolean isRunning() {
        return mIsRecording;
    }

    /**
     * Start recording process
     */
    public void start() {
        int totalBytesRead;
        int bytesReadPerTime;
        int bufferSizeToReadFromMic;
        byte[] sData;
        double[] fft;
        List<double[]> fftList;
        byte[] compressedDataPerFP;
        ByteArrayOutputStream generalBuffer;
        List<String> FPHexList;
        List<byte[]> FPByteList;
        int currentPacketPos;
        String[] temporallyBundle;
        Date now;
        String fileName;
        int iter;
        String currentFPString;
        byte[] bufferToFFt;
        int offsetOfReadBytes;


        if (!this.mIsRecording) {

            this.mStopped = false;

            mCircularByteBuffer = new CircularByteBuffer(5*1024*1024); // 5MB

            // This thread reads from audioRecorder mTotalBytesToRecord and write on the circular buffer
            new Thread(new Runnable() {
                @Override
                public void run() {

                    byte[] recorderBuffer = new byte[64000];
                    int recorded = 0;

                    int mMinBufferSize = AudioRecord.getMinBufferSize(mRecorderSampleRate, mRecorderChannels, mRecorderAudioEncoding);
                    mRecorder = new AudioRecord(mAudioSource, mRecorderSampleRate, mRecorderChannels, mRecorderAudioEncoding, 64000);

                    mRecorder.startRecording();


                    int remainingBytesToRead =  mTotalBytesToRecord;
                    Log.d("AUDIO_SDK", "Writing to circular buffer");

                    while (( remainingBytesToRead > 0) && !mStopped) {
                        recorded = mRecorder.read(recorderBuffer, 0, 16000); //FIXME se the chunk size


                        try {
                            if ((remainingBytesToRead - recorded) > 0){
                                if (isDebugging) {
                                    System.arraycopy(recorderBuffer, 0, audioOfTotalBytesToRecord, mTotalBytesToRecord - remainingBytesToRead, recorded);
                                }
                                remainingBytesToRead -= recorded;
                                mCircularByteBuffer.getOutputStream().write(recorderBuffer, 0, recorded);

                            }
                            else {
                                mCircularByteBuffer.getOutputStream().write(recorderBuffer, 0, remainingBytesToRead);

                                if (isDebugging){
                                    System.arraycopy(recorderBuffer, 0, audioOfTotalBytesToRecord, mTotalBytesToRecord - remainingBytesToRead, remainingBytesToRead);
                                }
                                remainingBytesToRead = 0;
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }).start();

            try {

                // this variable is defined because of we want to interrupt the recording process
                this.mIsRecording = true;
                int remainBytesToRead = mTotalBytesToRecord;
                totalBytesRead = 0;
                sData = new byte[this.mFFTInputSizeInBytes];

                generalBuffer = new ByteArrayOutputStream();
                FPHexList = new ArrayList<String>();
                FPByteList = new ArrayList<byte[]>();
                fftList = new  ArrayList<double[]>();


                temporallyBundle = new String[FP_PACKAGE_SIZE];
                currentPacketPos = 0;
                iter = 0; // variable to control the first record of each "mTotalBytesToRecord", also used to name the file to upload

                // iterates to read from the circular buffer until read mTotalBytesToRecord
                while (((totalBytesRead) < mTotalBytesToRecord) && this.mIsRecording) {


                    // Specify the limit to read in order to support overlay
                    // For example, when 4PFs, it read 8000 bytes (4000 samples) and the last iteration read only 2000
                    offsetOfReadBytes = 0;
                    int limit=0;
                    if(iter == 0){
                        bufferSizeToReadFromMic = this.mFFTInputSizeInBytes;

                        // if I have less bytes to read than the mFFTInputSizeInBytes, I've to read only the remainBytes, else mFFTInputSizeInBytes
                        limit = (remainBytesToRead < this.mFFTInputSizeInBytes) ? remainBytesToRead : this.mFFTInputSizeInBytes;
                    }
                    else{
                        bufferSizeToReadFromMic = mOffsetForFFT;

                        // if I have less bytes to read than the offsetForFFt, I've to read only the remainBytes, else mFFTInputSizeInBytes
                        // shuld be allways mOffsetForFFT (at least for 4FPS)
                        limit = (remainBytesToRead < mOffsetForFFT) ? remainBytesToRead : mOffsetForFFT;
                    }

                    // loop until get the mFFTInputSizeInBytes
                    Log.d("AUDIO_SDK", "Reading from circular buffer");

                    while (offsetOfReadBytes < limit && this.mIsRecording) {

                        // read the audio from Buffer
                        bytesReadPerTime = mCircularByteBuffer.getInputStream().read(sData, offsetOfReadBytes, bufferSizeToReadFromMic);

                        // move the offset to save the following data in sData correctly
                        offsetOfReadBytes += bytesReadPerTime;

                        // set the size to do recording
                        // for 4FPs the fist time, it reads the maximum (8000 bytes, 4000samples), the following only 4000 (2000samples)
                        // for 16 FPs, as there aren't over layered, reads always  offsetOfReadBytes (1000 bytes, 500 samples)
                        if (iter == 0) {
                            if (isDebugging) {
                                System.arraycopy(sData, 0, audioOfTotalBytesToRecordRead, offsetOfReadBytes - bytesReadPerTime, bytesReadPerTime);
                            }
                            bufferSizeToReadFromMic = this.mFFTInputSizeInBytes - offsetOfReadBytes;//bytesReadPerTime;
                        } else {
                            if (isDebugging) {
                                if (mOffsetForFFT != mFFTInputSizeInBytes) {
                                    System.arraycopy(sData, 0, audioOfTotalBytesToRecordRead, mOffsetForFFT + iter * mOffsetForFFT + offsetOfReadBytes - bytesReadPerTime, bytesReadPerTime);
                                }
                                else{
                                    System.arraycopy(sData, 0, audioOfTotalBytesToRecordRead, iter * mOffsetForFFT + offsetOfReadBytes - bytesReadPerTime, bytesReadPerTime);
                                }
                            }
                            bufferSizeToReadFromMic = mOffsetForFFT - offsetOfReadBytes;
                        }

                        remainBytesToRead -= bytesReadPerTime;
                        totalBytesRead += bytesReadPerTime;

                    }

                    // create a new buffer to send to the FFT function
                    bufferToFFt = new byte[this.mFFTInputSizeInBytes];

                    if (this.mFFTInputSizeInBytes != mOffsetForFFT) { // at least for 16FPS, there is no overlay (mFFTInputSizeInBytes == mOffsetForFFT)

                        if (iter > 0) { // in the first iteration, bufferAux = sData
                            // copy the first part of previous sData (mBufferAux) to the bufferToFFT
                            System.arraycopy(mBufferAux, 0, bufferToFFt, 0, mOffsetForFFT);

                            // copy the second part of sdata to the bufferToFFT
                            System.arraycopy(sData, 0, bufferToFFt, mOffsetForFFT, mOffsetForFFT);
                        } else {
                            // copy the sData to the bufferToFFT
                            System.arraycopy(sData, 0, bufferToFFt, 0, this.mFFTInputSizeInBytes);
                        }

                        mBufferAux = new byte [mOffsetForFFT];
                        // save the second part of sData to the bufferAux in order to use in the next fft
                        System.arraycopy(bufferToFFt, mOffsetForFFT, mBufferAux, 0, mOffsetForFFT);
                    }
                    else{
                        // copy the sData to the bufferToFFT
                        System.arraycopy(sData, 0, bufferToFFt, 0, this.mFFTInputSizeInBytes);

                    }

                    if (isDebugging) {
                        byte test1;
                        byte test2;
                        for (int y = 0; (y < this.mFFTInputSizeInBytes); y++) {
                            if (iter == 0) {
                                test1 = audioOfTotalBytesToRecord[currentPacketPos * mFFTInputSizeInBytes + y];
                                test2 = bufferToFFt[y];
                                if (test1 != test2) {
                                    Log.d("AUDIO_SDK", "negative match y:" + y + " test1 " + test1 + " test2 " + test2 + "CurrentPacket " + currentPacketPos);
                                }
                            } else {
                                if (this.mFFTInputSizeInBytes != mOffsetForFFT) {
                                    test1 = audioOfTotalBytesToRecord[(iter * mOffsetForFFT ) + y];
                                } else {
                                    test1 = audioOfTotalBytesToRecord[(iter * mFFTInputSizeInBytes) + y];
                                }
                                test2 = bufferToFFt[y];
                                if (test1 != test2) {
                                    Log.d("AUDIO_SDK", "negative match y:" + y + " test1 " + test1 + " test2 " + test2 + "CurrentPacket " + currentPacketPos);
                                }
                            }
                        }
                    }

/*
                    for(int y = 0;(y < this.mFFTInputSizeInBytes); y++) {
                        if(iter == 0){
                            if (bufferToFFt[y] != sData[y]) {
                                Log.d("AUDIO_SDK", "ERROR VALUE: y="+ y + " " + bufferToFFt[y] + "<>" + sData[y]);
                            }
                        }
                        else {
                            if (y < mOffsetForFFT) {
                                if (bufferToFFt[y] != mBufferAux[y]) {
                                    Log.d("AUDIO_SDK", "ERROR VALUE y="+ y + " " + bufferToFFt[y] + "<>" + mBufferAux[y]);
                                }
                            } else {
                                if (bufferToFFt[y] != sData[y - this.mOffsetForFFT]) {
                                    Log.d("AUDIO_SDK", "ERROR VALUE: y="+ y + " "+ bufferToFFt[y] + "<>" + sData[y - this.mOffsetForFFT]);
                                }
                            }
                        }

                    }
*/


                    // do the fft method
                    fft = doFFT(bufferToFFt);

                    // fft could be greater than mFFTPoints but the getFingerPrint algorithm only takes into account
                    // the bytes between 100 - 3900
                    compressedDataPerFP = getFingerPrint(fft);


                    int timeStamp = rateToIncrementNumber(mMatchRate) * currentPacketPos + 1;
                    currentFPString = Utils.bytesToHex(compressedDataPerFP) + Utils.intToHexString(timeStamp, 5);


                    if (isDebugging) { // adds data to the corresponding list. At the end of the iteration, it save into files

                        FPByteList.add(compressedDataPerFP);

                        if (iter == 0 && generateFFTFiles) { // print FFT
                            String fileContent = "";
                            for (int j = 0; j < mFFTPoints; j++) {
                                Log.d("AUDIO_SDK", "j= " + j);
                                fileContent += (fft[j * 2] + ";" + fft[(j * 2) + 1] + "\n");
                            }
                            Utils.createStringFile(mNumberInFile + "_FFT.txt", fileContent, null);


                        }
                        fftList.add(fft);
                        FPHexList.add(currentFPString);
                        if (iter == 0) {
                            generalBuffer.write(sData, 0, this.mFFTInputSizeInBytes);
                        } else {
                            generalBuffer.write(sData, 0, mOffsetForFFT);
                        }

                    }

                    // add the current FPString to the temporally bundle
                    temporallyBundle[currentPacketPos] = currentFPString;

                    currentPacketPos++;
                    if (currentPacketPos == FP_PACKAGE_SIZE) { // if i have a bundle, add to this.mReadyFP

                        // Create bundle file name
                        now = Utils.localDateToGMT(Calendar.getInstance().getTime());
                        fileName = new SimpleDateFormat("yyyy-MM-dd").format(now) + "T" + new SimpleDateFormat("HH:mm:ss").format(now);
                        fileName += "+0000_bp" + rateToFilterNumber(mMatchRate) + "_fp.bin";

                        if (isDebugging) {
                            fileName = mNumberInFile + "_"+fileName;
                        }

                        // Add the bundle to upload to mReadyFP
                        this.mReadyFP.add(new Pair(fileName, temporallyBundle));

                        // reset temporally bundle
                        temporallyBundle = new String[FP_PACKAGE_SIZE];
                        currentPacketPos = 0;
                    }

                    // in this case, we have to upload the 32 FP when we reach that number
                    if (this.mUploadInterval == UploadInterval.IMMEDIATELY && this.mReadyFP.size() > 0) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                // start a new thread to upload the FP
                                sendBundle();

                            }
                        }).start();
                    }

                    iter++;


                }


                // Print files
                if (isDebugging) {

                    // check the audio read from mic with the audio read from circularbuffer
                    for (int i = 0; i<audioOfTotalBytesToRecord.length; i++){
                        if (audioOfTotalBytesToRecord[i] != audioOfTotalBytesToRecordRead[i]){
                            Log.d("AUDIO_SDK","Error i: "+ i +"  " +audioOfTotalBytesToRecord[i]+ "  "+ audioOfTotalBytesToRecordRead[i] );
                        }

                    }

                    FileOutputStream fos = null;
                    PrintWriter fOutputCompressedData = null;
                    try {

                        File f = Utils.getDefaultStorageFile(mContext);

                        // write the PCM file
                        fos = new FileOutputStream(new File(f, mNumberInFile + "_AudioFile.pcm"));
                        generalBuffer.writeTo(fos);
                        fos.close();
                        generalBuffer.flush();
                        generalBuffer.reset();

                        // Put the FP as hex string into the file
                        fOutputCompressedData = new PrintWriter(new File(f, mNumberInFile +"_FPHexData.txt"));
                        for (int i = 0; i < FPHexList.size(); i++) {
                            fOutputCompressedData.write(FPHexList.get(i));
                            fOutputCompressedData.println();
                        }
                        fOutputCompressedData.close();
                        FPHexList.clear();

                        // print FP as bytes
                        Utils.createListByteArrayAsStringFile(mNumberInFile + "_FPByteData.txt", FPByteList, null);
                        FPByteList.clear();

                    } catch (IOException ioe) {
                        // Handle exception here
                        throw ioe;
                    }
                    mNumberInFile++;
                }

                // allows to start again
                release();

                // if the repeat mode flag is OFF, then the OFF duration will do nothing
                if (this.mRepeatMode && !mStopped) {
                    this.mTimerOffDuration = new Timer();
                    this.mTimerTaskOffDuration = new TimerTaskOffDuration();
                    this.mTimerOffDuration.schedule(mTimerTaskOffDuration, this.mOffDuration * ONE_SECOND_IN_MILLISECOND);
                }
            } catch (Exception e) {
                if (isDebugging){
                    e.printStackTrace();
                }
                errorReceived(e);
            }

        } else {
            errorReceived(new Exception("The process is already running"));
        }
    }

    /**
     * Stop the recording process, after the execution of this method, it is necessary to execute the initialize
     * method
     */
    public void cancel() {

        if (this.mTimerOffDuration != null) {
            this.mTimerOffDuration.cancel();
        }
        if (this.mTimerUploadInterval != null) {
            this.mTimerUploadInterval.cancel();
        }
        if (this.mTimerNotificationResponseInterval != null){
            this.mTimerNotificationResponseInterval.cancel();
        }

        if (null != mRecorder) {
            mStopped = true;
            mIsRecording = false;
            mRecorder.stop();
            Log.i("AUDIO_SDK", "STOP EXECUTED");
            mRecorder.release();
            this.mReadyFP.clear();
            Log.i("AUDIO_SDK", "RELEASE EXECUTED");
            mRecorder = null;
        }
        // remove listeners
        for (int i=0; i<listeners.size();i++){
            listeners.remove(i);
        }
    }

    /**
     * @param listener
     */
    public void addOnResponseListener(InfoReceivedListener listener) {
        mServerComm.addOnResponseListener(listener);
        listeners.add(listener);
    }

    private void errorReceived(Exception e){
        // Notify everybody that may be interested.
        for (InfoReceivedListener hl : listeners) {
            hl.errorReceived(e);
        }
    }


    /**
     * *******************************PRIVATE METHODS***************************************
     */
    private void sendBundle() {

        try {

            List<Pair<String, String[]>> mReadyFP_copy = new ArrayList<Pair<String, String[]>>();
            mReadyFP_copy.addAll(this.mReadyFP);
            this.mReadyFP.clear();
            mServerComm.sendPackage(mReadyFP_copy);

        } catch (Exception e) {
            if (isDebugging){
                e.printStackTrace();
            }
            errorReceived(e);
        }
    }

    private void getServerResponse() {

        mServerComm.getResponseFromServer();

    }

    /**
     * @param fftSamples
     * @return
     */
    private byte[] getFingerPrint(double[] fftSamples) {

        final int FP_SIZE = 80;
        final int FFT_LOW_BAND = 100;
        final int FFT_HIGH_BAND = 3900;

        double acc = 0.000;
        int LOOP_COUNTER_1 = 0;
        int LOOP_COUNTER_2 = 0;
        int FIRST_PASS = 0;
        double FFT_SUM_NEW = 0;
        double FFT_SUM_OLD = 0;
        byte[] FP_BITS = new byte[FP_SIZE + 2];
        int SAMP_PER_STAIR2 = 4;
        double BETA = 0.75;
        double T = 0.000;
        byte[] groupedBitByteArray = new byte[10];

        // Out is the result of fft function. It has in the position i, the real part and in i+1 position the imaginary part


        for (int q = FFT_LOW_BAND; q <= FFT_HIGH_BAND; q++) {

            //double absolute = Math.abs(out[q]);
            // out has the 8192 FFT samples of the audio.  Loop through frequencies from 100-3900
            double absolute = Utils.absComplex(fftSamples[q * 2], fftSamples[(q * 2) + 1]);

            acc += absolute;  //  keep adding them up together
            if (LOOP_COUNTER_1 == SAMP_PER_STAIR2) {  //  If we have enough samples to match SAMP_PER_STAIR2 process the data.  SAMP_PER_STAIR2 is variable
                SAMP_PER_STAIR2 = SAMP_PER_STAIR2 + 1;  //  First run of the loop it was 4, then it goes up each time 5,6,7 etc

                FFT_SUM_NEW = acc;  //  We are making averages of the frequencies (basically summing up freq 100-104,105-110,111-117,118-125 etc)

                acc = 0;  // reset the variable that adds up each bin
                LOOP_COUNTER_1 = 0;
                if (FIRST_PASS == 0) {  //  If this is the first bin we have summed -  our T (threshold) will be set to the first bin value on the first bit of the signature
                    T = FFT_SUM_NEW; //beta in this case should be 7/8
                    FIRST_PASS = 1;
                } else {
                    T = (BETA * T) + ((1 - BETA) * FFT_SUM_NEW); //beta in this case should be 7/8.  This is moving average threshold.

                    //  Very simple algorithim that now compares the frequency bin to the threshold and if its greater then the bit is 1 else its 0

                    if (FFT_SUM_NEW - T > 0) {
                        FP_BITS[LOOP_COUNTER_2] = 1;

//
                    } else {
                        FP_BITS[LOOP_COUNTER_2] = 0;
                    }
                    LOOP_COUNTER_2 = LOOP_COUNTER_2 + 1;
                }
            } else {
                LOOP_COUNTER_1 = LOOP_COUNTER_1 + 1;
            }
        }

        //   3900-100 = 3801 frequncies.  our bin sizes are 5,6,7,8...... 86.  That makes a total of 82 bits. First and last bit are ignored.
        //    see below where its limited to FP_size which is 80.  You could also lower the high_band up top to 3750 and you would get 81 bits - the first bit which is ignored


        int byteNumber;
        for(int i=0; i< FP_SIZE ; i++){
            byteNumber = (i) / 8;
            if (FP_BITS[i] == 1){
                groupedBitByteArray[byteNumber] = (byte) ((groupedBitByteArray[byteNumber] << 1) | 0x01);
            } else {
                groupedBitByteArray[byteNumber] = (byte) ((groupedBitByteArray[byteNumber] << 1));
            }
        }

        System.out.println("FP");
        for (int i = 0; i < FP_BITS.length; i++)
            System.out.print(FP_BITS[i]);

        System.out.println("");


        // return FP_BITS_RETURN;
        return groupedBitByteArray;

    }

    /**
     * @param input
     * @return
     */
    private double[] doFFT(byte[] input) {

        DoubleFFT_1D fftDo;
        double[] inputInDouble;
        int bufferSizeOutputAfterFFT;
        double[] fft;
        short[] inputInShort;


        inputInShort = Utils.byteArrayToShortArray(input);
        // generate a double array from the input short array
        inputInDouble = Utils.convertShortArrayAsDoubleArray(inputInShort, this.mFFTInputSizeInBytes/2);

        // generate a double array from the input byte array
        //inputInDouble = Utils.byteArrayToDouble(input);

        // initialize the structure of DoubleFFT_1D with the FFT points selected for the output
        fftDo = new DoubleFFT_1D(this.mFFTPoints);

        // set the size of input/output array, be careful with this array because the size
        // should be at least the read elements*2
        bufferSizeOutputAfterFFT = Math.max(this.mFFTPoints * 2, inputInDouble.length * 2);

        fft = new double[bufferSizeOutputAfterFFT];

        // put the input read values from the recording into the input/output array
        System.arraycopy(inputInDouble, 0, fft, 0, inputInDouble.length);

        // do fft method
        fftDo.realForwardFull(fft);

        return fft;
    }

    /**
     * Stop and Release recording resources
     */
    private void release() {
        if (null != mRecorder) {
            mIsRecording = false;
            mRecorder.stop();
            Log.i("AUDIO_SDK", "STOP EXECUTED");
            mRecorder.release();
            Log.i("AUDIO_SDK", "RELEASE EXECUTED");
            mRecorder = null;

            mBufferAux = new byte[this.mFFTInputSizeInBytes];

            try {
                mCircularByteBuffer.getOutputStream().close();
                mCircularByteBuffer.getInputStream().close();
                Log.d("AUDIO_SDK", "Closing circular buffer");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int rateToFilterNumber(MatchRate matchRate){
        switch (matchRate) {
            case SIXTEEN_PER_SECOND:
                return 1;
            case EIGHT_PER_SECOND:
                return 2;
            case FOUR_PER_SECOND:
                return 0;
            case TWO_PER_SECOND:
                return 3;
            case ONE_PER_SECOND:
                return 4;
            default: return -1;
        }

        // 16/sec Filter number 1
        // 8/sec Filter number 2
        // 4/sec filter number 0
        // 2/sec filter number 3
        // 1/sec filter number 4
    }

    /**
     * Returns the maximun bytes to read given the matchRate and the seconds available in order to fill multiple of 32FP
     * @return
     */
    private int maximumBytesToRead(){
        int ret = 132000;
        int minimunBytesPerRate;
        switch (mMatchRate) {
            case FOUR_PER_SECOND: {
                minimunBytesPerRate = 132000;
                ret = (mAudioSizePerSecond * mOnDuration) / minimunBytesPerRate ;
                ret *= minimunBytesPerRate;
            }
            break;
            case SIXTEEN_PER_SECOND: {
                minimunBytesPerRate = 32000;
                ret = (mAudioSizePerSecond * mOnDuration) / minimunBytesPerRate ;
                ret *= minimunBytesPerRate;
            }
            break;
        }
        return ret;
    }

    private int fftSizeInBytes(){
        switch (mMatchRate) {
            case THIRTY_TWO_PER_SECOND:
                return -1;
            case SIXTEEN_PER_SECOND:
                return 1000;
            case EIGHT_PER_SECOND:
                return -1;
            case FOUR_PER_SECOND:
                return 8000;
            case TWO_PER_SECOND:
                return -1;
            case ONE_PER_SECOND:
                return -1;
            default: return -1;
        }

    }

    private int rateToIncrementNumber(MatchRate matchRate){
        switch (matchRate) {
            case THIRTY_TWO_PER_SECOND:
                return 1;
            case SIXTEEN_PER_SECOND:
                return 2;
            case EIGHT_PER_SECOND:
                return 4;
            case FOUR_PER_SECOND:
                return 1;
            case TWO_PER_SECOND:
                return 2;
            case ONE_PER_SECOND:
                return 4;
            default: return -1;
        }

     /*   1/sec Increment = 4
        2/sec Increment = 2
        4/sec Increment = 1
        8/sec Increment = 4
        16/sec Increment = 2
        32/sec Increment = 1*/
    }

}