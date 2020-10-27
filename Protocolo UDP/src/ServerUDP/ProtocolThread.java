package ServerUDP;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;

public class ProtocolThread extends Thread {

	public final static String ARCHIVO_PATH = "data/textos/";
	public final static int PACKAGE = 1024;

	private Socket socket = null;
	private static BufferedWriter logWriter;

	public String fileName = "";
	private int clientId;

	/**
	 * Genera el thread de comunicacion con el cliente respectivo
	 * @param pSocket socket de comunicacion
	 * @param nombreArchivo nombre del archivo a enviar
	 * @param logWriter log de la prueba actual
	 * @param id id del cliente
	 */
	public ProtocolThread(Socket pSocket, String nombreArchivo, BufferedWriter logWriter, int id) {
		socket = pSocket;
		ProtocolThread.logWriter = logWriter;
		clientId = id;
		fileName = ARCHIVO_PATH + nombreArchivo;
		try {
			socket.setSoTimeout(30000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
 
	}

	/**
	 * 
	 */
	public void run() {
		try {
			sendFile();
		} catch (IOException e1) {
			System.out.println("Error con el envio del archivo" + e1.getMessage());
			try {
				writeLog("Error con el envio del archivo al cliente " + clientId + ": " + e1.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			socket.close();
		} catch (IOException e) {
			System.out.println("Socket error - IOException: " + e.getMessage());
		}

	}

	/**
	 * @throws IOException 
	 * 
	 */
	public void sendFile() throws IOException {

		FileInputStream _FIS = null;
		BufferedInputStream _BIS = null;
		DataOutputStream _DOS = null;
		OutputStream _OUPUT = null;

		try {
			// BUSCA EL ARCHIVO A ENVIAR Y SE GENERA UN CANAL PARA LEER EL MISMO
			File file = new File(fileName);
			_FIS = new FileInputStream(file);
			_BIS = new BufferedInputStream(_FIS);

			// SE ALMACENA EL ARCHIVO EN BUFFER
			byte[] buffer = new byte[(int) file.length()];
			_BIS.read(buffer, 0, buffer.length);

			// SE GENERA EL HASH DEL ARCHIVO
			MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
			String hashEnviar = checkSum(shaDigest, file );

			// SE CREA UN CANAL DE COMUNICACION CON EL CLIENTE
			_DOS = new DataOutputStream(socket.getOutputStream());

			// SE ENVIA EL HASH DEL ARCHIVO
			_DOS.writeUTF(hashEnviar);

			// SE ENVIA EL TAMANIO DEL ARCHIVO
			_DOS.writeInt((int)file.length());

			// SE INICIA EL CANAL DE ENVIO
			System.out.println("Enviando "+ file.getName() + " de tamanio: " + buffer.length + " bytes al usuario " + clientId);
			_OUPUT = new BufferedOutputStream(socket.getOutputStream());

			// VARIABLE PARA MEDIR EL TIEMPO DE DESCARGA
			long totalTime = System.currentTimeMillis();
			int sendedBytes = 0;
			int cantPaquetes = 0;
			
			// ENVIO DEL ARCHIVO A TRAVES DE PAQUETES
			while (sendedBytes < buffer.length) {
				cantPaquetes++;
				if ((sendedBytes + PACKAGE) < buffer.length) {
					_OUPUT.write(buffer, sendedBytes, PACKAGE);
					sendedBytes += PACKAGE;
				} else {
					_OUPUT.write(buffer, sendedBytes, (buffer.length - sendedBytes));
					sendedBytes += (buffer.length - sendedBytes) + 1;
				}
			}
			totalTime = System.currentTimeMillis() - totalTime;

			

		} catch (Exception e) {
			System.out.println(e.getMessage());
			try {
				writeLog("Error de comunicacion con el cliente");
			} catch (Exception e1) {
			}
		}
		finally {
			logWriter.close();
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

	/**
	 * Calcula el hash del archivo usando un digest
	 * @param digest Digest a utilizar
	 * @param file Archivo
	 * @return El hash del archivo
	 * @throws IOException
	 */
	private static String checkSum(MessageDigest digest, File file) throws IOException
	{
		//Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		//Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0; 

		//Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		};

		//close the stream; We don't need it now.
		fis.close();

		//Get the hash's bytes
		byte[] bytes = digest.digest();

		//This bytes[] has bytes in decimal format;
		//Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< bytes.length ;i++)
		{
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		//return complete hash
		return sb.toString();
	}
}