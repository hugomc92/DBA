
package practica3;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.apache.commons.io.IOUtils;

/**
 * Clase que define al agente controlador
 * 
 * @author JoseDavid and Hugo Maldonado
 */
public class AgentController extends Agent {
	
	private static final int SUBS_MAP_EXPLORE = 0;
    private static final int WAKE_AGENTS_EXPLORE = 1;
    private static final int CHECK_AGENTS_EXPLORE = 2;
    private static final int RE_RUN = 3;
    private static final int SAVE_MAP = 4;
    private static final int CHECK_MAP = 5;
    private static final int SUBSCRIBE_MAP = 6;
    private static final int EXPLORE_MAP = 7;
    private static final int WAKE_AGENTS = 8;
    private static final int FINALIZE = 9;
    private static final int FUEL_INFORMATION = 10;
    private static final int CHOOSE_AGENTS = 11;
    private static final int CONTROL_AGENTS= 12;
    private static final int SEND_MAP = 13;
    private static final int CHECK_AGENTS= 14;
	
	private static final int WIDTH = 511, HEIGHT = 511;
	
	private static final boolean DEBUG = true;
	
	private final AgentID serverName;
	private final AgentID carNames[] = new AgentID[4];
    private final String map;
	
	private int [][] mapWorld;
	private int mapWorldSize;
	private int mapWorldPosX;
	private int mapWorldPosY;
	private boolean mapWorldCompleted;
    
    private int state;
    private String conversationID;
	private boolean wakedAgents;
	private boolean finish = false;
	
	private JsonObject savedMap;
    
	/**
	 * Constructor 
	 * 
	 * @param name El nombre de éste agente
	 * @param serverName El nombre del servidor
	 * @param map El nombre del mapa al que vamos a loguearnos
	 * @param car1Name El nombre del primer agente.
	 * @param car2Name El nombre del segundo agente.
	 * @param car3Name El nombre del tercer agente.
	 * @param car4Name El nombre del cuarto agente.
	 * 
	 * @throws Exception
	 * 
	 * @author JoseDavid and Hugo Maldonado
	 */
    public AgentController(AgentID name, AgentID serverName, String map, AgentID car1Name, AgentID car2Name, AgentID car3Name, AgentID car4Name) throws Exception {
		
        super(name);
		
        this.serverName = serverName;
		
		this.carNames[0] = car1Name;
        this.carNames[1] = car2Name;
		this.carNames[2] = car3Name;
		this.carNames[3] = car4Name;
		
        this.map = map;
    }
	
	/**
	  * Método de inicialización del agente Coche.
	  * 
	  * @author Hugo Maldonado
	  */
	@Override
	public void init() {
		
		this.state = CHECK_MAP;
		
		this.conversationID = "";
		this.wakedAgents = false;
		
		this.savedMap = new JsonObject();
		
		this.mapWorldSize = -1;
		
		this.mapWorldPosX = -1;
		this.mapWorldPosY = -1;
		
		this.mapWorldCompleted = false;
		
		System.out.println("AgetnController has just started");
	}
	
	/**
	 * Comprobar si el mapa está explorado o no
	 * 
     * @author Hugo Maldonado
    */
    private void stateCheckMap() {
		
		if(DEBUG)
			System.out.println("AgentController state: CHECK_MAP"); 
        
        String path = "maps/" + this.map + ".json";

		// Pasar el fichero a un JsonObject
		try {
			FileInputStream fisTargetFile = new FileInputStream(new File(path));

			String fileString = IOUtils.toString(fisTargetFile, "UTF-8");
			
			this.savedMap = Json.parse(fileString).asObject();

			boolean completed = savedMap.get("completed").asBoolean();
			
			this.mapWorldSize = savedMap.get("tam").asInt();
			
			mapWorld = new int[this.mapWorldSize][this.mapWorldSize];
			
			if(completed) {
				this.mapWorldCompleted = true;
				
				this.state = SUBSCRIBE_MAP;
			}
			else {
				this.state = SUBS_MAP_EXPLORE;
				
				this.mapWorldPosX = savedMap.get("pos").asObject().get("x").asInt();
				this.mapWorldPosY = savedMap.get("pos").asObject().get("y").asInt();
			}
		} catch(IOException ex) {
			
			if(DEBUG)
				System.out.println("MAP " + map + " NOT FOUND");
			
			mapWorld = new int[WIDTH][HEIGHT];
			
			this.mapWorldSize = WIDTH;
			
			this.state = SUBS_MAP_EXPLORE;
		}
    }
	
