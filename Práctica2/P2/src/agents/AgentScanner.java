
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
 * @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
 */
public class AgentScanner extends Agent {
    
    //Variables de estado
    private static final int IDLE = 0;
	private static final int PROCESS_DATA = 1;
	private static final int SEND_CONFIRMATION = 2;
    private static final int WAIT_MOVEMENT = 3;
	private static final int SEND_INFO = 4;
    private static final int FINISH = 5;
    private static final int WIDTH = 504;
    private static final int HEIGHT = 504;
	
	private static final boolean DEBUG = false;
    
    private int state;
    private final float [][] map_scanner = new float [WIDTH][HEIGHT];
    private final float [] local_scanner = new float [25];
    private boolean finish;
    
	private JsonObject responseObject;
	private JsonObject scannerObject;
	private JsonObject gpsObject;
    
    private final AgentID movementName;
    private final AgentID carName;
    private final AgentID gpsName;
    
	private String message;
	private String messageMovement;
	
    /**
     * Constructor 
	 * author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
     * @param scannerName El nombre de agente.
     * 
     * author Bryan Moreno Picamán & Hugo Maldonado.
     * @param movementName nombre del agente Movement (para comunicación)
     * @param gpsName nombre del agente Gps (para comunicación)
     * @param carName nombre del agente Car (para comunicación)
     * @throws java.lang.Exception en la creación del agente.
     */
    public AgentScanner(AgentID scannerName,AgentID movementName,AgentID gpsName, AgentID carName) throws Exception {
        super(scannerName);
		
        this.movementName = movementName;
        this.gpsName = gpsName;
		this.carName = carName;
    }
    
    
    /**
     * Método de inicialización del Agent Scanner
     * @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
     */
    @Override
    public void init(){
        state = IDLE;
        finish = false;
		message = "";
        
        this.responseObject = new JsonObject();
		
		this.scannerObject = new JsonObject();
		this.gpsObject = new JsonObject();
		
		if(DEBUG)
			System.out.println("AgentScanner has just started");
    }
    
	/**
	 * Estado IDLE: esperamos tanto los mensajes del server como del Gps y pasamos 
	 * al estado PROCESS_DATA. Si le llegara otro mensaje, iría a FINISH.
	 * @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
	 */
	private void stateIdle(){
		if(DEBUG)
			System.out.println("AgentScanner status: IDLE");

		boolean finalize = false;

		// Limpiamos variables;
		this.message = "";
		this.scannerObject = new JsonObject();
		this.gpsObject = new JsonObject();

		for(int i=0; i<2 && !finalize; i++) {
			this.message = this.receiveMessage();

			if(message.contains("scanner")){
				this.scannerObject = Json.parse(message).asObject();
			}
			else if(message.contains("gps")){
				this.gpsObject = Json.parse(message).asObject();
			}
			else if(message.contains("CRASHED") || message.contains("finalize"))
				finalize = true;
		}

		if(finalize)
			this.state = FINISH;
		else
			this.state = PROCESS_DATA;
	}
	
	/**
	 * Estado PROCESS_DATA: procesamos los datos del escaner y la del gps, y pasamos
	 * a SEND_CONFIRMATION.
	 * @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
	 */
	private void stateProcessData(){
		if(DEBUG)
			System.out.println("AgentScanner status: PROCESS_DATA");

		// Procesamos la información del scanner
		int pos = 0;
		for(JsonValue j : scannerObject.get("scanner").asArray()){
			local_scanner[pos] = j.asFloat();
			pos++;
		}

		// Procesamos la información del gps si no fue updated
		if(!this.gpsObject.get("gps").toString().contains("updated")) {

			int x, y;

			x = gpsObject.get("gps").asObject().get("x").asInt();
			y = gpsObject.get("gps").asObject().get("y").asInt();

			//Metemos los datos del scanner dados anteriormente en su posición en map_scanner
			int posi = 0;
			for(int i = y-2; i <= y+2; i++) {
				for(int j = x-2; j <= x+2; j++) {
					if(i>=0 && j >= 0 && i < WIDTH && j < HEIGHT) {
						map_scanner[i][j] = local_scanner[posi];
					}
					posi++;
				}
			}
		}

		gpsObject = new JsonObject();
		state = SEND_CONFIRMATION;
	}
	
