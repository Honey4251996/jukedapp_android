package audiosdk.test.interfaces;

import android.util.Pair;

import audiosdk.test.FPController;

public interface IAudioSDK {

    Pair<Boolean,String> start();

    Pair<Boolean, String> initialize(FPController.MatchRate matchRate, int onDuration, int offDuration, boolean repeatMode,
                                     FPController.UploadInterval uploadInterval, int uploadIntervalHours, int uploadIntervalMinutes,
                                     int notificationResponse);

    void setAudioRecordConfiguration(int audioSource, int sampleRateInHz, int channelConfig, int recorderAudioEncoding);

    void unbindService();

    void cancel();

    void addOnResponseListener(InfoReceivedListener listener);
}
