package practica3;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.List;
import javax.swing.JFrame;

/**
 * Clase que define cada uno de los agentes
 * 
 * @author Aaron Rodriguez and Bryan Moreno
 */
public class AgentCar extends Agent {
    
    private static final boolean DEBUG = true;
    
    private static final int IDLE = 0;
    private static final int GET_CAPABILITIES = 1;
    private static final int READY = 2;
    private static final int EXPLORE_MAP = 3;
    private static final int FINALIZE = 4;
    private static final int NOT_UND_FAILURE_REFUSE = 5;
    private static final int CALCULATE_PATH = 6;
    private static final int MOVE_TO_GOAL = 7;
    private static final int SEND_NECESSARY_FUEL = 8;
    private static final int ACCEPT_REFUSE_PROP = 9;
    private static final int POSITION_REQUESTING = 10;
    private int state;
	private boolean finish;
    
    private int fuelLocal;
	
	private final int SIZE_MAP = 510;
	private int [][] mapWorld;

    private int positionX;
    private int positionY;
    private int fuelGlobal;
    
    private int goalPositionX;
    private int goalPositionY;
    private List<Node> pathToGoal;
    private int fuelToGoal;
    
    private final AgentID serverName;
    private final AgentID controllerName;
    
    private String convIDServer;
    private String convIDController;
    private String replyWithServer;
    private String replyWithController;
    
    private int range;
    private int fuelRate;
    private TypeAgent type;
    private boolean inGoal;
    
    private String errorMessage;
    private int[][] radar;
    
    private int startExploringX;
    private int startExploringY;
    private String mapDirection;
	
	private JFrame jframe;
    private MyDrawPanel m;
	
	private boolean iniDraw;
    
    /**
     * Constructor
	 * 
     * @param name ID de este agente
     * @param controllerName ID del agente Controlador
     * @param serverName ID del agente Server
     * @throws Exception 
     * @author Aaron Rodriguez
     */
    public AgentCar(AgentID name, AgentID controllerName, AgentID serverName) throws Exception {
        super(name);
        this.serverName = serverName;
        this.controllerName = controllerName;
    }
	
    /**
      * Método de inicialización del agente Coche.
	  * 
      * @author Aaron Rodriguez and Bryan Moreno Picamán
      */
    @Override
    public void init() {

        this.state = IDLE;
		finish = false;
		
        this.convIDController = new String();
        this.convIDServer = new String();
        this.replyWithController = new String();
        this.replyWithServer = new String();
        
        this.range = -1;
        this.fuelRate = 999999;
        this.fuelLocal = -1;
        this.fuelGlobal = -1;
        this.fuelToGoal = -1;
        this.inGoal = false;
        this.mapWorld = new int[this.SIZE_MAP][this.SIZE_MAP];
		
		for(int y=0; y<SIZE_MAP; y++) {
			for(int x=0; x<SIZE_MAP; x++) {
				this.mapWorld[y][x] = -1;
			}
		}
		
		iniDraw = false;
        
        System.out.println("AgentCar " + this.getName() + " has just started");
    }

    /**
     * ESTADO IDLE: Espera a que el controlador le mande alguna acción,
     * y según lo que le pida pasará a un estado u otro.
	 * 
     * @author Aaron Rodriguez
     */
    private void stateIdle(){
        ACLMessage messageReceived = receiveMessage();
        
        if(messageReceived.getPerformativeInt() == ACLMessage.REQUEST) {
            this.state = FINALIZE;
        }
        else if (messageReceived.getPerformativeInt() == ACLMessage.INFORM){
            this.state = GET_CAPABILITIES;
            //Cogemos los dos convID
            JsonObject responseObject = Json.parse(messageReceived.getContent()).asObject();
            this.convIDServer = responseObject.get("conversationID-server").asString();
            
            this.convIDController = messageReceived.getConversationId();
            this.replyWithController = messageReceived.getReplyWith();
        }
    }
    
