import java.util.HashMap;
import java.util.Map;

public class MainClient {

    public static void main(String[] args) {

        Map<String, String> options = new HashMap<>();
        String ip = "";
        int port = -1;
        boolean automatic = false;
        boolean correct = true;

        int optIdx = 0;
        String opt;

        while(optIdx < args.length){

            opt = args[optIdx].toLowerCase();
            optIdx += 1;

            if(opt.equals("-h")){
                options.put(opt, "");
            }
            else{
                if(optIdx < args.length){
                    options.put(opt, args[optIdx]);
                    optIdx += 1;
                }
                else{
                    correct = false;
                }

            }
        }

        if(options.containsKey("-s") && correct) {
            ip = options.get("-s");
        }else{
            correct = false;
        }

        if(options.containsKey("-p") && correct) {
            try{
                port = Integer.parseInt(options.get("-p"));
            }catch (Exception e){
                correct = false;
            }
        }else{
            correct = false;
        }

        if(options.containsKey("-i") && correct){
            String i = options.get("-i");

            if(i.equals("0") || i.equals("1")){
                automatic = i.equals("1");
            }else{
                correct = false;
            }
        }

        if(!correct && !options.containsKey("-h")){
            System.out.println("Paràmetres incorrectes.");
        }

        if(options.containsKey("-h") || !correct) {
            System.out.println("Ús: java -jar client -s <maquina_servidora> -p <port>  [-i 0|1]");
        }

        else{

            Client client;

            if(automatic){
                client = new ClientAutomatic(ip, port);
            }
            else{
                client = new ClientManual(ip, port);
            }
            client.run();
        }
    }
}
