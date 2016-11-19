
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 * Clase que define al agente GPS, actua como controlador de AgentWorld y AgentRadar.
 * 
 * @author Bryan Moreno Picamán, Hugo Maldonado & Aarón Rodríguez Bueno.
 */
public class AgentGPS extends Agent {
	
	private static final int WAKE_WORLD = 0;
	private static final int IDLE= 1;
	private static final int PROCESS_DATA = 2;
	private static final int UPDATE_WORLD = 3;
	private static final int WAIT_WORLD = 4;
	private static final int SEND_CONFIRMATION = 5;
	private static final int WARN_RADAR = 6;
	private static final int FINISH_WORLD = 7;
	private static final int FINISH= 8;
	
	private static final boolean DEBUG = false;
	
    private JsonObject responseObject;
	private JsonObject commandObject;
	
	private String response;
        
	private int state;
	private boolean finish;
	private boolean needUpdate;
	private final AgentID worldName;
	private final AgentID radarName;
	private final AgentID carName;
	private final AgentID scannerName;
	private final AgentID gpsName;
	private final AgentID movementName;
	private final String map;

	private int cont;

	int coordX;
	int coordY;

	/**
     * @param radarName El nombre del agente radar (para comunicación)
     * @param carName El nombre del agente car (para comunicación)
     * @param gpsName El nombre del propio agente
     * @param worldName
     * @param scannerName
     * @param movementName El nombre del agente movement
     * @param map
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentGPS(AgentID gpsName,AgentID radarName,AgentID carName,AgentID movementName,AgentID worldName,AgentID scannerName, String map) throws Exception {
		super(gpsName);
		
		this.radarName=radarName;
		this.carName=carName;
		this.scannerName=scannerName;
		this.gpsName=gpsName;
		this.movementName=movementName;
		this.worldName=worldName;
		this.map = map;
		
		this.cont = 10;
		
		this.coordX = -1;
		this.coordX = -1;
	}


	 /**
	  * Método de inicialización del agente GPS.
	  */
	@Override
	public void init() {
		this.state = WAKE_WORLD;
		this.finish = false;
		this.responseObject = new JsonObject();
      	
		System.out.println("AgentGPS has just started");
	}
	
	/**
	  * Método de ejecución del agente GPS.
	  */
	@Override
	public void execute() {
		
		System.out.println("AgentGPS execution");
		
		while(!finish) {
			switch(state) {
				case WAKE_WORLD:
					
					if(DEBUG)
						System.out.println("AgentGPS status: WAKE_WORLD");
					
					Agent worldMap;
					try {
						worldMap = new AgentWorld(worldName,radarName,gpsName,movementName, map);
						worldMap.start();
						
						this.state = IDLE;
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
					}
					
					break;         
				case IDLE:
					
					if(DEBUG)
						System.out.println("AgentGPS status: IDLE");
					
					response = this.receiveMessage();
					
					if(response.contains("CRASHED") || response.contains("finalize")) 
						this.state = FINISH_WORLD;
					else {
						this.responseObject = Json.parse(response).asObject();
						
						this.state = PROCESS_DATA;
					}

					break;
				case PROCESS_DATA:
					
					if(DEBUG)
						System.out.println("AgentGPS status: PROCESS_DATA");
					
					JsonObject gpsData = responseObject.get("gps").asObject();
					
					int nX = gpsData.get("x").asInt();
					int nY = gpsData.get("y").asInt();
					
					if(nX+2==coordX&&nY+2==coordY)
						needUpdate = false;
					else {
						needUpdate = true;
						
						coordX=nX+2;
						coordY=nY+2;
						
						cont++;
					}
					
					this.state = UPDATE_WORLD;

					break;              
				case UPDATE_WORLD:
					
					if(DEBUG)
						System.out.println("AgentGPS status: UPDATE_WORLD");
					
					this.commandObject = new JsonObject();
					
					if(needUpdate) {					
						this.commandObject.add("gps", new JsonObject().add("x",coordX).add("y",coordY));
						
						this.commandObject.add("cont", cont);

					}
					else
						this.commandObject.add("gps","updated");
					
					
					this.sendMessage(worldName, commandObject.toString());
					this.sendMessage(scannerName, commandObject.toString());
					
					this.state = WAIT_WORLD;

					break;                   
				case WAIT_WORLD:
					
					if(DEBUG)
						System.out.println("AgentGPS status: WAIT_WORLD");
					
					String confirmation = this.receiveMessage();
					
					//System.out.println("AGENTGPS WAIT WORLD CONFIRMATION: " + confirmation);
					
					JsonObject confirmationObject = Json.parse(confirmation).asObject();
					
					//System.out.println("CONFIRMATION OBJECT TO STRING: " + confirmationObject.toString());
					
					boolean confirmationResult = confirmationObject.get("gps").asBoolean();
					
					if(!confirmationResult)
						this.state = UPDATE_WORLD;
					else
						this.state = WARN_RADAR;
					break;
                                        
				case WARN_RADAR:
					
					if(DEBUG)
						System.out.println("AgentGPS status: WARN_RADAR");
					
					this.commandObject = new JsonObject();
					this.commandObject.add("gps","ok");
					
					this.sendMessage(radarName, commandObject.toString());
					
					this.state = SEND_CONFIRMATION;
					
					break;                
				case SEND_CONFIRMATION:
					
					if(DEBUG)
						System.out.println("AgentGPS status: SEND_CONFIRMATION");
					
					this.commandObject = new JsonObject();
					
					this.commandObject.add("gps","ok");
					this.commandObject.add("cont", cont);
					
					this.sendMessage(carName, commandObject.toString());
					
					this.state = IDLE;
					
					break;
				case FINISH_WORLD:
					
					if(DEBUG)
						System.out.println("AgentGPS status: FINISH_WORLD");
					
					// Se ejecuta cuando se encentra logout o un CRASH. Mata a el agente world y pasa a estado de finalización.
					this.sendMessage(worldName, "finalize");
					
					// Mensaje de confirmación de terminación del agente World
					String message = this.receiveMessage();
					
					//System.out.println("AGENTGPS FINISH WORLD message: " + message);
					
					this.state = FINISH;
					
					break;             
				case FINISH:
					
					if(DEBUG)
						System.out.println("AgentGPS status: FINISH");
					
					if(this.response.contains("finalize")) {
						JsonObject confirmationMessage = new JsonObject();
						
						confirmationMessage.add("gps", "finish");

						this.sendMessage(carName, confirmationMessage.toString());
					}
					
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
		
		System.out.println("AgentGPS has just finished");
		
		super.finalize();
	}
}