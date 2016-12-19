/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import es.upv.dsic.gti_ia.core.AgentID;
import java.util.UUID;

/**
 *
 * @author JoseDavid
 */
public class Practica3 {

    private static final AgentID SERVER_AGENT = new AgentID("Izar");
	
	// Generamos los nombres de los agentes de forma aleatoria y Ãºnica en cada ejecuciÃ³n para evitar la duplicidad de nombres con otros agentes en la plataforma
	// Son constantes durante toda la ejecuciÃ³n.
	private static final AgentID CONTROLLER_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final AgentID CAR1_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final AgentID CAR2_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final AgentID CAR3_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final AgentID CAR4_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	
    
    public static void main(String[] args) {
        // TODO code application logic here
    }
    
}
