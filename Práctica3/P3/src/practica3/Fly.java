package practica3;

import java.util.List;

/**
 * Clase que define los agentes con capacidad de volar del sistema.
 * 
 * @author Bryan Moreno and Hugo Maldonado
 */
public class Fly extends TypeAgent{
    
    /**
     * Constructor por defecto para los agentes de tipo Fly
	 * 
     * @author Bryan Moreno and Hugo Maldonado
     */
    public Fly(){
		
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
				if(mapWorld[y][x] != 2 && mapWorld[y][x] != -1)
					nodes[y][x] = new Node(y, x, true);
				else
					nodes[y][x] = new Node(y, x, false);
			}
		}
		
		Map myMap = new Map(mapWorld.length, mapWorld.length, nodes);
		
        System.out.println("Calculating Path to Goal");
		
		return myMap.findPath(posX, posY, goalX, goalY);
    }
}