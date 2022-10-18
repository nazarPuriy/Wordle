import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;

public class UserStats {

    private int jugades;
    private int jugadesGuanyades;
    private float exits;
    private int ratxaAct;
    private int ratxaMax;
    private final int[] victories;


    /**
     * Inicialitzador de la classe stats. Aquesta classe serveix per guardar les estadístiques dels diferents usuaris,
     * passar aquestes a formats JSON o String per el log.
     */
    public UserStats(){

        jugades = 0;
        jugadesGuanyades = 0;
        exits = 100.0f;
        ratxaAct = 0;
        ratxaMax = 0;
        victories = new int[] {0, 0, 0, 0, 0, 0};

    }


    /**
     * Afegeix una derrota a les estadístiques de l'usuari.
     */
    public void addLoss(){

        jugades+=1;
        ratxaAct=0;
        exits = ((float)jugadesGuanyades/(float)jugades)*100;

    }


    /**
     * Afegeix una victòria a les estadístiques de l'usuari.
     * @param intent En quin intent ha guanyat la partida.
     */
    public void addWin(int intent){

        jugades+=1;
        jugadesGuanyades+=1;
        ratxaAct+=1;

        if(ratxaAct>ratxaMax){
            ratxaMax = ratxaAct;
        }

        exits = ((float)jugadesGuanyades/(float)jugades)*100;
        victories[intent-1] +=1;

    }


    /**
     * Crea i retorna un JSONObject amb les estadístiques del jugador.
     * @return String amb les estadístiques en format JSON.
     */
    public JSONObject getJSON(){

        JSONObject vict = new JSONObject();
        vict.put("1",victories[0]);
        vict.put("2",victories[1]);
        vict.put("3",victories[2]);
        vict.put("4",victories[3]);
        vict.put("5",victories[4]);
        vict.put("6",victories[5]);

        JSONObject stats = new JSONObject();
        stats.put("Jugades",jugades);
        stats.put("Exits",exits);
        stats.put("Ratxa Actual", ratxaAct);
        stats.put("Ratxa Maxima", ratxaMax);
        stats.put("Victories",vict);

        JSONObject finalJSON = new JSONObject();

        finalJSON.put("Stats",stats);

        return finalJSON;
    }


    /***
     * Crea i dona format del JSON object per al log.
     * També s'encarrega d'escriure aquest en l'arxiu.
     * @param outputlog Fitxer on hem d'escriure la string.
     * @throws IOException Excepcions provinents del comUtils relacionades amb la connexió.
     */
    public void logJSON(BufferedWriter outputlog) throws IOException {

        String spaces = "                                          ";
        String result = "STATS  C <------7 {\n";
        result+=spaces+"\"Stats\": {\n";
        result+=spaces+"  "+"\"Jugades\": " + String.valueOf(jugades)+",\n";
        result+=spaces+"  "+"\"Èxits %\": " + exits +",\n";
        result+=spaces+"  "+"\"Ratxa Actual\": " + ratxaAct +",\n";
        result+=spaces+"  "+"\"Victòries\":\n";
        result+=spaces+"    {\n";
        result+=spaces+"      "+"\"1\": "+victories[0]+",\n";
        result+=spaces+"      "+"\"2\": "+victories[1]+",\n";
        result+=spaces+"      "+"\"3\": "+victories[2]+",\n";
        result+=spaces+"      "+"\"4\": "+victories[3]+",\n";
        result+=spaces+"      "+"\"5\": "+victories[4]+",\n";
        result+=spaces+"      "+"\"6\": "+victories[5]+"\n";
        result+=spaces+"    }\n";
        result+=spaces+"}\n";
        result+="                                        } ------------ S\n";
        outputlog.append(result);
        outputlog.flush();

    }

}
