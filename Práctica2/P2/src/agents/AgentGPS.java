
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	
    private JsonObject responseObject;
	private JsonObject commandObject;
        
	private int state;
	private boolean finish;
	private boolean needUpdate;
	private AgentID worldName;
	private AgentID radarName;
	private AgentID carName;
	private AgentID scannerName;
	private AgentID gpsName;
	private AgentID movementName;

	private int cont;

	int coordX;
	int coordY;

	/**
	 * @param aid El ID de agente para crearlo.
         * @param radarName El nombre del agente radar (para comunicación)
         * @param carName El nombre del agente car (para comunicación)
         * @param gpsName El nombre del propio agente
         * @param movementName El nombre del agente movement
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentGPS(AgentID gpsName,AgentID radarName,AgentID carName,AgentID movementName,AgentID worldName,AgentID scannerName) throws Exception {
		super(gpsName);
		
		this.radarName=radarName;
		this.carName=carName;
		this.scannerName=scannerName;
		this.gpsName=gpsName;
		this.movementName=movementName;
		this.worldName=worldName;
		
		this.cont = 0;
		
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
					
					System.out.println("AgentGPS status: WAKE_WORLD");
					
					Agent worldMap;
					try {
						worldMap = new AgentWorld(worldName,radarName,gpsName,movementName);
						worldMap.start();
						
						this.state = IDLE;
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
					}
					
					break;         
				case IDLE:
					
					System.out.println("AgentGPS status: IDLE");
					
					String response = this.receiveMessage();
					
					if(response.contains("CRASHED") || response.contains("finalize")) 
						this.state = FINISH_WORLD;
					else {
						this.responseObject = Json.parse(response).asObject();
						
						this.state = PROCESS_DATA;
					}

					break;
				case PROCESS_DATA:
					
					System.out.println("AgentGPS status: PROCESS_DATA");
					
					JsonObject gpsData = responseObject.get("gps").asObject();
					
					int nX = gpsData.get("x").asInt();
					int nY = gpsData.get("y").asInt();
					if(nX==coordX&&nY==coordY){
						needUpdate=false;
					}
					else{
						needUpdate=true;
						coordX=nX;
						coordY=nY;
						cont++;
					}
					
					this.state = UPDATE_WORLD;

					break;              
				case UPDATE_WORLD:
					
					System.out.println("AgentGPS status: UPDATE_WORLD");
					
					this.commandObject = new JsonObject();
					
					if(needUpdate){
						JsonObject gpsCoords = new JsonObject();
						gpsCoords.add("x",""+coordX);
						gpsCoords.add("y",Integer.toString(coordY));
						
						this.commandObject.add("gps", gpsCoords);
						
						this.commandObject.add("cont", cont);

					}
					else
						this.commandObject.add("gps","updated");
					
					this.sendMessage(worldName, commandObject.toString());
					this.sendMessage(scannerName, commandObject.toString());
					
					this.state = WAIT_WORLD;

					break;                   
				case WAIT_WORLD:
					
					System.out.println("AgentGPS status: WAIT_WORLD");
					
					String confirmation = this.receiveMessage();
					
					JsonObject confirmationObject = Json.parse(confirmation).asObject();
					boolean confirmationResult = confirmationObject.get("gps").asBoolean();
					
					if(!confirmationResult)
						this.state = UPDATE_WORLD;
					else
						this.state = WARN_RADAR;
					break;
                                        
				case WARN_RADAR:
					
					System.out.println("AgentGPS status: WARN_RADAR");
					
					this.commandObject = new JsonObject();
					this.commandObject.add("gps","ok");
					
					this.sendMessage(radarName, commandObject.toString());
					
					this.state = SEND_CONFIRMATION;
					
					break;                
				case SEND_CONFIRMATION:
					
					System.out.println("AgentGPS status: SEND_CONFIRMATION");
					
					this.commandObject = new JsonObject();
					
					this.commandObject.add("gps","ok");
					this.commandObject.add("cont", cont);
					
					this.sendMessage(carName, commandObject.toString());
					
					this.state = IDLE;
					
					break;
				case FINISH_WORLD:
					
					System.out.println("AgentGPS status: FINISH_WORLD");
					
					// Se ejecuta cuando se encentra logout o un CRASH. Mata a el agente world y pasa a estado de finalización.
					this.sendMessage(worldName, "finalize");
					
					this.state=FINISH;
					
					break;             
				case FINISH:
					
					System.out.println("AgentGPS status: FINISH");
					
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