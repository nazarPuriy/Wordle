import org.json.simple.JSONObject;
import utils.ComUtils;

import java.io.*;
import java.net.Socket;

/**
 * Aquesta classe és una versió abstracta del nostre client.
 *
 * Proporciona totes les funcions de comunicació específiques d'aquest (notem que no importem ComUtils a les classes filles).
 *
 * També proporciona la lògica bàsica del joc i la navegació entre els diferents estats mostrats a l'Enum Estats.
 *
 * La interacció amb el jugador (o agent automàtic) es deixa a les classes filles, en un cas (ClientManual) serà per mitjà de la consola,
 * i en l'altre serà un algorisme que hem dissenyat qui prendrà totes les decisions. Tots els mètodes que interactuen amb un possible usuari
 * s'han deixat per tant en abstracte.
 *
 */
public abstract class Client{

    //Tots els estats en què es pot trobar el client
    enum Estat{

        //Encara ens hem de connectar amb el server
        CONNECT,

        //Iniciar sessió (enviar hello)
        INICI,

        //Esperem que el servidor ens digui que està llest
        WAITING_READY,

        //Sabem que el servidor està llest i volem jugar
        SERVER_READY,

        //Esperem que el servidor admeti la partida
        WAITING_ADMIT,

        //Podem enviar una paraula
        PLAYING,

        //Hem jugat una paraula i el servidor ens ha d'enviar el resultat
        WAITING_RESULT,

        //Hem acabat la partida i ens han d'enviar les nostres estadístiques després d'aquesta
        WAITING_STATS,

        //Esperem que el servidor ens reveli la paraula secreta
        WAITING_WORD,

        //Ens desconnectem
        FINAL
    }

    Socket socket;

    //Estat actual
    private Estat estat;
    //Estat previ, per a la gestió d'errors
    private Estat prevEstat;

    //Nombre d'intents restants a la partida actual
    private int restants;

    //Nom del jugador
    private String nom;
    //SessionId al server
    private int sessionId;
    //Per a la comunicació amb el server
    private ComUtils c;

    //IP i port als que ens hem de connectar
    private String serverIp;
    private int serverPort;


    /**
     * Constructor bàsic de Client.
     *
     * @param ip del server
     * @param port del server
     */
    public Client(String ip, int port){

        //Per defecte, sessionId és 0 (segons el protocol, significa que no en tenim)
        setSessionId(0);

        //El primer que farem serà connectar-nos al server
        estat = Estat.CONNECT;
        prevEstat = Estat.CONNECT;

        //Per tenir alguna cosa, les implementacions concretes se n'encarreguen
        setNom("");

        //Posem la ip i port als valors que toquen
        serverIp = ip;
        serverPort = port;

    }

