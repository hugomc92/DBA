
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

    public AgentCar(AgentID name) throws Exception {
        super(name);
    }
    
}
