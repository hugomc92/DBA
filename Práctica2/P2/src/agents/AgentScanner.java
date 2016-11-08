
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Clase que define al agente Scanner, el cual se va a encargar de controlar los datos relacionados
 * con el scanner dados por el servidor y compartir su informaciÃ³n con el AgentWorld.
 * 
 * @author Aarón Rodríguez Bueno, Bryan Moreno Picamán & Hugo Maldonado.
 */
public class AgentScanner extends Agent {
    
    //Variables de estado
    private static final int IDLE = 0;
    private static final int WAIT_GPS = 1;
    private static final int WAIT_MOVEMENT = 2;
    private static final int FINISH = 3;
    private static final int WIDTH = 500;
    private static int HEIGHT = 500;
    
    private int state;
    private float [][] map_scanner = new float [WIDTH][HEIGHT];
    private float [] local_scanner = new float [25];
    private boolean finish;
    private JsonObject responseObject;
    
    private String movementName;
    private String scannerName;
    private String carName;
    private String gpsName;
    
    /**
     * Constructor 
     * @param scannerName El nombre de agente para crearlo.
     * 
     * @throws java.lang.Exception en la creaciÃ³n del agente.
     */
    public AgentScanner(String scannerName,String movementName,String gpsName) throws Exception {
        super(scannerName);
        this.movementName = movementName;
        this.scannerName = scannerName;
        this.gpsName = gpsName;
    }
    
    
    /**
     * Método de inicialización del Agent Scanner
     * 
     */
    @Override
    public void init(){
        state = IDLE;
        finish = false;
        
        this.responseObject = new JsonObject();
        
        /*for (float [] i : map_scanner){
            for(float j : i){
                j = 0;
            }
        }*/
    }
    
    /**
    * Método de ejecución del agente Scanner
    */  
    @Override
    public void execute(){
        while(!finish){
            switch(state){
                case IDLE:  //Esperamos a que nos mande los datos del scanner el server
                    String message;
                    /*
                    message = this.receiveMessage("*");
                    if(message.contains("CRASHED")||message.contains("logout")) 
                        state = finish;
                    else{
                        ...
                    }
                    break;
                    */
                    message = this.receiveMessage();
                    
                    
                    this.responseObject = Json.parse(message).asObject();
                    
                    //Guardamos el array en un int de float mientras nos dan los datos del GPS
                    int pos = 0;
                    for (JsonValue j : responseObject.get("scanner").asArray()){
                        local_scanner[pos] = j.asFloat();
                        pos++;
                    }
                    
                    state = WAIT_GPS;
                    break;
                
                
                
                
                
                case WAIT_GPS:    //Esperamos a que nos mande los datos del GPS el GPS Agent
                    String messageGPS;
                    int x, y;
                    /*
                    message = this.receiveMessage("*");
                    if(message.contains("CRASHED")||message.contains("logout")) 
                        state = finish;
                    else{
                        ...
                    }
                    break;
                    */
                    messageGPS = this.receiveMessage();
                        
                    this.responseObject = Json.parse(messageGPS).asObject();
                    
                    x = responseObject.get("x").asInt();
                    y = responseObject.get("y").asInt();
                    
                    //Metemos los datos del scanner dados anteriormente en su posiciÃ³n en map_scanner
                    int posi = 0;
                    for (int i = x-1; i <= x+1; i++){
                        for (int j = y-1; j <= y+1; j++){
                            map_scanner[i][j] = local_scanner[posi];
                            posi++;
                        }
                    }
                    
                    //Avisamos al Agent Car que estamos listos
                    responseObject = new JsonObject(); //Lo limpiamos
                    responseObject.add(scannerName, "ok");
                    messageGPS = responseObject.toString();
                    sendMessage(carName, messageGPS);
                    
                    state = WAIT_MOVEMENT;
                    break;
                    
                    
                    
                    
                    
                case WAIT_MOVEMENT: //Esperamos a que nos avise el Movement Agent nos pida los datos del scanner
                    
                    String messageMovement;
                    /*
                    message = this.receiveMessage("*");
                    if(message.contains("CRASHED")||message.contains("logout")) 
                        state = finish;
                    else{
                        ...
                    }
                    break;
                    */
                    messageMovement = this.receiveMessage();
                        
                    this.responseObject = Json.parse(messageMovement).asObject();
                    
                    if(messageMovement.contains("request")){
                        responseObject = new JsonObject(); //Lo limpiamos
                        
                        //Empaquetamos el array entero en un JSon y se lo enviamos
                        JsonArray vector = new JsonArray();
                        for (float [] i : map_scanner){
                            for (float j : i){
                                vector.add(j);
                            }
                        }
                        responseObject.add("",vector);
                        messageMovement = responseObject.toString();
                        this.sendMessage(movementName,messageMovement);
                    }
                    else{
                        state = FINISH;
                    }
                    
                    break;
                    
                    
                
                    
                
                case FINISH:    //Matamos al agente
                    this.finalize();
                    break;
            }
        }
    }
    
    /**
    * Método de finalización del Agent Scanner
     * 
     * @author Aarón Rodríguez Bueno
    */
    @Override
    public void finalize() {
        super.finalize();
    }
}
