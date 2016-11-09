
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Clase que define al agente coche, que va a actuar como controlador y va a ser el único que se conecte al servidor.
 * 
 * @author Hugo Maldonado & Bryan Moreno Picamán & Aarón Rodríguez Bueno
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
	
	private final AgentID serverAgent;
	private final String map;
	private final AgentID movementName;
	private final AgentID scannerName;
	private final AgentID radarName;
	private final AgentID gpsName;
	private final AgentID batteryName;
	private final AgentID worldName;
	
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
	public AgentCar(AgentID carName, AgentID serverAgent, String map, AgentID movementName, AgentID scannerName, AgentID radarName, AgentID gpsName, AgentID worldName, AgentID batteryName) throws Exception {
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
					
					System.out.println("AgentCar status: WAKE_AGENTS");
					
					Agent movement;
					try {
						movement = new AgentMovement(movementName, worldName, scannerName, this.getAid());
						movement.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
						this.state = FINALIZE_MOVEMENT;
					}
					
					Agent scanner;
					try {
						 scanner = new AgentScanner(scannerName, movementName, gpsName);
						 scanner.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
						this.state = FINALIZE_MOVEMENT;
					}
					
					Agent radar;
					try {
						 radar = new AgentRadar(radarName, gpsName, worldName);
						 radar.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
						this.state = FINALIZE_MOVEMENT;
					}
					
					Agent gps;
					try {
						gps = new AgentGPS(gpsName, radarName, this.getAid(), movementName, worldName);
						gps.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
						this.state = FINALIZE_MOVEMENT;
					}

					Agent battery;
					try {
						battery = new AgentBattery(batteryName,this.getAid());
						battery.start();
						this.state = LOGIN_SERVER;
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
						this.state = FINALIZE_MOVEMENT;
					}

					
					break;
				case LOGIN_SERVER:
					
					System.out.println("AgentCar status: LOGIN_SERVER");
					
					JsonObject loginCommand = new JsonObject();
					
					loginCommand.add("command", "login");
					loginCommand.add("world", map);
					loginCommand.add("radar", radarName.getLocalName());
					loginCommand.add("scanner", scannerName.getLocalName());
					loginCommand.add("battery", batteryName.getLocalName());
					loginCommand.add("gps", gpsName.getLocalName());
					
					this.sendMessage(this.serverAgent, loginCommand.toString());
					
					this.state = WAIT_SERVER;
					
					break;
				case WAIT_SERVER:
					
					System.out.println("AgentCar status: WAIT_SERVER");
					
					String response = this.receiveMessage();
					
					System.out.println("SERVER MESSAGE AFTER LOGIN: \n" + response);
					
					if(response.contains("result")) {
						this.responseObject = Json.parse(response).asObject();
					
						String result = responseObject.get("result").asString();

						if(result.contains("BAD_"))
							this.state = FINALIZE_MOVEMENT;
						else {
							this.key = result;

							this.state = IDLE;
						}
					}
					else if(response.contains("trace")) {
						
						try {
							this.responseObject = Json.parse(response).asObject();

							JsonArray trace = responseObject.get("trace").asArray();

							byte data[] = new byte[trace.size()];

							for(int i=0; i<data.length; i++)
								data[i] = (byte) trace.get(i).asInt();
						
							FileOutputStream fos = new FileOutputStream("trace.png");
							fos.write(data);
							fos.close();

							System.out.println("Saved trace");
							
						} catch(IOException ex) {
							System.err.println("Error procesing trace");
							
							System.err.println(ex.getMessage());
						} finally {
							this.state = FINALIZE_MOVEMENT;
						}
					}
					else
						this.state = FINALIZE_MOVEMENT;
					
					break;
				case IDLE:
					
					System.out.println("AgentCar status: IDLE");
					
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
					
					System.out.println("AgentCar status: SEND_PROCEED");
					
					/*if(this.refuel) {
						
						this.commandObject = new JsonObject();
						
						this.commandObject.add("command", "refuel");
						this.commandObject.add("key", key);
						
						this.refuel = false;
						
						this.state = SEND_COMMAND;
					}
					else {*/
						JsonObject confirmation = new JsonObject();
					
						confirmation.add("calculate", gpsCont);

						this.sendMessage(movementName, confirmation.toString());
						
						this.state = WAIT_MOVEMENT;
					//}
					
					break;
				case WAIT_MOVEMENT:
					
					System.out.println("AgentCar status: WAIT_MOVEMENT");
					
					String movementExecution = this.receiveMessage();
					
					JsonObject movementObject = Json.parse(movementExecution).asObject();
					
					movementExecution = movementObject.get("mov").asString();
					
					this.commandObject = new JsonObject();
					
					this.commandObject.add("command", movementExecution);
					this.commandObject.add("key", key);
					
					// PROVISIONAL MIENTRAS BUSCO OTRA SOLUCIÓN AL INTERBLOQUEO
					if(this.refuel) {
						
						this.commandObject = new JsonObject();
						
						this.commandObject.add("command", "refuel");
						this.commandObject.add("key", key);
						
						this.refuel = false;
						
						this.state = SEND_COMMAND;
					}
					
					this.state = SEND_COMMAND;
					
					break;
				case SEND_COMMAND:
					
					System.out.println("AgentCar status: SEND_COMMAND");
					
					this.sendMessage(this.serverAgent, this.commandObject.toString());
					
					this.state = WAIT_SERVER;
					
					break;
				case FINALIZE_MOVEMENT:
					
					System.out.println("AgentCar status: FINALIZE_MOVEMENT");
					
					// Se ejecuta cuando se encentra logout o alguna petición mala. Mata a todos los agentes como controlador.
					this.sendMessage(movementName, "finalize");
					this.sendMessage(scannerName, "finalize");
					this.sendMessage(radarName, "finalize");
					this.sendMessage(gpsName, "finalize");
					this.sendMessage(batteryName, "finalize");
					
					this.finish = true;
					
					break;
				case FINISH:
					
					System.out.println("AgentCar status: FINISH");
					
					// Se ejecuta cuando se encuentra CRASHED, mata sólo a AgentMovement porque al resto le llegan los CRASHED igualmente.
					this.sendMessage(movementName, "finalize");
					
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
		
		System.out.println("AgentCar has just finished");
		
		super.finalize();
	}
}