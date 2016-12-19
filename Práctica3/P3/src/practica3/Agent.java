/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica3;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;

/**
 *
 * @author JoseDavid
 */
public class Agent extends SingleAgent {
    
    protected String convIDServer;
    protected String convIDAgents;
    
    /**
     * Constructor
	 * @author Jose David
     * @param name Nombre del agente
     * @throws Exception 
     */
    public Agent(AgentID name) throws Exception {
		
        super(name);   
    }
    
    /**
     * Obtiene el nombre de un agente
	 * @author Jose David
     * @return Nombre del agente
     */
    
    @Override
    public String getName() {
		
        return this.getAid().getLocalName();
    }

    /**
     * Envia un mensaje a otro agente
	 * @author Jose David
     * @param messageSend Datos del mensaje
     */
    public void sendMessage(toSend messageSend) {
        ACLMessage outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID(messageSend.getReceiver()));
        outbox.setPerformative(messageSend.getPerformative());
        String content = messageSend.getContent().toString();
        outbox.setContent(content);
        this.send(outbox);
        System.out.println(getName()+" -----> "+messageSend.getReceiver()+
                            " (" + outbox.getPerformative() + "): "+content);
    }
    
    /**
     * Recibir mensaje de otro agente
     * @author Jose David
     * @return Contenido del mensaje
     */
    
    public toReceive receiveMessage() {
        try {
            ACLMessage inbox = this.receiveACLMessage();
            JsonObject content = Json.parse(inbox.getContent()).asObject();
                    
            toReceive messageReceived = 
                    new toReceive(
                            inbox.getSender().getLocalName(),
                            inbox.getPerformativeInt(),
                            content);
            
            System.out.println(
                    getName()+" <----- "+inbox.getSender().getLocalName()+
                    " (" + inbox.getPerformative() + "): "+inbox.getContent());
            return messageReceived;
        } catch (InterruptedException ex) {
            return null;
        }
    }
    
    /**
     * @author Jose David
     * Sends an Inform message to an agent
     * @param agentName Name of receiver agent 
     * @param result param to send
    */
    public void sendInform(String agentName, String result) {
        JsonObject obj = Json.object().add("result", result); 
        toSend messageToSend = new toSend(agentName, ACLMessage.INFORM, obj);
        sendMessage(messageToSend);      
    }
    
    
    

}