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
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JoseDavid
 */
public class AgentController extends Agent{
    
    private int state; 
    private boolean mapExplored;
    private String response;
    private String conversationIDServer;
    
    private final AgentID nameServer;
    private final AgentID car1_name;
    private final AgentID car2_name;
    private final AgentID car3_name;
    private final AgentID car4_name;
    private String mapa;
    
    private static final int SUBS_MAP_EXPLORE = 0;
    private static final int WAKE_AGENTS_EXPLORE = 1;
    private static final int CHECK_AGENTS_EXPLORE = 2;
    private static final int RE_RUN = 3;
    private static final int SAVE_MAP = 4;
    private static final int LOAD_MAP = 5;
    private static final int CHECK_MAP = 6;
    private static final int SUBSCRIBE_MAP = 7;
    private static final int EXPLORE_MAP = 8;
    private static final int WAKE_AGENTS = 9;
    private static final int FINALIZE = 10;
    private static final int FUEL_INFORMATION = 11;
    private static final int CHOOSE_AGENTS = 12;
    private static final int CONTROL_AGENTS= 13;
    private static final int SEND_MAP = 14;
    private static final int CHECK_AGENTS= 15;
    
    
    public AgentController(AgentID name, AgentID nameServer,AgentID car1_name,AgentID car2_name,
                           AgentID car3_name,AgentID car4_name) throws Exception{
        super(name);
        this.nameServer=nameServer;
        this.car1_name=car1_name;
        this.car2_name=car2_name;
        this.car3_name=car3_name;
        this.car4_name=car4_name;
        mapa="";
        
    }
    
    
    /**
     * @author Jose David
     * Check if exists the file map 
    */
    private void stateCheckMap(){
        
        String sFichero = "C:/Users/JoseDavid/Desktop/mapa.txt";
        File fichero=new File(sFichero);
        
        if(fichero.exists()){
            this.state=LOAD_MAP;
        }else{
            this.state=SUBS_MAP_EXPLORE;
        }
        
    }
    
    /**
     * @author Jose David
     * Sends Subscribe message 
     * @param worldName World name 
    */
    private void sendSubscribe(String worldName) {
        JsonObject obj = Json.object().add("world", worldName); 
        ACLMessage inbox = new ACLMessage(ACLMessage.SUBSCRIBE);
        inbox.setContent(obj.asString());
        inbox.addReceiver(nameServer);
        sendMessage(inbox);
         
    }
    
    
    /**
     * @author Jose David
     * Receive subscribe result
     * @return conversationID if ACLMessage INFORM
     * @throws An exception if we get an error from the Controller
     */
    private String subscribeResult() throws Exception{
        //toReceive messageReceived = receiveMessage();
        ACLMessage receive=new ACLMessage();
        if(receive.getPerformativeInt() == ACLMessage.INFORM) {//¿Seguro? 
            
            String result =receive.getContent();
            System.out.println("SUSCRITO. ConvID:"+result);
            return result;
        }       
        
        throw new Exception("Error SUBSCRIBE");
    }
    
    /**
     * @author Jose David
     * Sends conversationID to the car agents
     * @param converID conversationID to send
     */
    private void sendConversationID(String converID){
        
        sendInform(car1_name, converID);
        sendInform(car2_name, converID);
        sendInform(car2_name, converID);
        sendInform(car2_name, converID);
    }
    
    /**
     * @author Jose David
     * Sends Cancel message to the server
     * @param converID conversationID 
    */
    private void sendCancel() {
        JsonObject obj = Json.object().add("conversationID",this.convIDAgents); 
        ACLMessage inbox = new ACLMessage();
        inbox.setPerformative(ACLMessage.CANCEL);
        inbox.addReceiver(nameServer);
        inbox.setContent(obj.asString());
        //toSend messageToSend = new toSend(nameServer.toString(), ACLMessage.CANCEL, obj);
        sendMessage(inbox); 
    }
    
    /**
     * @author Jose David
     * Sends Request message 
     * @param die  
    */
    private void sendRequestToDie(AgentID carName) {
        JsonObject obj;
        obj = new JsonObject().add("die","now");
        ACLMessage inbox = new ACLMessage();
        inbox.setPerformative(ACLMessage.REQUEST);
        inbox.addReceiver(carName);
        inbox.setContent(obj.asString());
        //toSend messageToSend = new toSend(carName.toString(), ACLMessage.REQUEST, obj);
        sendMessage(inbox);    
    }
    
    
    /**
     * @author Jose David
     * Receive the message that confirm carX_agent die   
    */
    private boolean dieAccept(){
        ACLMessage receive;
        receive = this.receiveMessage();
        String response;
        response=receive.getContent();
        if(response.contains("ok")){
            return true;
        }else
            return false;
    }
    
