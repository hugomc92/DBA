
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
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private boolean mapExplored;
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
		this.conversationID = "";
		wakedAgents = false;
    }
	
	
	/**
	 * Comprobar si el mapa está explorado o no
	 * 
     * @author Hugo Maldonado
    */
    private void stateCheckMap() {
        
        String path = "/maps/" + this.map;
		
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
	 * Funcionalidad del estado de subscribirse al mapa para la exploración del mismo
	 * 
	 * @author Hugo Maldonado
	 */
	private void stateSubMapExplore() {
		
		if(this.subscribe())
			this.state = WAKE_AGENTS_EXPLORE;
		else
			this.state = FINALIZE;
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
	 * Mandarle el conversationId a todos los agentes y empezar la negociación
	 */
	private void stateCheckAgentsExplore() {
		
		
	}
	
	
	/**
	 * Mandarle el conversationId a todos los agentes y esperar sus capacidades
	 */
	private void stateCheckAgents() {
		
		
	}
	
	
	/**
     * Si se han levantado los agentes, matarlos y hacer el CANCEL de la subscripción
	 * 
	 * @author Jose David and Hugo Maldonado
    */
    private void stateFinalize() {
		
		// Matar los agentes si es necesario
		if(this.wakedAgents) {
			JsonObject message = new JsonObject().add("die", "now");
			
			sendMessage(car1Name, ACLMessage.REQUEST, "", conversationID, message.asString());    
			sendMessage(car2Name, ACLMessage.REQUEST, "", conversationID, message.asString());    
			sendMessage(car3Name, ACLMessage.REQUEST, "", conversationID, message.asString());    
			sendMessage(car4Name, ACLMessage.REQUEST, "", conversationID, message.asString());  
			
			for(int i=0; i<4; i++) {
				ACLMessage receive = this.receiveMessage();
				
				if(receive.getPerformativeInt() == ACLMessage.AGREE && DEBUG)
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
        System.out.println("AgentCar execution");
		
		while(!finish) {
			switch(state) {
				case CHECK_MAP:
					
					this.stateCheckMap();

					break;
				case SUBS_MAP_EXPLORE:
					
					this.stateSubMapExplore();
					
					break;
				case WAKE_AGENTS_EXPLORE:
					
					if(this.wakeAgents())
						this.state = CHECK_AGENTS_EXPLORE;
					else
						this.state = FINALIZE;
                                    
					break;
				case CHECK_AGENTS_EXPLORE:
					
					this.stateCheckAgentsExplore();
          
					break;
				case EXPLORE_MAP:

					break;
				case SAVE_MAP:
					
					break;
				case RE_RUN:
					
					break;
				case LOAD_MAP:
					
					break;
				case SUBSCRIBE_MAP:
					
					break;
				case WAKE_AGENTS:
					
					if(this.wakeAgents())
						this.state = CHECK_AGENTS;
					else
						this.state = FINALIZE;              
					
					break;
				case CHECK_AGENTS:
					
					this.stateCheckAgents();
					
					break;
				case SEND_MAP:
					
					break;
				case FUEL_INFORMATION:
					
					break;
				case CHOOSE_AGENTS:
					
					break;
                case CONTROL_AGENTS:
					
					break;
                case FINALIZE:
						stateFinalize();  
					break;
			}
		}
    }
	
	/**
	 * Guarda la traza en un archivo png. 
	 * @param trace Array con los datos de la matriz del world
	 * @param error booleano para ponerle un nombre u otro en función de si la traza
	 * devuelta ha sido por un error o no.
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