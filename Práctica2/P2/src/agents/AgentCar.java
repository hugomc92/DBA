
package agents;

import agents.Agent;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Clase que define al agente coche, que va a actuar como controlador y va a ser el único que se conecte al servidor.
 * 
 * @author Hugo Maldonado & Bryan Moreno Picamán
 */
public class AgentCar extends Agent {
	
	private static final int WAKE_AGENTS = 0;
	private static final int LOGIN_SERVER = 1;
	private static final int WAIT_SERVER = 2;
	private static final int IDLE = 3;
	private static final int SEND_PROCEED = 4;
	private static final int WAIT_MOVEMENT = 5;
	private static final int SEND_MOVEMENT = 6;
	private static final int FINALIZE_MOVEMENT = 7;
	private static final int FINISH = 8;
	
	private int state;
	private boolean finish;
	private String key;
	private boolean refuel;
	
	private String world;
	private String movementName;
	private String scannerName;
	private String radarName;
	private String gpsName;
	private String batteryName;
        private String carName;
	
	private JsonObject responseObject;
	private JsonObject commandObject;
	
	private int numAgents;
	
	private int gpsCont;
	
	
	/**
	 * @param aid El ID de agente para crearlo.
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentCar(AgentID aid,String carName) throws Exception {
		super(aid);
                this.carName= carName;

	}
	
	 /**
	  * Método de inicialización del agente Coche.
	  */
	@Override
	public void init() {
		
		this.state = WAKE_AGENTS;
		this.finish = false;
		this.key = "";
		this.refuel = false;
		
		this.world = "map1";
		this.movementName = "Movement";
		this.scannerName = "Scanner";
		this.radarName = "Radar";
		this.gpsName = "GPS";
		this.batteryName = "Battery";
		this.responseObject = new JsonObject();
		
		this.numAgents = 4;
		
		this.gpsCont = -1;
		
		System.out.println("AgentCar has just started");
	}
	
	/**
	  * Método de ejecución del agente Coche.
          * @au
	  */
	@Override
	public void execute() {
		
		System.out.println("AgentCar execution");
		
		while(!finish) {
			switch(state) {
				case WAKE_AGENTS:
					Agent movement = new AgentMovement(new AgentID(movementName));
					movement.start();
					
					Agent scanner = new AgentScanner(new AgentID(scannerName));
					scanner.start();
					
					Agent radar = new AgentRadar(new AgentID(radarName));
					radar.start();
					
					Agent gps;
                                        try {
                                            gps = new AgentGPS(new AgentID(gpsName),radarName,carName,gpsName,movementName);
                                            gps.start();
                                        } catch (Exception ex) {
                                            Logger.getLogger(AgentCar.class.getName()).log(Level.SEVERE, null, ex);
                                        }
					
					Agent battery;
                                        try {
                                            battery = new AgentBattery(new AgentID(batteryName),this.carName);
                                            battery.start();
                                            this.state = LOGIN_SERVER;
                                        } catch (Exception ex) {
                                            Logger.getLogger(AgentCar.class.getName()).log(Level.SEVERE, null, ex);
                                        }

					
					break;
				case LOGIN_SERVER:
					JsonObject loginCommand = new JsonObject();
					
					loginCommand.add("command", "login");
					loginCommand.add("world", world);
					loginCommand.add("radar", radarName);
					loginCommand.add("scanner", scannerName);
					loginCommand.add("battery", batteryName);
					loginCommand.add("gps", gpsName);
					
					this.sendMessage("Izar", loginCommand.toString());
					
					this.state = WAIT_SERVER;
					
					break;
				case WAIT_SERVER:
					String response = this.receiveMessage("Izar");
					
					this.responseObject = Json.parse(response).asObject();
					
					String result = responseObject.get("result").asString();
					
					if(result.contains("BAD_") || result.equals("CRASHED"))
						this.state = FINALIZE_MOVEMENT;
					else {
						this.key = result;
						
						this.state = IDLE;
					}
					
					break;
				case IDLE:
					
					for(int i=0; i<numAgents; i++) {
						String message = this.receiveMessage("*");
						
						if(message.contains("battery")) {
							this.responseObject = Json.parse(message).asObject();
							
							this.refuel = responseObject.get("refuel").asBoolean();
						}
						else if(message.contains("gps")) {
							this.responseObject = Json.parse(message).asObject();
							
							this.gpsCont = responseObject.get("Cont").asInt();
						}
					}
					
					this.state = SEND_PROCEED;
					
					break;
				case SEND_PROCEED:
					if(this.refuel) {
						
						this.refuel = false;
						
						this.commandObject = new JsonObject();
						
						this.commandObject.add("command", "refuel");
						this.commandObject.add("key", key);
						
						this.state = SEND_MOVEMENT;
					}
					else {
						JsonObject confirmation = new JsonObject();
					
						confirmation.add("calculate", gpsCont);

						this.sendMessage(movementName, confirmation.toString());
					}
					
					break;
				case WAIT_MOVEMENT:
					
					String movement = this.receiveMessage(movementName);
					
					JsonObject movementObject = Json.parse(movement).asObject();
					
					movement = movementObject.get("mov").asString();
					
					this.commandObject = new JsonObject();
					
					this.commandObject.add("command", movement);
					this.commandObject.add("key", key);
					
					break;
				case SEND_MOVEMENT:
					this.sendMessage("Izar", this.commandObject.toString());
					
					this.state = WAIT_SERVER;
					
					break;
				case FINALIZE_MOVEMENT:
					// Se ejecuta cuando se encentra logout o alguna petición mala. Mata a todos los agentes como controlador.
					this.sendMessage(movementName, "finalize");
					this.sendMessage(scannerName, "finalize");
					this.sendMessage(radarName, "finalize");
					this.sendMessage(gpsName, "finalize");
					this.sendMessage(batteryName, "finalize");
					
					this.finish = true;
					break;
				case FINISH:
					// Se ejecuta cuando se encuentra CRASHED, mata sólo a AgentMovement porque al resto le llegan los CRASHED igualmente.
					this.sendMessage(movementName, "finalize");
					
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
		
		System.out.println("AgentCar has just finish");
		
		super.finalize();
	}
}