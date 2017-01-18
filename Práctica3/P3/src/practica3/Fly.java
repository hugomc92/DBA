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
	 * @param name El nombre del agente
     */
    public Fly(String name) {
		
		this.name = name;
    }
    
    /**
     * Calcula el camino hacia la posición que se le indica
	 * 
     * @author Bryan Moreno and Hugo Maldonado
     */
    @Override
    List<Node> calculatePath(int posX, int posY, int goalX, int goalY) {
		nodes = new Node[this.mapWorld.length][this.mapWorld.length];
		
		/*for(int y=0; y<mapWorld.length; y++) {
			for(int x=0; x<mapWorld.length; x++) {
				if(mapWorld[y][x] != 2 && mapWorld[y][x] != -1)
					nodes[y][x] = new Node(y, x, true);
				else {
					//nodes[y][x] = null;
					nodes[y][x] = new Node(y, x, false);
				}
			}
		}*/
		
		for(int x=0; x<mapWorld.length; x++) {
			for(int y=0; y<mapWorld.length; y++) {
				//System.out.println("mapWorld["+y+"]["+x+"]: " + mapWorld[y][x]);
				if(mapWorld[y][x] == 2 || mapWorld[y][x] == -1) {
					//System.out.println("if");
					nodes[x][y] = new Node(x, y, false);
				}
				else {
					//nodes[y][x] = null;
					//System.out.println("else");
					nodes[x][y] = new Node(x, y, true);
				}
			}
		}
		
		Map myMap = new Map(mapWorld.length, mapWorld.length, nodes);
		
        System.out.println(this.getName() + " Calculating Path to Goal");
		
		return myMap.findPath(posX, posY, goalX, goalY);
    }
}