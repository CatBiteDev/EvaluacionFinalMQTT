package com.example.android;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    private static String mqtthost = "tcp://catbitedev.cloud.shiftr.io:1883";
    private static String IdUsuario = "Android";

    private static String Topico = "Mensaje";
    private static String user = "";
    private static String pass = "";

    private TextView textView;
    private EditText editText;
    private Button botonEnvio;

    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textovista);
        editText = findViewById(R.id.txtNombreProducto);
        botonEnvio = findViewById(R.id.enviarMensaje);

        try {
            mqttClient = new MqttClient(mqtthost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(user);
            options.setPassword(pass.toCharArray());

            mqttClient.connect(options);

            Toast.makeText(this,"Aplicacion Conectada", Toast.LENGTH_SHORT).show();

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause){
                    Log.d("MQTT", "Conexion Perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message){
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> textView.setText(payload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token){
                    Log.d("MQTT" ,"Entrega hecha");
                }
            });
        } catch (MqttException e){
            e.printStackTrace();
        }

        botonEnvio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mensaje = editText.getText().toString();
                try {
                    if(mqttClient != null && mqttClient.isConnected()){
                        mqttClient.publish(Topico, mensaje.getBytes(), 0 , false);
                        textView.append("\n - "+ mensaje);
                        Toast.makeText(MainActivity.this, "Mensaje Enviado", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this, "Error: no se pudo enviar el mensaje, la conexion no esta activa", Toast.LENGTH_SHORT).show();
                    }
                }catch (MqttException e){
                    e.printStackTrace();
                }
            }
        });
    }
}