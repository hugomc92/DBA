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
	
    private final AgentID serverName;
    private AgentID carNames[] = new AgentID[4];
    private final String map;
    
    private int [][] mapWorld;
    private final int mapWorldSize = 510;
    private int mapWorldPosX;
    private int mapWorldPosY;
    private boolean mapWorldCompleted;
    private String mapWorldDirection;
    private int carsInGoal;
	
	private int globalFuel;
	private final int [][] carLocalInfo = new int[4][6];
    
    private int state;
    private String conversationIdServer;
	private String conversationIdController;
    private boolean wakedAgents;
    private boolean finish = false;

    private JsonObject savedMap;
    private int numSentCars;

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
			FileInputStream fisTargetFile = new FileInputStream(new File(path));
			String fileString = IOUtils.toString(fisTargetFile, "UTF-8");
			this.savedMap = Json.parse(fileString).asObject();
			boolean completed = savedMap.get("completed").asBoolean();
			mapWorld = new int[this.mapWorldSize][this.mapWorldSize];
			//Miramos si el estado del mapa guardado es "completo" (ya se conoce todo el mapa)
			if(completed) {
                //Marcamos la bandera en caso afirmativo y pasamos al estado de subscripción al mapa
				this.mapWorldCompleted = true;
				this.state = SUBSCRIBE_MAP;
			}
			else {
                //Pasamos al modo exploración y almacenamos la posición desde la que seguimos explorando"
				this.state = SUBS_MAP_EXPLORE;
				this.mapWorldPosX = savedMap.get("pos").asObject().get("x").asInt();
				this.mapWorldPosY = savedMap.get("pos").asObject().get("y").asInt();
                                this.mapWorldDirection = savedMap.get("direction").asString();
			}
		} catch(IOException ex) {
			//No existe mapa previo, por lo que entramos en modo exploración y inicializamos las estructuras necesarias.
			if(DEBUG)
				System.out.println("MAP " + map + " NOT FOUND");
			
			mapWorld = new int[mapWorldSize][mapWorldSize];
                        this.mapWorldPosX = 0;
                        this.mapWorldPosY = 0;
                        this.mapWorldDirection = "right";
			this.state = SUBS_MAP_EXPLORE;
		}
    }
	
	/**
	 * Subscribirse al mapa. Devuelve un booleano true si ha conseguido subscribirse.
     * Es un método aparte porque se va a utilizar en varios estados.
	 * 
	 * @author Hugo Maldonado
	 */
	private boolean subscribe() {
		JsonObject obj = Json.object().add("world", map);
		sendMessage(serverName, ACLMessage.SUBSCRIBE, "", "", obj.toString());
        ACLMessage receive = new ACLMessage();
		
        if(receive.getPerformativeInt() == ACLMessage.INFORM) {
            //Si el mensaje que obtenemos es un INFORM almacenamos el conversationID, para su uso posterior
            this.conversationIdServer = receive.getConversationId();
			
			if(DEBUG)
				System.out.println("SUSCRITO. ConvIDServer:" + conversationIdServer);
			
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
	 * @author Hugo Maldonado
	 */
	private void stateCheckAgentsExplore() {
		
		if(DEBUG)
			System.out.println("AgentController state: CHECK_AGENTS_EXPLORE");
        
        //Llamamos a la funcion que manda la conversation ID del servidor y espera por la confirmación
		boolean startCFP = this.requestCheckIn();

		JsonObject message = new JsonObject();
		message.add("checkMap", "flying");
		
        //Una vez tenemos los agentes despiertos y funcionando, pasamos a buscar el "volador"
		if(startCFP) {
			for(AgentID carName : carNames)
				sendMessage(carName, ACLMessage.CFP, this.generateReplyId(), this.conversationIdController, message.asString());
			
			boolean flyingFound = false;
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
						sendMessage(carName, ACLMessage.ACCEPT_PROPOSAL, this.generateReplyId(), conversationIdController, messageAccept.asString());
					else
						sendMessage(carName, ACLMessage.REJECT_PROPOSAL, this.generateReplyId(), conversationIdController, message.asString());
					
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
	 * Terminar la iteración y volver a empezar otra, pasamos al modo SUBS_MAP_EXPLORE
	 * 
	 * @author Bryan Moreno Picamán and Hugo Maldonado
	 */
	private void stateReRun() {
		
        System.out.println("AgentController state: RE_RUN");
        
		JsonObject message = new JsonObject();
		message.add("die", "now");

        for(AgentID carName : carNames)
			sendMessage(carName, ACLMessage.REQUEST, this.generateReplyId(), conversationIdController, message.asString());
        
		boolean allOk = true;
		
		//Esperamos una contestación por cada uno de los mensajes enviados
		for(AgentID carName : carNames) {
			ACLMessage receive = this.receiveMessage();
			//Si alguno de los mensajes no es un INFORM, la comunicación ha fallado
			if(receive.getPerformativeInt() != ACLMessage.AGREE){
				allOk = false;
			}
		}
        
		if(allOk)
			this.state = SUBS_MAP_EXPLORE;
		else
			this.state = FINALIZE;
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
		
		if(DEBUG)
			System.out.println("AgentController state: REQUEST_POSITION");
		
		JsonObject message = new JsonObject();
		
		message.add("givePosition", "ok");
		
		for(AgentID carName : carNames)
			sendMessage(carName, ACLMessage.REQUEST, this.generateReplyId(), conversationIdController, message.asString());

		boolean allOk = true;
		
		//Esperamos una contestación por cada uno de los mensajes enviados
		for(AgentID carName : carNames) {
			ACLMessage receive = this.receiveMessage();
			//Si alguno de los mensajes no es un INFORM, la comunicación ha fallado
			if(receive.getPerformativeInt() != ACLMessage.INFORM) {
				allOk = false;
			}
			else {
				int row = -1;

				if(receive.getSender() == carNames[0])
					row = 0;
				else if(receive.getSender() == carNames[1])
					row = 1;
				else if(receive.getSender() == carNames[2])
					row = 2;
				else if(receive.getSender() == carNames[3])
					row = 3;

				if(row != -1) {
					JsonObject response = Json.parse(receive.getContent()).asObject();

					int posX = response.get("posX").asInt();
					int posY = response.get("posY").asInt();

					this.carLocalInfo[row][INDEX_POSX] = posX;
					this.carLocalInfo[row][INDEX_POSY] = posY;
				}
				else {
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
		
		if(DEBUG)
			System.out.println("AgentController state: SEND_MAP");
		
		JsonObject message = new JsonObject();
		
		// Recorrer el mapa buscando todas las posiciones que sean objetivos
		ArrayList<Integer> posObj = new ArrayList<>();
		
		for(int y=0; y<this.mapWorldSize; y++) {
			for(int x=0; x<this.mapWorldSize; x++) {
				if(this.mapWorld[y][x] == 3) {
					posObj.add(x);
					posObj.add(y);
				}
			}
		}
		
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
						}
					}
				}
			}
			
			carLocalInfo[i][INDEX_OBJX] = objX;
			carLocalInfo[i][INDEX_OBJY] = objY;
		}
		
		for(int k=0; k<carNames.length; k++) {
			// Mandar el mapa con una pequeña modificación a cada uno, para hacer que los objetivos de los otros agentes sean muros
			int mapAux[][] = new int[this.mapWorldSize][this.mapWorldSize];
			for(int y=0; y<this.mapWorldSize; y++) {
				for(int x=0; x<this.mapWorldSize; x++) {
					mapAux[y][x] = this.mapWorld[y][x];
				}
			}
			
			for(int i=0; i<carNames.length; i++) {
				if(i != k) {
					mapAux[carLocalInfo[i][INDEX_OBJX]][carLocalInfo[i][INDEX_OBJY]] = 2;
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
		
			this.sendMessage(carNames[k], ACLMessage.INFORM, this.generateReplyId(), conversationIdController, message.asString());
		}
		
		this.state = FUEL_INFORMATION;
	}
	
	/**
	 * Obtener la información de la batería de los agentes
	 * 
	 * @author Hugo Maldonado and Bryan Moreno
	 */
	private void stateFuelInformation() {
		
		if(DEBUG)
			System.out.println("AgentController state: FUEL_INFORMATION");
		
		JsonObject message = new JsonObject();
		
		message.add("agent-info", "OK");
		
		// Enviar petición de información de batería
		for(AgentID carName : carNames)
			this.sendMessage(carName, ACLMessage.QUERY_REF, this.generateReplyId(), conversationIdController, message.asString());
		
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
				int row = -1;
				
				if(inbox.getSender() == carNames[0])
					row = 0;
				else if(inbox.getSender() == carNames[1])
					row = 1;
				else if(inbox.getSender() == carNames[2])
					row = 2;
				else if(inbox.getSender() == carNames[3])
					row = 3;
				
				if(row != -1) {
					JsonObject response = Json.parse(inbox.getContent()).asObject();
				
					this.globalFuel = response.get("global-fuel").asInt();
					int actualFuel = response.get("actual-fuel").asInt();
					int fuelToGoal = response.get("fuel-to-goal").asInt();

					this.carLocalInfo[row][INDEX_ACTUAL_FUEL] = actualFuel;
					this.carLocalInfo[row][INDEX_FUEL_TO_GOAL] = fuelToGoal;
					
					if(fuelToGoal == -1) {
						/*message = new JsonObject();
						
						message.add("die", "now");
						
						this.sendMessage(carName, ACLMessage.REQUEST, this.generateReplyId(), conversationIdController, message.asString());
						
						inbox = this.receiveMessage();
						
						if(inbox.getPerformativeInt() == ACLMessage.AGREE) {
							carNamesRemoved[cont] = carName;
							cont++;
						}*/
						allOk = false;	
					}
				}
				else {
					allOk = false;
				}
			}
		}
		
		AgentID newCarNames [] = new AgentID[carNames.length - cont];
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
		
		this.carNames = newCarNames;
		
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
                    fuelNeeded[i] = carLocalInfo[i][INDEX_FUEL_TO_GOAL] - carLocalInfo[i][INDEX_ACTUAL_FUEL];
                    contador+=fuelNeeded[i];
                }
            
                ArrayList <Integer> chosenList = new ArrayList <Integer>();

                int maxIndex;
                do{
                    maxIndex=this.calculateMaxIndex(fuelNeeded);
                    contador-=fuelNeeded[maxIndex];
                    fuelNeeded[maxIndex]=-1;
                }while(contador>this.globalFuel);
                
                for(int i=0;i<fuelNeeded.length;i++){
                    if(fuelNeeded[i]>=0)
                        chosenList.add(i);
                }
                this.numSentCars = chosenList.size();
                
                this.sendCars(chosenList);
                this.state = CONTROL_AGENTS;
	}
        
	/**
         * Enviar una confirmación de moverse al goal a los agentes elegidos
         * @param chosenList Lista de agentes elegidos
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno
         */
        private void sendCars(ArrayList<Integer> chosenList){
            for(Integer i : chosenList){ 
                JsonObject message = new JsonObject();
                message.add("go-to-goal", "OK");
                this.sendMessage(carNames[ i.intValue()], ACLMessage.REQUEST, this.generateReplyId(), conversationIdController, message.asString());
            }
                
        }
        
        
        /**
         * Calcula el índice del mayor elemento de una lista de enteros
         * @param array Lista de valores enteros
         * @return El índice del mayor valor
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
	 * @author
	 */
	private void stateControlAgents() {
            
            int numCars = this.numSentCars;
            while(numCars > 0){
                ACLMessage receive = this.receiveMessage();
                
                //Si es un move
                if(receive.getPerformativeInt() == ACLMessage.INFORM){
                    //Actualizamos el mapa
                    
                    //Si tenemos a otro car bloqueado, avisarlo
                    
                    //Si ha llegado a su goal, bajar numCars y subir carsInGoal
                }
                //Ha visto a otro car
                else if (receive.getPerformativeInt() == ACLMessage.QUERY_REF){
                    //Mirar si hay que bloquear a este car
                    
                    //Si hay que bloquearlo, enviarle el mensaje y meterlo en la lista de bloqueados
                }
                //Algo malo ha pasado
                else{
                    numCars--;
                }
            }
		
            state = FINALIZE;
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
				sendMessage(carName, ACLMessage.REQUEST, this.generateReplyId(), conversationIdController, message.asString());    
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
			sendMessage(carName, ACLMessage.INFORM, this.generateReplyId(), conversationIdController, message.asString());
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
