
package practica3;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
    private static final int LOAD_MAP = 5;
    private static final int CHECK_MAP = 6;
    private static final int SUBSCRIBE_MAP = 7;
    private static final int EXPLORE_MAP = 8;
    private static final int WAKE_AGENTS = 9;
    private static final int FINALIZE = 10;
    private static final int FUEL_INFORMATION = 11;
    private static final int CHOOSE_AGENTS = 12;
    private static final int CONTROL_AGENTS= 13;
    private static final int SEND_MAP = 14;
    private static final int CHECK_AGENTS= 15;
	
	private static final boolean DEBUG = true;
	
	private final AgentID serverName;
    private final AgentID car1Name;
    private final AgentID car2Name;
    private final AgentID car3Name;
    private final AgentID car4Name;
    private final String map;
    
    private int state;
    private String conversationID;
	private boolean wakedAgents;
	private boolean finish = false;
    
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
        this.car1Name = car1Name;
        this.car2Name = car2Name;
        this.car3Name = car3Name;
        this.car4Name = car4Name;
		
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
		wakedAgents = false;
		
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
        
        String path = "maps/" + this.map;
		
        File file = new File(path);
        
        if(file.exists()) {
			// Pasar el fichero a un JsonObject
			JsonObject savedMap = new JsonObject();
			
			// FALTA LA LECTURA DEL FICHERO EN EL JSON OBJECT
			
            boolean completed = savedMap.get("completed").asBoolean();
			
			if(completed)
				this.state = LOAD_MAP;
			else
				this.state = SUBS_MAP_EXPLORE;
        }
		else {
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
			Agent car1=new AgentCar(car1Name);
			car1.start();
			
			Agent car2=new AgentCar(car2Name);
			car2.start();
			
			Agent car3=new AgentCar(car3Name);
			car3.start();
			
			Agent car4=new AgentCar(car4Name);
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
	 * @author
	 */
	private void stateCheckAgentsExplore() {
		
		if(DEBUG)
			System.out.println("AgentController state: CHECK_AGENTS_EXPLORE");
		
	}
	
	/**
	 * Explorar el mapa
	 */
	private void stateExploreMap() {
		
		if(DEBUG)
			System.out.println("AgentController state: EXPLORE_MAP");
		
	}
	
	/**
	 * Guardar el estado del mapa
	 */
	private void stateSaveMap() {
		
		if(DEBUG)
			System.out.println("AgentController state: SAVE_MAP");
		
	}
	
	/**
	 * Terminar la iteración y volver a empezar otra
	 */
	private void stateReRun() {
		
		if(DEBUG)
			System.out.println("AgentController state: RE_RUN");
		
	}
	
	/**
	 * Cargar el mapa
	 */
	private void stateLoadMap() {
		
		if(DEBUG)
			System.out.println("AgentController state: LOAD_MAP");
		
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
	 */
	private void stateFuelInformation() {
		
		if(DEBUG)
			System.out.println("AgentController state: FUEL_INFORMATION");
		
	}
	
	/**
	 * Elegir qué agentes van al objetivo y cuáles no
	 */
	private void stateChooseAgents() {
		
		if(DEBUG)
			System.out.println("AgentController state: FUEL_INFORMATION");
		
	}
	
	/**
	 * Obtener información de los agentes durante su movimiento para evitar colisiones entre ellos
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
			
			sendMessage(car1Name, ACLMessage.REQUEST, "", conversationID, message.asString());    
			sendMessage(car2Name, ACLMessage.REQUEST, "", conversationID, message.asString());    
			sendMessage(car3Name, ACLMessage.REQUEST, "", conversationID, message.asString());    
			sendMessage(car4Name, ACLMessage.REQUEST, "", conversationID, message.asString());  
			
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
				case LOAD_MAP:
					
					this.stateLoadMap();
					
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
}