package practica3;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import java.util.ArrayList;

/**
 * Clase que define cada uno de los agentes
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
    
    private int fuelLocal;

    private int positionX;
    private int positionY;
    private int fuelGlobal;
    
    private int goalPositionX;
    private int goalPositionY;
    private ArrayList<ArrayList> pathtoGoal;
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
    
    /**
     * Constructor
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
      * @author Aaron Rodriguez and Bryan Moreno Picamán
      */
    @Override
    public void init() {

        this.state = IDLE;
        this.convIDController = new String();
        this.convIDServer = new String();
        this.replyWithController = new String();
        this.replyWithServer = new String();
        
        this.range = -1;
        this.fuelRate = 999999;
        this.fuelLocal=-1;
        this.fuelGlobal=-1;
        if (DEBUG)
            System.out.println("AgentCar " + this.getName() + " has just started");
    }

    /**
     * ESTADO IDLE: Espera a que el controlador le mande alguna acción,
     * y según lo que le pida pasará a un estado u otro.
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
     * @author Aaron Rodriguez and Bryan Moreno Picamán
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
            //Guardamos las capabilities
            myJson = Json.parse(messageReceived.getContent()).asObject();
            this.range = myJson.get("capabilities").asObject().get("range").asInt();
            this.fuelRate = myJson.get("capabilities").asObject().get("fuelrate").asInt();
            this.fuelLocal = 100; //EL fuel inicial es 100?
            if(myJson.get("capabilities").asObject().get("fly").asBoolean() == true)
                type = new Fly();
            else
                type = new NotFly();
            
            //Avisamos al server de que las hemos obtenido
            myJson = new JsonObject();
            myJson.add("capabilites","received");
            sendMessage(controllerName,ACLMessage.INFORM,replyWithController,convIDController,myJson.toString());
            
            this.state = READY;
        }
    }
    
    /**
     * ESTADO READY: está a la espera de recibir un mensaje para hacer entre varias acciones:
     * -Saber si vuela: Estado ACCEPT_REFUSE_PROP
     * -Pedir la posición: Estado POSITION_REQUESTING
     * -Calcular el camino al goal dado: Estado CALCULATE_PATH
     * -Petición de morirse: estado FINALIZE
     * @author Aaron Rodriguez and Bryan Moreno Picamán
     */
    private void stateReady(){
        ACLMessage messageReceived = receiveMessage();
        
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
            default:
                break;
        }
    }
    
    /**
     * Ejecución del controlador
     * 
     * @author Aaron Rodriguez and Bryan Moreno Picamán
     */
    @Override
    public void execute() {
	
        if (DEBUG)
            System.out.println("AgentCar " + this.getName() + " execution");
                
        while(true){
        
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
                    stateRefuseProp();
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
                    stateFinalize();
                    break;
            }
        }
    }
	
	/**
	  * Método de finalización del agente Controlador.
	  * @author Hugo Maldonado
	  */
	@Override
	public void finalize() {
		System.out.println("AgentCar " + this.getName() + " has just finished");
		super.finalize();
	}
    
	/**
     * ESTADO POSITION_REQUESTING
     * Coge su posición y se la manda al controlador
     * @author Bryan Moreno 
	 */
    private void statePositionRequesting() {
        //Avisamos al server de nuestra posición
        JsonObject myJson = new JsonObject();
        myJson.add("posX",positionX);
        myJson.add("posY",positionY);
        sendMessage(controllerName,ACLMessage.INFORM,replyWithController,convIDController,myJson.toString());
        this.state = READY;
    }
    
	/**
     * ESTADO CALCULATE_PATH
     * Calcula el camino entre el agente y el goal ademas del fuel necesario para alcanzarlo
     * @author Bryan Moreno 
	 */
    private void stateCalculatePath() {
        
        this.pathtoGoal=this.type.calculatePath(goalPositionX,goalPositionY);
        //El size de pathToGoal es el numero de "movimientos" hasta el mismo, por eso se usa para el calculo del fuel
        this.fuelToGoal=this.pathtoGoal.size()*this.fuelRate;
        this.state=SEND_NECESSARY_FUEL;
    }
    
	/**
     * ESTADO SEND_NECESSARY_FUEL
     * Espera la petición de información.
     * Manda el fuel calculado para llegar al goal al controlador y espera su decisión. 
     * El controlador puede decidir si el agente va al goal o muere.
     * @author Bryan Moreno 
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
            sendMessage(controllerName,ACLMessage.INFORM,replyWithController,convIDController,myJson.toString());
            
            ACLMessage actionReceived = receiveMessage();
                if(actionReceived.getPerformativeInt() == ACLMessage.QUERY_REF && messageReceived.getContent().contains("go-to-goal") ){
                    this.state= MOVE_TO_GOAL;
                }
                else
                    this.state= FINALIZE;
        }
        else //Si no es una petición, algo ha ido mal, finalizamos
            this.state= NOT_UND_FAILURE_REFUSE;
    }
    
	/**
     * ESTADO NOT_UND_FAILURE_REFUSE
     * Informa al controlador de la respuesta no entendida y espera a la petición de muerte.
     * @author Bryan Moreno 
	 */
    private void stateNotUndFailureRefuse() {
        //No se que contenido mandar en el NOT_UND, no lo veo especificado
        JsonObject myJson = new JsonObject();
        myJson.add("details","No entendido");
        sendMessage(controllerName,ACLMessage.NOT_UNDERSTOOD,replyWithController,convIDController,myJson.toString());
        
        //Esperamos la recepción del mensaje con la petición de muerte.
        ACLMessage messageReceived = receiveMessage();
        if (messageReceived.getPerformativeInt() == ACLMessage.REQUEST && messageReceived.getContent().contains("die")){
            myJson = new JsonObject();
            myJson.add("die","ok");
            sendMessage(controllerName,ACLMessage.AGREE,replyWithController,convIDController,myJson.toString());
            this.state=FINALIZE;
        }
    }
    
	/**
     * ESTADO FINALIZE
     * Mata al agente.
     * @author Bryan Moreno 
	 */
    private void stateFinalize() {
        this.finalize();
    }

    private void stateRefuseProp() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void stateMoveToGoal() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void stateExploreMap() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    
}
