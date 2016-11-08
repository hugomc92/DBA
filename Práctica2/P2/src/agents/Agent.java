
package agents;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
/**
 *Define una clase abstracta para un agente.
 * @author Jose David & Hugo Maldonado.
 */
public abstract class Agent extends SingleAgent {
    
     /**
     * Constructor
     * @param name Nombre del agente
     * @throws Exception 
     */
    public Agent(String name) throws Exception {
        super(new AgentID(name));        
    }
    
    /**
     * Obtiene el nombre de un agente
     * @return Nombre del agente
     */
    
    @Override
    public String getName() {
        return this.getAid().getLocalName();
    }

    /**
     * Envia un mensaje a otro agente
     * @param sendTo Nombre agente que recibira el mensaje
     * @param content Contenido del mensaje
     */
    public void sendMessage(String sendTo, String content) {
        ACLMessage outbox = new ACLMessage();
        outbox.setSender(this.getAid());
        outbox.setReceiver(new AgentID(sendTo));
        outbox.setContent(content);
        this.send(outbox);
        System.out.println(getName() + " ---> " + sendTo +" : " + content);
    }
    
    /**
     * Recibir mensaje de otro agente
     * @return Contenido del mensaje
     */
    public String receiveMessage() {
        try {
            ACLMessage inbox = this.receiveACLMessage();
            System.out.println(getName()+" <--- " + inbox.getSender().getLocalName() + " : " + inbox.getContent());
            return inbox.getContent();
        } catch (InterruptedException ex) {
            return null;
        }
    }
}