    /**
     * ESTADO GET_CAPABILITIES: hace check in en el Server, y luego avisa al Controller
     * de que ya está hecho.
	 * 
     * @author Aaron Rodriguez and Bryan Moreno Picamán and Hugo Maldonado
     */
    private void stateGetCapabilities(){
        //Mandamos el checkin
        JsonObject myJson = new JsonObject();
        myJson.add("command","checkin");
        sendMessage(serverName,ACLMessage.REQUEST,"",convIDServer,myJson.toString());
    
        //Esperamos las capabilities
        ACLMessage messageReceived = receiveMessage();
        if(messageReceived.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD){
            state = NOT_UND_FAILURE_REFUSE;
        }
        else{
            replyWithServer = messageReceived.getReplyWith();
            //Guardamos las capabilities
            myJson = Json.parse(messageReceived.getContent()).asObject();
            this.range = myJson.get("capabilities").asObject().get("range").asInt();
            this.radar = new int [range][range];
            this.fuelRate = myJson.get("capabilities").asObject().get("fuelrate").asInt();
            this.fuelLocal = 100; //EL fuel inicial es 100?
            if(myJson.get("capabilities").asObject().get("fly").asBoolean() == true)
                type = new Fly();
            else
                type = new NotFly();
            
            //Avisamos al server de que las hemos obtenido
            myJson = new JsonObject();
            myJson.add("capabilites","received");
            this.answerMessage(controllerName,ACLMessage.INFORM,replyWithController,convIDController,myJson.toString());
            
            this.state = READY;
        }
    }
    
    /**
     * ESTADO READY: está a la espera de recibir un mensaje para hacer entre varias acciones:
     * -Saber si vuela: Estado ACCEPT_REFUSE_PROP
     * -Pedir la posición: Estado POSITION_REQUESTING
     * -Calcular el camino al goal dado: Estado CALCULATE_PATH
     * -Petición de morirse: estado FINALIZE
	 * 
     * @author Aaron Rodriguez and Bryan Moreno Picamán
     */
    private void stateReady(){
        ACLMessage messageReceived = receiveMessage();
        replyWithController = messageReceived.getReplyWith();
        
        switch (messageReceived.getPerformativeInt()) {
            case ACLMessage.CFP:
                //Solo hay 1 CPF. No hace falta chequearlo
                state = ACCEPT_REFUSE_PROP;
                break;
            case ACLMessage.QUERY_REF:
                //Solo hay 1 QUERY_REF
                state = CALCULATE_PATH;
                break;
            case ACLMessage.REQUEST:
                //Comprobamos mensaje
                if(messageReceived.getContent().contains("die")){
                    state = FINALIZE;
                }
                else if (messageReceived.getContent().contains("givePosition")){
                    state = POSITION_REQUESTING;
                }
                else{
                    System.out.println("La has liado, cebollón");
                }   
                break;
        }
    }
    
    /**
     * Petición de movimiento al server hacia una casilla anexa a nuestra posición actual,
     * y respuesta de éste.
     * @param newX Coordenada x hacia la que queremos movernos.
     * @param newY Coordenada y hacia la que queremos movernos.
     * @author Aarón Rodríguez
     */
    private void commandMove(int newX, int newY){
        String movement;
        //Calculamos el movimiento que queremos hacer, traduciendo la x y la y
        if(newX == positionX-1) {		//Se mueve hacia el Oeste
            if(newY == positionY-1)	//Se mueve hacia Norte
		movement = "moveNW";
            else if(newY == positionY)
		movement = "moveW";
            else
		movement = "moveSW";
        }
        else if(newX == positionX) {
            if(newY == positionY-1)
		movement = "moveN";
            else
                    movement = "moveS";
        }
        else {
            if(newY == positionY-1)
                    movement = "moveNE";
            else if(newY == positionY)
                    movement = "moveE";
            else
                    movement = "moveSE";
        }
        
        //Se lo enviamos al Server
        JsonObject myJson = new JsonObject();
        myJson.add("command",movement);
        answerMessage(serverName,ACLMessage.REQUEST,replyWithServer,convIDServer,myJson.toString());       
        
        //Esperamos respuesta del server
        ACLMessage messageReceived = receiveMessage();
        
        //Si es NOT UNDERSTOOD o FAILURE, pasamos al estado NOT_UND_FAILURE_REFUSE
        //Guardamos el string con el tipo de fallo
        if(messageReceived.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD ||
                messageReceived.getPerformativeInt() == ACLMessage.FAILURE){
            state = NOT_UND_FAILURE_REFUSE;
            if(messageReceived.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD)
                errorMessage = "NOT_UNDERSTOOD";
            else
                errorMessage = "FAILURE";
        }
        
        //Si no, guardamos la nueva posición y guardamos el replyWith
        else{
            replyWithServer = messageReceived.getReplyWith();
            positionX = newX;
            positionY = newY;
            fuelLocal -= fuelRate;
            fuelToGoal -= fuelRate;
        }
    }
    
