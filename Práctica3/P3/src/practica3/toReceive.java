/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import com.eclipsesource.json.JsonObject;

/**
 *
 * @author JoseDavid
 */
public class toReceive {
     private String sender;
    private int performative;
    private JsonObject content;
    
    public toReceive(String sender, int performative, JsonObject content) {
        this.sender = sender;
        this.performative = performative;
        this.content = content;
    }
    
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public int getPerformative() {
        return performative;
    }
    
    public void setPerformative(int performative) {
        this.performative = performative;
    }

    public JsonObject getContent() {
        return content;
    }

    public void setContent(JsonObject content) {
        this.content = content;
    }  
}
