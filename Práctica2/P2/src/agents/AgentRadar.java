/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agents;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

/**
 *
 * @author JoseDavid
 */
public class AgentRadar extends Agent {
 
   /*
    private static final int IDLE= 0;
    private static final int PROCESS_DATA = 1;
    private static final int WAIT_GPS = 2;
    private static final int SEND_DATA = 3;
    private static final int WAIT_WORLD = 4;
    private static final int SEND_CONFIRMATION = 5;
    private static final int FINISH= 6;
    */    
    private int[] infoRadar;//Array que guarda el estado de cada celda
    JsonObject datosRadar, datosGps; //Datos radar y datos gps
    String value; //Valor a enviar
    boolean activo;
    
    public AgentRadar(String name) throws Exception {
        super(name);
        infoRadar = new int[25];
        activo = true;
    }
    
    public void processRadar() {
        JsonArray arrayDatos = datosRadar.get("radar").asArray(); //obtener datos del radar en array 
        JsonObject datGps = datosGps.get("gps").asObject();

        int x = datGps.getInt("x", -1);
        int y = datGps.getInt("y", -1);

        JsonArray arrayRadar = (JsonArray) Json.array();
        for(int i=0;i<25;i++){
                arrayRadar.add(x-2+(i % 5));
                arrayRadar.add(y-2+i/5);
                arrayRadar.add(arrayDatos.get(i));
        }

        //Creamos objeto json para enviarlo tras convertirlo a string(value)
        JsonObject obj = Json.object().add("radar", arrayRadar);
        value = obj.toString();
    }
    
    @Override
    public void execute() {
        String mensaje;
        while (activo) {
            mensaje = receiveMessage();	//Estamos esperando un mensaje

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
