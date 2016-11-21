
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 * Clase que define al agente Rada, el cual maneja los datos del radar enviados por el server.
 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
 */
public class AgentRadar extends Agent {
 
   
    private static final int IDLE = 0;
    private static final int PROCESS_DATA = 1;
    private static final int SEND_DATA = 2;
    private static final int WAIT_WORLD = 3;
    private static final int SEND_CONFIRMATION = 4;
    private static final int FINISH = 5;
	
	private static final boolean DEBUG = false;
        
    private int[] infoRadar;		//Array que guarda el estado de cada celda
    private int state;
    private JsonObject dataRadar;	//Datos radar y datos gps
	private boolean gpsProcced;
    private String radarToUpdate;	//Valor a enviar
    private boolean finish;
	
	private String message;
	
	private final AgentID carName;
	private final AgentID worldName;
    
    /**
     * Constructor
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
     * @param radarName nombre del agente Radar
     * @param carName nombre del agente Car (para comunicación)
     * @param worldName nombre del agente World (para comunicación)
     * @throws Exception Excepción en el constructor de Agent
	 * 
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
     */
    public AgentRadar(AgentID radarName, AgentID carName, AgentID worldName) throws Exception {
        super(radarName);
        
		this.carName = carName;
		this.worldName = worldName;
    }
    
    /**
     * Inicializa al agente
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
     */
    @Override
    public void init(){
        state = IDLE;
        
		infoRadar = new int[25];
        finish = false;
        
        this.dataRadar = new JsonObject();
		
		System.out.println("AgentRadar has just started");
    }
    
    /**
     * Obtenemos el array dado por string
	 * @author Jose David
     */
    public void processRadar() {
		
		JsonArray arrayDatos = dataRadar.get("radar").asArray(); //obtener datos del radar en array 
		JsonObject obj = Json.object().add("radar",arrayDatos);
		radarToUpdate = obj.toString();
    }
    
	/**
	 * Estado IDLE: recibe los datos del radar del server. Si es el array o el GPS, 
	 * pasa al estado PROCESS_DATA. Si es otro mensaje, pasa a FINISH.
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateIdle(){
		if(DEBUG)
			System.out.println("AgentRadar status: IDLE");

		boolean finalize = false;

		for(int i=0; i<2 && !finalize; i++) {
			message = this.receiveMessage();

			if(message.contains("radar"))	//Del server
				dataRadar = (JsonObject) Json.parse(message);    
			else if(message.contains("gps")) {
				this.gpsProcced = !message.contains("updated");
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
	 * Estado PROCESS_DATA: si el mensaje anterior era del radar, lo procesamos
	 * y vamos al estado SEND_DATA. Si era del server, vamos a SEND_CONFIRMATION.
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateProcessData(){
		if(DEBUG)
			System.out.println("AgentRadar status: PROCESS_DATA");

		if(this.gpsProcced) {
			processRadar();

			this.state = SEND_DATA;
		}
		else
			this.state = SEND_CONFIRMATION;
	}
	
	/**
	 * Estado SEND_DATA: enviamos los datos al World y vamos al estado WAIT_WORLD.
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateSendData(){
		if(DEBUG)
			System.out.println("AgentRadar status: SEND_DATA");

		sendMessage(worldName, radarToUpdate);

		this.state = WAIT_WORLD;
	}
	
	/**
	 * Estado WAIT_WORLD: esperamos al mensaje de confirmación del World. Si contiene
	 * "ok", vamos a SEND_CONFIRMATION. En otro caso, a FINISH.
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateWaitWorld(){
		if(DEBUG)
			System.out.println("AgentRadar status: WAIT_WORLD");

		String confirmation = this.receiveMessage();

		JsonObject confirmationObject = Json.parse(confirmation).asObject();
		String worldMessage = confirmationObject.get("radar").asString();

		if(worldMessage.contains("ok"))//confirmacion del world
			this.state = SEND_CONFIRMATION;
		else
			this.state = FINISH;
	}
	
	/**
	 * Estado SEND_CONFIRMATION: enviamos al coche que ya nos hemos comunicado con
	 * los demás agentes. Pasamos al estado IDLE.
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateSendConfirmation(){
		if(DEBUG)
			System.out.println("AgentRadar status: SEND_CONFIRMATION");

		JsonObject statusWorld = new JsonObject();

		statusWorld.add("radar", "ok");

		sendMessage(carName, statusWorld.toString());	//Enviamos confirmacion a car

		this.state = IDLE;
	}
	
	/**
	 * Estado FINISH: enviamos confirmación al Car de que vamos a morir y nos disponemos
	 * a ello.
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateFinish(){
		if(DEBUG)
			System.out.println("AgentRadar status: FINISH");

		if(this.message.contains("finalize")) {
			JsonObject confirmationMessage = new JsonObject();

			confirmationMessage.add("radar", "finish");

			this.sendMessage(carName, confirmationMessage.toString());
		}

		this.finish = true;
	}
	
    /**
     * Método de ejecución del agente Radar.
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
     */
    @Override
    public void execute() {
		
		System.out.println("AgentRadar execution");
		
        while (!finish) {    
            switch(state) {
                case IDLE:
					
					stateIdle();
					
					break;
                case PROCESS_DATA:
					
					stateProcessData();
					
					break;  
				case SEND_DATA:
					
					stateSendData();

					break;
                case WAIT_WORLD:
					
					stateWaitWorld();
					
					break;
				case SEND_CONFIRMATION:
					
					stateSendConfirmation();
					
					break;		
				case FINISH:
					
					stateFinish();
					
					break;
            }
        }
    }
    
    /**
     * Método de finalización del agente Radar.
	 * @author Jose David and Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
     */
    @Override
	public void finalize() {
		
		System.out.println("AgentRadar has just finished");
		
		super.finalize();
	}
}