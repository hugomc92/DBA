
package practica2;

import agents.Agent;
import agents.AgentCar;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.AgentsConnection;

/**
 * Clase principal que lanzará toda la aplicación.
 * Básicamente lanzará el agente coche, que empezará con la ejecución de toda la lógica.
 * 
 * @author Hugo Maldonado.
 */
public class Practica2 {
	
	/**
	 * @param args Los argumentos pasados por línea de comandos.
	 * 
	 * @throws java.lang.Exception en la conexión con el servidor.
	 */
	public static void main(String[] args) throws Exception {
		
		// Nuestra configuración privada de conexión con el servidor
		AgentsConnection.connect("isg2.ugr.es", 6000, "Izar", "Cadenas", "Toro", false);
		
		Agent car = new AgentCar(new AgentID("Car"));
		
		car.start();
	}
}