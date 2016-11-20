
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
	
	private static final boolean DEBUG = false;
    
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
    
	private boolean rotationLeft;
	private boolean rotationChosen;
	private boolean goalSeen;
	private int xPosGoalSeen, yPosGoalSeen;
	
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
		
		this.rotationChosen = false;
		this.goalSeen = false;
        
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
    
    /**Comprueba si el segundo punto está a la izquierda del primero 
	 * (desde el punto de vista de nuestras coordenadas GPS)
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return 
	 */
	boolean isLeft(int x1, int y1, int x2, int y2){
		//casuísticas
		boolean isLeft = false;
		if(x1 == x-1 && y1 == y-1	//Caso 0
			&& x2 == x1+1 && y2 == y1	
		  )
			isLeft = true;
		else if(x1 == x-1 && y1 == y	//Caso 1
			&& x2 == x1 && y2 == y1-1	
		  )
			isLeft = true;
		else if(x1 == x-1 && y1 == y+1	//Caso 2
			&& x2 == x1 && y2 == y1-1	
		  )
			isLeft = true;
		else if(x1 == x && y1 == y+1	//Caso 3
			&& x2 == x1-1 && y2 == y1	
		  )
			isLeft = true;
		else if(x1 == x+1 && y1 == y+1	//Caso 4
			&& x2 == x1-1 && y2 == y1	
		  )
			isLeft = true;
		else if(x1 == x+1 && y1 == y	//Caso 5
			&& x2 == x1 && y2 == y1+1	
		  )
			isLeft = true;
		else if(x1 == x+1 && y1 == y-1	//Caso 6
			&& x2 == x1 && y2 == y1+1	
		  )
			isLeft = true;
		else if(x1 == x && y1 == y-1	//Caso 7
			&& x2 == x1+1 && y2 == y1	
		  )
			isLeft = true;
		return isLeft;
	}
	
	//Comprueba si al nº de steps podría llegar a él (en caso de no obstáculos)
	boolean goalInXSteps(int x, int y, int steps){
		boolean founded = false;
		for (int i = y-steps; i <= y+steps && !founded; i++){
			for (int j = x-steps; j <= x+steps && !founded; j++){
				if((i == y-steps || i == y+steps)||(j == x-steps || j == x+steps)){	//Bordes del 5x5
					if(map_world[i][j] == 2) founded = true;
				}
			}
		}
		return founded;
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
						
						/*int posix = 0, posiy = 0;
						
						for(JsonValue j : responseObject.get("world").asArray()) {
							map_world[posiy][posix] = j.asInt();
							posix++;
							if(posix % WIDTH == 0) {
								posix = 0;
								posiy++;
							}
						}*/
						
						int posix = x-2, posiy = y-2;
						
						for(JsonValue j : responseObject.get("world").asArray()) {
							map_world[posiy][posix] = j.asInt();
							
							if(posix == x+2) {
								posix = x-2;
								posiy++;
							}
							else
								posix++;
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
			
						
						// ESTA HEURÍSTICA LOS RESUELVE TODOS (a excepción de detectar la no solución del 9)
						/*for(int i=y-1; i<=y+1 && !goalFound; i++) {
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
									/*if(map_world[i][j] < minWorld || (minWorld == map_world[i][j] && map_scanner[newY-y+2][newX-x+2] > map_scanner[i-y+2][j-x+2])) {
										
										minWorld = map_world[i][j];
										newX = j;
										newY = i;
                                        minScanner=map_scanner[i-y+2][j-x+2];
										/*System.out.println("\nless: " + minWorld);
										System.out.println("newX: " + newX);
										System.out.println("newY: " + newY + "\n");*/
									/*}
								}
							}
						}*/
                        
                        /*Muestra radar parcial y scanner parcial con el resultado elegido de la heuristica */
                        /*System.out.println("MATRIZ MAPSCANNER");
                        System.out.println("Se ha elegido el valor: "+minScanner);
                        for(int i=1;i<4;i++)
                                System.out.println("["+map_scanner[i][1]+"]"+"["+map_scanner[i][2]+"]"+"["+map_scanner[i][3]+"]");
						
                        for(int i=y-1; i<=y+1; i++) {
                                System.out.println("["+map_world[i][x-1]+"]"+"["+map_world[i][x]+"]"+"["+map_world[i][x+1]+"]");
                        }
                        */
						
						// HEURÍSTICA MEJORA QUE AHORRA LAGUNAS
						// Sacamos la posición óptima según el scanner solamente
						float minScanner = map_scanner[2][2];
						int minPosXScanner = 2;
						int minPosYScanner = 2;
						
						/*float minScannerSecond = map_scanner[2][2];
						int minPosXScannerSecond = 2;
						int minPosYScannerSecond = 2;*/
						
						//Buscamos la posición del mejor scanner
						for(int i=1; i<4; i++) {
							for(int j=1; j<4; j++) {
								if(map_scanner[i][j] < minScanner) {
									/*minScannerSecond = minScanner;
									minPosXScannerSecond = minPosXScanner;
									minPosYScannerSecond = minPosYScanner;*/
									
									minScanner = map_scanner[i][j];
									
									minPosXScanner = j;
									minPosYScanner = i;
								}
								/*else if(map_scanner[i][j] < minScannerSecond) {
									minScannerSecond = map_scanner[i][j];
									minPosXScannerSecond = j;
									minPosYScannerSecond = i;
								}*/
							}
						}
						
						
						//Buscamos el segundo mejor scanner, que será seguro anexo al mejor
						/*float minScannerSecond = map_scanner[2][2];
						int minPosXScannerSecond = 2;
						int minPosYScannerSecond = 2;
						
						for(int i=1; i<4; i++) {
							for(int j=1; j<4; j++) {
								if(map_scanner[i][j] < minScannerSecond && !(minPosXScannerSecond == minPosXScanner && minPosYScannerSecond == minPosYScanner)) {
									minScannerSecond = map_scanner[i][j];
									minPosXScannerSecond = j;
									minPosYScannerSecond = i;
								}
							}
						}	*/
						
						
						
						int movimiento = -1;
						
						System.out.println("Mejor scanner: [" + minPosYScanner +"][" + minPosXScanner + "]");
						//System.out.println("Segundo mejor scanner: [" + minPosYScannerSecond +"][" + minPosXScannerSecond + "]");
						
						if(minPosYScanner == 1 && minPosXScanner == 1)
							movimiento = 0;
						else if(minPosYScanner == 1 && minPosXScanner == 2)
							movimiento = 1;
						else if(minPosYScanner == 1 && minPosXScanner == 3)
							movimiento = 2;
						else if(minPosYScanner == 2 && minPosXScanner == 3)
							movimiento = 3;
						else if(minPosYScanner == 3 && minPosXScanner == 3)
							movimiento = 4;
						else if(minPosYScanner == 3 && minPosXScanner == 2)
							movimiento = 5;
						else if(minPosYScanner == 3 && minPosXScanner == 1)
							movimiento = 6;
						else if(minPosYScanner == 2 && minPosXScanner == 1)
							movimiento = 7;
						
						newX = x + minPosXScanner - 2;
						newY = y + minPosYScanner - 2;
						
										
						/*rotationLeft = false;
						if(movimiento == 0 || movimiento == 1 || movimiento == 2)
							rotationLeft = true;*/
						
						//En la misma pared, no se cambia de rotación 
						/*if(map_world[newY][newX] == 1){	//Estamos contra una pared
							if(!rotationChosen){	//Elegimos rotación
								rotationLeft = isLeft(minPosXScanner, minPosYScanner, minPosXScannerSecond, minPosYScannerSecond);
								rotationChosen = true;
							}
						}
						else{	//Hemos salido de la pared, por lo que liberamos rotación
							rotationChosen = false;
						}*/
						
						while(map_world[newY][newX] == 1 || map_world[newY][newX] > 10) {
							//if(rotationLeft){
								/*movimiento--;
								
								if(movimiento < 0)
									movimiento = 7;
							/*}
							else*/
								movimiento=(movimiento+1)%8;					
							
							switch(movimiento) {
								case 0:
									newX = x - 1;
									newY = y - 1;
								break;
								case 1:
									newX = x;
									newY = y - 1;
								break;
								case 2:
									newX = x + 1;
									newY = y - 1;
								break;
								case 3:
									newX = x + 1;
									newY = y;
								break;
								case 4:
									newX = x + 1;
									newY = y + 1;
								break;
								case 5:
									newX = x;
									newY = y + 1;
								break;
								case 6:
									newX = x - 1;
									newY = y + 1;
								break;
								case 7:
									newX = x - 1;
									newY = y;
								break;
							}
						}
						
						System.out.println("movimiento: " + movimiento);
						
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