
package agents;

import gui.myDrawPanel;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.AgentID;
import javax.swing.JFrame;

/**
 * Clase que define al agente World.
 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
 */
public class AgentWorld extends Agent {
	
	private static final int IDLE = 0;
	private static final int WAIT_GPS= 1;
	private static final int WAIT_RADAR = 2;
	private static final int WARN_RADAR = 3;
	private static final int WAIT_MOVEMENT = 4;
	private static final int SEND_INFO = 5;
	private static final int FINISH = 6;
    
    JFrame jframe = new JFrame();
    myDrawPanel m = new myDrawPanel();

	private static final boolean DEBUG = false;
	
    private JsonObject responseObject;
	private JsonObject gpsObject;
	private JsonObject commandObject;
	
	private String response;
        
	private int state;
	private int coordX,coordY;
	private boolean finish;
	private final AgentID radarName;
	private final AgentID gpsName;
	private final AgentID movementName;
    private static final int WIDTH = 504;
    private static final int HEIGHT = 504;
    private final int [][] map_world = new int [WIDTH][HEIGHT];
    private final int [] local_world = new int [25];	
	private int cont;
	private final String map;
	
	private final int [][] updateWorld = new int[5][5];
        
	/**
	 * Constructor
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 * @param worldName El nombre de agente.
	 * @param radarName nombre del agente Radar (para comunicación)
	 * @param gpsName nombre del agente Gps (para comunicación)
	 * @param movementName nombre del agente Movement (para comunicación)
     * @param map nombre del mapa que vamos a intentar resolver.
	 * 
	 * @throws java.lang.Exception en la creación del agente.
	 */
	public AgentWorld(AgentID worldName,AgentID radarName,AgentID gpsName,AgentID movementName, String map) throws Exception {
		super(worldName);
		
		this.radarName=radarName;
		this.gpsName=gpsName;
		this.movementName=movementName;
		
		this.map = map;
	}
	
	 /**
	  * Método de inicialización del agente World.
	  * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	  */
	@Override
	public void init() {
		this.state = IDLE;
		this.finish = false;
		this.responseObject = new JsonObject();
		this.gpsObject = new JsonObject();
		
		coordX = coordY = -1;
		this.cont = 0;
		
		for(int i=0; i<WIDTH; i++)
			for(int j=0; j<HEIGHT; j++)
				map_world[i][j] = -1;
		
		for(int i=0; i<5; i++)
			for(int j=0; j<5; j++)
				updateWorld[i][j] = -1;
        
        jframe.add(m);
        jframe.setSize(WIDTH+10, HEIGHT+50);
        jframe.setVisible(true);
        
        //jframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CL‌​OSE);
		//jframe.setUndecorated(true);
		jframe.setTitle(map);
        
		System.out.println("AgentWorld has just started");
	}
	
