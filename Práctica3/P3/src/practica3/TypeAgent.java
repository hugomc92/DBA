package practica3;

import java.util.ArrayList;

/**
 * Clase abstracta que define las capacidades comunes de los agentes del sistema
 * 
 * @author Bryan Moreno Picamán and Hugo Maldonado
 */
public abstract class TypeAgent {
    public int [][] mapWorld;
    
    /**
     * Constructor por defecto
	 * 
     * @author Bryan Moreno 
     */
    public TypeAgent(){
        
    }
    
     /**
     * Recibe el mapa completo del entorno
     *  
	 * @param mapWorld El mapa del mundo
	 * 
	 * @author Bryan Moreno and Hugo Maldonado
     */
    public void setMap(int [][] mapWorld){
        
		for(int i=0; i<mapWorld.length; i++)
			for(int j=0; j<mapWorld[i].length; j++)
				this.mapWorld[i][j] = mapWorld[i][j];
    }

    abstract ArrayList<ArrayList> calculatePath(int x,int y);
}