	/**
	 * Subscribirse al mapa.
	 * 
	 * Es un método aparte porque se va a utilizar en varios estados.
	 * 
	 * @author Hugo Maldonado
	 */
	private boolean subscribe() {
		
		JsonObject obj = Json.object().add("world", map); 
		
		sendMessage(serverName, ACLMessage.SUBSCRIBE, "", "", obj.toString());
		
        ACLMessage receive = new ACLMessage();
		
        if(receive.getPerformativeInt() == ACLMessage.INFORM) {
            this.conversationID = receive.getConversationId();
			
			if(DEBUG)
				System.out.println("SUSCRITO. ConvID:" + conversationID);
			
			return true;
        }
		else
			return false;
	}
	
	/**
     * Despertar y lanzar el resto de agentes.
	 * 
	 * @author Jose David and Hugo Maldonado
    */
    private boolean wakeAgents() {
        
		try {
			Agent car1=new AgentCar(carNames[0]);
			car1.start();
			
			Agent car2=new AgentCar(carNames[1]);
			car2.start();
			
			Agent car3=new AgentCar(carNames[2]);
			car3.start();
			
			Agent car4=new AgentCar(carNames[3]);
			car4.start();
			
			this.wakedAgents = true;
			
			return true;
		} catch(Exception ex) {
			System.err.println("Error waking agents");

			System.err.println(ex.getMessage());
			
			return false;
		}
    }
	
	/**
	 * Mandarle el conversationId a todos los agentes y empezar la negociación para ver quién va a explorar el mapa
	 * 
	 * @author Hugo Maldonado
	 */
	private void stateCheckAgentsExplore() {
		
		if(DEBUG)
			System.out.println("AgentController state: CHECK_AGENTS_EXPLORE");
		
		JsonObject message = new JsonObject();
		
		message.add("conversationID-server", this.conversationID);
		
		for(AgentID carName : carNames) {
			sendMessage(carName, ACLMessage.INFORM, "", conversationID, message.asString());
		}
		
		boolean startCFP = true;
		
		for(int i=0; i<4; i++) {
			ACLMessage receive = this.receiveMessage();
			
			if(receive.getPerformativeInt() != ACLMessage.INFORM)
				startCFP = false;
		}
		
		message = new JsonObject();
		
		message.add("checkMap", "flying");
		
		if(startCFP) {
			for(AgentID carName : carNames) {
				sendMessage(carName, ACLMessage.CFP, "", conversationID, message.asString());
			}
			
			boolean flyingFound = false;
			AgentID flyingAgent = null;
			
			for(int i=0; i<4 && flyingFound; i++) {
				ACLMessage receive = this.receiveMessage();

				if(receive.getPerformativeInt() == ACLMessage.AGREE) {
					flyingFound = true;
					
					flyingAgent = receive.getSender();
				}	
			}
			
			if(flyingFound) {
				
				JsonObject messageAccept = new JsonObject(message);
				
				messageAccept.add("startX", this.mapWorldPosX);
				messageAccept.add("startY", this.mapWorldPosY);
				messageAccept.add("size", this.mapWorldSize);
				
				for(AgentID carName : carNames) {
					if (carName == flyingAgent) {
						sendMessage(flyingAgent, ACLMessage.ACCEPT_PROPOSAL, "", conversationID, messageAccept.asString());
					} else {
						sendMessage(flyingAgent, ACLMessage.REJECT_PROPOSAL, "", conversationID, message.asString());	
					}
					
					this.state = EXPLORE_MAP;
				}
			}
			else 
				this.state = RE_RUN;
		}
		else 
			this.state = FINALIZE;
	}
	
