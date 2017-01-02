
package practica3;

import es.upv.dsic.gti_ia.core.AgentID;

/**
 * Clase que define cada uno de los agentes
 * @author
 */
public class AgentCar extends Agent {
    
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
    private int state;
    
    private AgentID serverName;
    private AgentID controllerName;
    
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
      * @author Aaron Rodriguez
      */
    @Override
    public void init() {

        this.state = IDLE;

        System.out.println("AgentCar " + this.getName() + " has just started");
    }

    /**
     * ESTADO IDLE: Espera a que el controlador le mande alguna acción,
     * y según lo que le pida pasará a un estado u otro.
     * @author Aaron Rodriguez
     */
    private void stateIdle(){
    
    }
    
    /**
     * Ejecución del controlador
     * 
     * @author Aaron Rodriguez
     */
    @Override
    public void execute() {
		
        System.out.println("AgentCar " + this.getName() + " execution");
                
        while(true){
        
            switch(state){
                case IDLE:
                    stateIdle();
                    break;
                    
                case GET_CAPABILITIES:
                    stateGetCapabilities();
                    break;
                    
                case ACCEPT_REFUSE_PROP:
                    stateRefuseProp();
                    break;
                    
                case READY:
                    stateReady();
                    break;
                    
                case CALCULATE_PATH:
                    stateCalculatePath();
                    break;
                   
                case MOVE_TO_GOAL:
                    stateMoveToGoal();
                    break;
                    
                case EXPLORE_MAP:
                    stateExploreMap();
                    break;
                    
                case SEND_NECESSARY_FUEL:
                    stateSendNecessaryFuel();
                    break;
                    
                case NOT_UND_FAILURE_REFUSE:
                    stateNotUndFailureRefuse();
                    break;
                    
                case FINALIZE:
                    stateFinalize();
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
}
