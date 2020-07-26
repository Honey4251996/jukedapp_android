Juked-Android
=============

Inside the root folder, there are two important folders: "SDK" which is the purpose of this project, and "App" which is an example app that uses the SDK. 
The SDK is completely independent from the app.

This SDK does the following:
In a thread, it reads audio from the device's mic and saves it using a circular buffer. This thread reads all the possible data to generate a multiple of 32 FP (1 bundle). 
For example, for 4FPS we need 66000 samples (132000 bytes), if the onDuration parameter is set to 20 seconds, this process will read only 132000 samples  (264000 bytes) instead of the 160000 samples available (320000 bytes).  

In another thread, it reads from the circular buffer until it gets all the possible data (same data that was previously readed from the mic ). Inside this thread, there's a "while" instruction which keeps reading from the circular buffer until it get enought data to generate the FFT input (4000 samples). 
When it gets the data, it send it to the FFT funcion and then to FP.

There´s a lot of code which only executes when the property "isDebugging" (in the app.properties file) is set to true. This code is generally to execute some controls in the audio, as for generating fft, FP, audioData and the data to upload files. This files are generated in the sdCard in the folder AudioSDK. If there's no SDCard, the SDK uses the default data directory.

There are some crucial files:
	*FPController does the logic described above.
	*FPServices is the one which is in charge of keeping the app live when the user "hides" it.
	*Utils Include some operations used in many locations of the project.

As the reading process has to run on background and the app should not lose data when it goes to background, the comunication to the service is asyncronous so the app can't return data or errors syncronously. 

In the MainActivity class (in the app project) you can see that the class implements InfoReceivedListener which is the iterface to receive the data described above. You should implement both methods dataReceived(String xml) and errorReceived(Exception e). Also, to interact with the SDK, it uses the interface AudioSDK.
