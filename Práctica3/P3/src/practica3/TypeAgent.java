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
		
		//drawMap();
    }
	
	public void drawMap() {
		System.out.println("Drawing Map");
        for (int i = 0; i < mapWorld.length; i++) {
                System.out.print(" _"); // boarder of map
        }
        System.out.print("\n");

        for (int j = 0; j < mapWorld.length; j++) {
            System.out.print("|"); // boarder of map
            for (int i = 0; i < mapWorld.length; i++) {
                if (mapWorld[j][i] == 0) {
                    System.out.print("   ");
                } else {
                    System.out.print(" #"); // draw unwakable
                }
            }
            System.out.print("|\n"); // boarder of map
        }

        for (int i = 0; i <= mapWorld.length; i++) {
                System.out.print(" _"); // boarder of map
        }
		
		System.out.println("");
    }

    abstract List<Node> calculatePath(int posX, int posY, int x,int y);
}