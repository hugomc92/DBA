
package practica3;

import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
import java.util.UUID;

/**
 * Clase que lanza el programa
 * 
 * @author Hugo Maldonado
 */
public class Practica3 {
	
	private static final boolean SHENRON = false;

    private static final AgentID SERVER_AGENT = new AgentID("Izar");
	
	// Generamos los nombres de los agentes de forma aleatoria y única en cada ejecución para evitar la duplicidad de nombres con otros agentes en la plataforma
	// Son constantes durante toda la ejecución.
	private static final AgentID CONTROLLER_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5) + "_CONTROLLER");
	private static final String MAP = "map9";
	private static final AgentID CAR1_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5) + "_CAR0");
	private static final AgentID CAR2_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5) + "_CAR1");
	private static final AgentID CAR3_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5) + "_CAR2");
	private static final AgentID CAR4_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5) + "_CAR3");
	
    /**
	 * Inicio del programa
	 * 
	 * @param args Argumentos de la línea de comandos al iniciar el programa
	 * 
	 * @author Hugo Maldonado
	 */
    public static void main(String[] args) {
		
		// Nuestra configuración privada de conexión con el servidor
		if(!SHENRON)
			AgentsConnection.connect("isg2.ugr.es", 6000, SERVER_AGENT.getLocalName(), "Cadenas", "Toro", false);
		else
			AgentsConnection.connect("isg2.ugr.es", 6000, "test", "Cadenas", "Toro", false);
		
		// Lanzamos el agente controlador de los demás
		try {
			// Se inicializa el agente controlador y se le pasa la ID para todos los agent car que se van a despertar
			AgentController controller = new AgentController(CONTROLLER_NAME, SERVER_AGENT, MAP, CAR1_NAME, CAR2_NAME, CAR3_NAME, CAR4_NAME);
			
			//Se lanza el agente controlador
			controller.start();

			//Se muestra información cobre el mapa y los coches.
			System.out.println("\nINFORMATION:");
			System.out.println("MAP: " + MAP);
			System.out.println("CAR 0: " + CAR1_NAME.getLocalName());
			System.out.println("CAR 1: " + CAR2_NAME.getLocalName());
			System.out.println("CAR 2: " + CAR3_NAME.getLocalName());
			System.out.println("CAR 3: " + CAR4_NAME.getLocalName());
		} catch(Exception ex) {
			System.err.println("Error starting controller");

			System.err.println(ex.getMessage());
			
			System.exit(-1);
		}
    }
}