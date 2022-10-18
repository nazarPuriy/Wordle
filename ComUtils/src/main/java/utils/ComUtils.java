package utils;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.util.HashMap;

public class ComUtils {
    private final int WORDLE_SIZE = 5;

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private HashMap<Integer, String> missatges;
    private HashMap<String, Integer> codis;

    public ComUtils(InputStream inputStream, OutputStream outputStream) throws IOException {
        dataInputStream = new DataInputStream(inputStream);
        dataOutputStream = new DataOutputStream(outputStream);
        createDics();
    }

    /**
     * Aquest mètode envia l'opcode per la sortida.
     *
     * @param opCode Enter representant l'operation code.
     * @throws IOException
     */
    public void sendOpCode(int opCode) throws IOException {
        byte message[] = new byte[1];
        message[0] = (byte) opCode;
        dataOutputStream.write(message, 0, 1); //L'escrivim.
    }

    /**
     * Aquest mètode llegeix un bit i el transforma a un enter.
     *
     * @return Un enter que representa l'operation code.
     * @throws IOException
     */
    public int readOpCode() throws IOException {
        byte message[] = read_bytes(1);
        return (int) message[0];
    }

    /**
     * Comprova que no queden bytes a llegir.
     *
     * @return true si no queden bytes a llegir, false en cas contrari.
     * @throws IOException
     */
    public boolean checkEmpty() throws IOException {
        return dataInputStream.available() == 0;
    }

    /**
     * Ens permet escriure un byte.
     * @param val el byte a escriure
     * @throws IOException
     */
    public void write1b(byte val) throws IOException {
        byte message[] = new byte[1];
        message[0] = val;
        dataOutputStream.write(message, 0, 1); //L'escrivim.
    }

    /**
     * Ens permet llegir un byte.
     *
     * @return Retorna el valor del byte en forma d'enter.
     * @throws IOException
     */
    public byte read1b() throws IOException {
        byte message[] = read_bytes(1);
        return message[0];
    }


    /**
     * Llegeix 1byte (enter) i interpreta aquest com un boleà de true o false.
     *
     * @return Boleà indicant true o false (Utilitzat en el mètode admit).
     * @throws IOException
     */
    public boolean readAdmit() throws IOException {
        int val = (int) read1b();
        return (val == 1) ? true : false;
    }

    /**
     * Envia 5 Bytes + STRSIZE corresponent al paquet Hello.
     *
     * @param sessionId enter indicant l'identificador de la sessió.
     * @param name      String de llargada màxima STRSIZE que contè el nom del jugador.
     * @throws IOException
     */
    public void sendHello(int sessionId, String name) throws IOException {
        sendOpCode(code("HELLO"));
        write_int32(sessionId);
        write_string0(name);
    }

    /**
     * Envia 5 bytes corresponents al paquet ready.
     *
     * @param sessionId enter indicant l'identificador de la sessió.
     * @param sessionId
     * @throws IOException
     */
    public void sendReady(int sessionId) throws IOException {
        sendOpCode(code("READY"));
        write_int32(sessionId);
    }

    /**
     * Envia 5 bytes corresponents al paquet play.
     *
     * @param sessionId enter indicant l'identificador de la sessió.
     * @throws IOException
     */
    public void sendPlay(int sessionId) throws IOException {
        sendOpCode(code("PLAY"));
        write_int32(sessionId);
    }

    /**
     * Envia 2 bytes corresponents al paquet admit
     *
     * @param adm Booleà indicant l'admissió, o no.
     * @throws IOException
     */
    public void sendAdmit(boolean adm) throws IOException {
        sendOpCode(code("ADMIT"));
        write1b((byte) ((adm) ? 1 : 0));
    }

    /**
     * Aquest mètode envia els 6 bytes corresponents a la trama WORD
     *
     * @param word la paraula a enviar
     * @throws IOException
     */
    public void sendWord(String word) throws IOException {
        sendOpCode(code("WORD")); //Enviem l'opcode
        write_string(word, WORDLE_SIZE); //Enviem els 5 bytes de la paraula
    }

