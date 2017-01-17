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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.swing.JFrame;
import org.apache.commons.io.IOUtils;

/**
 * Clase que define al agente controlador
 * 
 * @author Hugo Maldonado and Bryan Moreno and Aarón Rodríguez and JoseDavid
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
    private static final int CONTROL_AGENTS = 12;
	private static final int CHECK_AGENTS = 13;
	private static final int REQUEST_POSITION = 14;
    private static final int SEND_MAP = 15;
	
    private static final boolean DEBUG = true;
	
	private static final int INDEX_POSX = 0;
	private static final int INDEX_POSY = 1;
	private static final int INDEX_ACTUAL_FUEL = 2;
	private static final int INDEX_FUEL_TO_GOAL = 3;
	private static final int INDEX_OBJX = 4;
	private static final int INDEX_OBJY = 5;
	private static final int INDEX_STEPS_TO_GOAL = 6;
	
    private final AgentID serverName;
    private final AgentID carNames[] = new AgentID[4];
    private final String replyWithAgents [] = new String[4];
    private final String map;
    
    private int [][] mapWorld;
    private final int mapWorldSize = 510;
    private int mapWorldPosX;
    private int mapWorldPosY;
    private boolean mapWorldCompleted;
    private String mapWorldDirection;
    private int carsInGoal;
	
	private int globalFuel;
	private final int [][] carLocalInfo = new int[4][7];
    
    private int state;
    private String conversationIdServer;
	private String conversationIdController;
    private boolean wakedAgents;
    private boolean finish = false;

    private JsonObject savedMap;
    private int numSentCars;
	
	private JFrame jframe;
    private MyDrawPanel m;

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
		//Inicializamos los nombres del servidor y los agentCar
        this.serverName = serverName;
        this.carNames[0] = car1Name;
        this.carNames[1] = car2Name;
        this.carNames[2] = car3Name;
        this.carNames[3] = car4Name;
        System.out.println("Nombre: "+carNames[0] +" "+carNames[0].getLocalName());
        //Se almacena el mapa en el que vamos a trabajar.
        this.map = map;
    }
	
	/**
	  * Método de inicialización del agente Coche.
	  * 
	  * @author Hugo Maldonado
	  */
	@Override
	public void init() {
		//Iniciamos en el estado de chequeo del mapa para ver si está completo o necesitamos seguir explorando.
		this.state = CHECK_MAP;
		
		this.conversationIdServer = "";
		this.conversationIdController = UUID.randomUUID().toString().substring(0, 5);
		
		this.wakedAgents = false;
		
		this.savedMap = new JsonObject();
		
		this.globalFuel = -1;
		
		this.mapWorldPosX = -1;
		this.mapWorldPosY = -1;
		this.mapWorldDirection = "";
		this.mapWorldCompleted = false;
		this.numSentCars = 0;
		this.carsInGoal = 0;
		
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

        //Leemos el fichero de "mapa" y lo pasamos a JSon
        try {
            System.out.println("ABRIENDO EL ARCHIVO: "+path);
            FileInputStream fisTargetFile = new FileInputStream(new File(path));
            String fileString = IOUtils.toString(fisTargetFile, "UTF-8");
            this.savedMap = Json.parse(fileString).asObject();
            boolean completed;
            if(savedMap.get("completed").asString().equals("true"))
                completed = true;
            else
                completed = false;;
            mapWorld = new int[this.mapWorldSize][this.mapWorldSize];

            int x = 0, y = 0;
            for(JsonValue pos : this.savedMap.get("map").asArray()) {
                this.mapWorld[y][x] = pos.asInt();
                x++;
                if(x == this.mapWorldSize){
                    x = 0;
                    y++;
                }
            }
			
            //Miramos si el estado del mapa guardado es "completo" (ya se conoce todo el mapa)
            if(completed) {
                System.out.println("MAPA COMPLETO");
                //Marcamos la bandera en caso afirmativo y pasamos al estado de subscripción al mapa
                this.mapWorldCompleted = true;
                this.state = SUBSCRIBE_MAP;

                jframe = new JFrame();
                m = new MyDrawPanel(this.mapWorld);
                jframe.add(m);
                jframe.setSize(this.mapWorldSize+10, this.mapWorldSize+50);
                jframe.setVisible(true);

                //jframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CL‌​OSE);
                //jframe.setUndecorated(true);
                jframe.setTitle(this.map);
            }
            else {
                System.out.println("MAPA INCOMPLETO");
                //Pasamos al modo exploración y almacenamos la posición desde la que seguimos explorando"
                this.state = SUBS_MAP_EXPLORE;
                this.mapWorldPosX = savedMap.get("pos").asObject().get("x").asInt();
                this.mapWorldPosY = savedMap.get("pos").asObject().get("y").asInt();
                this.mapWorldDirection = savedMap.get("direction").asString();
            }
        } catch(IOException ex) {
            //No existe mapa previo, por lo que entramos en modo exploración y inicializamos las estructuras necesarias.
            if(DEBUG)
                System.out.println("MAP " + map + " IN FILE NOT FOUND");

            mapWorld = new int[mapWorldSize][mapWorldSize];
            for (int i = 0; i < mapWorldSize; i++){
                for(int j = 0; j < mapWorldSize; j++){
                    mapWorld[i][j] = -1;
                }
            }
            this.mapWorldPosX = 5;
            this.mapWorldPosY = 5;
            this.mapWorldDirection = "right";
            this.state = SUBS_MAP_EXPLORE;
        }
    }
	
	/**
	 * Subscribirse al mapa. Devuelve un booleano true si ha conseguido subscribirse.
     * Es un método aparte porque se va a utilizar en varios estados.
	 * 
	 * @author Hugo Maldonado and Aaron Rodriguez Bueno
	 */
	private boolean subscribe() {
            JsonObject obj = Json.object().add("world", map);
            System.out.println(serverName.toString());
            System.out.println(map);
            sendMessage(serverName, ACLMessage.SUBSCRIBE, "", "", obj.toString());
            
	
            /*while(true){
                ACLMessage receive = this.receiveMessage();
                
                if(receive.getPerformativeInt() == ACLMessage.FAILURE || receive.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD) {
                    return false;
                }
                else if(receive.getContent().contains("trace")){
                    System.out.println("Es la traza y su ide es"+receive.getConversationId());
                    JsonArray trace = Json.parse(receive.getContent()).asObject().get("trace").asArray();
                    this.saveTrace(trace, true);
                }
                else if(receive.getPerformativeInt() == ACLMessage.INFORM){
                    receive = this.receiveMessage();
                    this.conversationIdServer = receive.getConversationId();
                    if(DEBUG)
                        System.out.println("SUSCRITO. ConvIDServer:" + conversationIdServer);
                    return true;
                }
            }*/
            
            ACLMessage receive;
            
            receive = this.receiveMessage();
            if(receive.getPerformativeInt() == ACLMessage.FAILURE || receive.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD) {
                return false;
            }
			else {
				if(receive.getContent().contains("trace")) {
					JsonArray trace = Json.parse(receive.getContent()).asObject().get("trace").asArray();
					this.saveTrace(trace, true);
					
					receive = this.receiveMessage();
				}
				
                this.conversationIdServer = receive.getConversationId();
				
                if(DEBUG)
                    System.out.println("SUSCRITO. ConvIDServer:" + conversationIdServer);
				
                return true;
            }
	}
	
    /**
    * Despertar y lanzar el resto de agentes.
    * 
    * @author Jose David and Hugo Maldonado
    */
    private boolean wakeAgents() {
        
	try {
            //Creamos a los agentes y los despertamos utilizando los nombres pasados en el constructor.
            Agent car1 = new AgentCar(carNames[0], this.getAid(), this.serverName);
            car1.start();

            Agent car2 = new AgentCar(carNames[1], this.getAid(), this.serverName);
            car2.start();

            Agent car3 = new AgentCar(carNames[2], this.getAid(), this.serverName);
            car3.start();

            Agent car4 = new AgentCar(carNames[3], this.getAid(), this.serverName);
            car4.start();
			
            //Cambiamos el estado del sistema para que conste que estan despiertos los agentes.
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
	 * @author Hugo Maldonado and Aaron Rodriguez Bueno
	 */
	private void stateCheckAgentsExplore() {
        
            //Llamamos a la funcion que manda la conversation ID del servidor y espera por la confirmación
            boolean startCFP = this.requestCheckIn();

            JsonObject message = new JsonObject();
            message.add("checkMap", "flying");
		
            //Una vez tenemos los agentes despiertos y funcionando, pasamos a buscar el "volador"
            if(startCFP) {
                for(AgentID carName : carNames)
                    
                    sendMessage(carName, ACLMessage.CFP, this.generateReplyId(), this.conversationIdController, message.toString());

                /*boolean flyingFound = false;
                AgentID flyingAgents [] = new AgentID[4];
                int cont = 0;

                for(int i=0; i<4 && flyingFound; i++) {
                    ACLMessage receive = this.receiveMessage();

                    if(receive.getPerformativeInt() == ACLMessage.AGREE) {
                        flyingFound = true;

                        flyingAgents[cont] = receive.getSender();
                        cont++;
                    }	
                }
			
                if(flyingFound) {
                    //Si tenemos el agente volador, mandamos confirmación y la informacion necesaria y rechazamos al resto.
                    JsonObject messageAccept = new JsonObject(message);

                    messageAccept.add("startX", this.mapWorldPosX);
                    messageAccept.add("startY", this.mapWorldPosY);
                    messageAccept.add("size", this.mapWorldSize);
                    messageAccept.add("direction", this.mapWorldDirection);

                    for(AgentID carName : flyingAgents) {
                        if(carName == flyingAgents[0])
                            sendMessage(carName, ACLMessage.ACCEPT_PROPOSAL, this.generateReplyId(), conversationIdController, messageAccept.toString());
                        else
                            sendMessage(carName, ACLMessage.REJECT_PROPOSAL, this.generateReplyId(), conversationIdController, message.toString());

                        this.state = EXPLORE_MAP;
                    }
                }
                else
                    this.state = RE_RUN;
            }*/
                boolean flyingFound = false;
                AgentID flyingAgent = new AgentID();
                for (int i = 0; i < carNames.length; i++){
                    ACLMessage receive = this.receiveMessage();
                    AgentID thisAgent = receive.getSender();
                    
                    if(receive.getPerformativeInt() == ACLMessage.AGREE){
                        if(!flyingFound){
                            flyingFound = true;
                            flyingAgent = thisAgent;
                        }
                        else{
                            sendMessage(thisAgent, ACLMessage.REJECT_PROPOSAL, this.generateReplyId(), conversationIdController, receive.getContent());
                        }
                    }
                }
                //Damos via libre al agente elegido (mejor al final para que seguro el mensaje siguiente 
                //que nos manden sea el de lo explorado por el elegido)
                if(flyingFound){
                    JsonObject messageAccept = new JsonObject();
                    
                    messageAccept.add("startX", this.mapWorldPosX);
                    messageAccept.add("startY", this.mapWorldPosY);
                    messageAccept.add("direction", this.mapWorldDirection);
                    JsonArray myArray = new JsonArray();
                    for (int i = 0; i < mapWorldSize; i++){
                        for (int j = 0; j < mapWorldSize; j++){
                            myArray.add(this.mapWorld[i][j]);
                        }
                    }
                    messageAccept.add("map",myArray);
                    sendMessage(flyingAgent, ACLMessage.ACCEPT_PROPOSAL, this.generateReplyId(), conversationIdController, messageAccept.toString());
                    
                    this.state = EXPLORE_MAP;
                }
                else
                    this.state = RE_RUN;
            }else 
                this.state = FINALIZE;
	}
	
	/**
	 * Esperar por la exploración del mapa y guardar en memoria la información
	 * 
	 * @author Hugo Maldonado
	 */
	private void stateExploreMap() {
		
		ACLMessage receive = this.receiveMessage();
                System.out.println("ME HA LLEGADO EL CONTENIDO: "+receive.getContent());
		if(receive.getPerformativeInt() == ACLMessage.INFORM) {
			JsonObject responseObject = Json.parse(receive.getContent()).asObject();

			this.mapWorldPosX = responseObject.get("finalX").asInt();
			this.mapWorldPosY = responseObject.get("finalY").asInt();
                        this.mapWorldDirection = responseObject.get("direction").asString();
                        System.out.println("ANTES DEL BOOL");
			if(responseObject.get("completed").asString().equals("true"))
                            this.mapWorldCompleted = true;
                        else
                            this.mapWorldCompleted = false;
			int posix = 0, posiy = 0;
                    System.out.println("ANTES DEL FOR");
			for(JsonValue j : responseObject.get("map").asArray()) {
				mapWorld[posiy][posix] = j.asInt();
				posix++;

				if(posix % this.mapWorldSize == 0) {
					posix = 0;
					posiy++;
				}
			}
                        System.out.println("DESPUES DEL FOR");
                        this.state = SAVE_MAP;
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
		
		JsonObject mapToSave = new JsonObject();
		
		if(this.mapWorldCompleted)
			mapToSave.add("completed", "true");
		else
			mapToSave.add("completed", "false");
		
		JsonObject pos = new JsonObject();
		
		pos.add("x", this.mapWorldPosX);
		pos.add("y", this.mapWorldPosY);
		
		mapToSave.add("pos", pos);
		
                if(this.mapWorldDirection.equals("right"))
		    mapToSave.add("direction", "right");
                else
                    mapToSave.add("direction", "left");
		
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
			
			fileWriter.write(mapToSave.toString());
			
			fileWriter.flush();
			fileWriter.close();
                        this.state = RE_RUN;
		} catch(IOException ex) {
			System.err.println("Error processing map");

			System.err.println(ex.getMessage());
                        this.state = FINALIZE;
		}
	}
	
        /**
         * Mata a todos los cars, espera su respuesta, y hace cancel
		 * 
         * @author Aaron Rodriguez Bueno
         */
        private void killAgents() {
			
            System.out.println("EN KILL AGENTS");
            JsonObject message = new JsonObject();
            message.add("die", "now");

            for(AgentID carName : carNames)
                sendMessage(carName, ACLMessage.REQUEST, this.generateReplyId(), conversationIdController, message.toString());
            
            ACLMessage receive;
            for(int i=0; i<4; i++) {
                receive= this.receiveMessage();
                if(receive.getPerformativeInt() == ACLMessage.AGREE)
                    System.out.println("Agent Car :" + receive.getSender().toString() + "die");
            }
		
            // Mandar el CANCEL
            sendMessage(serverName, ACLMessage.CANCEL, "", "", "");
            
            //Espera los dos mensajes
			boolean received = false;
            while(!received){
                receive = receiveMessage();
                if(receive.getPerformativeInt() == ACLMessage.INFORM && receive.getContent().contains("trace")) {
                    JsonArray trace = Json.parse(receive.getContent()).asObject().get("trace").asArray();
                    this.saveTrace(trace, false);
					received = true;
                }
            }
            
        }
        
	/**
	 * Terminar la iteración y volver a empezar otra, pasamos al modo SUBS_MAP_EXPLORE
	 * 
	 * @author Bryan Moreno Picamán and Hugo Maldonado
	 */
	private void stateReRun() {
        
		killAgents();
		
        
		/*boolean allOk = true;
		
		//Esperamos una contestación por cada uno de los mensajes enviados
		for(AgentID carName : carNames) {
			ACLMessage receive = this.receiveMessage();
			//Si alguno de los mensajes no es un INFORM, la comunicación ha fallado
			if(receive.getPerformativeInt() != ACLMessage.AGREE){
				allOk = false;
			}
		}
        
		if(allOk)*/
			this.state = CHECK_MAP;
		/*else
			this.state = FINALIZE;*/
	}
	
	/**
	 * Mandarle el conversationId a todos los agentes
	 * 
	 * @author Bryan Moreno Picamán and Hugo Maldonado
	 */
	private void stateCheckAgents() {
        //Llamamos a la funcion que manda la conversation ID del servidor y espera por la confirmación
        if(this.requestCheckIn())
			this.state = REQUEST_POSITION;
		else
			this.state = FINALIZE;
	}
	
	/**
	 * Obtener la posición de cada uno de los agentes
	 * 
	 * @author Hugo Maldonado
	 */
	private void stateRequestPosition() {
		
		JsonObject message = new JsonObject();
		
		message.add("givePosition", "ok");
		
		for(AgentID carName : carNames)
			sendMessage(carName, ACLMessage.REQUEST, this.generateReplyId(), conversationIdController, message.toString());

		boolean allOk = true;
		
		//Esperamos una contestación por cada uno de los mensajes enviados
		for(AgentID carName : carNames) {
			ACLMessage receive = this.receiveMessage();
			//Si alguno de los mensajes no es un INFORM, la comunicación ha fallado
			if(receive.getPerformativeInt() != ACLMessage.INFORM) {
				allOk = false;
			}
			else {
				int row = getIndexCar(receive.getSender());

				if(row != -1) {
					JsonObject response = Json.parse(receive.getContent()).asObject();

					int posX = response.get("posX").asInt();
					int posY = response.get("posY").asInt();

					this.carLocalInfo[row][INDEX_POSX] = posX;
					this.carLocalInfo[row][INDEX_POSY] = posY;
				}
				else {
                                    System.out.println("NO PILLO BIEN EL ROW");
					allOk = false;
				}
			}
		}
		
		if(allOk)
			this.state = SEND_MAP;
		else
			this.state = FINALIZE;
	}
	
	/**
	 * Función que calcula la distancia Euclídea de 2 puntos
	 * 
	 * @author Hugo Maldonado
	 */
	private double euclideanDist(int posXIni, int posYIni, int posXFin, int posYFin) {
		
		return Math.sqrt((Math.pow((posXFin - posXIni), 2) + Math.pow((posYFin - posYIni), 2)));
	}
	
	/**
	 * Enviar el mapa al resto de agentes así como las posiciones de los objetivos de cada uno
	 * 
	 * @author Hugo Maldonado
	 */
	private void stateSendMap() {
		
		JsonObject message = new JsonObject();
		
		// Recorrer el mapa buscando todas las posiciones que sean objetivos
		ArrayList<Integer> posObj = new ArrayList<>();
		
                if(DEBUG){
                    System.out.println("AÑADIMOS TODAS LAS POSICIONES GOAL");
                    for(int y=0; y<this.mapWorldSize; y++) {
						for(int x=0; x<this.mapWorldSize; x++) {
							if(this.mapWorld[y][x] == 3) {
								posObj.add(x);
								posObj.add(y);
							}
						}
                    }
                }
                
                /*System.out.println("GOALS EXISTENTES:");
                for(int i = 0; i < posObj.size(); i+=2){
                    System.out.println("("+Integer.toString(posObj.get(i+1))+","+Integer.toString(posObj.get(i))+")");
                }*/
		
		// Calcular el X y el Y de los objetivos de cada uno de los agentes
		for(int i=0; i<carNames.length; i++) {
			int objX = -1;
			int objY = -1;
			
			// Calcular la distancia euclídea de la posición del agente al objetivo
			double minDist = 99999999;
			
			// Recorrer todas las posiciones del objetivo para sacar la distancia euclídea mínima
			for(int k=0; k<posObj.size(); k+=2) {
				int objPosX = posObj.get(k);
				int objPosY = posObj.get(k+1);
				
				double dist = euclideanDist(carLocalInfo[i][INDEX_POSX], carLocalInfo[i][INDEX_POSY], objPosX, objPosY);
				
				if(dist < minDist) {
					minDist = dist;
					
					objX = objPosX;
					objY = objPosY;
				}
			}
			
			// Comprobar que los objetivos de los siguientes agentes (1, 2 y 3) no coinciden con ninguno de los anteriores. Si coinciden volver a calcular las posiciones objetivo 
			for(int j=0; j<i; j++) {
				while(objX == carLocalInfo[j][INDEX_OBJX] && objY == carLocalInfo[j][INDEX_OBJY]) {
					// Coinciden,luego recalcular el proceso
					minDist = 99999999;
			
					// Recorrer todas las posiciones del objetivo para sacar la distancia euclídea mínima
					for(int k=0; k<posObj.size(); k+=2) {
						int objPosX = posObj.get(k);
						int objPosY = posObj.get(k+1);

						double dist = euclideanDist(carLocalInfo[i][INDEX_POSX], carLocalInfo[i][INDEX_POSY], objPosX, objPosY);

						if(dist < minDist && objPosX != carLocalInfo[j][INDEX_OBJX] && objPosY != carLocalInfo[j][INDEX_OBJY]) {
							minDist = dist;

							objX = objPosX;
							objY = objPosY;
                                                        /*if(DEBUG)    
                                                            System.out.println("GOAL ELEGIDO PARA "+carNames[j]+": ("+objX+","+objY+")");*/
						}
					}
				}
			}
			
			carLocalInfo[i][INDEX_OBJX] = objX;
			carLocalInfo[i][INDEX_OBJY] = objY;
		}
		
		if(DEBUG)
			for (int k = 0; k < carNames.length; k++) {
				System.out.println("Pos car "+carNames[k].getLocalName()+": ("+carLocalInfo[k][INDEX_POSY]+","+carLocalInfo[k][INDEX_POSX]+"); Pos goal: ("+carLocalInfo[k][INDEX_OBJY]+","+carLocalInfo[k][INDEX_OBJX]+")");
			}
                
		for(int k=0; k<carNames.length; k++) {
			// Mandar el mapa con una pequeña modificación a cada uno, para hacer que los objetivos de los otros agentes sean muros
			int mapAux[][] = new int[this.mapWorldSize][this.mapWorldSize];
			for(int y=0; y<this.mapWorldSize; y++) {
				for(int x=0; x<this.mapWorldSize; x++) {
					mapAux[y][x] = this.mapWorld[y][x];
				}
			}
			
			System.out.println("GOALS METIDOS COMO OBSTACULOS INSALVABLES PARA AGENTE: "+carNames[k].getLocalName());
			for(int i=0; i<carNames.length; i++) {
				if(i != k) {
					mapAux[carLocalInfo[i][INDEX_OBJY]][carLocalInfo[i][INDEX_OBJX]] = 2;
					System.out.println("("+carLocalInfo[i][INDEX_OBJY]+","+carLocalInfo[i][INDEX_OBJX]+")");         
				}
			}
			
			
			JsonArray sendMap = new JsonArray();
			
			for(int y=0; y<this.mapWorldSize; y++) {
				for(int x=0; x<this.mapWorldSize; x++) {
					sendMap.add(mapAux[y][x]);
				}
			}
		
			message.add("map", sendMap);
			message.add("goalX", carLocalInfo[k][INDEX_OBJX]);
			message.add("goalY", carLocalInfo[k][INDEX_OBJY]);
		
			this.sendMessage(carNames[k], ACLMessage.INFORM, this.generateReplyId(), conversationIdController, message.toString());
		}
		
		this.state = FUEL_INFORMATION;
	}

        /**
         * Devuelve el índice de un AgentID dado
         * @param thisAgent AgentID del agente a buscar su índice
         * @return Índice del AgentID dado
		 * 
         * @author Aaron Rodriguez Bueno and Jose David Torres de las Morenas
         */
        int getIndexCar(AgentID thisAgent){
			
            int row = -1;
				
            if(thisAgent.getLocalName().equals(carNames[0].getLocalName()))
				row = 0;
            else if(thisAgent.getLocalName().equals(carNames[1].getLocalName()))
				row = 1;
            else if(thisAgent.getLocalName().equals(carNames[2].getLocalName()))
				row = 2;
            else if(thisAgent.getLocalName().equals(carNames[3].getLocalName()))
				row = 3;
            
            return row;
        }
        
	/**
	 * Obtener la información de la batería de los agentes
	 * 
	 * @author Hugo Maldonado and Bryan Moreno
	 */
	private void stateFuelInformation() {
		
		JsonObject message = new JsonObject();
		
		message.add("agent-info", "OK");
		
		// Enviar petición de información de batería
		for(AgentID carName : carNames)
			this.sendMessage(carName, ACLMessage.QUERY_REF, this.generateReplyId(), conversationIdController, message.toString());
		
		boolean allOk = true;
		
		AgentID carNamesRemoved [] = new AgentID[4];
		int cont = 0;
		
		// Recibir información de batería
		for(AgentID carName : carNames) {
			ACLMessage inbox = this.receiveMessage();
			
			if(inbox.getPerformativeInt() != ACLMessage.INFORM) {
				allOk = false;
			}
			else {
				// Guardar la información de la batería de cada agente para procesarla en el estado CHOOSE_AGENTS
				int row = getIndexCar(inbox.getSender());
				
				if(row != -1) {
					JsonObject response = Json.parse(inbox.getContent()).asObject();
				
					this.globalFuel = response.get("global-fuel").asInt();
					int actualFuel = response.get("actual-fuel").asInt();
					int fuelToGoal = response.get("fuel-to-goal").asInt();
					int steps = response.get("num-steps").asInt();

					this.carLocalInfo[row][INDEX_ACTUAL_FUEL] = actualFuel;
					this.carLocalInfo[row][INDEX_FUEL_TO_GOAL] = fuelToGoal;
					this.carLocalInfo[row][INDEX_STEPS_TO_GOAL] = steps;
					
					if(fuelToGoal == -1) {
						
						this.carLocalInfo[row][INDEX_STEPS_TO_GOAL] = -1;
						/*message = new JsonObject();
						
						message.add("die", "now");
						
						this.sendMessage(carName, ACLMessage.REQUEST, this.generateReplyId(), conversationIdController, message.toString());
						
						inbox = this.receiveMessage();
						
						if(inbox.getPerformativeInt() == ACLMessage.AGREE) {
							carNamesRemoved[cont] = carName;
							cont++;
						}*/
					}
				}
				else {
					allOk = false;
				}
			}
		}
		
		/*AgentID newCarNames [] = new AgentID[carNames.length - cont];
		int cont2 = 0;
		
		for(AgentID carName : carNames) {
			boolean added = false;
			for(AgentID carNameRemove : carNamesRemoved) {
				if(carName != carNameRemove && !added) {
					newCarNames[cont2] = carName;
					cont2++;
					added = true;
				}
			}
		}
		
		this.carNames = newCarNames;*/
		
		if(allOk)
			this.state = CHOOSE_AGENTS;
		else
			this.state = FINALIZE;
		
	}
	
	/**
	 * Elegir qué agentes van al objetivo y cuáles no
	 * 
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private void stateChooseAgents() {
		int [] fuelNeeded = new int [4];
		int contador = 0;
		
		for (int i = 0; i < 4; i++){
			if(carLocalInfo[i][INDEX_FUEL_TO_GOAL] - carLocalInfo[i][INDEX_ACTUAL_FUEL] < 0)
				fuelNeeded[i] = 0;
			else
				fuelNeeded[i] = carLocalInfo[i][INDEX_FUEL_TO_GOAL] - carLocalInfo[i][INDEX_ACTUAL_FUEL];
			
			contador+=fuelNeeded[i];
		}
		if(DEBUG){
			System.out.println("contador: "+contador);
			System.out.println("globalFuel: "+globalFuel);
			System.out.println("FUEL NEEDED");
			
			for(int i=0;i<fuelNeeded.length;i++){
				System.out.println(fuelNeeded[i]);
            }
        }
		
		ArrayList <Integer> chosenList = new ArrayList <Integer>();

		int maxIndex;
                
		while(contador>this.globalFuel){
			System.out.println("HAY QUE ELIMINAR UNO");
			maxIndex=this.calculateMaxIndex(fuelNeeded);
			contador-=fuelNeeded[maxIndex];
			fuelNeeded[maxIndex]=-1;
		}

		for(int i=0;i<fuelNeeded.length;i++){
			if(fuelNeeded[i]>=0){
				chosenList.add(i);
                                System.out.println(chosenList.get(chosenList.size()-1));
                        }
		}
		this.numSentCars = chosenList.size();

		this.sendCars(chosenList);
		this.state = CONTROL_AGENTS;
	}
        
	/**
	* Enviar una confirmación de moverse al goal a los agentes elegidos
	* @param chosenList Lista de agentes elegidos
	* 
	* @author Bryan Moreno Picamán and Aarón Rodríguez Bueno
	*/
   private void sendCars(ArrayList<Integer> chosenList){
	   for(Integer i : chosenList){ 
               System.out.println("AVISANDO AL CAR: "+carNames[i]);
		   JsonObject message = new JsonObject();
		   message.add("go-to-goal", "OK");
		   this.sendMessage(carNames[i], ACLMessage.REQUEST, this.generateReplyId(), conversationIdController, message.toString());
	   }

   }
        
        
	/**
	 * Calcula el índice del mayor elemento de una lista de enteros
	 * @param array Lista de valores enteros
	 * @return El índice del mayor valor
	 * 
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno
	 */
	private int calculateMaxIndex(int [] array){               
		int maxIndex = 0;
		for (int i = 1; i < array.length; i++){
			int newnumber = array[i];
			if ((newnumber > array[maxIndex])){
				maxIndex = i;
			}
		}
		return maxIndex;
	}
        
	/**
	 * Obtener información de los agentes durante su movimiento para evitar colisiones entre ellos
	 * 
	 * @author Aaron Rodriguez Bueno and Jose David Torres and Hugo Maldonado and Bryan Moreno
	 */
	private void stateControlAgents() {
            int [] bloquedCars = new int [4];
            for (int i = 0; i < bloquedCars.length; i++)
                bloquedCars[i] = -1;
            int numCars = this.numSentCars;
            int rowAgent;
            
            while(numCars > 0){
                ACLMessage receive = this.receiveMessage();
                rowAgent = getIndexCar(receive.getSender());
                this.replyWithAgents[rowAgent] = receive.getReplyWith();
                JsonObject content = Json.parse(receive.getContent()).asObject();
                
				//Si es un move
				switch(receive.getPerformativeInt()) {
					case ACLMessage.INFORM:
						
						//Actualizamos su posición y pasos al objetivo
						carLocalInfo[rowAgent][INDEX_POSX] = content.get("x").asInt();
						carLocalInfo[rowAgent][INDEX_POSY] = content.get("y").asInt();
						carLocalInfo[rowAgent][INDEX_STEPS_TO_GOAL]--;
						
						//Actualizamos el mapa
						m.updatePos(carLocalInfo[rowAgent][INDEX_POSX], carLocalInfo[rowAgent][INDEX_POSY], rowAgent);
						m.repaint();
						
						//Si tenemos a otro car bloqueado y hay una distancia mínima
						//entre dicho coche y el que había avistado, avisarlo
						for (int i = 0; i < bloquedCars.length; i++){
							if(bloquedCars[i] == rowAgent){  //Está bloqueando al car de índice i
								if(Math.max(Math.abs(carLocalInfo[rowAgent][INDEX_POSX]-carLocalInfo[i][INDEX_POSX])    //Cogemos la mayor diferencia de X o de Y entre los dos cars
									,Math.abs(carLocalInfo[rowAgent][INDEX_POSY]-carLocalInfo[i][INDEX_POSY]))
									>= 7){
									//Mensaje de puede moverse
									JsonObject message = new JsonObject();
									message.add("canMove","OK");
									answerMessage(carNames[i],ACLMessage.INFORM,this.replyWithAgents[rowAgent],this.conversationIdController,message.toString());
									
									//Liberamos bloquedCars[i]
									bloquedCars[i] = -1;
								}
							}
						}	//Si ha llegado a su goal, bajar numCars, subir carsInGoal y bajar el
						//nº de pasos del coche hasta el goal
						if(carLocalInfo[rowAgent][INDEX_POSX] == carLocalInfo[rowAgent][INDEX_OBJX] && carLocalInfo[rowAgent][INDEX_POSY] == carLocalInfo[rowAgent][INDEX_OBJY]){
							if (DEBUG)
								System.out.println("AGENT CAR "+carNames[rowAgent].getLocalName()+" HA LLEGADO A SU GOAL");
							carsInGoal++;
							numCars--;
							
							//Desbloqueamos los que estuvieran bloqueados por este coche
							for (int i = 0; i < bloquedCars.length; i++){
								if (bloquedCars[i] == rowAgent){
									//Mensaje de puede moverse
									JsonObject message = new JsonObject();
									message.add("canMove","OK");
									sendMessage(carNames[i],ACLMessage.INFORM,this.generateReplyId(),this.conversationIdController,message.toString());
									
									//Liberamos bloquedCars[i]
									bloquedCars[i] = -1;
								}
							}
						}	
						break;
					case ACLMessage.QUERY_IF:
						
						System.out.println("bloquedCars");
						for(int i=0; i<bloquedCars.length; i++) {
							System.out.println("bloqued[" + i + "]: " + bloquedCars[i]);
						}
						System.out.println("");
						
						//Mirar si hay que bloquear a este car si alguno de los otros tiene prioridad y no está ya en el goal
						//Recorrer el array mandado en el mensaje. Para cada car visto:
						boolean bloqued = false;
						
						int cont = 0;
						int xOtherAgent = -1, yOtherAgent = -1;
						
						JsonArray otherAgents = content.get("otherAgents").asArray();
						
						System.out.println("otherAgents " + otherAgents);
						
						for(JsonValue j : otherAgents) {
							if(cont % 2 == 0) {
								xOtherAgent = j.asInt();
							}
							else {
								yOtherAgent = j.asInt();
								
								System.out.println("xOtherAgent " + xOtherAgent);
								System.out.println("yOtherAgent " + yOtherAgent);
							
								//Buscamos quién es el car de esa posición
								boolean found = false;
								int posFound = -1;

								//Miramos qué car es el que se ha detectado y guardamos su indice
								for (int i = 0; i < carLocalInfo.length && !found; i++){
									if(carLocalInfo[i][INDEX_POSX] == xOtherAgent && carLocalInfo[i][INDEX_POSY] == yOtherAgent) {
										found = true;
										posFound = i;
									}
								}

								System.out.println("posFound: " + posFound);

								//Si los steps del car que ha mandado el mensaje son mayores que el de ese car Y ese car no está ya en el goal
								if((posFound != -1) && (carLocalInfo[rowAgent][INDEX_STEPS_TO_GOAL] > carLocalInfo[posFound][INDEX_STEPS_TO_GOAL]) && (carLocalInfo[posFound][INDEX_STEPS_TO_GOAL] > 0)) {   //Bloqueamos
									bloqued = true;

									//Metes al otro car en bloquedCars[rowAgent]
									bloquedCars[rowAgent] = posFound;
								}
							}
							
							cont++;
						}
						
						if(bloqued){
							//Avisas por mensaje que está bloqueado
							JsonObject answer = new JsonObject();
							answer.add("canMove", "notOK");
							answerMessage(carNames[rowAgent], ACLMessage.DISCONFIRM, this.replyWithAgents[rowAgent], conversationIdController, answer.toString());
						}
						else{   //Si no está bloqueado
							//Mandarle mensaje diciendole que continue
							JsonObject answer = new JsonObject();
							answer.add("canMove", "OK");
							answerMessage(carNames[rowAgent], ACLMessage.CONFIRM, this.replyWithAgents[rowAgent], conversationIdController, answer.toString());
						}
						break;
					default:
						numCars--;
						break;
				}
            }
		
            state = FINALIZE;
	}
	
	/**
     * Si se han levantado los agentes, matarlos y hacer el CANCEL de la subscripción
	 * 
	 * @author Hugo Maldonado and Aaron Rodríguez
    */
    private void stateFinalize() {
		//jframe.setVisible(false);
		//jframe.dispose();
                
		//Matamos agentes
		killAgents();

		//Recibimos la traza y el agree

//        
//        //Recibimos el agree
//        ACLMessage receive = receiveMessage();
//        if(receive.getPerformativeInt()==ACLMessage.AGREE)
//            System.out.println("Trace Agree ");
//
//        // Guardar la traza si es necesario
//        receive = receiveMessage();
//        if(receive.getPerformativeInt()==ACLMessage.INFORM)
//            System.out.println("Trace Inform");
//
//        //HABRIA QUE GUARDAR LA TRAZA
        
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
			switch(this.state) {
				case CHECK_MAP:
					//Primer estado, chequeo del mapa
					this.stateCheckMap();

					break;
				case SUBS_MAP_EXPLORE:
					
					if(DEBUG)
						System.out.println("AgentController state: SUBS_MAP_EXPLORE");
					
					//Iniciamos subscripción, si se completa pasamos al siguiente estado, en caso contrário finalizamos.
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
					if(DEBUG)
						System.out.println("AgentController state: CHECK_AGENTS_EXPLORE");
					
					this.stateCheckAgentsExplore();
          
					break;
				case EXPLORE_MAP:
					if(DEBUG)
						System.out.println("AgentController state: EXPLORE_MAP");
						
					this.stateExploreMap();

					break;
				case SAVE_MAP:
					if(DEBUG)
						System.out.println("AgentController state: SAVE_MAP");
										
					this.stateSaveMap();
					
					break;
				case RE_RUN:
					if(DEBUG)
						System.out.println("AgentController state: RE_RUN");
										
					this.stateReRun();
					
					break;
				case SUBSCRIBE_MAP:
					
					if(DEBUG)
						System.out.println("AgentController state: SUBSCRIBE_MAP");
					
					if(this.subscribe())
						this.state = WAKE_AGENTS;
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
					if(DEBUG)
						System.out.println("AgentController state: CHECK_AGENTS");
										
					this.stateCheckAgents();
					
					break;
				case REQUEST_POSITION:
					if(DEBUG)
						System.out.println("AgentController state: REQUEST_POSITION");
					this.stateRequestPosition();
					
					break;
				case SEND_MAP:
					if(DEBUG)
						System.out.println("AgentController state: SEND_MAP");
										
					this.stateSendMap();
					
					break;
				case FUEL_INFORMATION:
					if(DEBUG)
						System.out.println("AgentController state: FUEL_INFORMATION");
										
					this.stateFuelInformation();
					
					break;
				case CHOOSE_AGENTS:
					if(DEBUG)
						System.out.println("AgentController state: CHOOSE_AGENTS");
										
					this.stateChooseAgents();
					
					break;
				case CONTROL_AGENTS:
					if(DEBUG)
						System.out.println("AgentController state: CONTROL_AGENTS");
										
					this.stateControlAgents();
					
					break;
				case FINALIZE:
					if(DEBUG)
						System.out.println("AgentController state: FINALIZE");
										
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
		
		System.out.println("AgentController has just finished!");
		
		this.jframe.setTitle("Agent Controller Finished");
		
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
	private void saveTrace(JsonArray trace, boolean error) {
        System.out.println(trace);

		try {
			byte data[] = new byte[trace.size()];

			for(int i=0; i<data.length; i++)
				data[i] = (byte) trace.get(i).asInt();

			FileOutputStream fos;
			DateFormat df = new SimpleDateFormat("dd-MM-yyyy-HH.mm");
			Date today = Calendar.getInstance().getTime();        

			String date = df.format(today);
			
//			if(error)
//				fos = new FileOutputStream(new File("traces/" + map + "/Error-Trace." + map + "." + date + "." + Integer.toString(numSentCars) + "." + Integer.toString(this.carsInGoal) +  ".png"));
//			else
//				fos = new FileOutputStream(new File("traces/" + map + "/Trace." + map + "." + date + "." + Integer.toString(numSentCars) + "." + Integer.toString(this.carsInGoal) +  ".png"));

			if(error)
				fos = new FileOutputStream(new File("traces/" + map + "/Error-Trace." + map + "." + date + "." + this.conversationIdServer + ".png"));
			else
				fos = new FileOutputStream(new File("traces/" + map + "/Trace." + map + "." + date + "." + this.conversationIdServer +  ".png"));


			fos.write(data);
			
			System.out.println("WRITE");

			fos.close();

			if(DEBUG)
				System.out.println("Saved trace");

		} catch(IOException ex) {
			System.err.println("Error processing trace");

			System.err.println(ex.getMessage());
		}
	}
    
    /**
	 * Función para solicitar el check-in de todos los agentes
	 * 
	 * @return true si todos han podido hacer el check-in o false de otra forma.
	 * 
	 * @author Bryan Moreno and Hugo Maldonado
	 */
    private boolean requestCheckIn() {
        //Creamos el mensaje con la conversationID
        JsonObject message = new JsonObject();
        message.add("conversationID-server", this.conversationIdServer);
        //Por cada uno de los agentCar, mandamos un mensaje
        for(AgentID carName : carNames) {
            String elReply = super.generateReplyId();

            sendMessage(carName, ACLMessage.INFORM, elReply, this.conversationIdController, message.toString());
        }

        //Esperamos una contestación por cada uno de los mensajes enviados
        for(AgentID carName : carNames) {
            ACLMessage receive = this.receiveMessage();
            //Si alguno de los mensajes no es un INFORM, la comunicación ha fallado
            if(receive.getPerformativeInt() != ACLMessage.INFORM){
                return false;
            }
        }

        return true;
    }
}