	/**
	 * Estado IDLE: esperamos un mensaje. Si es del Gps, le mandamos una confirmación
	 * de que nos han llegado sus coordenadas y vamos al WAIT_RADAR. En caso contrario,
	 * vamos a FINISH.
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateIdle(){
		if(DEBUG)
			System.out.println("AgentWorld status: IDLE");

		response = this.receiveMessage();

		if(response.contains("CRASHED") || response.contains("finalize"))
			this.state = FINISH;
		else {
			this.responseObject = Json.parse(response).asObject();
			this.gpsObject = Json.parse(response).asObject();

			String resultGPS = responseObject.get("gps").toString();

			boolean ok = true;

			if(!resultGPS.contains("updated"))
				ok = this.updateWorld(resultGPS);

			JsonObject sendConfirmation = new JsonObject();

			sendConfirmation.add("gps", ok);

			this.sendMessage(gpsName, sendConfirmation.toString());

			this.state = WAIT_RADAR;
		}
	}
	
	/**
	 * Estado WAIT_RADAR: esperamos a que nos lleguen los datos del radar y vamos a
	 * WARN_RADAR.
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateWaitRadar(){
		if(DEBUG)
			System.out.println("AgentWorld status: WAIT_RADAR");

		String responseRadar = this.receiveMessage();

		this.updateWorld(responseRadar);

		this.state = WARN_RADAR;
	}
	
	/**
	 * Estado WARN_RADAR: le enviamos un mensaje al radar de que nos ha llegado,
	 * y pasamos al estado WAIT_MOVEMENT.
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateWarnRadar(){
		if(DEBUG)
			System.out.println("AgentWorld status: WARN_RADAR");

		this.commandObject = new JsonObject();

		this.commandObject.add("radar","ok");

		this.sendMessage(this.radarName, commandObject.toString());

		this.state = WAIT_MOVEMENT;
	}
	
	/**
	 * Estado WAIT_MOVEMENT: actualizamos el mundo con el radar y el gps dados
	 * anteriormente, y pintamos en un JFrame el mundo. Luego esperamos al Movement,
	 * y avanzamos a SEND_INFO. En caso contrario, vamos a FINISH.
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateWaitMovement(){
		if(DEBUG)
			System.out.println("AgentWorld status: WAIT_MOVEMENT");

		// Actualizamos el mundo visualmente
		m.updateRadarImg(updateWorld, coordX, coordY);
		m.updateGPSImg(coordX, coordY);

		//jframe.setSize(WIDTH, HEIGHT);

		m.repaint();

		String confirmation = this.receiveMessage();

		JsonObject confirmationObject = Json.parse(confirmation).asObject();

		String confirmationResult = confirmationObject.get("sendWorld").toString();

		if(confirmationResult.contains("request"))
			this.state = SEND_INFO;
		else
			this.state = FINISH;
	}
	
	/**
	 * Estado SEND_INFO: le mandamos nuestros datos al Movement y volvemos al IDLE.
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateSendInfo(){
		if(DEBUG)
			System.out.println("AgentWorld status: SEND_INFO");

		this.sendWorld();

		this.state = IDLE;
	}
	
	/**
	 * Estado FINISH: avisamos al gps que vamos a morir, y nos preparamos para ello.
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
	private void stateFinish(){
		if(DEBUG)
			System.out.println("AgentWorld status: FINISH");

		if(this.response.contains("finalize")) {
			JsonObject confirmationMessage = new JsonObject();

			confirmationMessage.add("world", "finish");

			this.sendMessage(gpsName, confirmationMessage.toString());
		}

		this.finish = true;
	}
	
	/**
	  * Método de ejecución del agente World.
	  * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	  */
	@Override
	public void execute() {
		
		if(DEBUG)
			System.out.println("AgentWorld execution");
		
		while(!finish) {
			switch(state) {   
				case IDLE:
					
					stateIdle();
					
					break;
				case WAIT_RADAR:
					
					stateWaitRadar();
					
					break;                  
				case WARN_RADAR:
					
					stateWarnRadar();

					break;                
				case WAIT_MOVEMENT:
					
					stateWaitMovement();
					
					break;
                                        
				case SEND_INFO:
					
					stateSendInfo();
					
					break;
           
				case FINISH:
					
					stateFinish();
					
					break;
			}
		}
	}
	
	/**
	  * Método de finalización del agente Coche.
	  * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	  */
	@Override
	public void finalize() {
		
		if(DEBUG)
			System.out.println("AgentWorld has just finished");
		
		super.finalize();
	}

	/**
	 * Actualiza el mundo de con los datos obtenidos del radar.
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 * @param resultMessage mensaje del radar
	 * @return true si se ha actualizado.
	 */
    private boolean updateWorld(String resultMessage) {
		if(DEBUG)
			System.out.println("AgentWorld updating world");
		
		JsonObject parse = Json.parse(resultMessage).asObject();
		
		if(resultMessage.contains("radar")) {
			int pos = 0;
			for (JsonValue j : parse.get("radar").asArray()){
				local_world[pos] = j.asInt();
				pos++;
			}
			
			//Ahora lo pasamos al mapa
			int posi = 0;
			int posx = 0, posy = 0;
			
			for(int i = coordY-2; i <= coordY+2; i++) {
				for(int j = coordX-2; j <= coordX+2; j++) {
					if(map_world[i][j] < 10) {
						map_world[i][j] = local_world[posi];
                        updateWorld[posy][posx] = local_world[posi];
                    }
                    else
                        updateWorld[posy][posx] = map_world[i][j];
                    
					posi++;
					posx++;
				}
				
				posx = 0;
				posy++;
			}
			
		}
		else {
			coordX = parse.get("x").asInt();
			coordY = parse.get("y").asInt();

			cont = gpsObject.get("cont").asInt();
			
			if(map_world[coordY][coordX] != 2)
				map_world[coordY][coordX] = cont;
            
		}
        
		return true;
    }

	/**
	 * Empaqueta el array del mundo y las coordenadas del gps, y se lo manda a Movement.
	 * @author Bryan Moreno Picamán and Aarón Rodríguez Bueno and Hugo Maldonado
	 */
    private void sendWorld() {
		
		responseObject = new JsonObject(); //Lo limpiamos

		//Empaquetamos el array entero en un JSon y se lo enviamos
		JsonArray vector = new JsonArray();
		
		this.responseObject.add("x",coordX);
		this.responseObject.add("y",coordY);
		
		//Para el world entero
		/*for(int i=0; i<HEIGHT; i++) {
			for(int j=0; j<WIDTH; j++) {
				vector.add(map_world[i][j]);
			}
		}*/
		
		//Para el 5x5
		for(int i=coordY-2; i<=coordY+2; i++) {
			for(int j=coordX-2; j<=coordX+2; j++) {
				vector.add(map_world[i][j]);
			}
		}
		
		responseObject.add("world",vector);
		
		String messageMovement = responseObject.toString();
		
		this.sendMessage(movementName,messageMovement);
    }
}