package android.reader;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import java.io.File;

import com.das.dascard.MifareCardProcess;

/**
 * This class echoes a string called from JavaScript.
 */
public class rfidreader extends CordovaPlugin {
    SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    protected ReaderAndroid mSerialPort;
    protected int handle;
    protected String selDevice;
    protected String selBaudrate;
    protected int selBlock;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("Test")) {
            String message = args.getString(0);
            this.Test(message, callbackContext);
            return true;
        }
        else if (action.equals("ReadCardSN")) {
            String device = args.getString(0);
            String baudrate = args.getString(1);
            this.ReadCardSN(device,baudrate, callbackContext);
            return true;
        }
        else if (action.equals("ReadCardData")) {
            String device = args.getString(0);
            String baudrate = args.getString(1);
            String block=args.getString(2);
            String strPWS=args.getString(3);
            this.ReadCardData(device,baudrate,block, strPWS,callbackContext);
            return true;
        }
        else if (action.equals("ReadCardEsquelSN")) {
            String device = args.getString(0);
            String baudrate = args.getString(1);
            this.ReadCardEsquelSN(device,baudrate,callbackContext);
            return true;
        }
        return false;
    }

    private void Test(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success("return:"+message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void ReadCardSN(String device, String baudrate, CallbackContext callbackContext){
        try {
            if(mSerialPort==null|| device !=selDevice || baudrate!=selBaudrate) {
                selDevice=device;selBaudrate=baudrate;
                mSerialPort = new ReaderAndroid(new File(selDevice),Integer.parseInt(selBaudrate));
                handle = mSerialPort.getHandle();
            }
            String re= _readcardSN();
            callbackContext.success(re);
        }
        catch (Exception ex) {
            Log.e("=============>Exception", ex.getMessage());
            callbackContext.error("err:"+ex.getMessage());
        }
    }
    /*
    1.2.3 getCardSN
功能	集成了寻卡，防冲突，选卡等操作，一个命令完成读取卡片序列号的操作
函数原型	int getCardSN (HANDLE  commHandle,int DeviceAddress, unsigned char mode,unsigned char cmd,unsigned char *flag, unsigned char *buffer)
描述	I
输入参数：
commHandle － 串口句柄
DeviceAddress － 设备地址
要通讯的设备地址号，范围从0－255
mode
   0x00: All模式   0x01: Idle模式
cmd
   0x00: 不需要执行halt指令   0x01: 读写器 执行halt指令
输出参数：
flag － 单卡多卡标志
    0x00 – 检测到一张卡  0x01 – 检测到多张卡.(再次调用得到另一张Uid)
buffer
如果成功，buffer[0-3]: 4 字节卡序列号 (从低到高)

如果失败，buffer[0]: 返回状态，具体含义见附表。
返回值	成功返回0否则为非0, 具体含义见附表。

    */
    String _readcardSN(){
        byte mode = (byte)0x00;
        byte halt = (byte)0x00;
        byte[] card_flag=new byte[2];
        byte[] ver=new byte[4];
        int status=mSerialPort.getCardSN(handle, (byte)0x00, mode, halt, card_flag, ver);
        if(status==4) return ("");
        if(0 ==status)
        {
            return ("data:"+byteToInt2(ver));
        }
        else
        {
            return ("err:Read card  error code:"+status);
        }
    }

    private void ReadCardData(String device, String baudrate, String block, String strPWS, CallbackContext callbackContext){
        try {
            if(mSerialPort==null|| device !=selDevice || baudrate!=selBaudrate) {
                selDevice=device;selBaudrate=baudrate;
                mSerialPort = new ReaderAndroid(new File(selDevice),Integer.parseInt(selBaudrate));
                handle = mSerialPort.getHandle();
            }
            String re= _readcardData(Integer.parseInt(block),strPWS);
            callbackContext.success(re);
        }
        catch (Exception ex) {
            Log.e("=============>Exception", ex.getMessage());
            callbackContext.error("err:"+ex.getMessage());
        }
    }

    /*
     MFRead
功能	集成寻卡，防冲突，选卡，验证密码，读卡等操作，一个命令完成读卡操作。
函数原型	int MFRead (HANDLE commHandle, int DeviceAddress,unsigned char mode,  unsigned char blk_add, unsigned char num_blk,unsigned char *snr, unsigned char *buffer)
描述	发送ISO14443 A 读卡指令
输入参数：
commHandle － 串口句柄
DeviceAddress － 设备地址
要通讯的设备地址号，范围从0－255
mode读取模式控制
0x00: Idle模式 + Key A
0x01: All模式  + Key A
0x02: Idle模式 + Key B
0x03: All模式  + Key B
blk_add
   要读的块的起点地址, 范围从0--63
num_blk
   要读的块数长度值, 范围从1—4（m1卡）
snr
   6个字节的密钥
输出参数：
snr
   如果成功，4 字节卡序列号 (从低到高)
buffer
    如果成功，buffer[0-N]: 从卡上返回的数据(16*num_blk字节)
如果失败，buffer[0]: 返回状态，具体含义见附表。

注：只能读取本扇区的块，因为每个扇区有自己独立的密钥
返回值	成功返回0否则为非0, 具体含义见附表。

    */
    String _readcardData(int block,String strPWS){
        byte[] bPWS=new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};

        byte[] ver=new byte[20];
        byte mblock=(byte)(block & 0xff);
        byte cmd=0x00;

        if(strToByte(strPWS,6,bPWS)!=0)
        {
            return "err:length of value is not 6 byte or 12 Character.";
        }
        int status= mSerialPort.MFRead(handle, (byte)0x00, (byte)0x00, mblock, (byte)(0x01 & 0xff), bPWS, ver);
        if(status==4) return ("");
        if(0 ==status)
        {
            return ("data:"+ByteToStr(16, ver));
        }
        else
        {
            return ("err:Read card  error code:"+status);
        }
    }

    
    private void ReadCardEsquelSN(String device, String baudrate, CallbackContext callbackContext){
        try {
            if(mSerialPort==null|| device !=selDevice || baudrate!=selBaudrate) {
                selDevice=device;selBaudrate=baudrate;
                mSerialPort = new ReaderAndroid(new File(selDevice),Integer.parseInt(selBaudrate));
                handle = mSerialPort.getHandle();
            }
            String re= _readCardEsquelSN();
            callbackContext.success(re);
        }
        catch (Exception ex) {
            Log.e("=============>Exception", ex.getMessage());
            callbackContext.error("err:"+ex.getMessage());
        }
    }

    String _readCardEsquelSN(){
        byte mode = (byte)0x00;
        byte halt = (byte)0x00;
        byte[] card_flag=new byte[2];
        byte[] bSN=new byte[4];
        int status=mSerialPort.getCardSN(handle, (byte)0x00, mode, halt, card_flag, bSN);
        if(status==4) return ("");
        if(0 !=status)
        {
            return ("err:Read card  error code:"+status);
        }

        byte[ ]  keyA  =  new  byte[6];			//密钥计算结果
        keyA  =  MifareCardProcess. calculateKey(bSN);	//计算认证密钥

        byte[] bPWS=new byte[]{(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
        byte[] ver=new byte[16];
        byte mblock=(byte)12;
        //byte cmd=0x00;

        status=mSerialPort.MFRead(handle, (byte)0x00, (byte)0x00, mblock, (byte)(0x01 & 0xff), keyA, ver);
        
        if(0 == status)
        {
            long  cardNumber ;//卡流水号
            cardNumber  =  MifareCardProcess. getCardNumber(ver);//获取卡流水号
            //re.append("Decode cardNumber:"+cardNumber);
            return ("data:"+cardNumber);
        }
        else
        {
            return ("err:Read card  error code:"+status);
        }
    }
    String  ByteToStr(int byteSize,byte[] in)
    {
        String ret=new String("");
        if(in.length<byteSize)
            return ret;

        for(int i=0;i<byteSize;i++)
        {
            ret=ret.concat(String.format("%1$02X ", in[i]));
        }
        return ret;
    }
    private  int  strToByte(String in,int byteSize,byte[] out)
    {
        String str=in.replace(" ", "");
        if(str.length()!=byteSize*2 || out==null)
        {
            return -1;
        }
        char[] hexChars = str.toCharArray();
        if(hexChars==null)
        {
            return -1;
        }
        for (int i = 0; i < byteSize; i++) {
            int pos = i * 2;
            out[i] = (byte) ((charToByte(hexChars[pos]) << 4 )| (charToByte(hexChars[pos + 1])));
        }
        return 0;
    }
    private int charToByte(char c) {
        return  "0123456789ABCDEF".indexOf(c);
    }
    private void closeSerialPort() {
        if (mSerialPort != null) {
            mSerialPort.close(mSerialPort.getHandle());
            mSerialPort = null;
        }
    }

    private int byteToInt(byte[] bytes) {

        int mask=0xff;
        int temp=0;
        int n=0;
        for(int i=0;i<bytes.length;i++){
            n<<=8;
            temp=bytes[i]&mask;
            n|=temp;
        }
        return n;
    }
    private int byteToInt2(byte[] bytes) {
        //反序
        byte[] b=new byte[bytes.length];
        for (int i=0;i<bytes.length;i++){
            b[bytes.length-i-1]=bytes[i];
        }

        int mask=0xff;
        int temp=0;
        int n=0;
        for(int i=0;i<b.length;i++){
            n<<=8;
            temp=b[i]&mask;
            n|=temp;
        }
        return n;
    }
}