    /**
     * Bucle que crida a les diferents funcions depenent de l'estat en què som
     */
    public void run(){

        //Si l'estat és FINAL, no continuem i ens desconnectem
        while(estat != Estat.FINAL){

            try {

                //Si hem d'enviar alguna cosa, anem a la funció corresponent
                switch (estat) {

                    case CONNECT:
                        //Ens connectem
                        connect();
                        break;

                    case INICI:
                        //Comprovem que no ens hagin enviat res
                        errorIfNotEmpty();
                        //Iniciem sessió
                        inicia();
                        break;

                    case SERVER_READY:
                        //Comprovem que no ens hagin enviat res
                        errorIfNotEmpty();
                        //Iniciem partida
                        start_partida();
                        break;

                    case PLAYING:
                        //Comprovem que no ens hagin enviat res
                        errorIfNotEmpty();
                        //Enviem una paraula
                        word();
                        break;

                    default:
                        //Si no és cap dels anteriors, esperem que el server ens enviï un paquet
                        read();
                        break;
                }

            //Timeout del servidor: Al carrer
            }catch (InterruptedIOException i) {

                System.out.println("Servidor absent.");
                if(!demanaSortir(true)){
                    changeEstat(Estat.CONNECT);
                }


            }

            //Error de ComUtils, caldrà tornar-se a connectar o donar-ho per perdut i acabar
            catch(IOException e){
                e.printStackTrace();
                if(!demanaSortir(true)){
                    changeEstat(Estat.CONNECT);
                }
            }
        }
        System.out.println("Fi de la sessió. Adéu.");
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Crea el Socket, obté els seus Streams i amb aquests crea un objecte ComUtils, que utilitzem per enviar i rebre paquets
     * @throws IOException per les funcions de comUtils o per error del Socket
     */
    private void connect() throws IOException {
        socket = new Socket(serverIp, serverPort);
        //Com a molt 30 segons d'espera al servidor
        socket.setSoTimeout(30 * 1000);
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        DataInputStream dIn = new DataInputStream(socket.getInputStream());
        this.c = new ComUtils(dIn, dOut);
        //Llestos per enviar HELLO
        changeEstat(Estat.INICI);
    }

    /**
     * Mètode per iniciar sessió
     * Obté el nom i sessionID i envia el paquet HELLO corresponent
     * @throws IOException per les funcions de comUtils
     */
    private void inicia() throws IOException {
        //Obtenim el nom i sessionId que calgui (depèn del tipus de Client)
        setNomISessionId();
        //Enviem el paquet HELLO
        c.sendHello(getSessionId(), getNom());
        //Esperem que ens contesti amb el paquet READY
        changeEstat(Estat.WAITING_READY);
    }

    /**
     * Inicialitza la partida, i avisa al Server enviant PLAY
     * @throws IOException per les funcions de comUtils
     */
    private void start_partida() throws IOException {
        //Inicialitzacions que es facin a principi de partida, sobretot per client automàtic
        initPartida();
        //Tenim 6 intents
        restants = 6;
        System.out.println("Comencem la partida");
        //Enviem el paquet PLAY
        c.sendPlay(getSessionId());
        //Esperem que ens enviïn ADMIT
        changeEstat(Estat.WAITING_ADMIT);
    }

    /**
     * Selecciona una paraula, s'assegura que el seu format sigui correcte i l'envia
     * @throws IOException per les funcions de comUtils
     */
    private void word() throws IOException {

        System.out.println(restants + " intents restants.");

        //Seleccionem una paraula, el com depèn del tipus de Client, i l'enviem
        c.sendWord(selectWord());
        //Esperem rebre un RESULT
        changeEstat(Estat.WAITING_RESULT);

    }

    /**
     * Gestionem un paquet READY del qual només hem llegit l'OPCODE
     * @return true, si esperàvem aquest paquet, false en cas contrari
     * @throws IOException per les funcions de comUtils
     */
    private boolean readReady() throws IOException {

        //Llegim el camp SESSIONID
        int s = c.read_int32();
        if(estat == Estat.WAITING_READY) {
            //Canviem el nostre sessionId (útil si no en teníem i hem enviat 0).
            setSessionId(s);
            System.out.println("El teu SessionId és: " + getSessionId());
            //El servidor està llest
            changeEstat(Estat.SERVER_READY);
            return true;
        }
        return false;
    }

    /**
     * Gestionem un paquet ADMIT del qual només hem llegit l'OPCODE
     * @return true, si esperàvem aquest paquet, false en cas contrari
     * @throws IOException per les funcions de comUtils
     */
    private boolean readAdmit() throws IOException {
        //llegim el booleà
        boolean admit = c.readAdmit();
        if(estat == Estat.WAITING_ADMIT){

            //Si ens admeten, ja podem jugar
            if(admit){
                changeEstat(Estat.PLAYING);
            }else{
                System.out.println("El servidor no ha acceptat la solicitud de jugar.");
                //Si el client ho vol tornar a intentar, haurem d'enviar PLAY un altre cop
                if(!demanaSortir(true)){
                    changeEstat(Estat.SERVER_READY);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Gestionem un paquet RESULT del qual només hem llegit l'OPCODE
     * @return true, si esperàvem aquest paquet, false en cas contrari
     * @throws IOException per les funcions de comUtils
     */
    private boolean readResult() throws IOException {

        //Obtenim la String que ens han enviat, de 5 caràcters
        String res = c.read_string(5);
        if(estat == Estat.WAITING_RESULT) {

            System.out.println(res);
            //Tenim un intent menys
            restants -= 1;

            //En aquest cas hem guanyat i esperem rebre STATS
            if (res.equals("^^^^^")) {
                System.out.println("Has guanyat!");
                changeEstat(Estat.WAITING_STATS);
            }

            //En aquest altre, hem perdut i esperem rebre la WORD correcta
            else if (restants == 0) {
                System.out.println("Has perdut :(");
                changeEstat(Estat.WAITING_WORD);
            }

            //Si no és cap dels casos anteriors, podem seguir jugant
            else{
                //Gestionem el resultat, sobretot pel client automàtic
                gestionaResult(res);
                changeEstat(Estat.PLAYING);
            }

            return true;
        }

        return false;

    }

    /**
     * Gestionem un paquet WORD del qual només hem llegit l'OPCODE
     * @return true, si esperàvem aquest paquet, false en cas contrari
     * @throws IOException per les funcions de comUtils
     */
    private boolean readWord() throws IOException {
        //Llegim la paraula
        String w = c.read_string(5);
        if(estat == Estat.WAITING_WORD){
            System.out.println("La paraula era: " + w);
            //Hem perdut i tenim la paraula: esperem STATS
            changeEstat(Estat.WAITING_STATS);
            return true;
        }
        return false;
    }

    /**
     * Gestionem un paquet STATS del qual només hem llegit l'OPCODE
     * @return true, si esperàvem aquest paquet, false en cas contrari
     * @throws IOException per les funcions de comUtils
     */
    private boolean readStats() throws IOException {

        //TODO guardar-lo?
        JSONObject j = c.getStats();

        if(estat == Estat.WAITING_STATS){

            //Cas del JSON mal format. Com no talla la partida ni és essencial, continuem com si res
            if(j == null){
                System.out.println("No es poden mostrar les estadístiques.");
                c.sendError(6, "El JSON no es correcte.");
            }else{
                System.out.println(j);
            }

            //Veiem si el Client decideix fer una altra partida.
            if(!demanaSortir(false)){
                changeEstat(Estat.SERVER_READY);
            }
            return true;

        }
        return false;

    }

    /**
     * Mètode per llegir un error i derivar la seva gestió al mètode abstracte corresponent.
     * (utilitzem mètodes abstractes perquè normalment la gestió d'errors implica input de l'usuari, o bé una decisió automàtica).
     * No enviem cap error perquè podríem entrar en bucle depenent de la implementació del servidor Intentem resoldre l'error i si no tanquem la sessió.
     * @throws IOException per les funcions de comUtils
     */
    private void readError() throws IOException {
        //

        int code = c.read1b();
        String message = c.read_string0();
        System.out.println("Error: " + message);

        switch (code) {

            case 1:
                //Caràcter invàlid
                gestionaCaracterInvalid();
                break;

            case 5:
                //Paraula desconeguda
                gestionaParaulaErronia();
                break;

            case 4:
                //Login incorrecte
                gestionaLoginIncorrecte();
                break;

            default:
                //En la resta de casos, el servidor no ha canviat d'estat -> nosaltres tampoc, en principi l'error no és nostre. Per defecte sortim.
                if (!demanaSortir(true)) {
                    changeToPrevEstat();
                }
                break;
        }

    }

    /**
     * Llegim un paquet entrant. Comprovarem que sigui l'esperat i en tal cas el gestionem.
     * Si no ho és, enviem el paquet d'error corresponent i ens desconnectem.
     * @throws IOException al llegir utilitzant comUtils
     */
    private void read() throws IOException {

        //Llegim OPCODE
        int code = c.readOpCode();

        //Si ens envien un paquet desconegut, ens desconnectem enviant primer un error.
        if(code < 1 || code > 8){
            c.sendError(2, "Missatge desconegut.");
            System.out.println("Error del servidor: missatge desconegut.");
            changeEstat(Estat.FINAL);
            return;
        }

        boolean expected = false;
        String mes = c.message(code);

        //Depenent de l'opcode, llegim el que sigui. Comprovem en cada cas que correspongui amb l'estat actual
        switch (mes){

            case "READY":
                expected = readReady();
                break;

            case "ADMIT":
                expected = readAdmit();
                break;

            case "RESULT":
                expected = readResult();
                break;

            case "WORD":
                expected = readWord();
                break;

            case "STATS":
                expected = readStats();
                break;

            case "ERROR":
                //Always expect the unexpected
                //No enviem cap error perquè podriem entrar en bucle depenent de la implementació del servidor.
                expected = true;
                readError();

        }

        //Si ens envien un paquet que no toca, ens desconnectem enviant primer un error.
        if(!expected){
            c.sendError(3, "Error de protocol: No toca aquest paquet.");
            System.out.println("Error de protocol del servidor.");
            changeEstat(Estat.FINAL);
        }
    }

    /**
     * Canviem l'estat al passat per paràmetre, actualitzant tant l'estat actual com l'anterior
     * @param e l'estat al que volem canviar
     */
    void changeEstat(Estat e){
        prevEstat = estat;
        estat = e;
    }

    /**
     * Canviem a l'estat anterior
     */
    void changeToPrevEstat(){
        changeEstat(prevEstat);
    }

    /**
     * Si, a l'acabar de llegir un paquet, queden bytes per llegir, ens han enviat massa bytes
     * @throws IOException per l'ús de ComUtils per a la comunicació
     */
    private void errorIfNotEmpty() throws IOException {
        //mal format: arriben massa bytes
        if(!c.checkEmpty()){
            c.sendError(6, "El missatge es massa llarg.");
            System.out.println("Error del servidor.");
            changeEstat(Estat.FINAL);
        }
    }

    /**
     * Inicialitza la partida, depèn de la implementació concreta
     */
    abstract void initPartida();

    /**
     * Selecciona una paraula, depèn de la implementació concreta de Client
     * @return la paraula seleccionada
     */
    abstract String selectWord();

    /**
     * Gestiona l'error de paraula desconeguda, depèn de la implementació concreta de Client
     */
    abstract void gestionaParaulaErronia();

    /**
     * Gestiona l'error de caràcter desconegut, depèn de la implementació concreta de Client
     */
    abstract void gestionaCaracterInvalid();

    /**
     * Gestiona l'error de login incorrecte, depèn de la implementació concreta de Client
     */
    abstract void gestionaLoginIncorrecte();

    /**
     * Gestiona el resultat rebut després de jugar una paraula, actualitzant el propi estat.
     * Depèn de la implementació concreta de Client.
     * Sobretot és útil a ClientAutomatic
     * @param result el resultat que ens ha enviat el servidor
     */
    abstract void gestionaResult(String result);

    /**
     * Determina si l'usuari vol sortir, i en tal cas canvia l'estat a FINAL. Es podria deure al final de la partida o a un error.
     * Depèn de la implementació concreta de Client.
     * @param default_option opció predeterminada, que és la que agafarà sempre el ClientAutomatic.
     * @return si al final sortim de la partida o no.
     */
    abstract boolean demanaSortir(boolean default_option);

    /**
     * Obté el nom i sessionId del Client.
     * Depèn de la implementació concreta de Client.
     */
    abstract void setNomISessionId();

    /**
     * Getter de nom
     * @return el nostre nom
     */
    public String getNom() {
        return nom;
    }

    /**
     * Setter de nom
     * @param nom el nom que volem utilitzar
     */
    public void setNom(String nom) {
        this.nom = nom;
    }

    /**
     * Getter de sessionId
     * @return el nostre sessionId
     */
    public int getSessionId() {
        return sessionId;
    }

    /**
     * Setter de sessionId
     * @param sessionId la ID que volem utilitzar
     */
    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }
}

