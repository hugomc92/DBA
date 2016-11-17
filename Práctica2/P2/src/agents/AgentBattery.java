
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 * Clase que define al agente Battery, actua como controlador de AgentWorld y AgentRadar.
 * 
 * @author Bryan Moreno Picamán, Aarón Rodríguez Bueno & Hugo Maldonado
 */
public class AgentBattery extends Agent {
	
	private static final int IDLE = 0;
	private static final int PROCESS_DATA = 1;
	private static final int SEND_CONFIRMATION = 2;
	private static final int FINISH = 3;
	
	private static final int BATTERY_LIMIT = 5;
	
    private JsonObject responseObject;
	private JsonObject commandObject;
	
	private String response;
        
	private int state;
	private boolean finish;
	private boolean refuel=false;
	private final AgentID carName;


	/**
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
	  */
	@Override
	public void init() {
		this.finish = false;
		this.responseObject = new JsonObject();
		this.state = IDLE;
		
		System.out.println("AgentBattery has just started");
	}
	
	/**
	  * Método de ejecución del agente Battery.
	  */
	@Override
	public void execute() {
		
		System.out.println("AgentBattery execution");
		
		while(!finish) {
			switch(state) {
				case IDLE:
					
					System.out.println("AgentBattery status: IDLE");
					
					response = this.receiveMessage();
					
					if(response.contains("CRASHED") || response.contains("finalize"))
						this.state = FINISH;
					else {
						this.responseObject = Json.parse(response).asObject();

						this.state = PROCESS_DATA;
					}
					
					break;
				case PROCESS_DATA:
					
					System.out.println("AgentBattery status: PROCESS_DATA");
					
					float result = responseObject.get("battery").asFloat();
					
					refuel = result <= BATTERY_LIMIT;
					
					this.state = SEND_CONFIRMATION;
					
					break;
				case SEND_CONFIRMATION:
					
					System.out.println("AgentBattery status: SEND_CONFIRMATION");
					
					this.commandObject = new JsonObject();
					
					this.commandObject.add("battery", refuel);
					
					this.sendMessage(carName, commandObject.toString());
					
					this.state = IDLE;
					
					break;
				case FINISH:
					
					System.out.println("AgentBattery status: FINISH");
					
					if(this.response.contains("finalize")) {
						JsonObject confirmationMessage = new JsonObject();
						
						confirmationMessage.add("battery", "finish");

						this.sendMessage(carName, confirmationMessage.toString());
					}
					
					this.finish = true;
					
					break;
			}
		}
	}
	
	/**
	  * Método de finalización del agente Battery.
	  */
	@Override
	public void finalize() {
		System.out.println("AgentBattery has just finished");
		
		super.finalize();
	}
}