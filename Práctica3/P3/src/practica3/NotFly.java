package practica3;

import java.util.List;

/**
 * Clase que define los agentes sin capacidad de volar del sistema.
 * 
 * @author Bryan Moreno Picamán and Hugo Maldonado
 */
public class NotFly extends TypeAgent{
	
    /**
     * Constructor por defecto para los agentes de tipo NotFly
	 * 
     * @author Bryan Moreno and Hugo Maldonado
	 * @param name El nombre del agente
     */
    public NotFly(String name) {
		
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
		
		for(int x=0; x<mapWorld.length; x++) {
			for(int y=0; y<mapWorld.length; y++) {
				if(mapWorld[y][x] == 1 || mapWorld[y][x] == 2 || mapWorld[y][x] == -1) {
					nodes[x][y] = new Node(x, y, false);
				}
				else {
					nodes[x][y] = new Node(x, y, true);
				}
			}
		}
		
		Map myMap = new Map(mapWorld.length, mapWorld.length, nodes);
		
		//myMap.drawMap();
		
        System.out.println(this.getName() + " Calculating Path to Goal");
		
		List<Node> path = myMap.findPath(posX, posY, goalX, goalY);
		
		/*System.out.println("CAMINO");
		for (Node n : path){
			System.out.print("("+n.getyPosition()+","+n.getxPosition()+")"+"("+n.isWalkable()+")");
			
			Node n2 = nodes[n.getyPosition()][n.getxPosition()];
			
			System.out.print("\t("+n2.getyPosition()+","+n2.getxPosition()+")"+"("+n2.isWalkable()+")");
			
			System.out.println("");
		}*/
        
		return path;
    }
}