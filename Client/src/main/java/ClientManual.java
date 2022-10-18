import java.text.Normalizer;
import java.util.Scanner;

/**
 * Implementació del client manual.
 * Aquí trobareu tota la interacció amb l'usuari que no es fa en mode automàtic:
 * prints i captures de l'input per consola.
 */
public class ClientManual extends Client{

    //El que escrivim per consola
    private Scanner keyboard;
    //Ens diu si ja hem iniciat sessió o no, per mantenir-la en cas afirmatiu
    private boolean login;

    /**
     * Fem el mateix que en abstracte (super()) i inicialitzem les variables anteriors.
     * @param ip del servidor
     * @param port del servidor
     */
    public ClientManual(String ip, int port){

        super(ip, port);
        keyboard = new Scanner(System.in);
        login = false;

    }

    /**
     * Per a l'inici de sessió manual. Llegim de consola.
     */
    @Override
    void setNomISessionId() {
        if(!login){
            System.out.println("Introdueix el teu nom:");
            setNom(keyboard.nextLine());
            setSessionId(getSessionIdManual());
        }
        login = true;
    }

    /**
     * No cal inicialitzar res perquè totes les decisions les pren el jugador.
     */
    @Override
    void initPartida() {

    }


    /**
     * Demanem a l'usuari que introdueixi la paraula.
     * @return la paraula introduïda per consola.
     */
    @Override
    String selectWord() {
        System.out.println("Introdueix la paraula que vols provar:");
        String s = keyboard.nextLine();

        //Comprovem que tingui 5 lletres
        if(s.length() == 5){
            //retornem la paraula, sense accents i reemplaçant ç per c, en majúscules, per si algun servidor no ho passa a majúscules
            return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").toUpperCase();
        }else{
            //Si no, avisem i no canviem d'estat, per tornar a executar aquesta funció
            System.out.println("La paraula ha de tenir 5 lletres.");
            return selectWord();
        }
    }

    /**
     * Totes les decisions les fa el jugador i per tant no cal fer res.
     * @param result el resultat que ens ha enviat el servidor.
     */
    @Override
    void gestionaResult(String result) {
        //no cal fer res
    }

    /**
     * Error d'input de la paraula, demanem a l'usuari que ho repeteixi i tornem a l'estat anterior.
     */
    @Override
    void gestionaParaulaErronia() {
        System.out.println("Introdueix una paraula del diccionari.");
        changeToPrevEstat();

    }

    /**
     * Error d'input d'un caràcter inesperat, demanem a l'usuari que ho repeteixi i tornem a l'estat anterior.
     */
    @Override
    void gestionaCaracterInvalid() {
        System.out.println("Introdueix caràcters vàlids.");
        changeToPrevEstat();
    }

    /**
     * Error al fer login: Tornem a iniciar sessió.
     */
    @Override
    void gestionaLoginIncorrecte() {
        login = false;
        changeEstat(Estat.INICI);
    }

    /**
     * Demanem a l'usuari si vol sortir del joc.
     * @param default_option opció predeterminada, que en aquest cas ignorem.
     * @return true, si l'usuari decideix sortir, false si no.
     */
    @Override
    boolean demanaSortir(boolean default_option) {
        System.out.println("Vols sortir? S/N");
        String sortir = keyboard.nextLine();

        //Si ens diu que sí, retornem true i anem a l'estat final
        if(sortir.equals("s") || sortir.equals("S")){
            changeEstat(Estat.FINAL);
            return true;
        }

        //Si ens diu que no, retornem false
        if(sortir.equals("n") || sortir.equals("N")){
            return false;
        }

        //Si s'introdueix una altra cosa, Informem de l'error i tornem a provar
        System.out.println("Opció invàlida");
        return demanaSortir(default_option);
    }

    /**
     * Mètode per obtenir un sessionID per consola (semblant a demanaSortir)
     * @return el sessionId obtingut.
     */
    private int getSessionIdManual(){
        //Implementació molt semblant al mètode anterior
        System.out.println("Escriu S si tens un SessionId assignat, o N en cas contrari.");
        String b = keyboard.nextLine();

        if(b.equals("s") || b.equals("S")) {
            System.out.println("Introdueix el sessionId:");
            int n = keyboard.nextInt();
            keyboard.nextLine();
            if(n == 0){
                System.out.println("El sessionId no pot ser 0");
                return getSessionIdManual();
            }else{
                return n;
            }
        }else if (b.equals("n") || b.equals("N")){
            return 0;
        }else{
            System.out.println("Opció invàlida.");
            return getSessionIdManual();
        }
    }
}
