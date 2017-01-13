package practica3;

import es.upv.dsic.gti_ia.core.ACLMessage;
import es.upv.dsic.gti_ia.core.AgentID;
import es.upv.dsic.gti_ia.core.SingleAgent;
import java.util.UUID;

/**
 * Clase que define una clase abstracta para un agente.
 * 
 * @author JoseDavid and Hugo Maldonado
 */
public class Agent extends SingleAgent {
	
	private static final boolean DEBUG = true;
    
    /**
     * Constructor
     * @param aid ID del agente
     * @throws Exception 
     * 
     * @author Jose David and Hugo Maldonado
     */
    public Agent(AgentID aid) throws Exception {
        super(aid);   
    }

    /**
     * Envia un mensaje a otro agente
	 * 
	 * @param receiver El agente que va a recivir el mensaje
	 * @param performative La performativa usada para la transmisión del mensaje
	 * @param replyWidth El id al que se tiene que responder
	 * @param conversationId El Id de la conversación
	 * @param message El mensaje a enviar
	 * 
	 * @author Hugo Maldonado
     */
    public void sendMessage(AgentID receiver, int performative, String replyWidth, String conversationId, String message) {
        System.out.println("EN SEND MESSAGE");
		ACLMessage outbox = new ACLMessage();
        
		outbox.setSender(this.getAid());
		outbox.setReceiver(receiver);
		
		outbox.setPerformative(performative);
		outbox.setReplyWith(replyWidth);
		outbox.setConversationId(conversationId);
                
		outbox.setContent(message);
        
		this.send(outbox);
		
		if(DEBUG)
			System.out.println(this.getName() + " -----> " + receiver.getLocalName() + " (" + outbox.getPerformative() + "): " + message);
    }
    
    /**
     * Contesta a un mensaje previo de otro agente
	 * 
	 * @param receiver El agente que va a recivir el mensaje
	 * @param performative La performativa usada para la transmisión del mensaje
	 * @param inReplyTo El id al que se tiene que responder
	 * @param conversationId El Id de la conversación
	 * @param message El mensaje a enviar
	 * 
	 * @author Aaron Rodriguez
     */
    public void answerMessage(AgentID receiver, int performative, String inReplyTo, String conversationId, String message) {
        
		ACLMessage outbox = new ACLMessage();
        
		outbox.setSender(this.getAid());
        outbox.setReceiver(receiver);
		
        outbox.setPerformative(performative);
		outbox.setInReplyTo(inReplyTo);
		outbox.setConversationId(conversationId);
                
		outbox.setContent(message);
        
		this.send(outbox);
		
		if(DEBUG)
			System.out.println(this.getName() + " -----> " + receiver.getLocalName() + " (" + outbox.getPerformative() + "): " + message);
    }
    
    /**
     * Recibir mensaje de otro agente
     * 
     * @return El mensaje entero (ACLMessage)
	 * 
	 * @author Jose David and Hugo Maldonado
     */
    
    public ACLMessage receiveMessage() {
        try {
            ACLMessage inbox = this.receiveACLMessage();
           
			if(DEBUG)
				System.out.println(this.getName() + " <----- " + inbox.getSender().getLocalName() + " (" + inbox.getPerformative() + "): " + inbox.getContent());
            
            return inbox;
        } catch (InterruptedException ex) {
            return null;
        }
    }
    
    /**
	 * Función privada para generar un Id de respuesta aleatorio y único para mandarse los mensajes
	 * 
	 * @return el Id de respuesta
	 * 
	 * @author Hugo Maldonado
	 */
	String generateReplyId() {
		
		return UUID.randomUUID().toString().substring(0, 5);
	}
}