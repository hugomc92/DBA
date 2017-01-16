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
    public void setMap(int [][] mapWorld){
                this.mapWorld = new int [mapWorld.length][mapWorld.length];
        
		for(int i=0; i<mapWorld.length; i++)
			for(int j=0; j<mapWorld[i].length; j++)
				this.mapWorld[i][j] = mapWorld[i][j];
    }

    abstract  List<Node> calculatePath(int posX, int posY, int x,int y);
}