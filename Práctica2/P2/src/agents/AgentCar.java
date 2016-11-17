
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
	
	private boolean loggedIn;
	private boolean logout;
	
	
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
		
		this.loggedIn = false;
		
		this.logout = false;
		
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
						 scanner = new AgentScanner(scannerName, movementName, gpsName, this.getAid());
						 scanner.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
						this.state = FINALIZE_MOVEMENT;
					}
					
					Agent radar;
					try {
						 radar = new AgentRadar(radarName, this.getAid(), worldName);
						 radar.start();
					} catch (Exception ex) {
						System.err.println(ex.getMessage());
						this.state = FINALIZE_MOVEMENT;
					}
					
					Agent gps;
					try {
						gps = new AgentGPS(gpsName, radarName, this.getAid(), movementName, worldName,scannerName);
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
					
					
					//SIMULACIÓN SERVER
					
					
//					JsonObject simulacro;
//					
//					//Scanner
//					simulacro = new JsonObject();
//					JsonArray jscanner = new JsonArray();
//					float  vscanner [] = {75.00667f,75.690155f,76.38062f, 77.07788f,77.781746f,75.74299f,76.41989f, 77.10383f,77.7946f,78.492035f,76.48529f, 77.155685f,77.83315f,78.51752f,79.20859f,77.23341f,77.89737f,78.56844f,79.24645f,79.93122f,77.987175f,78.64477f,79.30952f,79.98125f,80.65978f};
//					for (int i = 0; i < vscanner.length; i++){
//						jscanner.add(vscanner[i]);
//					}
//					simulacro.add("scanner",jscanner);
//					this.sendMessage(scannerName, simulacro.toString());
//					
//					//radar
//					simulacro = new JsonObject();
//					JsonArray jradar = new JsonArray();
//					int  vradar [] = {1,1,1,0,0,1,1,0,0,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
//					for (int i = 0; i < vradar.length; i++){
//						jradar.add(vscanner[i]);
//					}
//					simulacro.add("radar",jradar);
//					this.sendMessage(radarName, simulacro.toString());
//					
//					//gps
//					simulacro = new JsonObject();
//					simulacro.add("gps",new JsonObject().add("x",94).add("y",94));					
//					this.sendMessage(gpsName, simulacro.toString());
//					
//					//battery
//					simulacro = new JsonObject();
//					simulacro.add("battery",95.0f);					
//					this.sendMessage(batteryName, simulacro.toString());
					
					this.state = WAIT_SERVER;
					
					break;
				case WAIT_SERVER:
					
					System.out.println("AgentCar status: WAIT_SERVER");
					
					String response = this.receiveMessage();
					
					System.out.println("SERVER MESSAGE: " + response);
					
					if(response.contains("result")) {
						
						System.out.println("LOGGED IN: " + loggedIn);
						
						this.responseObject = Json.parse(response).asObject();
					
						String result = responseObject.get("result").asString();
						
						if(result.contains("BAD_"))
							this.state = FINALIZE_MOVEMENT;
						else if(result.contains("CRASHED"))
							this.state = FINISH;
						else {
							if(!loggedIn) {
								this.key = result;

								loggedIn = true;
							}

							this.state = IDLE;
						}
					}
					else if(response.contains("trace") && loggedIn) {
						
						System.out.println("SERVER MESSAGE TRACE: \n" + loggedIn);
						
						try {
							this.responseObject = Json.parse(response).asObject();

							JsonArray trace = responseObject.get("trace").asArray();
							
							printTrace(trace);
						} finally {
							this.state = FINALIZE_MOVEMENT;
						}
						
						/*try {
							this.responseObject = Json.parse(response).asObject();

							JsonArray trace = responseObject.get("trace").asArray();

							byte data[] = new byte[trace.size()];

							for(int i=0; i<data.length; i++)
								data[i] = (byte) trace.get(i).asInt();
						
							FileOutputStream fos;
							DateFormat df = new SimpleDateFormat("MM/dd/yyyy-HH:mm");
							Date today = Calendar.getInstance().getTime();        

							String date = df.format(today);

							fos = new FileOutputStream(new File("Trace-" + map + "-" + date +  ".png"));
							fos.write(data);
							fos.close();

							System.out.println("Saved trace");
							
						} catch(IOException ex) {
							System.err.println("Error procesing trace");
							
							System.err.println(ex.getMessage());
						} finally {
							this.state = FINALIZE_MOVEMENT;
						}*/
					}
					
					break;
				case IDLE:
					
					System.out.println("AgentCar status: IDLE");
					
					for(int i=0; i<numAgents; i++) {
						String message = this.receiveMessage();
						
						if(message.contains("battery")) {
							this.responseObject = Json.parse(message).asObject();

							this.refuel = responseObject.get("battery").asBoolean();
							
							System.out.println("AGENTCAR IDLE REFUEL: " + refuel);
						}
						else if(message.contains("gps")) {
							this.responseObject = Json.parse(message).asObject();

							this.gpsCont = responseObject.get("cont").asInt();
						}
						else if(message.contains("radar")) {
							this.responseObject = Json.parse(message).asObject();

						}
						else if(message.contains("scanner")) {
							System.out.println("Llega el mensaje de scanner");
							this.responseObject = Json.parse(message).asObject();
						}
					}
					
					this.state = SEND_PROCEED;
					
					break;
				case SEND_PROCEED:
					
					System.out.println("AgentCar status: SEND_PROCEED");
					
					int gpsConfirmation = gpsCont;
					
					if(this.refuel)
						gpsConfirmation = -1;
					
					JsonObject confirmation = new JsonObject();
					
					System.out.println("\n\nAGENTCAR SEND_PROCEED gpsConfirmation: " + gpsConfirmation);

					confirmation.add("calculate", gpsConfirmation);

					this.sendMessage(movementName, confirmation.toString());

					this.state = WAIT_MOVEMENT;
					
					break;
				case WAIT_MOVEMENT:
					
					System.out.println("AgentCar status: WAIT_MOVEMENT");
					
					String movementExecution = this.receiveMessage();
					
					JsonObject movementObject = Json.parse(movementExecution).asObject();
					
					movementExecution = movementObject.get("mov").asString();
					
					if(!movementExecution.contains("NO")) {
						this.commandObject = new JsonObject();

						this.commandObject.add("command", movementExecution);
						this.commandObject.add("key", key);
					}
					else {
						this.commandObject = new JsonObject();

						this.commandObject.add("command", "refuel");
						this.commandObject.add("key", key);

						this.refuel = false;
					}
					
					// PRUEBAS PARA LOGOUT PREMATUROS
					if(this.gpsCont > 80) {
						this.logout = true;
						this.commandObject.add("command", "logout");
						this.commandObject.add("key", key);
						this.state = FINALIZE_MOVEMENT;
					}
					else
						this.state = SEND_COMMAND;

					break;
				case SEND_COMMAND:
					
					System.out.println("AgentCar status: SEND_COMMAND");
					
					if(!commandObject.toString().contains("logout")) {
						this.state = WAIT_SERVER;
						
						this.sendMessage(this.serverAgent, this.commandObject.toString());
					}
					else {
						this.responseObject = commandObject;
						
						this.logout = true;
						
						this.state = FINALIZE_MOVEMENT;
					}
					
					break;
				case FINALIZE_MOVEMENT:
					
					System.out.println("AgentCar status: FINALIZE_MOVEMENT");
					
					// Se ejecuta cuando se encentra logout o alguna petición mala. Mata a todos los agentes como controlador.
					this.sendMessage(movementName, "finalize");
					this.sendMessage(scannerName, "finalize");
					this.sendMessage(radarName, "finalize");
					this.sendMessage(gpsName, "finalize");
					this.sendMessage(batteryName, "finalize");
					
					for(int i=0; i<=numAgents; i++) {
						String message = this.receiveMessage();
						
						System.out.println("FINALIZE MOVEMENT( " + i + "): " + message);
					}
					
					if(this.logout)
						this.sendMessage(this.serverAgent, this.commandObject.toString());
					
					this.state = FINISH;
					
					break;
				
				case FINISH:
					
					System.out.println("AgentCar status: FINISH");
					
					System.out.println("AGENTCAR FINISH responseObjet: " + responseObject.toString());

					if(!responseObject.toString().contains("BAD_") && !responseObject.toString().contains("CRASHED")) {
						// Esperamos los mensajes del logout, es decir, la traza y el OK.
						for(int j=0;j<2;j++) {
							String responseF = this.receiveMessage();

							System.out.println("SERVER MESSAGE LOGOUT: " + responseF);

							if(responseF.contains("result")) {
								System.out.println("Resultado: " + responseF);
							}
							else if(responseF.contains("trace") && loggedIn) {

								System.out.println("SERVER MESSAGE TRACE: " + loggedIn);
								
								this.responseObject = Json.parse(responseF).asObject();

								JsonArray trace = responseObject.get("trace").asArray();

								printTrace(trace);
							}
						}
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
		
		System.out.println("AgentCar has just finished");
		
		super.finalize();
		
		//System.exit(0);
	}
	
	private void printTrace(JsonArray trace) {
			
		try {
			byte data[] = new byte[trace.size()];

			for(int i=0; i<data.length; i++)
				data[i] = (byte) trace.get(i).asInt();

			FileOutputStream fos;
			DateFormat df = new SimpleDateFormat("dd-MM-yyyy-HH.mm");
			Date today = Calendar.getInstance().getTime();        

			String date = df.format(today);

			fos = new FileOutputStream(new File("traces/" + map + "/Trace." + map + "." + date +  ".png"));
			fos.write(data);
			fos.close();

			System.out.println("Saved trace");

		} catch(IOException ex) {
			System.err.println("Error procesing trace");

			System.err.println(ex.getMessage());
		}
	}
}