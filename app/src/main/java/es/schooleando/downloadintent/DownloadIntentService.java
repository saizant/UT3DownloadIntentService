package es.schooleando.downloadintent;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class DownloadIntentService extends IntentService {
    // definimos tipos de mensajes que utilizaremos en ResultReceiver
    public static final int PROGRESS = 0;
    public static final int FINSISHED = 1;
    public static final int ERROR = 2;


    private static final String TAG = "DownloadIntentService";

    private int codigoRespuesta, tamanyoRecurso;
    private String tipo;

    //Constructor
    public DownloadIntentService() {
        super("DownloadIntentService");
    }

    //Método para realizar la tarea en el Worker Thread
    @Override
    protected void onHandleIntent(Intent intent) {

        ResultReceiver resultado;

        // Ejemplo de como logear
        Log.d(TAG, "Servicio arrancado!");

        if (intent != null) {
            //ResultReceiver recogido del intent recibido
            resultado = intent.getParcelableExtra("receiverTag");

            //Obtener URL del Intent
            String urlString = intent.getStringExtra("urlTag");
            Log.d(TAG, "URL del intent: " + urlString);

            Bundle b = new Bundle();//

            ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnected()) {
                // Aquí deberás descargar el archivo y notificar a la Activity mediante un ResultReceiver que recibirás en el intent.
                try {
                    URL url = new URL(urlString);
                    HttpURLConnection conexion = (HttpURLConnection)url.openConnection();
                    //Petición de descarga y conexión
                    //conexion.setRequestMethod("GET");
                    conexion.setRequestMethod("HEAD");
                    conexion.connect();

                    //Datos de la conexión y la descarga
                    codigoRespuesta = conexion.getResponseCode();
                    tipo = conexion.getContentType();
                    tamanyoRecurso = conexion.getContentLength();

                    if (codigoRespuesta==200) {
                        if (tipo.startsWith("image/")) {    //
                            //Flujos i/o
                            //InputStream is = conexion.getInputStream();
                            InputStream is = url.openStream();
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();

                            byte[] buffer = new byte[1024];
                            int n = 0;
                            int total = 0;

                            Log.d(TAG,"Descargando...");

                            //Mientras se vaya leyendo del buffer
                            while ((n = is.read(buffer)) != -1) {
                                //Escribir
                                bos.write(buffer,0,n);
                                total += n;

                                if (tamanyoRecurso != -1) {
                                    Integer porcentaje = (total*100 / tamanyoRecurso);
                                    SystemClock.sleep(100);
                                    b = new Bundle();
                                    b.putString("DownloadService", String.valueOf(porcentaje));
                                    resultado.send(this.PROGRESS, b);
                                } else {
                                    Integer porcentaje = total;
                                    SystemClock.sleep(100);
                                    b = new Bundle();
                                    b.putString("DownloadService", String.valueOf(porcentaje));
                                    resultado.send(this.PROGRESS, b);
                                }
                            }

                            String[] datos = urlString.split("/");
                            String[] trozo = datos[datos.length-1].split("\\.");

                            if (trozo.length<2) {
                                trozo = new String[]{"unknown", "jpg"};
                            }

                            //Archivo temporal
                            File outputDir = getExternalCacheDir();

                            File outputFile = File.createTempFile(trozo[0], "." + trozo[1], outputDir);
                            outputFile.deleteOnExit();      //Eliminar archivo temporal al SALIR

                            FileOutputStream fos = new FileOutputStream(outputFile);
                            fos.write(bos.toByteArray());

                            b.putString("ruta", outputFile.getPath());
                            resultado.send(FINSISHED, b);

                            //Cerrar flujos i/o
                            bos.close();
                            is.close();

                            Log.d(TAG, "Descarga realizada");

                        } else {
                            Log.d(TAG, "Error: la URL no corresponde a una imagen");
                            b.putString("DownloadService", "Error: la URL no corresponde a una imagen");
                            resultado.send(ERROR, b);
                        }
                    } else {
                        Log.d(TAG, "Error al conectar. Código de respuesta: " + codigoRespuesta);
                        b.putString("DownloadService", "Error al conectar.");
                        resultado.send(ERROR, b);
                    }

                } catch (MalformedURLException e) {
                    b = new Bundle();
                    b.putString("DownloadService", "URL incorrecta");
                    resultado.send(this.ERROR, b);
                    e.printStackTrace();
                } catch (IOException e) {
                    b = new Bundle();
                    b.putString("DownloadService", "Error de descarga");
                    resultado.send(this.ERROR, b);
                    e.printStackTrace();
                }

            } else {
                b.putString("DownloadService", "Error de conexión");
                resultado.send(ERROR, b);
            }

        } else {
            Log.d(TAG, "Error en el DownloadService");
        }


        // Deberamos obtener el ResultReceiver del intent
        // intent.getParcelableExtra("receiver");

        // Es importante que definas el contenido de los Intent.

        // Por ejemplo:
        //  - que enviarás al IntentService como parámetro inicial (url a descargar)
        //         intent.getgetStringExtra("url")
        //  - que enviarás a ResultReceiver para notificarle incrementos en el porcentaje de descarga (número de 0 a 100)
        //         receiver.send(PROGRESS, Bundle);
        //  - que enviarás a ResultReceiver cuando se haya finalizado la descarga (nombre del archivo temporal)
        //         receiver.send(FINISHED, Bundle);




    }
}