    /**
     * Petición al server de repostar, y respuesta de éste.
     * @author Aarón Rodríguez
     */
    private void commandRefuel(){
        //Mandamos el command refuel al server
        JsonObject myJson = new JsonObject();
        myJson.add("command","refuel");
        answerMessage(serverName,ACLMessage.REQUEST,replyWithServer,convIDServer,myJson.toString());  
        
        //Esperamos su respuesta
        ACLMessage messageReceived = receiveMessage();
        
        //Si es NOT UNDERSTOOD o REFUSE, pasamos al estado NOT_UND_FAILURE_REFUSE
        //Guardamos el string con el tipo de fallo
        if(messageReceived.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD ||
                messageReceived.getPerformativeInt() == ACLMessage.REFUSE){
            state = NOT_UND_FAILURE_REFUSE;
            if(messageReceived.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD)
                errorMessage = "NOT_UNDERSTOOD";
            else
                errorMessage = "REFUSE";
        }
        
        //Si no, actualizamos el fuel y guardamos el replyWith
        else{
            replyWithServer = messageReceived.getReplyWith();
            fuelLocal = 100;
        }
    }
    
	/**
	 * Petición al servidor y respuesta de éste sobre las percepciones.
	 *
	 * @author Aaron Rodríguez
	 */
	private void requestPerceptions(){
        //Mandamos la petición de las percepciones al server
        answerMessage(serverName,ACLMessage.QUERY_REF,replyWithServer,convIDServer,"");  
        
        //Esperamos su respuesta
        ACLMessage messageReceived = receiveMessage();
        
        //Si es NOT UNDERSTOOD, pasamos al estado NOT_UND_FAILURE_REFUSE
        if(messageReceived.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD){
            state = NOT_UND_FAILURE_REFUSE;
            if(messageReceived.getPerformativeInt() == ACLMessage.NOT_UNDERSTOOD)
                errorMessage = "NOT_UNDERSTOOD";
            else
                errorMessage = "REFUSE";
        }
        
        //Si no, actualizamos las percepciones y guardamos el replyWith
        else{
            replyWithServer = messageReceived.getReplyWith();
            String message = messageReceived.getContent();
            JsonObject myJson = Json.parse(message).asObject();
            positionX = myJson.get("result").asObject().get("x").asInt()+5;
            positionY = myJson.get("result").asObject().get("y").asInt()+5;
            fuelLocal = myJson.get("result").asObject().get("battery").asInt();
            fuelGlobal = myJson.get("result").asObject().get("energy").asInt();
            inGoal = myJson.get("result").asObject().get("goal").asBoolean();
            int x = 0,y = 0;
            for(JsonValue j : myJson.get("result").asObject().get("sensor").asArray()){
                radar[y][x] = j.asInt();
                x++;
                if(x == range-1){
                    x = 0;
                    y++;
                }
            }
        }
    }
    
