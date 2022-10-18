import java.io.*;
import java.net.Socket;
import utils.ComUtils;
import org.json.simple.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Server extends Thread{

    public enum estados{START,READY,GAME}
    private estados estado;
    private ComUtils comUtils;
    HashMap<Integer, UserStats> usersStats;
    HashMap<Integer, String> usersNames;
    ArrayList<String> words;
    BufferedWriter outputLog;

    //Variables de lògica.
    private UserStats userStats;
    private String word = ""; //Paraula que l'usuari ha d'adivinar.
    private int sessionId;
    private int attempt;
    private String message;

    private Socket socket;


    /***
     * Inicialitzador de la classe servidor.
     * @param s Socket que la propia classe gestiona.
     * @param usersNames Hashmap que permet identificar ids d'usuaris amb noms iq ue és compartit entre els diferents threads.
     * @param usersStats Estadístiques d'usuari que s'accedeixen mitjançant l'identificador.
     * @param words Diccionari amb les paraules disponibles.
     * @throws IOException Excepcions provinents del comUtils relacionades amb la connexió.
     */
    public Server(Socket s, HashMap<Integer, String> usersNames,HashMap<Integer,UserStats> usersStats,ArrayList<String> words) throws IOException{

        this.usersStats = usersStats;
        this.usersNames = usersNames;
        this.words = words;
        this.socket = s;
        estado = estados.START;
        comUtils = new ComUtils(s.getInputStream(),s.getOutputStream());
        String logName = "Server"+currentThread().getName()+".log";
        outputLog = new BufferedWriter(new FileWriter(logName, true));
        outputLog.append("\n\nC- [TCP Connect]\nS- [TCP Accept]\n\n");
        outputLog.flush();

    }


    public Server(){} //Constructor sense paràmetres per tal de provar el mètode matchString en els tests de forma fàcil.


    /***
     * Mètode que s'executa automàticament en crear una instància de Server al ser aquest una extensió de Thread.
     * Aquest mètode gestiona totes les comunicacions amb UN client determinat fins que aquest decideix tancar la sessió.
     */
    public void run() {

        try {

            while(true){

                //Anem canviant els estats del servidor.
                switch (estado){


                    case START:

                        int opCode = comUtils.readOpCode();

                        if(opCode == 8){logError();break;} //Ens ha arribat un error. El servidor no fa res.

                        if(opCode >8 || opCode == 0) {
                            message="ERROR  C -------8 " + " 2 " + "L'operation code no correspon a cap missatge conegut";
                            regLog(true,true,true);
                            comUtils.sendError(2,"L'operation code no correspon a cap missatge conegut");

                        }else if(opCode != 1){
                            message="ERROR  C -------8 " + " 3 " + "Espero un session ID i un nom de jugador";
                            regLog(true,true,true);
                            comUtils.sendError(3,"Espero un session ID i un nom de jugador");

                        }else{startService();} //Cridem la dinàmica del start.

                        break;


                    case READY:

                        opCode = comUtils.readOpCode();

                        if(opCode == 8){logError();break;} //Ens ha arribat un error. El servidor no fa res.

                        if(opCode >8 || opCode == 0) {
                            message="ERROR  C -------8 " + " 2 " + "L'operation code no correspon a cap missatge conegut";
                            regLog(true,true,true);
                            comUtils.sendError(2,"L'operation code no correspon a cap missatge conegut");

                        }else if(opCode != 3){
                            message="ERROR  C -------8 " + " 3 " + "Espero que em confirmis un inici de partida";
                            regLog(true,true,true);
                            comUtils.sendError(3,"Espero que em confirmis un inici de partida");}

                        else{ready();} //Cridem la dinàmica del ready.

                        break;


                    case GAME:

                        opCode = comUtils.readOpCode();

                        if(opCode == 8){logError();break;} //Ens ha arribat un error. El servidor no fa res.

                        if(opCode >8 || opCode == 0) {
                            message="ERROR  C -------8 " + " 2 " + "L'operation code no correspon a cap missatge conegut";
                            regLog(true,true,true);
                            comUtils.sendError(2,"L'operation code no correspon a cap missatge conegut");

                        }else if(opCode != 5) {
                            message="ERROR  C -------8 " + " 3 " + "Espero rebre una paraula";
                            regLog(true,true,true);
                            comUtils.sendError(3, "Espero rebre una paraula");

                        }else{game();} //Cridem la dinàmica del game.

                        break;
                }
            }

        //Timeout
        }catch (InterruptedIOException i){

            //Si l'usuari triga 1 hora ens esconnectem i li sumem una derrota.
            if (estado == estados.GAME) {
                userStats.addLoss();
            }


            //Guardem al log que estem sortint.
            try {
                outputLog.append("\nS- [conexion closed]\nC- [conexion closed]");
                outputLog.flush();
                outputLog.close();
                socket.close();

            }

            catch (IOException io2) {
                System.out.println("S'ha produït algun error amb el fitxer de log!");
            }

            return;

        }

        catch (IOException io) {

            //Si l'usuari tanca una partida mentre està en ella li sumem una derrota.
            if (estado == estados.GAME) {
                userStats.addLoss();
            }

            //Guardem al log que estem sortint.
            try {
                outputLog.append("\nC- [conexion closed]\nS- [conexion closed]");
                outputLog.flush();
                outputLog.close();
                socket.close();
            }

            catch (IOException io2) {
                System.out.println("S'ha produït algun error amb el fitxer de log!");
            }

        }

    }


    /***
     * Aquest mètode s'encarrega d'acabar de llegir el missatge de Hello.
     * Realitza les tasques necessàries per comprovar l'existència de sessionId o sessionId-userName.
     * S'encarrega també d'obtenir tota aquesta informació o guardar-la en els hashmaps.
     * @throws IOException Excepcions provinents del comUtils relacionades amb la connexió.
     */
    private void startService() throws IOException{

        //Llegim les dades.
        sessionId = comUtils.read_int32();
        String name = comUtils.read_string0();
        message = "HELLO C -------1 " + sessionId + " " + name + " 0";
        regLog(true,false,false);

        //Mirem si volem generar una nova sessió.
        if(sessionId == 0){
            //Hem de realitzar l'operació d'assignació de sessionId de forma sincrona entre els threads.
            synchronized(this){
                sessionId = usersNames.size() + 1; //Generem un sessionID
            }

            usersNames.put(sessionId, name);
            userStats = new UserStats();
            usersStats.put(sessionId,userStats);

        }else{
            //Si volem entrar a una sessió que no existeix retornem un missatge d'error.
            if(!usersNames.containsKey(sessionId)) {
                message="ERROR  C <------8 " + " 4 " + "El sessionId no existeix";
                regLog(false,true,true);
                comUtils.sendError(4,"El sessionId no existeix");
                return;
            }

            String idName = usersNames.get(sessionId);

            //Comprovem que el session ID correspon amb el nom de l'usuari.
            if(!idName.equals(name)){
                message="ERROR  C <------8 " + " 4 " + "Assignació de session ID no correspon al nom del jugador. Torna a enviar les dades";
                regLog(false,true,true);
                comUtils.sendError(4, "Assignacio de session ID no correspon al nom del jugador. Torna a enviar les dades");
                return;

            }else{
                userStats = usersStats.get(sessionId);
            }
        }

        estado = estados.READY;
        message = "READY C <------2 " + sessionId;
        regLog(false,false,true);
        comUtils.sendReady(sessionId);

    }


    /***
     * Mètode encarregat de tornar a comprovar que el número de sessió sigui correcte, per confirmar que l'usuari
     * ha rebut el seu sessionID.
     * S'encarrega també de generar la paraula i reiniciar la partida.
     * @throws IOException Excepcions provinents del comUtils relacionades amb la connexió.
     */
    private void ready() throws IOException{

        if(sessionId!=comUtils.read_int32()){
            message = "ADMIT C <------4 0";
            regLog(false,false,true);
            comUtils.sendAdmit(false);

        }else {
            message = "PLAY  C -------3 " + sessionId;
            regLog(true,false,false);
            int randomNum = ThreadLocalRandom.current().nextInt(0, words.size());
            word = words.get(randomNum);
            attempt = 0;
            estado = estados.GAME;
            message = "ADMIT C <------4 1";
            regLog(false,false,true);
            comUtils.sendAdmit(true);
        }

    }


    /***
     * Mètode encarregat de gestionar els asserts quan es prediu una paraula.
     * @throws IOException Excepcions provinents del comUtils relacionades amb la connexió.
     */
    private void game() throws IOException{

        //Paraula que l'usuari utilitzarà per adivinar.
        String gWord = comUtils.read_string(5).toUpperCase();
        message = "WORD   C -------5 " + gWord;
        regLog(true,false,false);
        //Comprovem si la paraula contè caràcters vàlids.

        if(!gWord.matches("[a-zA-Z]+")){
            message="ERROR  C <------8 " + " 1 " + "La paraula conte un caracter no valid";
            regLog(false,true,true);
            comUtils.sendError(1,"La paraula conte un caracter no valid");}

        else if(!words.contains(gWord)){
            message="ERROR  C <------8 " + " 5 " + "La paraula no es troba al diccionari";
            regLog(false,true,true);
            comUtils.sendError(5,"La paraula no es troba al diccionari");}

        else{
            //Paraula que representa els encerts realitzats per l'usuari.
            String sWord = matchString(word, gWord);
            attempt += 1;
            //Enviem el resultat que ha obtingut.
            comUtils.sendResult(sWord);
            message = "RESULT C <------6 " + sWord;
            regLog(false,false,true);
            //Comprovem si el jugador ha guanyat o acabat el nombre d'intents.
            JSONObject stats;

            if(sWord.equals("^^^^^")){
                 //L'accés als stats ha de realitzar-se de forma síncrona.
                 synchronized(this) {
                     userStats.addWin(attempt);
                     stats = userStats.getJSON();
                     userStats.logJSON(outputLog);
                 }

                 estado = estados.READY;
                 comUtils.sendStats(stats);

             }else if(attempt == 6){
                 comUtils.sendWord(word);
                 message = "WORD   C <------6 " + word;
                 regLog(false,false,true);

                 //L'accés als stats ha de realitzar-se de forma síncrona.
                 synchronized(this) {
                     userStats.addLoss();
                     stats = userStats.getJSON();
                     userStats.logJSON(outputLog);
                 }

                 outputLog.flush();
                 estado = estados.READY;
                 comUtils.sendStats(stats);
             }

         }

    }


    /**
     * Mètode encarregat de retornar la string de coincidències en el format indicat en el protocol.
     * @param s1 paraula a predir.
     * @param s2 paraula predita.
     * @return es retorna la string de coincidències.
     */
    public String matchString(String s1,String s2){

        String s3 = "      "; //String on guardarem el resultat.
        HashMap<Character,Integer> opt = new HashMap<>();

        //Primer matches directes.
        for(int i =0; i<5;i++){

            if(s1.charAt(i) == s2.charAt(i)){
                s3 = s3.substring(0,i)+"^"+s3.substring(i+1,5);
                s1 = s1.substring(0,i)+"0"+s1.substring(i+1,5); //Ens servirà per indicar després que no hem d'utilitzar aquesta posició.

            }else{

                //Anem guardant possibles matches de posicions incorrectes.
                if(opt.containsKey(s1.charAt(i))){
                    opt.put(s1.charAt(i),opt.get(s1.charAt(i))+1);
                }else{
                    opt.put(s1.charAt(i),1);
                }

            }
        }

        //Matches, però no de posició no correcta. Notes que les lletres utilitzades estan en el diccionari opt.
        for(int i =0; i<5;i++){

            if(s1.charAt(i) != '0' && opt.containsKey(s2.charAt(i)) && 0!=opt.get(s2.charAt(i))){
                opt.put(s2.charAt(i),opt.get(s2.charAt(i))-1);
                s3 = s3.substring(0,i)+"?"+s3.substring(i+1,5);

            }else if(s1.charAt(i) != '0'){
                s3 = s3.substring(0, i) + "*" + s3.substring(i + 1, 5);
            }

        }

        return s3;

    }


    /***
     * Mètode de suport per donar un style als logs que sigui visualment agradable.
     * @param dirSer Boleà que indica si el missatge és enviat cap el servidor.
     * @param errorMP Boleà que indica si el missatge és de tipus error.
     * @param flush Boleà que indica si hem d'escriure immediatament el missatge en l'arxiu log.
     * @throws IOException Excepcions provinents del comUtils relacionades amb la connexió.
     */
    private void regLog(boolean dirSer,boolean errorMP, boolean flush) throws IOException{

        //Aquest mètode no conté comentaris ja que no és de gran interès per l'assignatura (pure Style).
        int logLineSize = 38;

        if(errorMP){
            logLineSize = 80;
        }

        int actualSize = message.length();
        int fill = logLineSize - actualSize - 4;
        message+=" ";

        for(int i=0; i<fill; i++) message += "-";

        if(dirSer){
            message+="> S\n";

        }else{
            message+="- S\n";

        }
        outputLog.append(message);

        if(flush){
            outputLog.flush();
        }

    }


    /***
     * Aquest mètode serveix per acabar de llegir els missatges d'error. Tot i que fem cap acció envers aquest si que
     * hem de guardar el seu contingut en el log del servidor.
     * @throws IOException Excepcions provinents del comUtils relacionades amb la connexió.
     */
    private void logError() throws IOException{

        int numError = comUtils.read_int32();
        String nameError = comUtils.read_string0();
        message = "ERROR  C -------8 "+numError+" "+nameError;
        regLog(true,true,true);

    }

}
