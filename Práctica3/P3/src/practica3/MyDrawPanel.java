
package practica3;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Clase para ir dibujando la traza que va haciendo el coche.
 * 
 * @author Bryan Moreno Picamán and Hugo Maldonado
 */
public class MyDrawPanel extends javax.swing.JPanel {
    
    BufferedImage image = new BufferedImage(510, 510, BufferedImage.TYPE_INT_RGB);

	/**
	 * Constructor
	 * 
	 * @param mapWorld el Mundo que se va a ir dibujando
	 * 
	 * @author Bryan Moreno Picamán and Hugo Maldonado
	 */
    public MyDrawPanel(int [][] mapWorld) {
        initComponents();
		
		for(int y = 0; y < mapWorld.length; ++y)
            for(int x = 0; x < mapWorld.length; ++x)
				paintCoord(mapWorld, x, y);
    }
    
	/**
	 * Actualiza parcialmente la imagen de la representación del mundo.
	 * 
	 * @param updateWorld Matriz parcial que se le pasa para actualizar la imagen.
	 * @param coordX coordenada x de la posición actual.
	 * @param coordY coordenada y de la posición actual.
	 * 
	 * @author Hugo Maldonado and Bryan Moreno
	 */
    public void updateMap(int[][] updateWorld, int coordX, int coordY) {
        
        for(int y = 0; y < updateWorld.length; y++)
            for(int x = 0; x < updateWorld.length; x++)
                paintCoord(updateWorld, x+coordX-5, y+coordY-5);
    }
    
	/**
	 * Actualiza la posición actual del agente en el mundo
	 * 
	 * @param coordX Coordenada x de la posición.
	 * @param coordY Coordenada y de la posición.
	 * @param agent El identificador del agente para diferenciar el color con el que pintamos la traza
	 * 
	 * @author Hugo Maldonado and Bryan Moreno
	 */
    public void updatePos(int coordX, int coordY, int agent) {
        
		Color color;
		
        switch(agent) {
			case 0:
				color = Color.GREEN;
			break;
			case 1:
				color = Color.BLUE;
			break;
			case 2:
				color = Color.ORANGE;
			break;
			case 3:
				color = Color.PINK;
			break;
			default:
				color = Color.WHITE;
				break;
		}
        
        image.setRGB(coordX, coordY, color.getRGB());
    }
	
	/**
	 * Método para pintar un sólo pixel de distintos colores según que represente
	 * 
	 * @param mapWorld
	 * @param x
	 * @param y 
	 * 
	 * @author Hugo Maldonado and Bryan Moreno
	 */
	private void paintCoord(int [][] mapWorld, int x, int y) {
		Color color;
		
		switch(mapWorld[y][x]) {
			case -1:
				color = Color.GRAY;

				image.setRGB(x, y, color.getRGB());
			break;
			case 0:
				color = Color.WHITE;

				image.setRGB(x, y, color.getRGB());
			break;
			case 1: case 2:
				color = Color.BLACK;

				image.setRGB(x, y, color.getRGB());
			break;
			case 3:
				color = Color.RED;

				image.setRGB(x, y, color.getRGB());
			break;
		}
	}
    
	/**
	 * Pinta la imagen
	 * @param g Objecto graphics para pintar
	 * 
	 * @author Bryan Moreno
	 */
	@Override
    public void paint(Graphics g) {
        
        g.drawImage(image, 0, 0,image.getHeight(),image.getWidth(), null);
    }

    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
