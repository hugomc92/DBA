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
public class toSend {
    
    private String receiver;
    private int performative;
    private JsonObject content;
    
    public toSend(String receiver, int performative, JsonObject content) {
        this.receiver = receiver;
        this.performative = performative;
        this.content = content;
    }
    
    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
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