	/**
	 * Estado SEND_CONFIRMATION: enviamos la confirmación al Car y pasamos a WAIT_MOVEMENT.
	 * @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
	 */
	private void stateSendConfirmation(){
		if(DEBUG)
			System.out.println("AgentScanner status: SEND_CONFIRMATION");

		//Avisamos al Agent Car que estamos listos
		responseObject = new JsonObject(); //Lo limpiamos
		responseObject.add("scanner", "ok");
		message = responseObject.toString();
		sendMessage(carName, message);

		state = WAIT_MOVEMENT;
	}
	
	/**
	 * Estado WAIT_MOVEMENT: esperamos a que nos pida el escáner el Movement y pasamos al
	 * estado SEND_INFO. Si hubiera llegado otro mensaje, vamos a FINISH.
	 * @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
	 */
	private void stateWaitMovement(){
		if(DEBUG)
			System.out.println("AgentScanner status: WAIT_MOVEMENT");

		messageMovement = this.receiveMessage();

		this.responseObject = Json.parse(messageMovement).asObject();

		if(responseObject.get("sendScanner").asString().contains("request"))
			state = SEND_INFO;
		else
			state = FINISH;
	}
	
	/**
	 * Estado SEND_INFO: le enviamos la información del escáner al Movement y pasamos
	 * al estado IDLE.
	 * @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
	 */
	private void stateSendInfo(){
		if(DEBUG)
			System.out.println("AgentScanner status: SEND_INFO");

		responseObject = new JsonObject(); //Lo limpiamos

		//Empaquetamos el array entero en un JSon y se lo enviamos
		/*JsonArray vector = new JsonArray();

		for(float [] i : map_scanner){
			for(float j : i){
				vector.add(j);
			}
		}

		responseObject.add("scanner",vector);*/

		responseObject.add("scanner", scannerObject.get("scanner"));

		messageMovement = responseObject.toString();

		this.sendMessage(movementName, messageMovement);

		state = IDLE;
	}
	
	/**
	 * Estado FINISH: avisamos al car que vamos a morir, y nos preparamos para ello.
	 * @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
	 */
	private void stateFinish(){
		if(DEBUG)
			System.out.println("AgentScanner status: FINISH");

		if(this.message.contains("finalize")) {
			JsonObject confirmationMessage = new JsonObject();

			confirmationMessage.add("scanner", "finish");

			this.sendMessage(carName, confirmationMessage.toString());
		}

		this.finish = true;
	}
	
    /**
    * Método de ejecución del agente Scanner
	* @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
    */  
    @Override
    public void execute(){
		
		if(DEBUG)
			System.out.println("AgentScanner execution");
		
        while(!finish){
            switch(state){
                case IDLE:  // Esperamos a que nos lleguen todos los mensajes necesarios
					
					stateIdle();
						
                    break;
                case PROCESS_DATA:
					
					stateProcessData();
					
                    break;
				case SEND_CONFIRMATION:
					
					stateSendConfirmation();
					
					break;
					
                case WAIT_MOVEMENT:		//Esperamos a que el Movement Agent nos pida los datos del scanner
					
					stateWaitMovement();
                    
                    break; 
					
				case SEND_INFO:
					
					stateSendInfo();
					
                    break;
                case FINISH:    //Matamos al agente
					
					stateFinish();
					
                    break;
            }
        }
    }
    
    /**
    * Método de finalización del Agent Scanner
     * 
     * @author Aarón Rodríguez Bueno and Bryan Moreno Picamán and Hugo Maldonado
    */
    @Override
    public void finalize() {
		
		if(DEBUG)
			System.out.println("AgentScanner has just finished");
		
        super.finalize();
    }
}