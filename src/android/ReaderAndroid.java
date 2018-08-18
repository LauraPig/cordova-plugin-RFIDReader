package android.reader;

import android.util.Log;
import android.widget.Toast;

import java.io.File;

public class ReaderAndroid {
    // JNI
    private native static int open(String path, int baudrate);
    public native void close(int handle);
    // For ISO14443 Type-A
    public native int getCardSN(int handle,int address,byte mode,byte cmd,byte[] hald_flag,byte[] SN);
    public native int MFRead(int handle,int address,byte mode,byte blk_add,byte num_blk,byte[] PWS,byte[] buffer);
    public native int MFWrite(int handle,int address,byte mode,byte blk_add,byte num_blk,byte[] PWS,byte[] buffer);

    // For CPU Card
    public native int CPU_RATS(int handle, int address, byte[] param, byte[] buff, byte[] retlen);
    public native int CPU_APDU(int handle, int address, byte[] param, byte paramlen, byte[] buff, byte[] retlen);
    public native int CPU_RST_Ant(int handle, int address, byte[] buff, byte[] retlen);


    static {
        //

        Log.i("ReaderAndroid", "Android LIB load!!!");
        System.loadLibrary("ReaderAndroid");
        Log.i("ReaderAndroid", "Android LIB end");
    }

    private static final String TAG = "ReaderAndroid";
    private int mFd;
    public  ReaderAndroid(File device, int baudrate) throws Exception {
        Log.i("ReaderLIB", "ReaderAndroid");
        /* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            Log.i("ReaderLIB", "device chmod");
            try {
                Process su;
                su = Runtime.getRuntime().exec("/system/xbin/su");
                String cmd = "chmod 777 " + device.getAbsolutePath() + "\n" + "exit\n";
                Log.i("level", "command = " + cmd);
                su.getOutputStream().write(cmd.getBytes());
                su.getOutputStream().flush();
                if ((su.waitFor() != 0) ||
                        !device.canRead()
                        || !device.canWrite()) {
                    throw new Exception("You do not have read/write permission to the serial port.");
                }
            }
            catch (Exception e) {
                throw e;
            }
        }
        try {

            String absPath=device.getAbsolutePath();
            Log.e(TAG, "=================================start open"+absPath+" =============================");
            mFd = open(absPath, baudrate);
            Log.e(TAG, "=================================end open"+absPath+" =============================");
        }catch (Exception ex){
            throw  ex;
        }
        if (mFd == -1) {
            Log.e(TAG, "native open returns null");
            throw new  Exception("native open returns null");
        }
    }

    // Getters and setters
    public int getHandle() {
        return mFd;
    }
}
