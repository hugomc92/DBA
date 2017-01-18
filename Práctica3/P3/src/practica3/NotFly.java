package practica3;

import java.util.List;

/**
 * Clase que define los agentes sin capacidad de volar del sistema.
 * 
 * @author Bryan Moreno Picamán and Hugo Maldonado
 */
public class NotFly extends TypeAgent{
	
    /**
     * Constructor por defecto para los agentes de tipo NoFly
	 * 
     * @author Bryan Moreno 
     */
    public NotFly(){
    }
    
    /**
     * Calcula el camino hacia la posición que se le indica
	 * 
     * @author Bryan Moreno and Hugo Maldonado
     */
    @Override
	List<Node> calculatePath(int posX, int posY, int goalX, int goalY) {
		
		nodes = new Node[this.mapWorld.length][this.mapWorld.length];
		
		for(int y=0; y<mapWorld.length; y++) {
			for(int x=0; x<mapWorld.length; x++) {
				//System.out.println("mapWorld["+y+"]["+x+"]: " + mapWorld[y][x]);
				if(mapWorld[y][x] == 1 || mapWorld[y][x] == 2 || mapWorld[y][x] == -1) {
					//System.out.println("if");
					nodes[y][x] = new Node(y, x, false);
				}
				else {
					//nodes[y][x] = null;
					//System.out.println("else");
					nodes[y][x] = new Node(y, x, true);
				}
			}
		}
		
		Map myMap = new Map(mapWorld.length, mapWorld.length, nodes);
		
		myMap.drawMap();
		
        System.out.println("Calculating Path to Goal");
        
		return myMap.findPath(posX, posY, goalX, goalY);
    }
}