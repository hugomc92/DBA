
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
     * @param sendTo Nombre agente que recibira el mensaje
     * @param content Contenido del mensaje
     */
    public void sendMessage(AgentID sendTo, String content) {
		
        ACLMessage outbox = new ACLMessage();
		
        outbox.setSender(this.getAid());
        outbox.setReceiver(sendTo);
        outbox.setContent(content);
		
        this.send(outbox);
    }
    
    /**
     * Recibir mensaje de otro agente
	 * @author Jose David
     * @return Contenido del mensaje
     */
    public String receiveMessage() {
		
        try {
			ACLMessage inbox = this.receiveACLMessage();
			
            return inbox.getContent();
        } catch (InterruptedException ex) {
			System.err.println(ex.getMessage());
			
            return null;
        }
    }
}