
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
    
    private int state;
    private boolean finish;
    private int x,y;
    private static final int WIDTH = 504, HEIGHT = 504;
    private final float [][] map_scanner = new float [WIDTH][HEIGHT];
    private final int [][] map_world = new int [WIDTH][HEIGHT];
	
	private int gpsCont;
    
    private JsonObject responseObject;
    
    private final AgentID worldName;
    private final AgentID scannerName;
    private final AgentID carName;
    
    String message;
    
    /**
     * Constructor 
     * @param aid El ID de agente para crearlo.
     * 
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
                j = 0;
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
					
					System.out.println("AgentMovement status: WAIT_SCANNER");

                    message = this.receiveMessage();
                    this.responseObject = Json.parse(message).asObject();
                    
                    //Actualizamos nuestro map_scanner
                    int posx = 0;
                    int posy = 0;
                    for (JsonValue j : responseObject.get("scanner").asArray()) {
                        map_scanner[posx][posy] = j.asFloat();
                        posy++;
                        if(posy%WIDTH == 0) {
                            posy = 0;
                            posx++;
                        }
                    }
                    
                    //Pedimos al Agent World los datos
                    System.out.println("Pedimos al Agent World los datos");
                    responseObject = new JsonObject(); //Lo limpiamos
                    responseObject.add("sendWorld", "request");
                    message = responseObject.toString();
                    sendMessage(worldName, message);
                    
                    state = WAIT_WORLD;
                    break;
                case WAIT_WORLD:    //Esperando los datos del Agent World
                    
                    System.out.println("AgentMovement status: WAIT_WORLD");
					
					message = this.receiveMessage();
                    this.responseObject = Json.parse(message).asObject();
                    
                    //Actualizamos nuestro world_scanner
                    this.x = responseObject.get("x").asInt();
                    this.y = responseObject.get("y").asInt();
                    int posix = 0, posiy = 0;
                    for (JsonValue j : responseObject.get("world").asArray()) {
                        map_world[posix][posiy] = j.asInt();
                        posiy++;
                        if(posiy%WIDTH == 0) {
                            posiy = 0;
                            posix++;
                        }
                    }
                    
                    
                    state = EXECUTE;
                    break;
                case EXECUTE:   //Calcula la decisión y se la manda al Agent Car
					
					System.out.println("AgentMovement status: EXECUTE");
                    
                    responseObject = new JsonObject(); //Lo limpiamos
					
					if(map_world[x][y] == 2) {	//Estamos en el goal
						responseObject.add("mov", "logout");
						System.out.println("Hemos encontrado la solución "+map_world[x][y]+" En las coordenadas"+x+y);
					}
					else {
						//Una vez tenemos todos los datos, calculamos el mejor movimiento
						//HEURISTICA: elegimos de la siguiente manera
						//	-Si está el goal en una posición inmediata, vamos directamente
						//	-Si no es pared, elegimos por la zona que llevemos más tiempo sin pasar
						//	(si nunca se ha pasado se toma como la zona más lejana)
						//		*Si hay varias zonas con la misma prioridad (es decir, 
						//			hay varias zonas por la que no hemos pasado), 
						//			priorizamos el que esté más cerca del goal
						float less;
						int newX, newY;
						
						//EN ESTAS TRES LINEAS HAY FALLO!! NO SE CONTEMPLA SI map_world[X-1][X-1] NO SEA 1. NO SE SI ESTÁ SOLUCIONADO AÚN
						
						if(map_scanner[x-1][y-1] != 1) {
							less = map_scanner[x-1][y-1];	
							newX = x-1;
							newY = y-1;
						}
						else if(map_scanner[x][y-1] != 1) {
							less = map_scanner[x][y-1];
							newX = x;
							newY = y-1;
						}
						else if(map_scanner[x-1][y] != 1) {
							less = map_scanner[x-1][y];
							newX = x-1;
							newY = y;
						}
						else {
							less = map_scanner[x][y];
							newX = x;
							newY = y;
						}
						
						boolean goal_found = false;

						for (int i = x-1; i <= x+1 && !goal_found; i++) {
							for (int j = y-1; j <= j+1 && !goal_found; j++) {
								if(i>=0 && j >= 0 && i<WIDTH && j < HEIGHT) {	//Está dentro de la matriz
									if(map_world[i][j] == 2) {	//goal
										goal_found = true;
										newX = i;
										newY = j;
									}
									else if(map_world[i][j] != 1) {	//No es pared, luego es transitable
										if(less < map_world[i][j] ||	//Hace menos que ha pasado por esta posición
											(less == map_world[i][j] &&	//Dos zonas por donde no ha pasado
											 map_scanner[newX][newY] > map_scanner[i][j]	//Este está más cerca del goal
											)
										   ) {

											newX = i;
											newY = j;
											less = map_world[i][j];
										}
									}
								}
							}
						}

						//Comprobamos qué posición respecto a nuestra posición es la que hemos elegido
						if(newX == x-1) {		//Se mueve hacia el Oeste
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
						}

						//SÓLO PARA EL PRIMER MAPA
						//responseObject.add("mov", "moveSW");
					}
                    message = responseObject.toString();
                    sendMessage(carName, message);
                    
                    state = IDLE;
					
                    break;
                case FINISH:    //Matamos al agente
					
					System.out.println("AgentMovement status: FINISH");
					
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