    /**
     * @author Jose David
     * Wake up and start the agents car  
    */
    private void stateWakeAgentsExplore() throws Exception{

        Agent car1;
        Agent car2;
        Agent car3; 
        Agent car4;
        
        car1=new AgentCar(car1_name);
        car1.start();
        car2=new AgentCar(car2_name);
        car2.start();
        car3=new AgentCar(car3_name);
        car3.start();
        car4=new AgentCar(car4_name);
        car4.start();
    }
    
    
    /**
     * @author Jose David
     * Send a request to all the agents
     * to die waiting their confirmations
     * and cancel the subscription
    */
    private void stateFinalize(){
    
        sendRequestToDie(car1_name);
        sendRequestToDie(car2_name);
        sendRequestToDie(car3_name);
        sendRequestToDie(car4_name);
        
        for(int i=0; i<4; i++) {
            if(dieAccept()){
                System.out.println("Agent Car :"+i+"die");
            }
        }   
        sendCancel();
        
        //FALTA GUARDAR LA TRAZA
    }
    
    
    
    @Override
    public void execute() {
        System.out.println("AgentCar execution");
		
		while(!mapExplored) {
			switch(state) {
				case SUBS_MAP_EXPLORE:
					
				    sendSubscribe(mapa);
                                    
                                    {
                                        try {
                                            subscribeResult();
                                        } catch (Exception ex) {
                                            Logger.getLogger(AgentController.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                    
                                    this.state=WAKE_AGENTS_EXPLORE;
					
					break;
				case WAKE_AGENTS_EXPLORE:
				
                                    {
                                        try {
                                            stateWakeAgentsExplore();
                                        } catch (Exception ex) {
                                            Logger.getLogger(AgentController.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                    this.state=CHECK_AGENTS_EXPLORE;
                                    
					break;
				case CHECK_AGENTS_EXPLORE:
		
                                    {
                                        try {
                                              conversationIDServer=subscribeResult();
                                        } catch (Exception ex) {
                                            Logger.getLogger(AgentController.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                    sendConversationID(conversationIDServer);
                                    
                                    //Esperamos roles de agentes
                                    
                                    
                                    
					break;
				case RE_RUN:
					stateFinalize();
                                        this.state=CHECK_MAP;
					
					
					break;
				case SAVE_MAP:
					
					
					
					break;
				case LOAD_MAP:
					
					

					break;
				case CHECK_MAP:
					
                                    stateCheckMap();
					
					break;
				case SUBSCRIBE_MAP:
					
				    sendSubscribe(mapa);
                                    {
                                        try {
                                            subscribeResult();
                                            
                                        } catch (Exception ex) {
                                            Logger.getLogger(AgentController.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }	
				     
                                    this.state=WAKE_AGENTS;
                                    
					break;
				
				case EXPLORE_MAP:
					
					
					
					break;
                                case WAKE_AGENTS:
					
                                    
                                    {
                                        try {
                                            stateWakeAgentsExplore();
                                        } catch (Exception ex) {
                                            Logger.getLogger(AgentController.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                    this.state=CHECK_AGENTS;
                                   
					
					break;
                                case FINALIZE:
					
				    
                                    stateFinalize();
                                    
                                    //TENEMOS QUE GUARDAR LA TRAZA QUE NOS ENVIA EL SERVER    
                                    
                                    
					break;
                                case FUEL_INFORMATION:
					
					
					
					break;
                                case CHOOSE_AGENTS:
					
					
					
					break;
                                case CONTROL_AGENTS:
					
					
					
					break;
                                case SEND_MAP:
					
					
					break;
                                case CHECK_AGENTS:
					
                                    {
                                        try {
                                            conversationIDServer=subscribeResult();
                                        } catch (Exception ex) {
                                            Logger.getLogger(AgentController.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                    sendConversationID(conversationIDServer);
                                    
                                    this.state=SEND_MAP;
					
					break;
			}
		}
    }
    
    
}

		
					
