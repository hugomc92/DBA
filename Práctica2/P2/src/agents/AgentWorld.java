
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 * Clase que define al agente World.
 * @author Bryan Moreno Picamán, Aarón Rodríguez Bueno & Hugo Maldonado.
 */
public class AgentWorld extends Agent {
	
	private static final int IDLE = 0;
	private static final int WAIT_GPS= 1;
	private static final int WAIT_RADAR = 2;
	private static final int WARN_RADAR = 3;
	private static final int WAIT_MOVEMENT = 4;
	private static final int SEND_INFO = 5;
	private static final int FINISH = 6;
	
    private JsonObject responseObject;
	private JsonObject commandObject;
        
	private int state;
	private boolean finish;
	private final AgentID radarName;
	private final AgentID gpsName;
	private final AgentID movementName;

	private int cont;
        
	/**
	 * @param worldName El nombre de agente para crearlo.
	 * @param radarName
	 * @param gpsName
	 * @param movementName
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentWorld(AgentID worldName,AgentID radarName,AgentID gpsName,AgentID movementName) throws Exception {
		super(worldName);
		
		this.radarName=radarName;
		this.gpsName=gpsName;
		this.movementName=movementName;
	}
	
	 /**
	  * Método de inicialización del agente World.
	  */
	@Override
	public void init() {
		this.state = IDLE;
		this.finish = false;
		this.responseObject = new JsonObject();
		
		this.cont = 0;
      	
		System.out.println("AgentWorld has just started");
	}
	
	/**
	  * Método de ejecución del agente World.
	  */
	@Override
	public void execute() {
		
		System.out.println("AgentWorld execution");
		
		while(!finish) {
			switch(state) {   
				case IDLE:
					
					System.out.println("AgentWorld status: IDLE");
					
					String responseGPS = this.receiveMessage();
					
					if(responseGPS.contains("CRASHED") || responseGPS.contains("finalize"))
						this.state = FINISH;
					else {
						this.responseObject = Json.parse(responseGPS).asObject();

						String resultGPS = responseObject.get("gps").asObject().toString();

						boolean ok = true;
						
						if(!resultGPS.contains("updated"))
							ok = this.updateWorld(resultGPS);
						
						JsonObject sendConfirmation = new JsonObject();
						
						sendConfirmation.add("gps", ok);
						
						this.sendMessage(gpsName, sendConfirmation.toString());

						this.state = WAIT_RADAR;
					}
					
					break;
				case WAIT_RADAR:
					
					System.out.println("AgentWorld status: WAIT_RADAR");
					
					String responseRadar = this.receiveMessage();
					
					this.responseObject = Json.parse(responseRadar).asObject();
					
					JsonArray resultRadar = responseObject.get("radar").asArray();
					
					this.updateWorld(resultRadar.toString());
					
					this.state = WARN_RADAR;
					
					break;                  
				case WARN_RADAR:
					
					System.out.println("AgentWorld status: WARN_RADAR");
					
					this.commandObject = new JsonObject();

					this.commandObject.add("gps","updated");

					this.sendMessage(this.getAid(), commandObject.toString());
					
					this.state = WAIT_MOVEMENT;

					break;                
				case WAIT_MOVEMENT:
					
					System.out.println("AgentWorld status: WAIT_MOVEMENT");
					
					String confirmation = this.receiveMessage();
					
					JsonObject confirmationObject = Json.parse(confirmation).asObject();
					
					String confirmationResult = confirmationObject.get("sendWorld").toString();
					
					if(confirmationResult.contains("request"))
						this.state = SEND_INFO;
                                        
				case SEND_INFO:
					
					System.out.println("AgentWorld status: SEND_INFO");
					
					this.sendWorld();
					
					this.state=IDLE;
					
					break;
           
				case FINISH:
					
					System.out.println("AgentWorld status: FINISH");
					
					this.finish = true;
					
					break;
			}
		}
	}
	
	/**
	  * Método de finalización del agente Coche.
	  */
	@Override
	public void finalize() {
		System.out.println("AgentWorld has just finished");
		
		super.finalize();
	}

    private boolean updateWorld(String resultMessage) {
        System.out.println("AgentWorld updating world");
		
		return true;
    }

    private void sendWorld() {
        // Construir el mensaje que vamos a enviar al movemen
		JsonObject world = new JsonObject();
		world.add("world", "");
		
		this.sendMessage(movementName, world.toString());
    }
}