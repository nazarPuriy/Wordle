import java.io.*;
import java.util.*;

/**
 * Implementació del client automàtic.
 * Aquí trobareu totes les decisions que aquest pren automàticament,
 * incloent la selecció de la paraula fent un algoritme greedy que funciona força bé.
 */
public class ClientAutomatic extends Client {

    //Els següents atributs ajuden a seleccionar la paraula a cada torn

    //Llista de paraules del diccionari
    ArrayList<String> words;
    //Paraules que queden per provar a la partida actual
    ArrayList<String> wordsRestants;
    //Paraula que acabem d'enviar
    String sentWord;

    /**
     * Fem el mateix que en abstracte (super()) i inicialitzem les variables anteriors.
     * @param ip del servidor
     * @param port del servidor
     */
    public ClientAutomatic(String ip, int port){
        super(ip, port);

        //Inicialitztem el diccionari de paraules a provar
        initDiccionari();
        //Ordenem les paraules segons quines ens haurien de donar més informació
        sortDiccionari();
    }

    /**
     * En aquest cas el sessionId és 0 (no el tenim, en volem un de nou) i el nom és un per defecte.
     */
    @Override
    void setNomISessionId() {
        setSessionId(0);
        setNom("Client Automatic");
    }

    /**
     * Comencem la partida i per tant pot ser qualsevol paraula: cal actualitzar wordsRestants.
     */
    @Override
    void initPartida() {
        wordsRestants = words;
    }

    /**
     * Selecciona una paraula automàticament.
     * @return la paraula seleccionada.
     */
    @Override
    String selectWord() {

        String s;

        //per si de cas: Si no ens queden paraules per provar, enviem aquesta
        if(wordsRestants.isEmpty()) {
            s = "REINA";
        }else{
            //Seleccionem la primera paraula possible en la llitsta ordenada per preferència
            s = wordsRestants.get(0);
        }

        //Imprimim la paraula seleccionada
        System.out.println(s);

        //Actualitzem la paraula enviada
        sentWord = s;
        return s;
    }

    /**
     * Actualitzem la llista de paraules que poden ser.
     * @param res el resultat enviat pel servidor.
     */
    @Override
    void gestionaResult(String res) {

        //nova llista de paraules restants
        ArrayList<String> possibleWords = new ArrayList<>();

        //Per cada paraula restant,
        for(String word : wordsRestants){
            //Mirem si pot ser pel resultat rebut
            if(matchesReceivedPattern(word, sentWord, res)){
                //En tal cas, l'afegim a la nova llista
                possibleWords.add(word);
            }
        }

        //Actualitzem wordsRestants a la nova llista
        wordsRestants = possibleWords;
        System.out.println(wordsRestants.size() + " paraules possibles restants.");

    }

    /**
     * En cas de paraula errònia no la volem torna a enviar
     */
    @Override
    void gestionaParaulaErronia() {
        //Si ens queden paraules per enviar,
        if(!wordsRestants.isEmpty()){
            //Eliminem la paraula enviada
            wordsRestants.remove(0);
            //Al rebre un error, tornem a fer el mateix sense aquesta paraula
            changeToPrevEstat();
        }
    }

    /**
     * Fa exactament el mateix que el mètode anterior, de moment
     */
    @Override
    void gestionaCaracterInvalid() {

        if(!wordsRestants.isEmpty()){
            wordsRestants.remove(0);
            changeToPrevEstat();
        }
    }

    /**
     * Fem la gestió del login incorrecte.
     * Com hem enviat sessionId 0, si rebem aquest error és un problema del server.
     */
    @Override
    void gestionaLoginIncorrecte() {
        System.out.println("Error del servidor.");
        //Sortim
        changeEstat(Estat.FINAL);
    }

    /**
     * Decidim si sortim o no. En el cas automàtic, és fàcil:
     * @param default_option opció predeterminada, que és la que nosaltres agafem sempre.
     * @return la pròpia acció default escollida.
     */
    @Override
    boolean demanaSortir(boolean default_option) {
        if(default_option){
            changeEstat(Estat.FINAL);
        }
        return default_option;
    }

