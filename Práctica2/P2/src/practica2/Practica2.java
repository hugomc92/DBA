
package practica2;

import agents.Agent;
import agents.AgentCar;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;
import java.util.UUID;

/**
 * Clase principal que lanzará toda la aplicación.
 * Básicamente lanzará el agente coche, que empezará con la ejecución de toda la lógica.
 * 
 * @author Hugo Maldonado and Bryan Moreno Picamán
 */
public class Practica2 {
	
	private static final AgentID SERVER_AGENT = new AgentID("Izar");
	
	// Generamos los nombres de los agentes de forma aleatoria y única en cada ejecución para evitar la duplicidad de nombres con otros agentes en la plataforma
	// Son constantes durante toda la ejecución.
	private static final AgentID CAR_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final String MAP = "map11";
	private static final AgentID MOVEMENT_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final AgentID SCANNER_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final AgentID RADAR_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final AgentID GPS_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final AgentID WORLD_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	private static final AgentID BATTERY_NAME = new AgentID(UUID.randomUUID().toString().substring(0, 5));
	
	/**
	 * @param args Los argumentos pasados por línea de comandos.
	 * 
	 * @throws java.lang.Exception en la conexión con el servidor.
	 * 
	 * @author Hugo Maldonado and Bryan Moreno Picamán
	 */
	public static void main(String[] args) throws Exception {
		
		// Nuestra configuración privada de conexión con el servidor
		AgentsConnection.connect("isg2.ugr.es", 6000, SERVER_AGENT.getLocalName(), "Cadenas", "Toro", false);
		
		// Lanzamos el agente controlador de los demás, es además el único que se va a conectar con el servidor
		Agent car = new AgentCar(CAR_NAME, SERVER_AGENT, MAP, MOVEMENT_NAME, SCANNER_NAME, RADAR_NAME, GPS_NAME, WORLD_NAME, BATTERY_NAME);
		
		car.start();
		
		System.out.println("\nINFORMATION:");
		System.out.println("SERVER: " + SERVER_AGENT.getLocalName());
		System.out.println("MAP: " + MAP);
		System.out.println("CAR: " + CAR_NAME.getLocalName());
		System.out.println("MOVEMENT: " + MOVEMENT_NAME.getLocalName());
		System.out.println("SCANNER: " + SCANNER_NAME.getLocalName());
		System.out.println("RADAR: " + RADAR_NAME.getLocalName());
		System.out.println("GPS: " + GPS_NAME.getLocalName());
		System.out.println("WORLD: " + WORLD_NAME.getLocalName());
		System.out.println("BATTERY: " + BATTERY_NAME.getLocalName());
	}
}