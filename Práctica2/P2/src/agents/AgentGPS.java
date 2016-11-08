
package agents;

import agents.Agent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase que define al agente GPS, actua como controlador de AgentWorld y AgentRadar.
 * 
 * @author Bryan Moreno Picamán & Aarón Rodríguez Bueno
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
        private String worldName = "";
        private String radarName = "";
        private String carName = "";
        private String gpsName = "";
        private String movementName = "";
        
        private int cont=0;
        
        int coordX=-1;
        int coordY=-1;

	/**
	 * @param aid El ID de agente para crearlo.
         * @param radarName El nombre del agente radar (para comunicación)
         * @param carName El nombre del agente car (para comunicación)
         * @param gpsName El nombre del propio agente
         * @param movementName El nombre del agente movement
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentGPS(String gpsName,String radarName,String carName,String movementName,String worldName) throws Exception {
		super(gpsName);
                this.radarName=radarName;
                this.carName=carName;
                this.gpsName=gpsName;
                this.movementName=movementName;
                this.worldName=worldName;
	}


	 /**
	  * Método de inicialización del agente GPS.
	  */
	@Override
	public void init() {
		this.state = WAKE_WORLD;
      		this.finish = false;
                this.responseObject = new JsonObject();
      	
		System.out.println("AgentGPS awake.");
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
                                Agent worldMap;
                                try {
                                    worldMap = new AgentWorld(new AgentID(this.worldName),radarName,gpsName,movementName);
                                    worldMap.start();			
                                    this.state = IDLE;
                                } catch (Exception ex) {
                                    Logger.getLogger(AgentGPS.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                    break;
                                    
				case IDLE:
                                    String response = this.receiveMessage("Izar");
                                    this.responseObject = Json.parse(response).asObject();
                                    String result = responseObject.get("gps").asString();

                                    if(result.contains("BAD_") || result.equals("CRASHED"))
                                        this.state = FINISH_WORLD;
                                    else {
                                        this.state = PROCESS_DATA;    
                                    }
					
                                    break;
                                    
                                case PROCESS_DATA:
                                    int nX = responseObject.get("x").asInt();
                                    int nY = responseObject.get("y").asInt();
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
                                    this.commandObject = new JsonObject();
                                    if(needUpdate){
                                        this.commandObject.add(Integer.toString(coordX),Integer.toString(coordY));
                                        this.commandObject.add("cont",Integer.toString(cont));

                                    }
                                    else
                                        this.commandObject.add("gps","updated");

                                        this.sendMessage(worldName, commandObject.toString());
                                        this.state=WAIT_WORLD;
                                        
                                    break;
                                        
				case WAIT_WORLD:
                                    String confirmation = this.receiveMessage(this.worldName);
                                    JsonObject confirmationObject = Json.parse(confirmation).asObject();
                                    String confirmationResult = confirmationObject.get("gps").toString();
                                    if(confirmationResult.contains("not_ok"))
                                            this.state = UPDATE_WORLD;
                                    else
                                            this.state= WARN_RADAR;
                                    break;
                                        
				case WARN_RADAR:
                                    this.commandObject = new JsonObject();
                                    this.commandObject.add("gps","ok");
                                    this.sendMessage(radarName, commandObject.toString());
                                    this.state=SEND_CONFIRMATION;
                                    break;
                                    
				case SEND_CONFIRMATION:
                                    this.commandObject = new JsonObject();
                                    this.commandObject.add("gps","ok");
                                    this.commandObject.add("cont",Integer.toString(cont));
                                    this.sendMessage(carName, commandObject.toString());
                                    this.state=IDLE;
                                    break;

				case FINISH_WORLD:
                                    // Se ejecuta cuando se encentra logout o un CRASH. Mata a el agente world y pasa a estado de finalización.
                                    this.sendMessage(worldName, "finalize");
                                    this.state=FINISH;
                                    break;
                                    
				case FINISH:
                                    this.finish = true;
                                    break;
			}
		}
		
		this.finalize();
	}
	
	/**
	  * Método de finalización del agente Coche.
	  */
	@Override
	public void finalize() {
		System.out.println("AgentGPS has just finish");
		super.finalize();
	}

}