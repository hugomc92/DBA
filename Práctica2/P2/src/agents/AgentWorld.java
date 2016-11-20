
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
 * @author Bryan Moreno Picamán, Aarón Rodríguez Bueno & Hugo Maldonado.
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
	 * @param worldName El nombre de agente para crearlo.
	 * @param radarName
	 * @param gpsName
	 * @param movementName
     * @param map
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
	  * Método de ejecución del agente World.
	  */
	@Override
	public void execute() {
		
		System.out.println("AgentWorld execution");
		
		while(!finish) {
			switch(state) {   
				case IDLE:
					
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
					
					break;
				case WAIT_RADAR:
					
					if(DEBUG)
						System.out.println("AgentWorld status: WAIT_RADAR");
					
					String responseRadar = this.receiveMessage();
					
					this.updateWorld(responseRadar);
					
					this.state = WARN_RADAR;
					
					break;                  
				case WARN_RADAR:
					
					if(DEBUG)
						System.out.println("AgentWorld status: WARN_RADAR");
					
					this.commandObject = new JsonObject();

					this.commandObject.add("radar","ok");

					this.sendMessage(this.radarName, commandObject.toString());
					
					this.state = WAIT_MOVEMENT;

					break;                
				case WAIT_MOVEMENT:
					
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
					
					break;
                                        
				case SEND_INFO:
					
					if(DEBUG)
						System.out.println("AgentWorld status: SEND_INFO");
					
					this.sendWorld();
					
					this.state = IDLE;
					
					break;
           
				case FINISH:
					
					if(DEBUG)
						System.out.println("AgentWorld status: FINISH");
					
					if(this.response.contains("finalize")) {
						JsonObject confirmationMessage = new JsonObject();
						
						confirmationMessage.add("world", "finish");

						this.sendMessage(gpsName, confirmationMessage.toString());
					}
					
					this.finish = true;
					
					break;
			}
		}
	}
	
	/**
	  * Método de finalización del agente Coche.
	  */
	@Override
	public void finalize() {
		
		System.out.println("AgentWorld has just finished");
		
		super.finalize();
	}

    private boolean updateWorld(String resultMessage) {
		
        //System.out.println("AgentWorld updating world");
		
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
			
            //m.updateRadarImg(updateWorld, coordX, coordY);
		}
		else {
			coordX = parse.get("x").asInt();
			coordY = parse.get("y").asInt();

			cont = gpsObject.get("cont").asInt();
			
			if(map_world[coordY][coordX] != 2)
				map_world[coordY][coordX] = cont;
            
            //m.updateGPSImg(coordX, coordY);
		}
        
        //m.calculateImg(map_world, 500, 500, coordX, coordY);
        
        /*jframe.setSize(510, 510);
        
        m.repaint();*/
        
		return true;
    }

    private void sendWorld() {
		
		responseObject = new JsonObject(); //Lo limpiamos

		//Empaquetamos el array entero en un JSon y se lo enviamos
		JsonArray vector = new JsonArray();
		
		this.responseObject.add("x",coordX);
		this.responseObject.add("y",coordY);
		
		/*for(int i=0; i<HEIGHT; i++) {
			for(int j=0; j<WIDTH; j++) {
				vector.add(map_world[i][j]);
			}
		}*/
		
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