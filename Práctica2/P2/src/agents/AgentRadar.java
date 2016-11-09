
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

/**
 *
 * @author JoseDavid & Hugo Maldonado
 */
public class AgentRadar extends Agent {
 
   
    private static final int IDLE= 0;
    private static final int PROCESS_DATA = 1;
    private static final int WAIT_GPS = 2;
    private static final int SEND_DATA = 3;
    private static final int WAIT_WORLD = 4;
    private static final int SEND_CONFIRMATION = 5;
    private static final int FINISH= 6;
        
    private int[] infoRadar;//Array que guarda el estado de cada celda
    private int state;
    private JsonObject dataRadar; //Datos radar y datos gps
    private String value; //Valor a enviar
    private boolean finish;
	
	private final String carName;
	private final String worldName;
    
    public AgentRadar(String radarName, String carName, String worldName) throws Exception {
        super(radarName);
        
		this.carName = carName;
		this.worldName = worldName;
    }
    
    @Override
    public void init(){
        state = IDLE;
        
		infoRadar = new int[25];
        finish = false;
        
        this.dataRadar = new JsonObject();
       
    }
    
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
        value = obj.toString();
    }
    
    @Override
    public void execute() {
        String mensaje;
        while (!finish) {
                    
            switch(state){
                case IDLE:
                    mensaje = receiveMessage();
                    if (mensaje.contains("radar")) { //Recibimos radar primero
                        dataRadar = (JsonObject) Json.parse(mensaje);    
                        this.state=PROCESS_DATA;
                    }
                    
                    if(mensaje.contains("gps")){
                            this.state=WAIT_GPS;
                    }
                    
                    if(mensaje.contains("radarConfirmation")){//Recibimos confirmacion de world (Debemos comprobar si ok o not_ok)
                        if(mensaje.contains("not_ok")){
                            this.state=SEND_DATA;
                            
                        }else{    
                            this.state=SEND_CONFIRMATION;
                        }
                    }
                    if(mensaje.contains("finish")){
                        this.state=FINISH;
                    }
                break;
                 
                case PROCESS_DATA:
                    this.state=IDLE;
                break;
                
                case WAIT_GPS:
                    this.state=SEND_DATA;
                break;                        
                case SEND_CONFIRMATION:
                    JsonObject statusWorld = Json.object();
                    statusWorld.add("confirmation", "ok");
                    sendMessage(carName,statusWorld.toString());//Enviamos confirmacion a car
                    this.state=IDLE;
                break;
                
                case FINISH:
                    finish=true;
                    this.finalize();
                break;
                case SEND_DATA:
                    processRadar();
                    sendMessage(worldName,value);
                    this.state=IDLE;    
                break;
                    
                case WAIT_WORLD:
                    this.state=IDLE;
                break; 
            }
        }
        this.finalize();
    }
    
    @Override
	public void finalize() {
		System.out.println("RadarAgent has just finish");
		super.finalize();
	}
}
            
/*            mensaje = receiveMessage();	//Estamos esperando un mensaje

            if (mensaje.contains("radar")) { //Recibimos radar primero
                datosRadar = (JsonObject) Json.parse(mensaje);
                mensaje = receiveMessage();	//Esperamos un nuevo mensaje
                if (mensaje.contains("gps")) {//Recibimos gps tras recibir el radar
                    datosGps=(JsonObject) Json.parse(mensaje);
                    processRadar(); 
                    sendMessage(AgentNames.AgentWorld, value); //Enviamos los datos al world
                    mensaje = receiveMessage(); //Esperamos mensaje de confirmacion del world
                    if(mensaje.contains("radar")){//Recibimos confirmacion de world (Debemos comprobar si ok o not_ok)
                        JsonObject statusWorld = Json.object();
                        statusWorld.add("confirmation", "ok");
                        sendMessage(AgentNames.AgentCar,statusWorld.toString());//Enviamos confirmacion a car
                        mensaje = receiveMessage();
                    }
                }
            } else if (mensaje.contains("gps")) { //Primero recibe gps
                datosGps = (JsonObject) Json.parse(mensaje);
                mensaje = receiveMessage();	//Esperamos a recibir radar

                if (mensaje.contains("radar")) {//Recibimos radar
                    datosRadar = (JsonObject) Json.parse(mensaje);
                    processRadar(); 
                    sendMessage(AgentNames.AgentWorld, value); //Enviamos radar a World
                    mensaje = receiveMessage();//Esperamos confirmacion de world
                    if(mensaje.contains("radar")){
                        JsonObject statusWorld = Json.object();
                        statusWorld.add("confirmation", "ok");
                        sendMessage(AgentNames.AgentCar,statusWorld.toString());
                        mensaje = receiveMessage();
                    }
                }
            } else if (mensaje.contains("finish")) {
                activo = false;  // Fin
            }
        }

    }
    
}
*/