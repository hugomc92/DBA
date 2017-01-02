package practica3;
import java.util.ArrayList;

/**
 * Clase que define los agentes sin capacidad de volar del sistema.
 * @author Bryan Moreno Picamán
 */
public class NotFly extends TypeAgent{
    /**
     * Constructor por defecto para los agentes de tipo NoFly
     * @author Bryan Moreno 
     */
    public NotFly(){
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Calcula el camino hacia la posición que se le indica
     * @author Bryan Moreno 
     */
    @Override
    ArrayList<ArrayList> calculatePath(int x, int y) {
        System.out.println("Calculating Path to Goal");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
