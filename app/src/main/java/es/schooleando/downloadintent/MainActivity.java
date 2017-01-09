package es.schooleando.downloadintent;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    EditText urlET;
    Button descargarBTN;
    ProgressBar progressBar;
    TextView estado;

    String urlString;
    Intent intent;

    private static final String LOGTAG = "Main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Añade en el interfaz un botón y un TextView, como mínimo.
        urlET = (EditText)findViewById(R.id.urlET);
        descargarBTN = (Button)findViewById(R.id.descargarBTN);
        progressBar = (ProgressBar)findViewById(R.id.progress);
        estado = (TextView)findViewById(R.id.estadoTV);

        // cuando pulsemos el botón deberemos crear un Intent que contendrá un Bundle con:
        // una clave "url" con la dirección de descarga asociada.
        // una clave "receiver" con un objeto ResultReceiver.
        //
        // El objeto ResultReceiver contendrá el callback que utilizaremos para recibir
        // mensajes del IntentService.

        // después deberás llamar al servicio con el intent.
        descargarBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!urlET.getText().toString().trim().equalsIgnoreCase("")) {
                    //Instanciar Intent
                    intent = new Intent(MainActivity.this, DownloadIntentService.class);
                    //Obtener URL introducida
                    urlString = urlET.getText().toString();
                    //Introducir al Bundle la URL y la clave con el ResultReceiver
                    intent.putExtra("urlTag", urlString);

                    ResultReceiver receiver = new ResultReceiver(new Handler()) {
                        @Override
                        protected void onReceiveResult(int resultCode, Bundle resultData) {
                            //super.onReceiveResult(resultCode, resultData);
                            Log.d(LOGTAG, "ReceiverResult");
                            //UI Thread
                            //Según el mensaje de las constantes del DownloadIntentService
                            switch (resultCode) {
                                case DownloadIntentService.PROGRESS:
                                    int progreso = resultData.getInt("DownloadService");
                                    progressBar.setIndeterminate(progreso < 0);
                                    if (progreso > 0) {
                                        estado.setText("" + progreso + "%");
                                    }
                                    progressBar.setProgress(progreso);
                                    break;

                                case DownloadIntentService.FINSISHED:
                                    String urlStr = resultData.getString("ruta");
                                    estado.setText("");
                                    File fichero = new File(urlStr);
                                    if (fichero.exists()) {
                                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                                        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(fichero).toString());
                                        String tipo = mime.getMimeTypeFromExtension(extension);

                                        try {
                                            Intent i = new Intent();
                                            i.setAction(Intent.ACTION_VIEW);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                Uri contentUri = FileProvider.getUriForFile(MainActivity.this, "es.schooleando.downloadintent.fileProvider", fichero);
                                                i.setDataAndType(contentUri, tipo);
                                            } else {
                                                i.setDataAndType(Uri.fromFile(fichero), tipo);
                                            }
                                            startActivityForResult(i, 0);
                                            progressBar.setProgress(0);     //
                                            progressBar.setMax(100);    //

                                        } catch (ActivityNotFoundException e) {
                                            Toast.makeText(MainActivity.this, "Activity no encontrada", Toast.LENGTH_LONG).show();
                                        }

                                    } else {
                                        Toast.makeText(MainActivity.this, "El fichero no existe", Toast.LENGTH_LONG).show();
                                    }
                                    break;

                                case DownloadIntentService.ERROR:
                                    String mensaje = resultData.getString("DownloadService");
                                    estado.setText("");
                                    Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                                    progressBar.setProgress(0);     //
                                    progressBar.setMax(100);    //
                                    break;
                            }
                        }
                    };

                    intent.putExtra("receiverTag",receiver);
                    startService(intent);

                } else {
                    Toast.makeText(MainActivity.this, "No ha introducido una URL", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

}