    /**
     * Load de totes les paraules que ens han donat des d'un fitxer.
     */
    private void initDiccionari(){

        words = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("DISC2-LP-WORDLE.txt")));
            String word;
            while ((word = br.readLine()) != null) {
                //Comprovem que el format de cada paraula sigui correcte.
                if(word.matches("[a-zA-Z]+") && word.length() == 5){
                    words.add(word.toUpperCase());
                }
            }
            br.close();

        } catch (IOException e) {
            System.out.println("No s'ha pogut trobar el fitxer de paraules.");
            changeEstat(Estat.FINAL);
        }
    }

    /**
     * Ordenem el diccionari de manera que apareixen abans les paraules amb més caràcters comuns.
     */
    private void sortDiccionari(){

        //Per anotar-nos quantes vegades surt cada caràcter
        HashMap<Character, Integer> frequencies = new HashMap<>();

        //Ho fem
        for(String s : words){
            for(char c: s.toCharArray()){
                if(frequencies.containsKey(c)){
                    frequencies.put(c, frequencies.get(c) + 1);
                }else{
                    frequencies.put(c, 1);
                }
            }
        }

        //Ordenem les paraules sota el següent criteri d'ordre:
        words.sort((w1, w2) -> {

            //Inicialitzem les puntuacions de cada paraula a 0
            int total1 = 0, total2 = 0;

            //Recorrem les lletres i, si una paraula conté una lletra,
            //sumem a la puntuació de la paraula la freqüència de la lletra.
            for (char c : frequencies.keySet()) {

                if (w1.indexOf(c) >= 0) {
                    total1 += frequencies.get(c);
                }

                if (w2.indexOf(c) >= 0) {
                    total2 += frequencies.get(c);
                }
            }

            //Restem en aquest ordre perquè així surten primer les que ens interessen (puntuació alta)
            return total2 - total1;
        });
    }

    /**
     * Comprova si una paraula encaixa amb el resultat rebut després d'enviar una altra paraula
     *
     * @param testWord la paraula que mirem si encaixa
     * @param sentWord la paraula que hem enviat
     * @param pattern el result que hem rebut
     * @return true, si la paraula testWord encaixa, false si no.
     */
    static boolean matchesReceivedPattern(String testWord, String sentWord, String pattern){

        //Aparicions, com a mínim, de cada caràcter a testWord
        HashMap<Character, Integer> test = new HashMap<>();
        //Aparicions, com a mínim, de cada caràcter a sentWord
        HashMap<Character, Integer> real = new HashMap<>();
        //Caràcters dels que sabem el nombre exacte de repeticions
        Set<Character> knowHowMany = new HashSet<>();

        //Recorrem totes les posicions
        for(int i = 0; i < testWord.length(); i++){

            if(pattern.charAt(i) == '^'){
                //Si hem encertat el caràcter, però no coincideix amb testWord, no pot ser testWord.
                if(testWord.charAt(i) != sentWord.charAt(i)){
                    return false;
                }

            }else{
                //A l'inrevés: Si sabem que no hem encertat, els caràcters en aquesta posició han de ser diferents.
                if(testWord.charAt(i) == sentWord.charAt(i)){
                    return false;
                }
            }

            //Si no hi ha match de cap mena, sabem exactament quantes vegades apareix el caràcter enviat:
            //tantes com hi aparegui amb '?' o '^' al result.
            if(pattern.charAt(i) == '*'){
                knowHowMany.add(sentWord.charAt(i));
            }
            else{
                //Si hi ha alguna mena d'encert (encara que sense posició correcta):
                //Augmentem el mínim de vegades que apareix aquell caràcter
                if(real.containsKey(sentWord.charAt(i))){
                    real.put(sentWord.charAt(i), real.get(sentWord.charAt(i)) + 1);
                }else{
                    real.put(sentWord.charAt(i), 1);
                }
            }
        }

        //Actualitzem els valors que apareixen a la paraula que testegem.
        for(int i = 0; i < testWord.length(); i++){
            if(test.containsKey(testWord.charAt(i))){
                test.put(testWord.charAt(i), test.get(testWord.charAt(i)) + 1);
            }else{
                test.put(testWord.charAt(i), 1);
            }
        }

        //Recorrem les lletres que sabem que apareixen.
        //Si apareixen més (confirmats) a la paraula enviada que a la que testegem, no hi ha match possible
        for(char c : real.keySet()){

            if(!test.containsKey(c)){
                return false;
            }

            if(real.get(c) > test.get(c)){
                return false;
            }

        }

        //Mirem la igualtat estricta si sabem exactament quantes vegades apareix.
        for (Character c : knowHowMany) {

            if (real.containsKey(c) ^ test.containsKey(c)) {
                return false;
            }

            if (!Objects.equals(real.get(c), test.get(c))) {
                return false;
            }
        }

        return true;
    }

}