    /**
     * Aquest mètode envia els 6 bytes corresponents a la trama RESULT
     *
     * @param result el resultat a enviar
     * @throws IOException
     */
    public void sendResult(String result) throws IOException {

        sendOpCode(code("RESULT")); //Enviem l'opcode
        write_string(result, 5); //Enviem els 5 bytes del resultat
    }


    /**
     * Aquest mètode envia els bytes corresponents a les stats de l'usuari en format json
     *
     * @param j el JSONObject
     * @throws IOException
     */

    public void sendStats(JSONObject j) throws IOException {

        sendOpCode(code("STATS")); //Enviem l'opcode
        write_string0(j.toJSONString());

    }

    /**
     * Aquest mètode llegeix un JSON del paquet STATS com s'especifica al protocol
     * @return un JSONObject si hem llegit un JSON vàlid, null si no.
     * @throws IOException
     */

    public JSONObject getStats() throws IOException {

        String s = read_string0();
        return (JSONObject) JSONValue.parse(s);
    }

    /**
     * Aquest mètode envia un error seguint el protocol.
     * @param code el codi d'error
     * @param msg el missatge d'error
     * @throws IOException
     */
    public void sendError(int code, String msg) throws IOException {

        sendOpCode(code("ERROR"));
        write1b((byte) code);
        write_string0(msg);

    }

    /**
     * Aquest mètode llegeix un enter.
     *
     * @return L'enter llegit.
     * @throws IOException
     */
    public int read_int32() throws IOException {
        byte bytes[] = read_bytes(4); //Llegim els bytes.
        return bytesToInt32(bytes, Endianness.BIG_ENNDIAN); //Els passem a enter.
    }

    /**
     * Aquest mètode escriu un enter.
     *
     * @param number L'enter en qüestió.
     * @throws IOException
     */
    public void write_int32(int number) throws IOException {
        byte bytes[] = int32ToBytes(number, Endianness.BIG_ENNDIAN); //EL passem primer a bytes.
        dataOutputStream.write(bytes, 0, 4); //L'escrivim
    }


    /**
     * Aquest mètode llegeix byte a byte fins a arribar un 0. Els bytes llegits els interpreta com una string.
     *
     * @return Una string obtinguda dels bytes llegits.
     * @throws IOException
     */
    public String read_string0() throws IOException {
        String message = "";
        byte b = read1b();
        while (b != 0) {
            message = message + (char) b;
            b = read1b();
        }
        return message;
    }

    public void write_string0(String s) throws IOException {
        this.dataOutputStream.writeBytes(s);
        write1b((byte) 0);
    }


    /**
     * Aquest mètode és una mica més general que el seu igual sense paràmetre.
     * Llegim un nombre concret de caràcters, però el coneixem abans de començar (no el llegim de la capçalera)
     *
     * @param size el nombre de caracters que llegim
     * @return result, la string que hem llegit
     * @throws IOException
     */
    public String read_string(int size) throws IOException {
        String result; //Variable que ens serveix per emmagatzermar la String en questió.
        byte[] bStr;
        char[] cStr = new char[size];

        //Llegim els bytes.
        bStr = read_bytes(size);

        //Anem convertint els bytes a char.
        for (int i = 0; i < size; i++)
            cStr[i] = (char) bStr[i];

        result = String.valueOf(cStr); //Ho passem a string.
        return result;
    }


    /**
     * Ens permet escriure un string, escollint la seva mida però sense especificar-la (l'utilitzem a read i write de word i result, per exemple)
     *
     * @param str  El string que volem escriure.
     * @param size el nombre de caràcters a escriure
     * @throws IOException
     */
    public void write_string(String str, int size) throws IOException {

        char[] bytes = str.toCharArray();

        if (bytes.length != size) {
            throw new IOException("mida invalida");
        }

        dataOutputStream.writeBytes(str);
    }


