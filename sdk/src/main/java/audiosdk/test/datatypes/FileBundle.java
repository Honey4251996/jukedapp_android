package audiosdk.test.datatypes;

public class FileBundle {


    private String filename0;
    private byte[] binaryFile;

    public FileBundle( String filename0, byte[] binaryFile) {
        this.filename0 = filename0;
        this.binaryFile = binaryFile;
    }

    public String getFilename0() {
        return this.filename0;
    }

    public byte[] getBinaryFile() {
        return this.binaryFile;
    }


    /* Not use for now
    public String toJSONString(){
        String ret = "";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("filename0",this.filename0);
            jsonObject.put("file-count",this.fileCount);
            jsonObject.put("data0", this.binaryFile);
            ret = jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    */
}