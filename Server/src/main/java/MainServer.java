import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {

    public static void main(String[] args) {
        //Port base al que es connecta el servidor si no s'especifica res.
        int port = 4990;
        boolean isPort = false;

        //Part encarregada de llegir els paràmetres d'entrada.
        for(int i = 0; i< args.length; i+=2){

            if(args[i].toLowerCase().equals("-p")){
                port = Integer.parseInt(args[i+1]);
                isPort = true;

            }else if(args[i].toLowerCase().equals("-h")){
                System.out.println("Us: java -jar server.jar -p <port>");
                return;

            }else{
                System.out.println(args[i].toLowerCase() + " no correspon a cap opció permesa. Consulta -h per veure les opcions disponibles.");
                return;
            }

        }

        //Avisem que ens estem connectant a un port per defecte.
        if(!isPort){
            System.out.println("S'ha connectat per defecte al port 4990.");
        }


        //Creem els diccionaris que utilitzarem, de forma compartida, en els diferents threads.
        HashMap<Integer, UserStats> usersStats= new HashMap<>();
        HashMap<Integer, String> usersNames = new HashMap<>();

        //Diccionari de paraules que farem servir per al joc.
        ArrayList<String> words = new ArrayList<>();

        try {

            BufferedReader br = new BufferedReader(new InputStreamReader(MainServer.class.getClassLoader().getResourceAsStream("DISC2-LP-WORDLE.txt")));
            String word;

            while ((word = br.readLine()) != null) {
                //Comprovem que siguin adients pel nostre protocol i joc.
                if(word.matches("[a-zA-Z]+") && word.length() == 5){
                    words.add(word);
                }
            }

            br.close();

            //Creem un server socket encarregat d'anar escoltant les connexions que va rebent.
            ServerSocket ss = new ServerSocket(port);
            Socket s;

            //Mètode encarregat de llegit noves connexions i crear threads per gestionar aquestes.
            while (true) {
                System.out.println("Esperant noves connexions");
                s = ss.accept(); //Es queda esperant a rebre una nova connexió.
                //Al cap d'una hora marxem
                s.setSoTimeout(1000 * 60 * 60);
                Server server = new Server(s,usersNames,usersStats,words); //Compartim també les llistes de nom i estadístiques.
                server.start();
            }

            }catch(IOException io){
                io.printStackTrace();
                //En cas de no poder obrir l'arxiu de paraules no té sentit mantenir el servidor encès.
                System.out.println("No s'ha pogut obrir l'arxiu de paraules o no s'ha pogut crear el ServerSocket");
            }

    }

}
