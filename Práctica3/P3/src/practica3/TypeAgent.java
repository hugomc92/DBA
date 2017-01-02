package practica3;

import java.util.ArrayList;

/**
 * Clase abstracta que define las capacidades comunes de los agentes del sistema
 * @author Bryan Moreno Picamán
 */
public abstract class TypeAgent {
    public int [][] mapWorld;
    
    /**
     * Constructor por defecto
     * @author Bryan Moreno 
     */
    public TypeAgent(){
        
    }
    
     /**
     * Recibe el mapa completo del entorno
     * @author Bryan Moreno 
     */
    public void setMap(int [][] mapWorl){
        this.mapWorld=mapWorld;
    }

    abstract ArrayList<ArrayList> calculatePath(int x,int y);

}