	/**
	 * Esperar por la exploración del mapa y guardar en memoria la información
	 * 
	 * @author Hugo Maldonado
	 */
	private void stateExploreMap() {
		
		if(DEBUG)
			System.out.println("AgentController state: EXPLORE_MAP");
		
		ACLMessage receive = this.receiveMessage();
		
		if(receive.getPerformativeInt() == ACLMessage.INFORM) {
			JsonObject responseObject = Json.parse(receive.getContent()).asObject();

			this.mapWorldSize = responseObject.get("size").asInt();

			this.mapWorldPosX = responseObject.get("finalX").asInt();
			this.mapWorldPosY = responseObject.get("finalY").asInt();

			this.mapWorldCompleted = responseObject.get("completed").asBoolean();

			int posix = 0, posiy = 0;

			for(JsonValue j : responseObject.get("map").asArray()) {
				mapWorld[posiy][posix] = j.asInt();
				posix++;

				if(posix % this.mapWorldSize == 0) {
					posix = 0;
					posiy++;
				}
			}
		}
		else 
			this.state = FINALIZE;
		
	}
	
	/**
	 * Guardar el estado del mapa en el fichero así como su información relativa
	 * 
	 * @author Hugo Maldonado
	 */
	private void stateSaveMap() {
		
		if(DEBUG)
			System.out.println("AgentController state: SAVE_MAP");
		
		JsonObject mapToSave = new JsonObject();
		
		mapToSave.add("completed", this.mapWorldCompleted);
		mapToSave.add("tam", this.mapWorldSize);
		
		JsonObject pos = new JsonObject();
		
		pos.add("x", this.mapWorldPosX);
		pos.add("y", this.mapWorldPosY);
		
		mapToSave.add("pos", pos);
		
		// mapToSave.add("direction", "right");
		
		JsonArray sendMap = new JsonArray();

		for(int [] i : this.mapWorld){
			for(int j : i){
				sendMap.add(j);
			}
		}

		mapToSave.add("map", sendMap);
		
		File file = new File("maps/" + this.map + ".json");
		
		try {
			FileWriter fileWriter;
			
			fileWriter = new FileWriter(file);
			
			fileWriter.write(mapToSave.asString());
			
			fileWriter.flush();
			fileWriter.close();
		} catch(IOException ex) {
			System.err.println("Error procesing map");

			System.err.println(ex.getMessage());
		}
	}
	
	/**
	 * Terminar la iteración y volver a empezar otra
	 * 
	 * @author
	 */
	private void stateReRun() {
		
		if(DEBUG)
			System.out.println("AgentController state: RE_RUN");
		
	}
	
	/**
	 * Mandarle el conversationId a todos los agentes y esperar sus capacidades
	 * 
	 * @author
	 */
	private void stateCheckAgents() {
		
		if(DEBUG)
			System.out.println("AgentController state: CHECK_AGENTS");
		
	}
	
	/**
	 * Enviar el mapa al resto de agentes
	 * 
	 * @author
	 */
	private void stateSendMap() {
		
		if(DEBUG)
			System.out.println("AgentController state: SEND_MAP");
		
	}
	
	/**
	 * Obtener la información de la batería de los agentes
	 * 
	 * @author
	 */
	private void stateFuelInformation() {
		
		if(DEBUG)
			System.out.println("AgentController state: FUEL_INFORMATION");
		
	}
	
	/**
	 * Elegir qué agentes van al objetivo y cuáles no
	 * 
	 * @author
	 */
	private void stateChooseAgents() {
		
		if(DEBUG)
			System.out.println("AgentController state: FUEL_INFORMATION");
		
	}
	
	/**
	 * Obtener información de los agentes durante su movimiento para evitar colisiones entre ellos
	 * 
	 * @author
	 */
	private void stateControlAgents() {
		
		if(DEBUG)
			System.out.println("AgentController state: FUEL_INFORMATION");
		
	}
	
