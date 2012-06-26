package fr.utbm.roodroid.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import fr.utbm.roodroid.ConnectionWifi;

/**
 * ClientWifi
 * Extends Client in order to instantiate the ConnectionWifi.
 * 
 * @author Jonathan Perichon <jonathan.perichon@gmail.com>
 * @author Lucas Gerbeaux <lucas.gerbeaux@gmail.com>
 *
 */
public class ClientWifi extends Client {

	public ClientWifi(String id) {
		super(id);
	}
	
	public void connect(String ipAddress, int port) throws UnknownHostException, IOException { 
		this.connection = new ConnectionWifi(this.handler, new Socket(ipAddress, port));
        new Thread(this.connection).start();
        this.authenticate();
	}
}