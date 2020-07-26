package audiosdk.test.interfaces;

public interface InfoReceivedListener {

    public void dataReceived(String xml);
    public void errorReceived(Exception e);

}
