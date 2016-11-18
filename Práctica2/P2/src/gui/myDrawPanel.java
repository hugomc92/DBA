
package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Clase para ir dibujando la traza que va haciendo el coche.
 * 
 * @author Bryan Moreno Picamán
 */
public class myDrawPanel extends javax.swing.JPanel {
    
    BufferedImage image = new BufferedImage(504, 504, BufferedImage.TYPE_INT_RGB);

    public myDrawPanel() {
        initComponents();
        
        for(int y = 0; y < 504; ++y) {
            for(int x = 0; x < 504; ++x) {
                Color color = Color.GRAY;
                
                image.setRGB(x, y, color.getRGB());
            }
        }
    }
    
    public void calculateImg(int[][] map, int sizex, int sizey, int coordX, int coordY) {
        
        for(int y = 0; y < sizey; ++y) {
            for(int x = 0; x < sizex; ++x) {
                Color color = Color.GRAY;
                
                if(map[y][x] == 1)
                    color = Color.BLACK;
                else if(map[y][x] == 0)
                    color = Color.WHITE;
                else if(map[y][x] >= 10)
                    color = Color.GREEN;
                else if(map[y][x] == 2)
                    color = Color.RED;
                image.setRGB(x, y, color.getRGB());
            }
        }
        
        Color color = Color.YELLOW;
        
        image.setRGB(coordX, coordY, color.getRGB());
    }
    
    public void updateRadarImg(int[][] updateWorld, int coordX, int coordY) {
        
        for(int y = 0; y < 5; y++) {
			for(int x = 0; x < 5; x++) {
                Color color = Color.GRAY;
				int cell = updateWorld[y][x];
                
                if(cell == 1)
                    color = Color.BLACK;
                else if(cell == 0)
                    color = Color.WHITE;
                else if(cell >= 10)
                    color = Color.GREEN;
                else if(cell == 2)
                    color = Color.RED;
                
                image.setRGB(x+coordX-2, y+coordY-2, color.getRGB());
            }
        }
    }
    
    public void updateGPSImg(int coordX, int coordY) {
        
        Color color = Color.YELLOW;
        
        image.setRGB(coordX, coordY, color.getRGB());
    }
    
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