	/**
     * Si se han levantado los agentes, matarlos y hacer el CANCEL de la subscripción
	 * 
	 * @author Jose David and Hugo Maldonado
    */
    private void stateFinalize() {
		
		if(DEBUG)
			System.out.println("AgentController state: FINALIZE");
		
		// Matar los agentes si es necesario
		if(this.wakedAgents) {
			JsonObject message = new JsonObject().add("die", "now");
			
			for(AgentID carName : carNames) {
				sendMessage(carName, ACLMessage.REQUEST, "", conversationID, message.asString());    
			}
			
			for(int i=0; i<4; i++) {
				ACLMessage receive = this.receiveMessage();
				
				if(receive.getPerformativeInt() == ACLMessage.AGREE)
					System.out.println("Agent Car :" + i + "die");
			}
		}
		
		// Mandar el CANCEL
		sendMessage(serverName, ACLMessage.CANCEL, "", "", "");
        
        // Guardar la traza si es necesario
		
		// Terminar la ejecución
		this.finish = true;
    } 
    
	/**
	 * Ejecución del controlador
	 * 
	 * @author Hugo Maldonado
	 */
    @Override
    public void execute() {
        
		System.out.println("AgentController execution");
		
		while(!finish) {
			switch(state) {
				case CHECK_MAP:
					
					this.stateCheckMap();

					break;
				case SUBS_MAP_EXPLORE:
					
					if(DEBUG)
						System.out.println("AgentController state: SUBS_MAP_EXPLORE");
					
					if(this.subscribe())
						this.state = WAKE_AGENTS_EXPLORE;
					else
						this.state = FINALIZE;
					
					break;
				case WAKE_AGENTS_EXPLORE:
					
					if(DEBUG)
						System.out.println("AgentController state: WAKE_AGENTS_EXPLORE");
					
					if(this.wakeAgents())
						this.state = CHECK_AGENTS_EXPLORE;
					else
						this.state = FINALIZE;
                                    
					break;
				case CHECK_AGENTS_EXPLORE:
					
					this.stateCheckAgentsExplore();
          
					break;
				case EXPLORE_MAP:
					
					this.stateExploreMap();

					break;
				case SAVE_MAP:
					
					this.stateSaveMap();
					
					break;
				case RE_RUN:
					
					this.stateReRun();
					
					break;
				case SUBSCRIBE_MAP:
					
					if(DEBUG)
						System.out.println("AgentController state: SUBSCRIBE_MAP");
					
					if(this.subscribe())
						this.state = WAKE_AGENTS_EXPLORE;
					else
						this.state = FINALIZE;
					
					break;
				case WAKE_AGENTS:
					
					if(DEBUG)
						System.out.println("AgentController state: WAKE_AGENTS");
					
					if(this.wakeAgents())
						this.state = CHECK_AGENTS;
					else
						this.state = FINALIZE;              
					
					break;
				case CHECK_AGENTS:
					
					this.stateCheckAgents();
					
					break;
				case SEND_MAP:
					
					this.stateSendMap();
					
					break;
				case FUEL_INFORMATION:
					
					this.stateFuelInformation();
					
					break;
				case CHOOSE_AGENTS:
					
					this.stateChooseAgents();
					
					break;
                case CONTROL_AGENTS:
					
					this.stateControlAgents();
					
					break;
                case FINALIZE:
					
					this.stateFinalize();  
					
					break;
			}
		}
    }
	
	/**
	  * Método de finalización del agente Controlador.
	  * 
	  * @author Hugo Maldonado
	  */
	@Override
	public void finalize() {
		
		System.out.println("AgentController has just finished");
		
		super.finalize();
	}
	
	/**
	 * Guarda la traza en un archivo png. 
	 * 
	 * @param trace Array con los datos de la matriz del world
	 * @param error booleano para ponerle un nombre u otro en función de si la traza
	 * devuelta ha sido por un error o no.
	 * 
	 * @author Hugo Maldonado
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
	
	private String generateReplyId() {
		
		return UUID.randomUUID().toString().substring(0, 5);
	}
}