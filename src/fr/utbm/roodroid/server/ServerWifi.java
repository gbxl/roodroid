package fr.utbm.roodroid.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import fr.utbm.roodroid.ApplicationManager;
import fr.utbm.roodroid.Connection;
import fr.utbm.roodroid.ConnectionWifi;
import android.util.Log;

/**
 * ServerWifi
 * Extends Server in order to instantiate the ConnectionWifi.
 * 
 * @author Jonathan Perichon <jonathan.perichon@gmail.com>
 * @author Lucas Gerbeaux <lucas.gerbeaux@gmail.com>
 *
 */
public class ServerWifi extends Server {
	private ServerSocket serverSocket;

	public ServerWifi(AuthMethod authMethod, int maxNbMsgSent, int maxNbClients, int port) throws IOException {
		super(authMethod, maxNbMsgSent, maxNbClients);
		serverSocket = new ServerSocket(port);
	}

	@Override
	public void run() {
		Thread t = null;
		Connection con = null;
		try {
			while(isRunning()) {
				Socket clientSocket = serverSocket.accept();
				if (clientSocket != null) {
					ApplicationManager.appendLog(Log.DEBUG, "Server", "New client connected");
					con = new ConnectionWifi(handler, clientSocket);
					this.awaitingConnections.add(con);
					t = new Thread(con);
					t.start();
				}
			}
			con.disconnect();
		} catch (IOException e) {
			ApplicationManager.appendLog(Log.ERROR, "ServerWifi", e.getMessage());
			this.awaitingConnections.remove(con);
			this.activeConnections.values().remove(con);
			con.disconnect();
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getIPAddress() {
		return this.serverSocket.getInetAddress().toString();
	}
	
	public void closeServerSocket() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}