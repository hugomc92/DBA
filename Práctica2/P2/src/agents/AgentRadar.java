
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import es.upv.dsic.gti_ia.core.AgentID;

/**
 *
 * @author Jose David & Hugo Maldonado & Bryan Moreno Picamán
 */
public class AgentRadar extends Agent {
 
   
    private static final int IDLE = 0;
    private static final int PROCESS_DATA = 1;
    private static final int SEND_DATA = 2;
    private static final int WAIT_WORLD = 3;
    private static final int SEND_CONFIRMATION = 4;
    private static final int FINISH = 5;
	
	private static final boolean DEBUG = false;
        
    private int[] infoRadar;		//Array que guarda el estado de cada celda
    private int state;
    private JsonObject dataRadar;	//Datos radar y datos gps
	private boolean gpsProcced;
    private String radarToUpdate;	//Valor a enviar
    private boolean finish;
	
	private String message;
	
	private final AgentID carName;
	private final AgentID worldName;
    
    /**
     *
     * @param radarName
     * @param carName
     * @param worldName
     * @throws Exception
     */
    public AgentRadar(AgentID radarName, AgentID carName, AgentID worldName) throws Exception {
        super(radarName);
        
		this.carName = carName;
		this.worldName = worldName;
    }
    
    /**
     *
     */
    @Override
    public void init(){
        state = IDLE;
        
		infoRadar = new int[25];
        finish = false;
        
        this.dataRadar = new JsonObject();
		
		System.out.println("AgentRadar has just started");
    }
    
    /**
     *
     */
    public void processRadar() {
		
		JsonArray arrayDatos = dataRadar.get("radar").asArray(); //obtener datos del radar en array 
		//JsonObject datGps = datosGps.get("gps").asObject();

		//int x = datGps.getInt("x", -1);
		//int y = datGps.getInt("y", -1);

		//JsonArray arrayRadar = (JsonArray) Json.array();
		//for(int i=0;i<25;i++){
		//        arrayRadar.add(x-2+(i % 5));
		//        arrayRadar.add(y-2+i/5);
		//        arrayRadar.add(arrayDatos.get(i));
		//}

		//Creamos objeto json para enviarlo tras convertirlo a string(value)
		//JsonObject obj = Json.object().add("radar", arrayRadar);
		JsonObject obj = Json.object().add("radar",arrayDatos);
		radarToUpdate = obj.toString();
    }
    
    /**
     *
     */
    @Override
    public void execute() {
		
		System.out.println("AgentRadar execution");
		
        while (!finish) {    
            switch(state) {
                case IDLE:
					
					if(DEBUG)
						System.out.println("AgentRadar status: IDLE");
										
					boolean finalize = false;
					
					for(int i=0; i<2 && !finalize; i++) {
						message = this.receiveMessage();

						if(message.contains("radar"))	//Del server
							dataRadar = (JsonObject) Json.parse(message);    
						else if(message.contains("gps")) {
							this.gpsProcced = !message.contains("updated");
						}
						else if(message.contains("CRASHED") || message.contains("finalize"))
							finalize = true;
					}
					
					if(finalize)
						this.state = FINISH;
					else
						this.state = PROCESS_DATA;
					
					break;
                case PROCESS_DATA:
					
					if(DEBUG)
						System.out.println("AgentRadar status: PROCESS_DATA");
					
					if(this.gpsProcced) {
						processRadar();

						this.state = SEND_DATA;
					}
					else
						this.state = SEND_CONFIRMATION;
					
					break;  
				case SEND_DATA:
					
					if(DEBUG)
						System.out.println("AgentRadar status: SEND_DATA");
					
                    sendMessage(worldName, radarToUpdate);
					
					this.state = WAIT_WORLD;

					break;
                case WAIT_WORLD:
					
					if(DEBUG)
						System.out.println("AgentRadar status: WAIT_WORLD");
					
                    String confirmation = this.receiveMessage();
					
					JsonObject confirmationObject = Json.parse(confirmation).asObject();
					String worldMessage = confirmationObject.get("radar").asString();
                    
					if(worldMessage.contains("ok"))//confirmacion del world
						this.state = SEND_CONFIRMATION;
					else
						this.state = FINISH;
					
					break;
				case SEND_CONFIRMATION:
					
					if(DEBUG)
						System.out.println("AgentRadar status: SEND_CONFIRMATION");
					
                    JsonObject statusWorld = new JsonObject();
					
                    statusWorld.add("radar", "ok");
					
                    sendMessage(carName, statusWorld.toString());	//Enviamos confirmacion a car
					
                    this.state = IDLE;
					
					break;		
				case FINISH:
					
					if(DEBUG)
						System.out.println("AgentRadar status: FINISH");
					
					if(this.message.contains("finalize")) {
						JsonObject confirmationMessage = new JsonObject();
						
						confirmationMessage.add("radar", "finish");

						this.sendMessage(carName, confirmationMessage.toString());
					}
					
                    this.finish = true;
					
					break;
            }
        }
    }
    
    /**
     *
     */
    @Override
	public void finalize() {
		
		System.out.println("AgentRadar has just finished");
		
		super.finalize();
	}
}