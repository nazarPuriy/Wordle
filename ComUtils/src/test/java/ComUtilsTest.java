import org.json.simple.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import utils.ComUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.junit.Before;

public class ComUtilsTest {
    ComUtils comUtils;

    @Before
    public void SetUp(){
        File file = new File("test");
        try {
            file.createNewFile();
            comUtils = new ComUtils(new FileInputStream(file), new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSendHello() {
        try {
            comUtils.sendHello(12, "Test");
            int opCode = comUtils.readOpCode();
            int sessionId = comUtils.read_int32();
            String nom = comUtils.read_string0();
            assert(opCode == 1);
            assert(sessionId==12);
            assert(nom.equals("Test"));
            assert(comUtils.checkEmpty());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSendReady() {
        try {
            comUtils.sendReady(13);
            int opCode = comUtils.readOpCode();
            int sessionId = comUtils.read_int32();
            assert(opCode == 2);
            assert(sessionId==13);
            assert(comUtils.checkEmpty());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSendPlay() {
        try {
            comUtils.sendPlay(14);
            int opCode = comUtils.readOpCode();
            int sessionId = comUtils.read_int32();
            assert(opCode == 3);
            assert(sessionId==14);
            assert(comUtils.checkEmpty());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSendAdmit() {
        try {
            comUtils.sendAdmit(true);
            int opCode = comUtils.readOpCode();
            boolean admit = comUtils.readAdmit();
            assert(opCode == 4);
            assert(admit);
            assert(comUtils.checkEmpty());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSendWord(){
        try {
            String send = "REINA";
            //Escrivim la trama (això és el que testegem)
            comUtils.sendWord(send);

            //Rebem la trama per parts
            int code = comUtils.readOpCode();
            String receive = comUtils.read_string(5);

            //Mirem que siguin les esperades
            assertEquals(code, 5);
            assertEquals(send, receive);

            //Comprovem que la trama hagi acabat
            assert(comUtils.checkEmpty());

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testSendResult(){
        try {
            String send = "^^?**";

            //Escrivim la trama (això és el que testegem)
            comUtils.sendResult(send);

            //LLegim la trama per parts
            int code = comUtils.readOpCode();
            String receive = comUtils.read_string(5);

            //Mirem que siguin les esperades
            assertEquals(code, 6);
            assertEquals(send, receive);

            //Comprovem que la trama hagi acabat
            assert(comUtils.checkEmpty());


        }catch (IOException e){
            e.printStackTrace();
        }
    }


    @Test
    public void testSendStatsGetStats(){

        try{

            JSONObject o = new JSONObject();
            o.put("victories", new Integer(3));
            o.put("derrotes", new Integer(0));

            comUtils.sendStats(o);

            int code = comUtils.readOpCode();
            JSONObject p = comUtils.getStats();

            assertEquals(code, 7);
            assertEquals(Integer.parseInt(p.get("victories").toString()), 3);
            assertEquals(Integer.parseInt(p.get("derrotes").toString()), 0);

        }catch (IOException e){
            e.printStackTrace();
        }


    }

    @Test
    public void testSendError(){

        try{

            int sendErrCode = 3;
            String sendErrMsg = "error :-(";

            comUtils.sendError(sendErrCode, sendErrMsg);

            int code = comUtils.readOpCode();
            int errCode = comUtils.read1b();
            String msg = comUtils.read_string0();

            assertEquals(code, 8);
            assertEquals(sendErrCode, errCode);
            assertEquals(msg, sendErrMsg);

        }catch (IOException e){
            e.printStackTrace();
        }


    }

}
