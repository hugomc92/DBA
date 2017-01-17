package practica3;

import java.util.List;

/**
 * Clase abstracta que define las capacidades comunes de los agentes del sistema
 * 
 * @author Bryan Moreno Picamán and Hugo Maldonado
 */
public abstract class TypeAgent {
	
    protected int [][] mapWorld;
    protected Node[][] nodes;
	
     /**
     * Recibe el mapa completo del entorno
     *  
	 * @param mapWorld El mapa del mundo
	 * 
	 * @author Bryan Moreno and Hugo Maldonado
     */
    public void setMap(int [][] mapWorld) {
        
		this.mapWorld = new int [mapWorld.length][mapWorld.length];
        
		for(int y=0; y<mapWorld.length; y++)
			for(int x=0; x<mapWorld.length; x++)
				this.mapWorld[y][x] = mapWorld[y][x];
    }

    abstract List<Node> calculatePath(int posX, int posY, int x,int y);
}