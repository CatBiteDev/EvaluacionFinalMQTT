package com.example.android;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static String mqtthost = "tcp://catbitedev.cloud.shiftr.io:1883";
    private static String IdUsuario = "AppAndroid";

    private static String Topico = "Mensaje";
    private static String user = "catbitedev";
    private static String pass = "da5GvpDVvLkLSWhH";

    private TextView textView;
    private EditText editText,txtDireccion,txtCliente,txtNombreProducto,txtCodigoProducto;
    private ListView lista;
    private Spinner categoriaProducto;
    private FirebaseFirestore db;
    private Button botonEnvio;

    private MqttClient mqttClient;

    String[] TipoComida = {"Carbohidratos","Vegano","No vegano"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CargarListaFirestore();

        db = FirebaseFirestore.getInstance();

        txtCodigoProducto = findViewById(R.id.txtCodigoProducto);
        txtNombreProducto = findViewById(R.id.txtNombreProducto);
        txtCliente = findViewById(R.id.txtCliente);
        txtDireccion = findViewById(R.id.txtDireccion);
        categoriaProducto = findViewById(R.id.categoriaProducto);
        lista = findViewById(R.id.lista);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, TipoComida);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoriaProducto.setAdapter(adapter);

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
    public void enviarDatosFirestore(View view){
        String codigo = txtCodigoProducto.getText().toString();
        String nombre = txtNombreProducto.getText().toString();
        String cliente = txtCliente.getText().toString();
        String direccion = txtDireccion.getText().toString();
        String categoria = categoriaProducto.getSelectedItem().toString();

        Map<String, Object> comida = new HashMap<>();
        comida.put("codigo", codigo);
        comida.put("nombre", nombre);
        comida.put("cliente", cliente);
        comida.put("direccion", direccion);
        comida.put("categoria", categoria);

        db.collection("Comidas").document(codigo).set(comida).addOnSuccessListener(aVoid -> {
            Toast.makeText(MainActivity.this, "Datos Enviados Correctamente", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e ->{
            Toast.makeText(MainActivity.this, "Datos no enviados: "+ e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    public void CargarLista(View view){
        CargarListaFirestore();
    }
    public void CargarListaFirestore(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Comidas").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    List<String> listaComidas = new ArrayList<>();
                    for(QueryDocumentSnapshot document : task.getResult()){
                        String linea = "|| "+ document.getString("codigo")+ " || " +
                                document.getString("nombre") + " || "+
                                document.getString("cliente") + " || "+
                                document.getString("direccion") + " || ";
                        listaComidas.add(linea);
                    }
                    ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_list_item_1,
                            listaComidas
                            );
                    lista.setAdapter(adaptador);
                }
                else {
                    Log.e("TAG", "Error al obtener datos", task.getException());
                }
            }
        });
    }
}