
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 *
 * @author Aarón Rodríguez, Hugo Maldonado & Bryan Moreno Picamán
 */
public class AgentMovement extends Agent{
    private static final int IDLE = 0;
    private static final int WAIT_SCANNER = 1;
    private static final int WAIT_WORLD = 2;
    private static final int EXECUTE = 3;
    private static final int FINISH = 4;
	
	private static final boolean DEBUG = true;
    
    private int state;
    private boolean finish;
    private int x,y;
    private static final int WIDTH = 504, HEIGHT = 504;
    private final float [][] map_scanner = new float [5][5];
    private final int [][] map_world = new int [WIDTH][HEIGHT];
	
	private int gpsCont;
    
    private JsonObject responseObject;
    
    private final AgentID worldName;
    private final AgentID scannerName;
    private final AgentID carName;
    
    String message;
    
    /**
     * Constructor 
     * 
     * @param movementName
     * @param worldName
     * @param scannerName
     * @param carName
     * @throws java.lang.Exception en la creación del agente.
     * 
     * @author Aarón Rodríguez Bueno
     */
    public AgentMovement(AgentID movementName, AgentID worldName, AgentID scannerName, AgentID carName) throws Exception {
            super(movementName);
			
            this.scannerName= scannerName;
            this.worldName = worldName;
            this.carName = carName;
    }
    
