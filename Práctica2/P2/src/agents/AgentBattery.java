
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 * Clase que define al agente Battery, actua como controlador de AgentWorld y AgentRadar.
 * 
 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
 */
public class AgentBattery extends Agent {
	
	private static final int IDLE = 0;
	private static final int PROCESS_DATA = 1;
	private static final int SEND_CONFIRMATION = 2;
	private static final int FINISH = 3;
	private static final boolean DEBUG = false;
	
	private static final int BATTERY_LIMIT = 5;
	
    private JsonObject responseObject;
	private JsonObject commandObject;
	
	private String response;
        
	private int state;
	private boolean finish;
	
	private boolean refuel=false;
	
	private final AgentID carName;


	/**
	 * Constructor
	 * @author Bryan Moreno Picamán
	 * @param batteryName El nombre de agente para crearlo.
     * @param carName El nombre del agente car (para comunicación)
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentBattery(AgentID batteryName, AgentID carName) throws Exception {
		super(batteryName);
		
		this.carName=carName;
	}

	 /**
	  * Método de inicialización del agente Battery.
	  * @author Bryan Moreno Picamán
	  */
	@Override
	public void init() {
		this.finish = false;
		this.responseObject = new JsonObject();
		this.state = IDLE;
		
		System.out.println("AgentBattery has just started");
	}
	
	/**
	 * Estado IDLE: espera a la confirmación del server. Si todo va correcto,
	 * va a PROCESS_DATA, y si no, a FINISH
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateIdle(){
		if(DEBUG)
			System.out.println("AgentBattery status: IDLE");

		response = this.receiveMessage();

		if(response.contains("CRASHED") || response.contains("finalize"))
			this.state = FINISH;
		else {
			this.responseObject = Json.parse(response).asObject();

			this.state = PROCESS_DATA;
		}
	}
	
	/**
	 * Estado PROCESS_DATA: parsea el mensaje recibido en IDLE y guarda el fuel actual.
	 * Luego cambia a SEND_CONFIRMATION
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateProcessData(){
		if(DEBUG)
			System.out.println("AgentBattery status: PROCESS_DATA");

		float result = responseObject.get("battery").asFloat();

		refuel = result <= BATTERY_LIMIT;

		this.state = SEND_CONFIRMATION;
	}
	
	/**
	 * Estado SEND_CONFIRMATION: le da la información sobre el fuel al car y 
	 * vuelve al estado IDLE
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateSendConfirmation(){
		if(DEBUG)
			System.out.println("AgentBattery status: SEND_CONFIRMATION");

		this.commandObject = new JsonObject();

		this.commandObject.add("battery", refuel);

		this.sendMessage(carName, commandObject.toString());

		this.state = IDLE;
	}
	
	/**
	 * Estado FINISH: avisa al car que se va a finalizar este agente
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateFinish(){
		if(DEBUG)
			System.out.println("AgentBattery status: FINISH");

		if(this.response.contains("finalize")) {
			JsonObject confirmationMessage = new JsonObject();

			confirmationMessage.add("battery", "finish");

			this.sendMessage(carName, confirmationMessage.toString());
		}

		this.finish = true;
	}
	
	/**
	  * Método de ejecución del agente Battery.
	  *  @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	  */
	@Override
	public void execute() {
		
		System.out.println("AgentBattery execution");
		
		while(!finish) {
			switch(state) {
				case IDLE:
					
					stateIdle();
					
					break;
				case PROCESS_DATA:
					
					stateProcessData();
					
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
	  * Método de finalización del agente Battery.
	  *  @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	  */
	@Override
	public void finalize() {
		
		System.out.println("AgentBattery has just finished");
		
		super.finalize();
	}
}