
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
 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
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
	
	private static final boolean DEBUG = false;
	
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
	 * Constructor
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
	  * @author Hugo Maldonado
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
		
		if(DEBUG)
			System.out.println("AgentCar has just started");
	}
	
	/**
	 * Estado WAKE_AGENTS: levanta a los demás agentes y pasa al estado LOGIN_SERVER
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateWakeAgents(){
		if(DEBUG)
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
			gps = new AgentGPS(gpsName, radarName, this.getAid(), movementName, worldName,scannerName, map);
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
	}
	
	/**
	 * Estado LOGIN_SERVER: manda petición de login al server y pasa al estado WAIT_SERVER
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateLoginServer(){
		if(DEBUG)
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
	}
	
	/**
	 * Estado WAIT_SERVER: recibe mensaje del server. 
	 * Si devuelve la key, se ha logueado correctamente y va a IDLE.
	 * Si devuelve BAD_CONSEQUENCE, va a FINALIZE_MOVEMENT.
	 * Si devuelve CRASHED, va a FINISH.
	 * Si recibe la traze, la pinta
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateWaitServer(){
		if(DEBUG)
			System.out.println("AgentCar status: WAIT_SERVER");

		String response = this.receiveMessage();

		if(DEBUG)
			System.out.println("SERVER MESSAGE: " + response);

		if(response.contains("result")) {
			if(DEBUG)
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
		else if(response.contains("trace")) {

			if(DEBUG)
				System.out.println("SERVER MESSAGE TRACE: \n" + loggedIn);

			this.responseObject = Json.parse(response).asObject();

			JsonArray trace = responseObject.get("trace").asArray();

			printTrace(trace, true);
		}
	}

	/**
	 * Estado IDLE: espera a que le lleguen los OK de los demás agentes (en cualquier orden),
	 * y pasa a SEND_PROCEED
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateIdle(){
		if(DEBUG)
			System.out.println("AgentCar status: IDLE");

		for(int i=0; i<numAgents; i++) {
			String message = this.receiveMessage();

			if(message.contains("battery")) {
				this.responseObject = Json.parse(message).asObject();

				this.refuel = responseObject.get("battery").asBoolean();
			}
			else if(message.contains("gps")) {
				this.responseObject = Json.parse(message).asObject();

				this.gpsCont = responseObject.get("cont").asInt();
			}
			else if(message.contains("radar")) {
				this.responseObject = Json.parse(message).asObject();

			}
			else if(message.contains("scanner")) {
				this.responseObject = Json.parse(message).asObject();
			}
		}

		this.state = SEND_PROCEED;
	}

	/**
	 * Estado SEND_PROCEED: manda mensaje al Movement para que vaya calculando la traza,
	 * y va al estado WAIT_MOVEMENT
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateSendProceed(){
		if(DEBUG)
			System.out.println("AgentCar status: SEND_PROCEED");

		int gpsConfirmation = gpsCont;

		if(this.refuel)
			gpsConfirmation = -1;

		JsonObject confirmation = new JsonObject();

		confirmation.add("calculate", gpsConfirmation);

		this.sendMessage(movementName, confirmation.toString());

		this.state = WAIT_MOVEMENT;
	}

	/**
	 * Estado WAIT_MOVEMENT: espera el comando de movimiento del Movement, lo procesa
	 * y pasa al estado SEND_COMMAND.
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateWaitMovement(){
		if(DEBUG)
			System.out.println("AgentCar status: WAIT_MOVEMENT");

		String movementExecution = this.receiveMessage();

		JsonObject movementObject = Json.parse(movementExecution).asObject();

		movementExecution = movementObject.get("mov").asString();

		if(movementExecution.contains("NO")) {
			this.commandObject = new JsonObject();

			this.commandObject.add("command", "refuel");
			this.commandObject.add("key", key);

			this.refuel = false;
		}
		else if(movementExecution.contains("LO")) {
			this.logout = true;

			this.commandObject.add("command", "logout");
			this.commandObject.add("key", key);
		}
		else {
			this.commandObject = new JsonObject();

			this.commandObject.add("command", movementExecution);
			this.commandObject.add("key", key);
		}

		this.state = SEND_COMMAND;
	}
	
	/**
	 * Estado SEND_COMMAND: manda el comando al Server y pasa al estado FINALIZE_MOVEMENT.
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateSendCommand(){
		if(DEBUG)
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
	}
	
	/**
	 * Estado FINALIZE_MOVEMENT: mandamos al Movement la orden de que se finalice y pasamos 
	 * al estado FINISH
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateFinalizeMovement(){
		if(DEBUG)
			System.out.println("AgentCar status: FINALIZE_MOVEMENT");

		// Se ejecuta cuando se encentra logout o alguna petición mala. Mata a todos los agentes como controlador.
		this.sendMessage(movementName, "finalize");
		this.sendMessage(scannerName, "finalize");
		this.sendMessage(radarName, "finalize");
		this.sendMessage(gpsName, "finalize");
		this.sendMessage(batteryName, "finalize");

		for(int i=0; i<=numAgents; i++) {
			String message = this.receiveMessage();

			if(DEBUG)
				System.out.println("FINALIZE MOVEMENT(" + i + "): " + message);
		}

		if(this.logout)
			this.sendMessage(this.serverAgent, this.commandObject.toString());

		this.state = FINISH;
	}
	
	/**
	 * Estado FINISH: esperamos a los send del Server (el de logout ok y la traza),
	 * y pintamos la traza.
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateFinish(){
		if(DEBUG)
			System.out.println("AgentCar status: FINISH");

		if(DEBUG)
			System.out.println("AGENTCAR FINISH responseObjet: " + responseObject.toString());

		if(!responseObject.toString().contains("BAD_") && !responseObject.toString().contains("CRASHED")) {
			// Esperamos los mensajes del logout, es decir, la traza y el OK.
			for(int j=0;j<2;j++) {
				String responseF = this.receiveMessage();

				if(DEBUG)
					System.out.println("SERVER MESSAGE LOGOUT: " + responseF);

				if(responseF.contains("result")) {
					if(DEBUG)
						System.out.println("Resultado: " + responseF);
				}
				else if(responseF.contains("trace") && loggedIn) {

					if(DEBUG)
						System.out.println("SERVER MESSAGE TRACE: " + loggedIn);

					this.responseObject = Json.parse(responseF).asObject();

					JsonArray trace = responseObject.get("trace").asArray();

					printTrace(trace, false);
				}
			}
		}

		this.finish = true;
	}
	
	/**
	  * Método de ejecución del agente Coche.
	  * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	  */
	@Override
	public void execute() {
		
		if (DEBUG)
			System.out.println("AgentCar execution");
		
		while(!finish) {
			switch(state) {
				case WAKE_AGENTS:
					
					stateWakeAgents();
					
					break;
				case LOGIN_SERVER:
					
					stateLoginServer();
					
					break;
				case WAIT_SERVER:
					
					stateWaitServer();
					
					break;
				case IDLE:
					
					stateIdle();
					
					break;
				case SEND_PROCEED:
					
					stateSendProceed();
					
					break;
				case WAIT_MOVEMENT:
					
					stateWaitMovement();

					break;
				case SEND_COMMAND:
					
					stateSendCommand();
					
					break;
				case FINALIZE_MOVEMENT:
					
					stateFinalizeMovement();
					
					break;
				
				case FINISH:
					
					stateFinish();
					
					break;
			}
		}
	}
	
	/**
	  * Método de finalización del agente Coche.
	  * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	  */
	@Override
	public void finalize() {
		
		if(DEBUG)
			System.out.println("AgentCar has just finished");
		
		super.finalize();
	}
	
	/**
	 * Guarda la traza en un archivo png. 
	 * @param trace Array con los datos de la matriz del world
	 * @param error booleano para ponerle un nombre u otro en función de si la traza
	 * devuelta ha sido por un error o no.
	 * @author Hugo Maldonado and Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void printTrace(JsonArray trace, boolean error) {
			
		try {
			byte data[] = new byte[trace.size()];

			for(int i=0; i<data.length; i++)
				data[i] = (byte) trace.get(i).asInt();

			FileOutputStream fos;
			DateFormat df = new SimpleDateFormat("dd-MM-yyyy-HH.mm");
			Date today = Calendar.getInstance().getTime();        

			String date = df.format(today);
			
			if(error)
				fos = new FileOutputStream(new File("traces/" + map + "/Error-Trace." + map + "." + date +  ".png"));
			else
				fos = new FileOutputStream(new File("traces/" + map + "/Trace." + map + "." + date +  ".png"));
			
			fos.write(data);
			fos.close();

			if(DEBUG)
				System.out.println("Saved trace");

		} catch(IOException ex) {
			System.err.println("Error procesing trace");

			System.err.println(ex.getMessage());
		}
	}
}