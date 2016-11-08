
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

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
	private static final int SEND_COMMAND = 6;
	private static final int FINALIZE_MOVEMENT = 7;
	private static final int FINISH = 8;
	
	private int state;
	private boolean finish;
	private String key;
	private boolean refuel;
	
	private final String serverAgent;
	private final String map;
	private final String movementName;
	private final String scannerName;
	private final String radarName;
	private final String gpsName;
	private final String batteryName;
	private final String worldName;
	
	private JsonObject responseObject;
	private JsonObject commandObject;
	
	private int numAgents;
	
	private int gpsCont;
	
	
	/**
	 * @param carName El nombre del coche de agente para crearlo.
	 * @param serverAgent El nombre del agente del servidor con el que se va a comunicar
	 * @param map El mapa al que se va a conectar.
	 * @param movementName El nombre del agente encargado del movimiento.
	 * @param radarName El nombre del agente del radar
	 * @param scannerName El nombre del agente del scanner.
	 * @param gpsName El nombre del agente del GPS.
	 * @param worldName El nombre del agente del mundo.
	 * @param batteryName El nombre del agente de la batería.
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentCar(String carName, String serverAgent, String map, String movementName, String scannerName, String radarName, String gpsName, String worldName, String batteryName) throws Exception {
		super(carName);

		this.serverAgent = serverAgent;
		this.map = map;
		this.movementName = movementName;
		this.scannerName = scannerName;
		this.radarName = radarName;
		this.gpsName = gpsName;
		this.worldName = worldName;
		this.batteryName = batteryName;
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
		
		this.responseObject = new JsonObject();
		
		this.numAgents = 4;
		
		this.gpsCont = -1;
		
		System.out.println("AgentCar has just started");
	}
	
	/**
	  * Método de ejecución del agente Coche.
	  */
	@Override
	public void execute() {
		
		System.out.println("AgentCar execution");
		
		while(!finish) {
			switch(state) {
				case WAKE_AGENTS:
					
					Agent movement;
					try {
						movement = new AgentMovement(movementName, worldName, scannerName, this.getName());
						movement.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
					}
					
					Agent scanner;
					try {
						 scanner = new AgentScanner(scannerName, movementName, gpsName);
						 scanner.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
					}
					
					Agent radar;
					try {
						 radar = new AgentRadar(radarName);
						 radar.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
					}
					
					Agent gps;
					try {
						gps = new AgentGPS(gpsName, radarName, this.getName(), movementName, worldName);
						gps.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
					}

					Agent battery;
					try {
						battery = new AgentBattery(batteryName,this.getName());
						battery.start();
						this.state = LOGIN_SERVER;
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
					}

					
					break;
				case LOGIN_SERVER:
					JsonObject loginCommand = new JsonObject();
					
					loginCommand.add("command", "login");
					loginCommand.add("world", map);
					loginCommand.add("radar", radarName);
					loginCommand.add("scanner", scannerName);
					loginCommand.add("battery", batteryName);
					loginCommand.add("gps", gpsName);
					
					this.sendMessage(this.serverAgent, loginCommand.toString());
					
					this.state = WAIT_SERVER;
					
					break;
				case WAIT_SERVER:
					String response = this.receiveMessage();
					
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
						String message = this.receiveMessage();
						
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
						
						this.state = SEND_COMMAND;
					}
					else {
						JsonObject confirmation = new JsonObject();
					
						confirmation.add("calculate", gpsCont);

						this.sendMessage(movementName, confirmation.toString());
						
						this.state = WAIT_MOVEMENT;
					}
					
					break;
				case WAIT_MOVEMENT:
					
					String movementExecution = this.receiveMessage();
					
					JsonObject movementObject = Json.parse(movementExecution).asObject();
					
					movementExecution = movementObject.get("mov").asString();
					
					this.commandObject = new JsonObject();
					
					this.commandObject.add("command", movementExecution);
					this.commandObject.add("key", key);
					
					this.state = SEND_COMMAND;
					
					break;
				case SEND_COMMAND:
					this.sendMessage(this.serverAgent, this.commandObject.toString());
					
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