    /**
     * Aquest mètode donat un enter el converteix en una llista de 4 bytes.
     *
     * @param number     Enter que volem passar a bytes.
     * @param endianness Un enum que ens permet saber el most significant byte (MSB).
     * @return Llista de bytes emmagatzemant el número.
     */
    private byte[] int32ToBytes(int number, Endianness endianness) {
        byte[] bytes = new byte[4];

        //En aquest cas tenim que el primer byte és el MSB.
        //Notem que treballem a nivell de bit. L'operador << desplaça els bits i | fa un or entre bits.
        if (Endianness.BIG_ENNDIAN == endianness) {
            bytes[0] = (byte) ((number >> 24) & 0xFF);
            bytes[1] = (byte) ((number >> 16) & 0xFF);
            bytes[2] = (byte) ((number >> 8) & 0xFF);
            bytes[3] = (byte) (number & 0xFF);
        } else {
            bytes[0] = (byte) (number & 0xFF);
            bytes[1] = (byte) ((number >> 8) & 0xFF);
            bytes[2] = (byte) ((number >> 16) & 0xFF);
            bytes[3] = (byte) ((number >> 24) & 0xFF);
        }
        return bytes;
    }

    /**
     * Aquest mètode donada una llista de 4 bytes retorna el enter corresponent.
     *
     * @param bytes      Llista dels bytes que emmagatzemen l'enter. Només es llegeixen els 4 primers.
     * @param endianness Un enum que ens permet saber el most significant byte (MSB).
     * @return L'enter en questió.
     */
    private int bytesToInt32(byte bytes[], Endianness endianness) {

        int number; //El nombre que estem llegint.

        //En aquest cas tenim que el primer byte és el MSB.
        //Notem que treballem a nivell de bit. L'operador << desplaça els bits i | fa un or entre bits.
        if (Endianness.BIG_ENNDIAN == endianness) {
            number = ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) |
                    ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
        } else {
            number = (bytes[0] & 0xFF) | ((bytes[1] & 0xFF) << 8) |
                    ((bytes[2] & 0xFF) << 16) | ((bytes[3] & 0xFF) << 24);
        }
        return number;
    }

    /**
     * Aquest mètode llegeix del dataInputStream el nombre de bytes indicat per paràmetre i en retorna una llista.
     *
     * @param numBytes nombre de bytes que volem llegir.
     * @return Una llista amb els bytes llegits.
     * @throws IOException Es llençarà quan volguem llegir més bytes dels disponibles.
     */
    private byte[] read_bytes(int numBytes) throws IOException {
        int len = 0; //Indica en quina posició de la llista de bytes hem d'escriure l'element.
        byte bStr[] = new byte[numBytes]; //Anirem guardant aquí els bytes que anem llegint.
        int bytesread = 0; //Ens permet saber cada vegada que accedim al fitxer el nombre de bytes llegits.
        do {
            bytesread = dataInputStream.read(bStr, len, numBytes - len); //Indiquem on deixem dades, a partir de quina posició i quantes volem.
            if (bytesread == -1)
                throw new IOException("Broken Pipe");
            len += bytesread; //Actualitzem la posició on hem d'escriure les noves dades.
        } while (len < numBytes);
        return bStr;
    }


    /**
     * Mètodes cridat des del constructor per inicialitzar els diccionaris utilitzats en el protocol.
     */
    private void createDics() {
        missatges = new HashMap<Integer, String>();
        codis = new HashMap<String, Integer>();
        missatges.put(1, "HELLO");
        missatges.put(2, "READY");
        missatges.put(3, "PLAY");
        missatges.put(4, "ADMIT");
        missatges.put(5, "WORD");
        missatges.put(6, "RESULT");
        missatges.put(7, "STATS");
        missatges.put(8, "ERROR");
        codis.put("HELLO", 1);
        codis.put("READY", 2);
        codis.put("PLAY", 3);
        codis.put("ADMIT", 4);
        codis.put("WORD", 5);
        codis.put("RESULT", 6);
        codis.put("STATS", 7);
        codis.put("ERROR", 8);
    }


    /**
     * Permet donada una string d'operació obtenir el seu codi.
     *
     * @param message String indicant la funcionalitat del codi.
     * @return Codi d'operació del protocol.
     */
    public int code(String message) {
        return codis.get(message);
    }

    /**
     * Permet accedir a la string clarificadora d'un codi.
     *
     * @param key Codi d'operació del protocol.
     * @return String indicant la funcionalitat del codi.
     */
    public String message(int key) {
        return missatges.get(key);
    }

    //Aquest enum ens permet saber el MostSignificantBite.
    public enum Endianness {
        BIG_ENNDIAN, //MSB a l'esquerra.
        LITTLE_ENDIAN //MSB a la dreta.
    }
}


