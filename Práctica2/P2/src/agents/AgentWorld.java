
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * Clase que define al agente World.
 * @author Bryan Moreno Picamán & Aarón Rodríguez Bueno
 * 
 * @author Bryan Moreno Picamán & Hugo Maldonado.
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
        private boolean needUpdate;
        private String radarName = "";
        private String gpsName = "";
        private String movementName = "";
        private String world = "";
        
        private int cont=0;
        
	/**
	 * @param aid El ID de agente para crearlo.
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentWorld(String worldName,String radarName,String gpsName,String movementName) throws Exception {
		super(worldName);
		this.worldName = worldName;
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
      	
		System.out.println("AgentWorld awake.");
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
                                    String responseGPS = this.receiveMessage(this.gpsName);
                                    this.responseObject = Json.parse(responseGPS).asObject();
                                    String resultGPS = responseObject.get("gps").asString();

                                    if(!result.contains("updated"))
                                        this.updateWorld(resultGPS);
                                    this.state = WAIT_RADAR;
                                    break;
                                    
                                case WAIT_RADAR:
                                    String responseRadar = this.receiveMessage(this.gpsName);
                                    this.responseObject = Json.parse(responseRadar).asObject();
                                    String resultRadar = responseObject.get("gps").asString();
                                    this.updateWorld(resultRadar);
                                    this.state = WARN_RADAR;
                                    break;
                                    
				case WARN_RADAR:
					this.commandObject = new JsonObject();
					if(needUpdate)
						this.commandObject.add(Integer.toString(coordX),Integer.toString(coordY));
					else
						this.commandObject.add("gps","updated");

                                        this.sendMessage(worldName, commandObject.toString());
                                        this.state=WAIT_WORLD;
                                        
                                    break;
                                        
				case WAIT_MOVEMENT:
                                    String confirmation = this.receiveMessage(this.movementName);
                                    JsonObject confirmationObject = Json.parse(confirmation).asObject();
                                    String confirmationResult = confirmationObject.get("sendWorld").toString();
                                    if(confirmationResult.contains("request"))
                                        this.state= SEND_INFO;
                                        
				case SEND_INFO:
                                    this.sendWorld(movementName);
                                    this.state=IDLE;
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

    private void updateWorld(String resultRadar) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void sendWorld() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}