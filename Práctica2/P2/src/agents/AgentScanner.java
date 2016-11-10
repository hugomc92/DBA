
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 * Clase que define al agente Scanner, el cual se va a encargar de controlar los datos relacionados
 * con el scanner dados por el servidor y compartir su informaciÃ³n con el AgentWorld.
 * 
 * @author Aarón Rodríguez Bueno, Bryan Moreno Picamán & Hugo Maldonado.
 */
public class AgentScanner extends Agent {
    
    //Variables de estado
    private static final int IDLE = 0;
	private static final int PROCESS_DATA = 1;
	private static final int SEND_CONFIRMATION = 2;
    private static final int WAIT_MOVEMENT = 3;
	private static final int SEND_INFO = 4;
    private static final int FINISH = 5;
    private static final int WIDTH = 500;
    private static int HEIGHT = 500;
    
    private int state;
    private float [][] map_scanner = new float [WIDTH][HEIGHT];
    private float [] local_scanner = new float [25];
    private boolean finish;
    
	private JsonObject responseObject;
	private JsonObject scannerObject;
	private JsonObject gpsObject;
    
    private AgentID movementName;
    private AgentID carName;
    private AgentID gpsName;
    
    /**
     * Constructor 
     * @param scannerName El nombre de agente para crearlo.
     * 
     * @throws java.lang.Exception en la creación del agente.
     */
    public AgentScanner(AgentID scannerName,AgentID movementName,AgentID gpsName) throws Exception {
        super(scannerName);
		
        this.movementName = movementName;
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
		
		this.scannerObject = new JsonObject();
		this.gpsObject = new JsonObject();
        
        /*for (float [] i : map_scanner){
            for(float j : i){
                j = 0;
            }
        }*/
		
		System.out.println("AgentScanner has just started");
    }
    
    /**
    * Método de ejecución del agente Scanner
    */  
    @Override
    public void execute(){
		
		System.out.println("AgentScanner execution");
		
        while(!finish){
            switch(state){
                case IDLE:  // Esperamos a que nos lleguen todos los mensajes necesarios
					
					System.out.println("AgentScanner status: IDLE");
					
                    String message;
					
					boolean finalize = false;
					
                    for(int i=0; i<2 && !finalize; i++) {
						message = this.receiveMessage();
						
						if(message.contains("scanner"))
							this.scannerObject = Json.parse(message).asObject();
						else if(message.contains("gps") && !message.contains("updated"))
							this.gpsObject = Json.parse(message).asObject();
						else if(message.contains("CRASHED") || message.contains("finalize"))
							finalize = true;
					}
					
					if(finalize)
						this.state = FINISH;
					else
						this.state = PROCESS_DATA;
						
                    break;
                case PROCESS_DATA:
					
					System.out.println("AgentScanner status: PROCESS_DATA");
					
					// Procesamos la información del scanner
					int pos = 0;
					for (JsonValue j : scannerObject.get("scanner").asArray()){
						local_scanner[pos] = j.asFloat();
						pos++;
					}
					
					scannerObject = new JsonObject();
					
					// Procesamos la información del gps
					int x, y;

					x = gpsObject.get("x").asInt();
					y = gpsObject.get("y").asInt();

					//Metemos los datos del scanner dados anteriormente en su posición en map_scanner
					int posi = 0;
					for(int i = x-1; i <= x+1; i++){
						for (int j = y-1; j <= y+1; j++){
							map_scanner[i][j] = local_scanner[posi];
							posi++;
						}
					}
					
					gpsObject = new JsonObject();
					
                    break;
				case SEND_CONFIRMATION:
					
					System.out.println("AgentScanner status: PROCESS_DATA");
					
					//Avisamos al Agent Car que estamos listos
					responseObject = new JsonObject(); //Lo limpiamos
					responseObject.add(this.getName(), "ok");
					message = responseObject.toString();
					sendMessage(carName, message);
					
                    break;
                case WAIT_MOVEMENT:		//Esperamos a que el Movement Agent nos pida los datos del scanner
					
					System.out.println("AgentScanner status: WAIT_MOVEMENT");
                    
                    String messageMovement;

                    messageMovement = this.receiveMessage();
                        
                    this.responseObject = Json.parse(messageMovement).asObject();
                    
                    if(messageMovement.contains("request"))
                        state = SEND_INFO;
                    else
                        state = FINISH;
                    
                    break; 
				case SEND_INFO:
					
					System.out.println("AgentScanner status: SEND_INFO");
					
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
					
                    break;
                case FINISH:    //Matamos al agente
					
					System.out.println("AgentScanner status: FINISH");
					
                    this.finish = true;
					
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
		
		System.out.println("AgentScanner has just finished");
		
        super.finalize();
    }
}