    /**
     * ESTADO POSITION_REQUESTING: pide las percepciones al server, se las mandamos
     * al Controller si no hay fallo y volvemos al estado READY.
	 * 
     * Si hubiera fallo, pasaríamos a NOT_UND_REFUSE_FAILURE.
     * @author Aaron Rodriguez and Hugo Maldonado
     */
    private void statePositionRequesting(){
        //Pedimos las percepciones para saber nuestra posición
        requestPerceptions();
        
        //Le mandamos las coordenadas al controller y volvemos al estado READY
        if (state != NOT_UND_FAILURE_REFUSE){
            JsonObject myJson = new JsonObject();
            myJson.add("posX",positionX);
            myJson.add("posY",positionY);
            answerMessage(controllerName,ACLMessage.INFORM,replyWithController,convIDController,myJson.toString());
            
            state = READY;
        }
    }
    
    /**
     * Ejecución del controlador
     * 
     * @author Aaron Rodriguez and Bryan Moreno Picamán
     */
    @Override
    public void execute() {

        System.out.println("AgentCar " + this.getName() + " execution");
                
        while(!finish) {
            switch(state){
                case IDLE:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado IDLE");
                    stateIdle();
                    break;
                    
                case GET_CAPABILITIES:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado GET_CAPABILITIES");
                    stateGetCapabilities();
                    break;
                 
                case POSITION_REQUESTING:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado POSITION_REQUESTING");
                    statePositionRequesting();
                    break;
                    
                case ACCEPT_REFUSE_PROP:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado ACCEPT_REFUSE_PROP");
                    stateAcceptRefuseProp();
                    break;
                    
                case READY:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado READY");
                    stateReady();
                    break;
                    
                case CALCULATE_PATH:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado CALCULATE_PATH");
                    stateCalculatePath();
                    break;
                   
                case MOVE_TO_GOAL:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado MOVE_TO_GOAL");
                    stateMoveToGoal();
                    break;
                    
                case EXPLORE_MAP:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado EXPLORE_MAP");
                    stateExploreMap();
                    break;
                    
                case SEND_NECESSARY_FUEL:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado SEND_NECESSARY_FUEL");
                    stateSendNecessaryFuel();
                    break;
                    
                case NOT_UND_FAILURE_REFUSE:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado NOT_UND_FAILURE_REFUSE");
                    stateNotUndFailureRefuse();
                    break;
                    
                case FINALIZE:
                    if (DEBUG)
                        System.out.println("AgentCar " + this.getName() + " en el estado FINALIZE");
                    
					this.finish = true;
					
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
		System.out.println("AgentCar " + this.getName() + " has just finished");
		super.finalize();
	}
    
	/**
     * ESTADO CALCULATE_PATH
	 * 
     * Primero recibe el mapa del controlador así como su objetivo y calcula el camino entre el agente y el goal ademas del fuel necesario para alcanzarlo
	 * 
     * @author Bryan Moreno and Hugo Maldonado
	 */
    private void stateCalculatePath() {
		
		ACLMessage inbox = this.receiveMessage();
		
		if(inbox.getPerformativeInt() == ACLMessage.INFORM) {
			JsonObject receive = Json.parse(inbox.getContent()).asObject();
			
			JsonArray mapArray = receive.get("map").asArray();
			
			// Pasar el mapa a la matriz
			int cont = 0;
			
			this.mapWorld = new int[SIZE_MAP][SIZE_MAP];
			
			for(int y=0; y<SIZE_MAP; y++) {
				for(int x=0; x<SIZE_MAP; x++) {
					this.mapWorld[y][x] = mapArray.get(cont).asInt();
					cont++;
				}
			}
			
			this.goalPositionX = receive.get("goalX").asInt();
			this.goalPositionY = receive.get("goalY").asInt();
			this.type.setMap(mapWorld);
    
			this.pathToGoal = this.type.calculatePath(positionX, positionY, goalPositionX, goalPositionY);
			
			if(this.pathToGoal != null)
				//El size de pathToGoal es el numero de "movimientos" hasta el mismo, por eso se usa para el calculo del fuel
				this.fuelToGoal = this.pathToGoal.size() * this.fuelRate;
				
			this.state = SEND_NECESSARY_FUEL;
		}
		else
			this.state = NOT_UND_FAILURE_REFUSE;
    }
    
	/**
     * ESTADO SEND_NECESSARY_FUEL
     * Espera la petición de información.
	 * 
     * Manda el fuel calculado para llegar al goal al controlador y espera su decisión. 
     * El controlador puede decidir si el agente va al goal o muere.
     * @author Bryan Moreno and Hugo Maldonado
	 */
    private void stateSendNecessaryFuel() {
        //Recibimos el mensaje
        ACLMessage messageReceived = receiveMessage();
        //Si es una petición de información continuamos
        if (messageReceived.getPerformativeInt() == ACLMessage.QUERY_REF && messageReceived.getContent().contains("agent-info")){
            //Avisamos al servidor de el fuel global, el actual y el necesario hasta el goal.
            JsonObject myJson = new JsonObject();
            myJson.add("global-fuel",this.fuelGlobal);
            myJson.add("actual-fuel",this.fuelLocal);
            myJson.add("fuel-to-goal",this.fuelToGoal);
            this.answerMessage(controllerName,ACLMessage.INFORM,replyWithController,convIDController,myJson.toString());
            
            ACLMessage actionReceived = receiveMessage();
			if(actionReceived.getPerformativeInt() == ACLMessage.REQUEST && messageReceived.getContent().contains("go-to-goal") ){
				this.state = MOVE_TO_GOAL;
			}
			else
				this.state = FINALIZE;
        }
        else //Si no es una petición, algo ha ido mal, finalizamos
            this.state= NOT_UND_FAILURE_REFUSE;
    }
    
	/**
     * ESTADO NOT_UND_FAILURE_REFUSE
     * Informa al controlador de la respuesta no entendida y espera a la petición de muerte.
	 * 
     * @author Bryan Moreno and Hugo Maldonado
	 */
    private void stateNotUndFailureRefuse() {
        //No se que contenido mandar en el NOT_UND, no lo veo especificado
        JsonObject myJson = new JsonObject();
        myJson.add("details","No entendido");
        this.answerMessage(controllerName,ACLMessage.NOT_UNDERSTOOD,replyWithController,convIDController,myJson.toString());
        
        //Esperamos la recepción del mensaje con la petición de muerte.
        ACLMessage messageReceived = receiveMessage();
        if (messageReceived.getPerformativeInt() == ACLMessage.REQUEST && messageReceived.getContent().contains("die")){
            myJson = new JsonObject();
            myJson.add("die","ok");
            this.answerMessage(controllerName,ACLMessage.AGREE,replyWithController,convIDController,myJson.toString());
            this.state=FINALIZE;
        }
    }

	/**
	 * Estado en que se envía si el agente vuela o no y se toma la decisión del controlador de explorar el mapa o no
	 * 
	 * @author Hugo Maldonado and Bryan Moreno and Aaron Rodriguez
	 */
    private void stateAcceptRefuseProp() {
		
		JsonObject message = new JsonObject();
		
		message.add("checkMap", "flying");
        
		if(type.getClass() == Fly.class) {
			this.answerMessage(controllerName, ACLMessage.AGREE, replyWithController, convIDController, message.asString());
			
			ACLMessage receiveAccept = this.receiveMessage();
			
			if(receiveAccept.getPerformativeInt() == ACLMessage.ACCEPT_PROPOSAL){
				this.state = EXPLORE_MAP;
                                replyWithController = receiveAccept.getReplyWith();
                                JsonObject responseObject = Json.parse(receiveAccept.getContent()).asObject();
                                this.startExploringX = responseObject.get("startX").asInt();
                                this.startExploringY = responseObject.get("startY").asInt();
                                this.mapDirection = responseObject.get("direction").asString();
                                
                                int x = 0, y = 0;
                                for (JsonValue j : responseObject.get("map").asArray()){
                                    mapWorld[y][x]=j.asInt();
                                    x++;
                                    if(x % SIZE_MAP == 0){
                                        x = 0;
                                        y++;
                                    }
                                }
                        }
			else
				this.state = READY;
		}
		else {
			this.answerMessage(controllerName, ACLMessage.REFUSE, replyWithController, convIDController, message.asString());
			
			this.state = READY;
		}
    }

	/**
	 * Estado para moverse al objetivo
	 * 
	 * @author Bryan Moreno and Hugo Maldonado
	 */
    private void stateMoveToGoal() {
        
		for(Node node : pathToGoal) {
			if(this.fuelLocal < fuelToGoal) {
				this.commandRefuel();
			}
			
			this.commandMove(node.getxPosition(), node.getyPosition());
			
			JsonObject message = new JsonObject();
			
			message.add("x", positionX);
			message.add("y", positionY);
			
			this.sendMessage(controllerName, ACLMessage.INFORM, this.generateReplyId(), convIDController, message.asString());
			
			this.requestPerceptions();
			
			boolean otherAgentFound = false;
			JsonArray otherAgentsPosition = new JsonArray();
			
			for(int y=0; y<range; y++)
				for(int x=0; x<range; x++)
					if(radar[y][x] == 4) {
						otherAgentFound = true;
						
						JsonObject position = new JsonObject();
						position.add("x", x);
						position.add("y", y);
						
						otherAgentsPosition.add(position);
					}
			
			if(otherAgentFound) {
				JsonObject messageCanMove = new JsonObject();
				
				message.add("canMove", "OK");
				message.add("otherAgents", otherAgentsPosition);
			
				this.sendMessage(controllerName, ACLMessage.INFORM, this.generateReplyId(), convIDController, messageCanMove.asString());
				
				ACLMessage inbox = this.receiveMessage();
				
				if(inbox.getPerformativeInt() == ACLMessage.DISCONFIRM) {
					inbox = this.receiveMessage();
					
					if(inbox.getPerformativeInt() != ACLMessage.INFORM)
						this.state = NOT_UND_FAILURE_REFUSE;
				}
				else if(inbox.getPerformativeInt() != ACLMessage.CONFIRM)
					this.state = NOT_UND_FAILURE_REFUSE;
			}
		}
    }
    
    /**
     * ESTADO EXPLORE_MAP: se dirige hacia la posición indicada por el controller,
     * y una vez llegue, comienza a escanear el mapa completo haciendo zig-zag en horizontal.
     * @author Aaron Rodriguez
     */
    private void stateExploreMap() {
        System.out.println("AgentCar " + this.getName() + " en el estado EXPLORE_MAP");
		
		if(!iniDraw) {
			jframe = new JFrame();
			m = new MyDrawPanel(this.mapWorld);
			jframe.add(m);
			jframe.setSize(this.SIZE_MAP+10, this.SIZE_MAP+50);
			jframe.setVisible(true);

			//jframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CL‌​OSE);
			jframe.setUndecorated(true);
			jframe.setTitle(this.getName());
			
			iniDraw = true;
		}
        
        //Primero hacemos un requestPerceptions para saber dónde nos encontramos
        requestPerceptions();

        //Aquí deberíamos crear la imagen a visualizar y pintarla
		m.updateMap(radar, positionX, positionY);
		
		m.repaint();
        
        //Avanzamos hacia la posición dada por el controller
        boolean inPosition = false;
        int newX, newY;
        while(!inPosition && state != NOT_UND_FAILURE_REFUSE){
            //Repostamos si estamos en números rojos
            if(fuelLocal <= fuelRate){
                commandRefuel();
            }
            else{
                if (positionX > startExploringX)
                    newX = positionX-1;
                else if(positionX == startExploringX)
                    newX = positionX;
                else
                    newX = positionX + 1;
                
                if (positionY > startExploringY)
                    newY = positionY-1;
                else if(positionY == startExploringY)
                    newY = positionY;
                else
                    newY = positionY + 1;
                
                //Comprobamos si estamos encima de la posición inicial
                if(newX == positionX && newY == positionY){
                    inPosition = true;
                }
                else{
                    //MOVIMIENTO
                    commandMove(newX,newY);

                    //PERCEPCIÓN
                    requestPerceptions();

                    //ACTUALIZAR IMAGEN A VISUALIZAR
					m.updateMap(radar, positionX, positionY);
		
					m.repaint();
                }
            }
        }

        //Iniciamos las variables pertinentes
        boolean goLeft, mapExplored = false, goDown = false;
        int depth = -1;
        if(mapDirection.equals("right"))
            goLeft = false;
        else
            goLeft = true;
        
        //Empezamos el zig-zag
        while (!mapExplored && state != NOT_UND_FAILURE_REFUSE){
            //Repostamos si estamos en números rojos
            if(fuelLocal <= fuelRate){
                commandRefuel();
            }
            else{
                //Si vamos en horizontal
                if(!goDown){
                    //Nos movemos en nuestra dirección si no hay pared externa,
                    //en caso contrario apuntamos para bajar
                    if((goLeft && mapWorld[positionX-1][positionY] != 2) ||
                            (!goLeft && mapWorld[positionX+1][positionY] != 2)){
                        //MOVIMIENTO
                        if(goLeft)
                            commandMove(positionX-1,positionY);
                        else
                            commandMove(positionX+1,positionY);

                        //PERCEPCIÓN
                        requestPerceptions();

                        //Guardamos la percepción en nuestro mapa
                        for (int j = positionX-1; j <= positionX+1; j++){   //Columnas
                            for (int i = positionY-1; i <= positionY+1; i++){    //Filas
                                mapWorld[i][j] = radar[i-(positionY-1)][j-(positionX-1)];
                            }
                        }
                        
                        //ACTUALIZAR IMAGEN A VISUALIZAR
                        
                    }
                    else{   //Hay pared en la dirección horizontal en la que nos movemos
                        //Intercambiamos direcciones para posteriores movimientos en horizontal
                        goLeft = !goLeft;
                        depth = positionY+3;
                        goDown = true;
                    }
                }
                //Si vamos en vertical
                else{
                    if(mapWorld[positionX][positionY+1] != 2 && positionY < depth){
                        //MOVIMIENTO
                        commandMove(positionX,positionY+1);

                        //PERCEPCIÓN
                        requestPerceptions();

                        //Guardamos la percepción en nuestro mapa
                        for (int j = positionX-1; j <= positionX+1; j++){   //Columnas
                            for (int i = positionY-1; i <= positionY+1; i++){    //Filas
                                mapWorld[i][j] = radar[i-(positionY-1)][j-(positionX-1)];
                            }
                        }

                        //ACTUALIZAR IMAGEN A VISUALIZA
                    }
                    else if (mapWorld[positionX][positionY+1] == 2){  //Tocamos la pared de abajo
                        if(depth - positionY <= 1){  //No es necesario explorar horizontalmente
                            goDown = false;
                        }
                        else{   //Hay que explorar la última fila
                            mapExplored = true;
                        }
                    }
                }
            }
        }
        
        //Enviamos los datos recabados al controller
        JsonObject myJson = new JsonObject();
			
        JsonArray mapArray = new JsonArray();
        for (int j = 0; j < SIZE_MAP; j++){ //Columnas
            for (int i = 0; i < SIZE_MAP; i++){  //Filas
                mapArray.add(mapWorld[i][j]);
            }
        }
        myJson.add("map",mapArray);
        myJson.add("finalX", positionX);
        myJson.add("finalY", positionY);
        if(mapExplored)
            myJson.add("completed","true");
        else
            myJson.add("completed","false");
        if(goLeft)
            myJson.add("direction", "left");
        else
            myJson.add("direction", "right");

        this.answerMessage(controllerName, ACLMessage.INFORM, replyWithController, convIDController, myJson.asString());
        
        
        if(state != NOT_UND_FAILURE_REFUSE)
            state = READY;
    }

}