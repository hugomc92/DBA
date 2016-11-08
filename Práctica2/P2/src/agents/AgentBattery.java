
package agents;

import agents.Agent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 * Clase que define al agente Battery, actua como controlador de AgentWorld y AgentRadar.
 * 
 * @author Bryan Moreno Picamán & Aarón Rodríguez Bueno 
 */
public class AgentBattery extends Agent {
	
	private static final int IDLE = 0;
	private static final int PROCESS_DATA= 1;
	private static final int SEND_CONFIRMATION = 2;
	private static final int FINISH= 3;
	
        private JsonObject responseObject;
	private JsonObject commandObject;
        
        private int state;
        private boolean finish;
        private boolean refuel=false;
        private String carName = "";
        private String batteryName = "";


	/**
	 * @param aid El ID de agente para crearlo.
         * @param carName El nombre del agente car (para comunicación)
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentBattery(String batteryName, String carName) throws Exception {
		super(batteryName);
                this.batteryName = batteryName;
                this.carName=carName;
	}


	 /**
	  * Método de inicialización del agente Battery.
	  */
	@Override
	public void init() {
      		this.finish = false;
                this.responseObject = new JsonObject();
                this.state=IDLE;
		System.out.println("AgentBattery awake.");
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
                                    //Mirar finalize o izar
                                    String response = this.receiveMessage(*);
                                    this.responseObject = Json.parse(response).asObject();
                                    this.state=PROCESS_DATA;
                                break;
                                case PROCESS_DATA:
                                    Double result = responseObject.get("battery").asDouble();
                                    if(result<=1)
                                       refuel=true;
                                    this.state=SEND_CONFIRMATION;
                                break;
                                case SEND_CONFIRMATION:
                                    this.commandObject = new JsonObject();
                                    this.commandObject.add("refuel", refuel);
                                    this.sendMessage(carName, commandObject.toString());
                                break;
                                case FINISH:
                                    this.finish=true;
                                break;
			}
		}
		
		this.finalize();
	}
	
	/**
	  * Método de finalización del agente Battery.
	  */
	@Override
	public void finalize() {
		System.out.println("AgentGPS has just finish");
		super.finalize();
	}

}