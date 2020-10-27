package ServerUDP;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

import ServerUDP.ProtocolThread;

public class Server {

	public static int CANT_THREADS;
	public final static String TEXT_PATH = "data/textos/";
	public final static String NOMBRE_1 = "texto1.txt";
	public final static String NOMBRE_2 = "texto2.txt";
	public final static String LOG_PATH = "data/informes/log-";
	public static BufferedWriter logWriter;
	
	public static int PUERTO = 49200;

	public static void main(String[] args) {
		System.out.println("Servidor UDP: ");
		
		// HERRAMIENTA PARA LECTURA DE LA CONFIGURACION DE LA CONEXION
		Scanner console = new Scanner(System.in);
		
		// PREPARACION DEL SOCKET
		ServerSocket socket = null;
		
		try {
			// INCIALIZACION DEL LOG SEGUN FECHA
			String time = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss").format(Calendar.getInstance().getTime());
			File logFile = new File(LOG_PATH + time + ".txt");
			logWriter = new BufferedWriter(new FileWriter(logFile));
			
			System.out.println("Escriba el puerto en el que quiere realizar la conexion");
			PUERTO = console.nextInt();
			
			// CREACION DEL SOCKET
			socket = new ServerSocket(PUERTO);

			System.out.println("Ingrese numero de conexiones: (MAX 25)");
			CANT_THREADS = console.nextInt();
			if(CANT_THREADS > 25)
				CANT_THREADS = 25;

			writeLog("Numero de conexiones: " + CANT_THREADS);

			String timeLog = new SimpleDateFormat("HH-mm-ss_dd/MM/yyyy").format(Calendar.getInstance().getTime());
			writeLog("Hora_Fecha: " + timeLog);
			
			// ELECCION DEL ARCHIVO
			System.out.println("Que archivo se va a enviar? (Escribir \"1\" para el de 100 MiB o \"2\" para el de 250 MiB)");
			
			String fileName;
			switch (console.nextInt()) {
			case 1:
				fileName = NOMBRE_1;
				break;
			case 2:
				fileName = NOMBRE_2;
				break;
			default:
				System.out.println("Error: Numero de archivo invalido");
				return;
			}
			writeLog("Nombre del archivo: " + fileName + ".");
			writeLog("Tamanio del archivo: " + (new File(TEXT_PATH + fileName).length() )  + " bytes.");
			console.close();
			
			// INICIALIZACION DE TODOS LOS SOCKETS
			Socket[] conections = new Socket[CANT_THREADS];
			System.out.println("Esperando conexiones...");
			int idCliente = 0;
			
			// RECEPCION DE UN NUEVO CLIENTE
			while (idCliente < CANT_THREADS) {
				DataOutputStream _DOS = null;
				DataInputStream _DIS = null;
				try {
					// LLEGADA DEL NUEVO CLIENTE
					conections[idCliente] = socket.accept();
					
					// CONFIGURACION DE CANALES CON EL CLIENTE
					_DOS = new DataOutputStream(conections[idCliente].getOutputStream());
					_DIS = new DataInputStream(conections[idCliente].getInputStream());
					idCliente++;
					System.out.println("Conectando con el cliente #: " + idCliente );
					
					// ENVIA SU NOMBRE Y ID
					_DOS.writeByte(1);
					_DOS.writeUTF(fileName);
					_DOS.flush();
					
					_DOS.write(idCliente);
					
					
					
				} catch (Exception e) {
					System.out.println("Error de conexion con los clientes");
				}
			}
			System.out.println("Inicio de envio del archivo " + fileName + " a los " + CANT_THREADS + " clientes.");
			for (int i = 0; i < conections.length; i++) {
				writeLog("Inicia envio al cliente #" + (i + 1));
				ProtocolThread thread = new ProtocolThread(conections[i], fileName, logWriter, (i + 1));
				thread.start();
			}
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
			try {
				writeLog("Error enviando el archivo: " + e.getMessage());
			} catch (Exception e1) {
				
			}
		}
	}
	
	/**
	 * Escribe el mensaje en el log writer
	 * @param log Mensaje a escribir
	 * @throws IOException
	 */
	private static void writeLog(String log) throws IOException {
		logWriter.write(log);
		logWriter.newLine();
		logWriter.flush();
	}
}