    /**
     * MÃ©todo de inicialización del Agent Movement
     * 
     * @author Aarón Rodríguez Bueno
     */
    @Override
    public void init() {
		
        state = IDLE;
        finish = false;
		
		this.gpsCont = -1;
        
        this.responseObject = new JsonObject();
        
        /*for (float [] i : map_scanner){
            for(float j : i){
                j = 0.0f;
            }
        }
        for (float [] i : map_world){
            for(float j : i){
                j = -1;
            }
        }
        x = y = -1;
        */
		
		System.out.println("AgentMovement has just started");
    }
    
    
    /**
    * MÃ©todo de ejecución del agente Scanner
    * 
    * @author Aarón Rodríguez Bueno & Bryan Moreno Picamán
    */  
    @Override
    public void execute() {
		
		System.out.println("AgentMovement execution");
		
        while(!finish) {
            switch(state) {
                case IDLE:  //Esperando al proceed del Agent Car
                    
					if(DEBUG)
						System.out.println("AgentMovement status: IDLE");
                    
                    message = this.receiveMessage();
					
					if(message.contains("finalize"))
						this.state = FINISH;
					else {
						JsonObject response = Json.parse(message).asObject();
						
						this.gpsCont = response.get("calculate").asInt();
					
                        //Le pedimos los datos al Agent Scanner
                        responseObject = new JsonObject(); //Lo limpiamos
                        responseObject.add("sendScanner", "request");
						
                        message = responseObject.toString();
						
                        sendMessage(scannerName, message);
                        
                        state = WAIT_SCANNER;
                    }               
                    break;
                case WAIT_SCANNER:  //Esperando los datos del Agent Scanner
					
					if(DEBUG)
						System.out.println("AgentMovement status: WAIT_SCANNER");

                    message = this.receiveMessage();
					
					//System.out.println("SCANNER MOVEMENT: " + message);
					
					if(this.gpsCont != -1) {
						this.responseObject = Json.parse(message).asObject();

						//Actualizamos nuestro map_scanner
						int posx = 0;
						int posy = 0;
						
						for(JsonValue j : responseObject.get("scanner").asArray()) {
							map_scanner[posy][posx] = j.asFloat();
							posx++;
							if(posx%5 == 0) {
								posx = 0;
								posy++;
							}
						}
					}
                    
                    //Pedimos al Agent World los datos
                    responseObject = new JsonObject(); //Lo limpiamos
					
                    responseObject.add("sendWorld", "request");
					
                    message = responseObject.toString();
					
                    sendMessage(worldName, message);
                    
                    state = WAIT_WORLD;
                    break;
                case WAIT_WORLD:    //Esperando los datos del Agent World
                    
					if(DEBUG)
						System.out.println("AgentMovement status: WAIT_WORLD");
					
					message = this.receiveMessage();
					
					if(this.gpsCont != -1) {
						this.responseObject = Json.parse(message).asObject();

						//Actualizamos nuestro map_world
						this.x = responseObject.get("x").asInt();
						this.y = responseObject.get("y").asInt();
						
						int posix = 0, posiy = 0;
						
						for(JsonValue j : responseObject.get("world").asArray()) {
							map_world[posiy][posix] = j.asInt();
							posix++;
							if(posix%WIDTH == 0) {
								posix = 0;
								posiy++;
							}
						}
						
						state = EXECUTE;
					}
					else {
						responseObject = new JsonObject();
						
						responseObject.add("mov", "NO");
						
						sendMessage(carName, responseObject.toString());

						state = IDLE;
					}
                    
                    break;
                case EXECUTE:   //Calcula la decisión y se la manda al Agent Car
					
					if(DEBUG)
						System.out.println("AgentMovement status: EXECUTE");
                    
                    responseObject = new JsonObject(); //Lo limpiamos
					
					if(map_world[y][x] == 2) {	//Estamos en el goal
						responseObject.add("mov", "logout");
						System.out.println("Hemos encontrado la solución " + map_world[y][x] + " En las coordenadas " + y + "," + x);
					}
					else {
						
						// MEJORA!! si en el radio del mundo que vemos está el objetivo, hacer el camino hasta el objetivo.
						/* if() {
						
						}
						*/
						
						//Una vez tenemos todos los datos, calculamos el mejor movimiento
						//HEURISTICA: elegimos de la siguiente manera
						//	-Si está el goal en una posición inmediata, vamos directamente
						//	-Si no es pared, elegimos por la zona que llevemos más tiempo sin pasar
						//	(si nunca se ha pasado se toma como la zona más lejana)
						//		*Si hay varias zonas con la misma prioridad (es decir, 
						//			hay varias zonas por la que no hemos pasado), 
						//			priorizamos el que esté más cerca del goal

						int minWorld = map_world[y][x];
						int newX = x;
						int newY = y;
						boolean goalFound = false;
                        float minScanner=0.0f;
						
						/*System.out.println("minScanner: " + minScanner);
						System.out.println("map_scanner[" + y + "][" + x + "]: " + map_scanner[y][x]);
						System.out.println("minWorld: " + minWorld);
						System.out.println("map_world[" + y + "][" + x + "]: " + map_world[y][x]);
						
						
						System.out.println("\nMAP WORLD:");
						for(int i=y-2; i<=y+2; i++) {
							for(int j=x-2; j<=x+2; j++) {
								System.out.print(map_world[i][j] + "\t");
							}
							System.out.println("");
						}
						
						System.out.println("\nMAP SCANNER:");
						for(int i=y-2; i<=y+2; i++) {
							for(int j=x-2; j<=x+2; j++) {
								System.out.print(map_scanner[i][j] + "\t");
							}
							System.out.println("");
						}
						
						System.out.println("");*/
			
						
						for(int i=y-1; i<=y+1 && !goalFound; i++) {
							for(int j=x-1; j<=x+1 && !goalFound; j++) {
								if(map_world[i][j] == 2) {
									goalFound = true;
									newX = j;
									newY = i;
								}
								else if(map_world[i][j] != 1) {
									/*if(map_scanner[i][j] < minScanner) {
										minScanner = map_scanner[i][j];
										newX = j;
										newY = i;
										System.out.println("\nminScanner: " + minScanner);
										System.out.println("newX: " + newX);
										System.out.println("newY: " + newY + "\n");
									}*/
									if(map_world[i][j] < minWorld || (minWorld == map_world[i][j] && map_scanner[newY-y+2][newX-x+2] > map_scanner[i-y+2][j-x+2])) {
										
										minWorld = map_world[i][j];
										newX = j;
										newY = i;
                                        minScanner=map_scanner[i-y+2][j-x+2];
										/*System.out.println("\nless: " + minWorld);
										System.out.println("newX: " + newX);
										System.out.println("newY: " + newY + "\n");*/
									}
								}
							}
						}
                        
                        /*Muestra radar parcial y scanner parcial con el resultado elegido de la heuristica */
                        /*System.out.println("MATRIZ MAPSCANNER");
                        System.out.println("Se ha elegido el valor: "+minScanner);
                        for(int i=1;i<4;i++)
                                System.out.println("["+map_scanner[i][1]+"]"+"["+map_scanner[i][2]+"]"+"["+map_scanner[i][3]+"]");
						
                        for(int i=y-1; i<=y+1; i++) {
                                System.out.println("["+map_world[i][x-1]+"]"+"["+map_world[i][x]+"]"+"["+map_world[i][x+1]+"]");
                        }
                        */
						String movement;
						
						if(newX == x-1) {		//Se mueve hacia el Oeste
							if(newY == y-1)	//Se mueve hacia Norte
								movement = "moveNW";
							else if(newY == y)
								movement = "moveW";
							else
								movement = "moveSW";
						}
						else if(newX == x) {
							if(newY == y-1)
								movement = "moveN";
							else
								movement = "moveS";
						}
						else {
							if(newY == y-1)
								movement = "moveNE";
							else if(newY == y)
								movement = "moveE";
							else
								movement = "moveSE";
						}
						
						System.out.println("\nMovimiento " + gpsCont + ": " + movement + "\n");
						
						responseObject.add("mov", movement);

						//Comprobamos qué posición respecto a nuestra posición es la que hemos elegido
						/*if(newX == x-1) {		//Se mueve hacia el Oeste
							if(newY == y-1)	//Se mueve hacia Norte
								responseObject.add("mov", "moveNW");
							else if(newY == y)
								responseObject.add("mov", "moveW");
							else
								responseObject.add("mov", "moveSW");
						}
						else if(newX == x) {
							if(newY == y-1)
								responseObject.add("mov", "moveN");
							else
								responseObject.add("mov", "moveS");
						}
						else{
							if(newY == y-1)
								responseObject.add("mov", "moveNE");
							else if(newY == y)
								responseObject.add("mov", "moveE");
							else
								responseObject.add("mov", "moveSE");
						}*/

						// PRUEBA PARA EL PRIMER MAPA
						//responseObject.add("mov", "moveSW");
					}
						
                    message = responseObject.toString();
                    sendMessage(carName, message);
                    
                    state = IDLE;
					
                    break;
                case FINISH:    //Matamos al agente
					
					if(DEBUG)
						System.out.println("AgentMovement status: FINISH");
					
					if(this.message.contains("finalize")) {
						JsonObject confirmationMessage = new JsonObject();
						
						confirmationMessage.add("movement", "finish");

						this.sendMessage(carName, confirmationMessage.toString());
					}
					
                    this.finish = true;
					
                    break;
            }
        }
    }
    
    /**
    * Método de finalización del Agent Movement
     * 
     * @author Aarón Rodríguez Bueno
    */
    @Override
    public void finalize() {
		
		System.out.println("AgentMovement has just finished");
		
        super.finalize();
    }
}