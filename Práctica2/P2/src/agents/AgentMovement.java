
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 * Clase que define al agente Movement, el cual decide el movimiento a tomar
 * en base a las percepciones de los demás agentes.
 * @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán
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
    private int x,y,newX,newY;
	private int movimiento;
    private static final int WIDTH = 504, HEIGHT = 504;
    private final float [][] map_scanner = new float [5][5];
    private final int [][] map_world = new int [WIDTH][HEIGHT];
	private float minScanner;
	private int minWorld;
	
	private int gpsCont;
    
    private JsonObject responseObject;
    
    private final AgentID worldName;
    private final AgentID scannerName;
    private final AgentID carName;
    
	private boolean rotationLeft;
	private boolean rotationChosen;
	private boolean goalSeen;
	private int xPosGoalSeen, yPosGoalSeen;
	private boolean canSolve;
	
    private String message, movement;
	
	private int minPosXScanner, minPosYScanner;
    
    /**
     * Constructor 
     * 
     * @param movementName nombre propio del agente
     * @param worldName nombre del agente World (para comunicación)
     * @param scannerName nombre del agente Scanner (para comunicación)
     * @param carName nombre del agente Car (para comunicación)
     * @throws java.lang.Exception en la creación del agente.
     * 
     * @author Aarón Rodríguez Bueno and Bryan Moreno and Hugo Maldonado
     */
    public AgentMovement(AgentID movementName, AgentID worldName, AgentID scannerName, AgentID carName) throws Exception {
            super(movementName);
			
            this.scannerName= scannerName;
            this.worldName = worldName;
            this.carName = carName;
    }
    
    /**
     * Método de inicialización del Agent Movement
     * 
     * @author Aarón Rodríguez Bueno and Bryan Moreno and Hugo Maldonado
     */
    @Override
    public void init() {
		
        state = IDLE;
        finish = false;
		
		this.gpsCont = -1;
        
        this.responseObject = new JsonObject();
		
		this.rotationChosen = false;
		this.goalSeen = false;
		this.canSolve = true;
		
		System.out.println("AgentMovement has just started");
    }
    
    /**Comprueba si el segundo punto está a la izquierda del primero 
	 * (desde el punto de vista de nuestras coordenadas GPS)
	 * @author Aarón Rodríguez Bueno and Bryan Moreno and Hugo Maldonado
	 * @param x1 coordenada x del punto 1
	 * @param y1 coordenada y del punto 1
	 * @param x2 coordenada x del punto 2
	 * @param y2 coordenada y del punto 2
	 * @return true si está a la izquierda, false en caso contrario.
	 */
	private boolean isLeft(int x1, int y1, int x2, int y2){
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
	
	/**Comprueba si al nº de steps podría llegar a él (en caso de no obstáculos)
	 * @author Aarón Rodríguez Bueno and Bryan Moreno and Hugo Maldonado
	 * @param x coordenada x de la posición a comprobar.
	 * @param y coordenada y de la posición a comprobar.
	 * @param steps Número de pasos a los que mirar si está el goal.
	 * @return true si está a ese número de pasos, false en caso contrario.
	 */
	private boolean goalInXSteps(int x, int y, int steps){
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
	 * Estado IDLE: espera confirmación del Car para empezar a trabajar.
	 * Si el mensaje fuera "finalize", se va al estado FINISH.
	 * Si no, se le piden los datos al agente Scanner y pasa a WAIT_SCANNER
	 * @author Aarón Rodríguez Bueno and Bryan Moreno and Hugo Maldonado
	 */
	private void stateIdle(){
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
	}
	
	/**
	 * Estado WAIT_SCANNER: recibimos los datos del Scanner, los guardamos, avisamos
	 * que nos mande sus datos el World y pasamos al estado WAIT_WORLD.
	 * @author Aarón Rodríguez Bueno and Bryan Moreno and Hugo Maldonado
	 */
	private void stateWaitScanner(){
		if(DEBUG)
			System.out.println("AgentMovement status: WAIT_SCANNER");

		message = this.receiveMessage();

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
	}
	
	/**
	 * Estado WAIT_WORLD: esperamos el mensaje del World, y en función de si el Car nos dijo cuando
	 * estábamos en el idle que tocaba refuel, vamos al estado IDLE.
	 * En caso contrario, guardamos sus datos del mundo, coordenadas actuales y 
	 * step en el que estamos, y pasamos a EXECUTE.
	 * @author Aarón Rodríguez Bueno and Bryan Moreno and Hugo Maldonado
	 */
	private void stateWaitWorld(){
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
				if(posix%WIDTH == 0) {
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
	}
	
	
		/**HEURISTICA: elegimos de la siguiente manera
		*	-Si está el goal en una posición inmediata, vamos directamente
		*	-Si no es pared, elegimos por la zona que llevemos más tiempo sin pasar
		*	(si nunca se ha pasado se toma como la zona más lejana)
		*		*Si hay varias zonas con la misma prioridad (es decir, 
		*			hay varias zonas por la que no hemos pasado), 
		*			priorizamos el que esté más cerca del goal
		* @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán 
		*/
	private void firstHeuristic(){
		minWorld = map_world[y][x];
		newX = x;
		newY = y;
		boolean goalFound = false;
		
		for(int i=y-1; i<=y+1 && !goalFound; i++) {
			for(int j=x-1; j<=x+1 && !goalFound; j++) {
				if(map_world[i][j] == 2) {
					goalFound = true;
					newX = j;
					newY = i;
				}
				else if(map_world[i][j] != 1) {
					if(map_world[i][j] < minWorld || (minWorld == map_world[i][j] && map_scanner[newY-y+2][newX-x+2] > map_scanner[i-y+2][j-x+2])) {

						minWorld = map_world[i][j];
						newX = j;
						newY = i;
						minScanner=map_scanner[i-y+2][j-x+2];
					}
				}
			}
		}
	}
	
	/**
	 * Traduce las posiciones de las coordenadas del menor scanner en un "enumerado".
	 * @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán
	 */
	private void positionToMovement(){
		movimiento = -1;

		if(DEBUG){
			System.out.println("Mejor scanner: [" + minPosYScanner +"][" + minPosXScanner + "]");
			//System.out.println("Segundo mejor scanner: [" + minPosYScannerSecond +"][" + minPosXScannerSecond + "]");
		}

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
	}
	
	/**
	 * Calcula el nuevo movimiento al que se pueda acceder en el sentido que se le de.
	 * @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán
	 * @param rotationLeft la dirección a la que se va a girar
	 */
	private void calculateNewMovement(boolean rotationLeft){
		int count = 0;

		while(map_world[newY][newX] == 1 || (map_world[newY][newX] > 10 && count <= 8)) {	//8 porque tiene 8 posibles elecciones
			count++;
			if(rotationLeft){
				movimiento--;
				if(movimiento < 0)
					movimiento = 7;
			}
			else
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

			if(DEBUG)
				System.out.println("movimiento: " + movimiento);

			if(goalSeen && xPosGoalSeen == newX && yPosGoalSeen == newY){	
				movement = "LO";
				canSolve = false;
			}
		}
	}
	
	/**
	 * Heurística que elige en la dirección a la que va a rotar cuando se encuentre
	 * con una pared en el mejor escaner, en función del segundo mejor escaner.
	 * @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán
	 */
	private void changeRotationHeuristic(){
		// Sacamos la posición óptima según el scanner solamente
		minScanner = map_scanner[2][2];
		minPosXScanner = 2;
		minPosYScanner = 2;

		float minScannerSecond = map_scanner[2][2];
		int minPosXScannerSecond = 2;
		int minPosYScannerSecond = 2;

		//Buscamos la posición del primer y segundo mejor scanner
		for(int i=1; i<4; i++) {
			for(int j=1; j<4; j++) {
				if(map_scanner[i][j] < minScanner) {
					minScannerSecond = minScanner;
					minPosXScannerSecond = minPosXScanner;
					minPosYScannerSecond = minPosYScanner;

					minScanner = map_scanner[i][j];

					minPosXScanner = j;
					minPosYScanner = i;
				}
				else if(map_scanner[i][j] < minScannerSecond) {
					minScannerSecond = map_scanner[i][j];
					minPosXScannerSecond = j;
					minPosYScannerSecond = i;
				}
			}
		}


		//Buscamos el segundo mejor scanner, que será seguro anexo al mejor
		minScannerSecond = map_scanner[2][2];
		minPosXScannerSecond = 2;
		minPosYScannerSecond = 2;

		for(int i=1; i<4; i++) {
			for(int j=1; j<4; j++) {
				if(map_scanner[i][j] < minScannerSecond && !(minPosXScannerSecond == minPosXScanner && minPosYScannerSecond == minPosYScanner)) {
					minScannerSecond = map_scanner[i][j];
					minPosXScannerSecond = j;
					minPosYScannerSecond = i;
				}
			}
		}	
		
		movimiento = -1;

		if(DEBUG){
			System.out.println("Mejor scanner: [" + minPosYScanner +"][" + minPosXScanner + "]");
			//System.out.println("Segundo mejor scanner: [" + minPosYScannerSecond +"][" + minPosXScannerSecond + "]");
		}

		positionToMovement();

		//En la misma pared, no se cambia de rotación 
		if(map_world[newY][newX] == 1){	//Estamos contra una pared
			if(!rotationChosen){	//Elegimos rotación
				rotationLeft = isLeft(minPosXScanner, minPosYScanner, minPosXScannerSecond, minPosYScannerSecond);
				rotationChosen = true;
			}
		}
		else{	//Hemos salido de la pared, por lo que liberamos rotación
			rotationChosen = false;
		}

		calculateNewMovement(rotationLeft);
	}
	
	/**
	 * Heurística que sigue el mejor escáner, y cuando se encuentre con una pared,
	 * rotará en la dirección que se le indique
	 * @param left sentido de la rotación. true si es a la izquierda.
	 * @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán
	 */
	private void rotationChosenHeuristic(boolean left){
		// Sacamos la posición óptima según el scanner solamente
		minScanner = map_scanner[2][2];
		minPosXScanner = 2;
		minPosYScanner = 2;

		//Buscamos la posición del primer mejor escaner
		for(int i=1; i<4; i++) {
			for(int j=1; j<4; j++) {
				if(map_scanner[i][j] < minScanner) {

					minScanner = map_scanner[i][j];

					minPosXScanner = j;
					minPosYScanner = i;
				}
			}
		}
		positionToMovement();
		
		calculateNewMovement(left);
	}
	
	/**
	 * Traduce las coordenadas elegidas al comando movimiento que le va a enviar
	 * al Car.
	 * @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán
	 */
	private void changeCoordinatesForMovement() {
		
		if(canSolve) {
			if(DEBUG)
				System.out.println("movimiento: " + movimiento);

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
		}
	}
	
	/**
	 * Estado EXECUTE: calculamos el movimiento en función de los datos obtenidos
	 * y mediante una heurística.
	 * @author Aarón Rodríguez Bueno and Bryan Moreno and Hugo Maldonado
	 */
	private void stateExecute(){
		if(DEBUG)
			System.out.println("AgentMovement status: EXECUTE");

		responseObject = new JsonObject(); //Lo limpiamos

		if(map_world[y][x] == 2) {	//Estamos en el goal
			responseObject.add("mov", "logout");
			if(DEBUG)
				System.out.println("Hemos encontrado la solución " + map_world[y][x] + " En las coordenadas " + y + "," + x);
		}
		else {

			//Para mapas sin resolución
			if(!goalSeen){
				if(goalInXSteps(x,y,2)){	//No se había visto hasta ahora, pero ahora sí lo vemos
					goalSeen = true;
					xPosGoalSeen = x;
					yPosGoalSeen = y;
				}
			}

			//Una vez tenemos todos los datos, calculamos el mejor movimiento
			
			// PRIMERA HEURISTICA
			// ESTA HEURÍSTICA LOS RESUELVE TODOS (a excepción de detectar la no solución del 9)
			//firstHeuristic();

			if(DEBUG){
				System.out.println("minScanner: " + minScanner);
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

				System.out.println("");
				
				/*Muestra radar parcial y scanner parcial con el resultado elegido de la heuristica */
				
				System.out.println("MATRIZ MAPSCANNER");
				System.out.println("Se ha elegido el valor: "+minScanner);
				
				for(int i=1;i<4;i++)
					System.out.println("["+map_scanner[i][1]+"]"+"["+map_scanner[i][2]+"]"+"["+map_scanner[i][3]+"]");
				
				System.out.println("MATRIZ MAPWORLD");
				
				for(int i=y-1; i<=y+1; i++)
					System.out.println("["+map_world[i][x-1]+"]"+"["+map_world[i][x]+"]"+"["+map_world[i][x+1]+"]");
			}

			
			//Heurística rotación a la izquierda
			rotationChosenHeuristic(true);
			
			//Heurística rotación a la derecha
			//rotationChosenHeuristic(false);
			
			//Heurística rotación variable
			//changeRotationHeuristic();

			changeCoordinatesForMovement();

			if(DEBUG)
				System.out.println("\nMovimiento " + gpsCont + ": " + movement + "\n");

			responseObject.add("mov", movement);

			// PRUEBA PARA EL PRIMER MAPA
			//responseObject.add("mov", "moveSW");
		}

		message = responseObject.toString();
		sendMessage(carName, message);

		state = IDLE;
	}
	
	/**
	 * Estado FINISH: le envía un mensaje al Car confirmándole que se va a finalizar.
	 * @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán
	 */
	private void stateFinish(){
		if(DEBUG)
			System.out.println("AgentMovement status: FINISH");

		if(this.message.contains("finalize")) {
			JsonObject confirmationMessage = new JsonObject();

			confirmationMessage.add("movement", "finish");

			this.sendMessage(carName, confirmationMessage.toString());
		}

		this.finish = true;
	}
	
    /**
    * Método de ejecución del agente Movement.
    * 
    * @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán
    */  
    @Override
    public void execute() {
		
		System.out.println("AgentMovement execution");
		
        while(!finish) {
            switch(state) {
                case IDLE:  //Esperando al proceed del Agent Car
                    
					stateIdle();
					
                    break;
                case WAIT_SCANNER:  //Esperando los datos del Agent Scanner
					
					stateWaitScanner();
					
                    break;
                case WAIT_WORLD:    //Esperando los datos del Agent World
                    
					stateWaitWorld();
                    
                    break;
                case EXECUTE:   //Calcula la decisión y se la manda al Agent Car
					
					stateExecute();
					
                    break;
                case FINISH:    //Matamos al agente
					
					stateFinish();
					
                    break;
            }
        }
    }
    
    /**
    * Método de finalización del Agent Movement
     * 
     * @author Aarón Rodríguez and Hugo Maldonado and Bryan Moreno Picamán
    */
    @Override
    public void finalize() {
		
		System.out.println("AgentMovement has just finished");
		
        super.finalize();
    }
}