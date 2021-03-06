 package fr.utbm.roodroid.server;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import fr.utbm.roodroid.ApplicationManager;
import fr.utbm.roodroid.Connection;
import fr.utbm.roodroid.Conversation;
import fr.utbm.roodroid.Message;
import fr.utbm.roodroid.Message.MessageStatus;
import fr.utbm.roodroid.PacketClient;
import fr.utbm.roodroid.TCPCommandType;
import fr.utbm.roodroid.TextMessage;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.util.Pair;

/**
 * Server
 * Abstract class derived in order to refined the Connection type (Wifi, Bluetooth).
 * 
 * Manages the authentication of new clients.
 * Keeps one connection with each connected client.
 * Routes new messages received to the clients.
 * Routes new messages from the clients to their recipients.
 * Notifies an handler when stats and info changes
 * (this is used to display real-time info on the server UI).
 * 
 * @author Jonathan Perichon <jonathan.perichon@gmail.com>
 * @author Lucas Gerbeaux <lucas.gerbeaux@gmail.com>
 *
 */
public abstract class Server implements Runnable {

	private boolean isRunning;
	private AuthMethod authMethod;
	private int nbMsgSent;
	private int maxNbMsgSent;
	private int maxNbClients;
	private Handler redrawHandler;
	protected HashMap<String, Pair<Connection, HashSet<String>>> activeConnections;
	protected Set<Connection> awaitingConnections;
	
	protected final Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			PacketClient p = (PacketClient)msg.obj;

			try {
				switch (p.getCommandType()) {
				case SEND_ID:
					onNewClientConnected(p.getId(), p.getAddressSource());
					break;
				case SEND_PASSWORD:
					onPasswordReceived(p.getId(), p.getAddressSource(), (String)p.getObj());
					break;
				case MSG_SENT:
					onMsgSent(p.getId(), (Conversation)p.getObj());
					break;
				case CLIENT_EXIT:
					onClientExit(p.getId());
					break;
				case CONNECTION_EXIT:
					onConnectionExit((Connection)p.getObj());
					break;
				default:
				}
			} catch (IOException e) {
				ApplicationManager.appendLog(Log.ERROR, "Server", e.getMessage());
				e.printStackTrace();
			}
		}
	};


	public Server(AuthMethod authMethod, int maxNbMsgSent, int maxNbClients) {
		this.isRunning = true;
		this.nbMsgSent = 0;
		this.maxNbMsgSent = maxNbMsgSent;
		this.activeConnections = new HashMap<String, Pair<Connection, HashSet<String>>>();
		this.awaitingConnections = new HashSet<Connection>();
		this.maxNbClients = maxNbClients;
		this.authMethod = authMethod;

		IntentFilter SMSfilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		ApplicationManager.getInstance().getApplicationContext().registerReceiver(this.receiver, SMSfilter);
	}
	
	abstract void closeServerSocket();
	
	public boolean isWifiOk() {
		WifiManager wifiManager = (WifiManager) ApplicationManager.getInstance().getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private void onConnectionExit(Connection con) {
		for (Map.Entry<String, Pair<Connection, HashSet<String>>> e : activeConnections.entrySet()) {
			if (e.getValue().first.equals(con)) {
				activeConnections.remove(e.getKey());
			}
		}
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, Intent intent) {
			Bundle bundle = intent.getExtras();        
			final Object[] pdus = (Object[]) bundle.get("pdus");
			if (pdus != null) {
				for (int i=0; i< pdus.length; i++) {
					SmsMessage msg = SmsMessage.createFromPdu((byte[])pdus[i]);
					String address = msg.getOriginatingAddress();
					String name = "";
					Cursor contactCursor = context.getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, address), new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
					if (contactCursor != null && contactCursor.moveToFirst()) {
						name = contactCursor.getString(contactCursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
					}    	

					try {
						broadcast(new Conversation(address, name, new TextMessage(new Date(msg.getTimestampMillis()), MessageStatus.RECEIVED, msg.getMessageBody())));
					} catch (IOException e) {
						ApplicationManager.appendLog(Log.ERROR, "Serveur", e.getMessage());
						e.printStackTrace();
					}
				}

			}
		}
	};

	public Class<? extends AuthMethod> getAuthMethod() {
		return authMethod.getClass();
	}
	
	public void onClientExit(Connection connect) {
		for (Map.Entry<String, Pair<Connection, HashSet<String>>> entry : activeConnections.entrySet()) {
			if (entry.getValue().first.equals(connect)) {
				activeConnections.remove(entry.getKey());
				break;
			}
		}
		redrawHandler.sendEmptyMessage(ApplicationManager.CLIENT_DISCONNECTED);
	}

	public void onClientExit(String id) {
		activeConnections.remove(id);
		redrawHandler.sendEmptyMessage(ApplicationManager.CLIENT_DISCONNECTED);
	}

	private boolean acceptNewClient() {
		return this.activeConnections.size() < this.maxNbClients;
	}

	private boolean idAllowed(String id) {
		return (getAuthMethod() == AuthByID.class) ? authMethod.isAuthorized(id) : true;
	}

	public boolean checkPassword(String password) {
		return (getAuthMethod() == AuthByPassword.class) ? authMethod.isAuthorized(password) : true;
	}
	
	public int getMaxNbClient() {
		return maxNbClients;
	}
	
	public int getNbAcceptedClients() {
		return activeConnections.size();
	}
	
	public int getNbTotalClients() {
		return activeConnections.size();
	}
	
	public int getNbAwaitingClients() {
		return awaitingConnections.size();
	}

	public void broadcast(Conversation conv) throws IOException {
		boolean activeClients = false;
		boolean inContacts = false;
		// sends to active clients
		for (Map.Entry<String, Pair<Connection, HashSet<String>>> client : activeConnections.entrySet()) {
			if (client.getValue().second.contains(conv.getContactPhoneNumber())) {
				activeClients = true;
				activeConnections.get(client.getKey()).first.write(TCPCommandType.MSG_RECEIVED, conv);
			}
		}
		// sends to clients who has the phone number
//		if (!activeClients) {
//			for (String id : activeConnections.keySet()) {
//				if (lookupContact(id, conv.getContactPhoneNumber())) {
//					inContacts = true;
//					activeConnections.get(id).first.write(TCPCommandType.MSG_RECEIVED, conv);
//				}
//			}
//		}
		// sends to all the clients
		if (!activeClients && !inContacts) {
			for (String username : this.activeConnections.keySet()) {
				activeConnections.get(username).first.write(TCPCommandType.MSG_RECEIVED, conv);
			}
		}
	}

//	private boolean lookupContact(String id, String phoneNumber) throws IOException {
//		this.activeConnections.get(id).first.write(TCPCommandType.REQUEST_HAS_CONTACT, phoneNumber);
//		return false;
//	}

	public synchronized void exit() throws IOException {
		this.isRunning = false;
		for (Map.Entry<String, Pair<Connection, HashSet<String>>> client : activeConnections.entrySet()) {
			client.getValue().first.write(TCPCommandType.SERVER_EXIT);
			client.getValue().first.disconnect();
		}
		redrawHandler.sendEmptyMessage(ApplicationManager.SERVER_DISCONNECTED);
	}
	
	protected synchronized boolean isRunning() {
		return isRunning;
	}
	
	private Connection getAwaitingConnection(String address) {
		Connection con = null;
		Iterator<Connection> it = awaitingConnections.iterator();
		while (it.hasNext()) {
			Connection tmp = it.next();
			if (tmp.getAddressSource().equals(address)) {
				con = tmp;
				break;
			}
		}
		return con;
	}

	private void onClientAccepted(String id, Connection con) throws IOException {
		this.activeConnections.put(id, new Pair<Connection, HashSet<String>>(con, new HashSet<String>()));
		this.awaitingConnections.remove(con);
		redrawHandler.sendEmptyMessage(ApplicationManager.NEW_CLIENT_ACCEPTED);
	}

	private void onNewClientConnected(String id, String address) throws IOException {
		Connection con;
		redrawHandler.sendEmptyMessage(ApplicationManager.NEW_CLIENT_CONNECTED);
		if ((con = getAwaitingConnection(address)) == null) {
			return;
		}
		
		if (activeConnections.containsKey(id)) {
			con.write(TCPCommandType.AUTH_FAILED, "Username already taken.");
			return;
		}
		if (!acceptNewClient()) {
			// limit nb clients reache
			con.write(TCPCommandType.AUTH_FAILED, "Limit nb clients reached.");
			return;
		}

		Class<? extends AuthMethod> authMethod = getAuthMethod();
		if (authMethod == AuthByPassword.class) {
			// request password
			con.write(TCPCommandType.REQUEST_PASSWORD);
			return;
		}
		if (!idAllowed(id)) {
			// client id refused
			con.write(TCPCommandType.AUTH_FAILED, "Client ID refused.");
		} else {
			onClientAccepted(id, con);
		}
	}

	private void onPasswordReceived(String id, String address, String passwd) throws IOException {
		Connection con;
		if ((con = getAwaitingConnection(address)) == null) {
			return;
		}
		if (checkPassword(passwd)) {
			onClientAccepted(id, con);
		} else {
			con.write(TCPCommandType.AUTH_FAILED, "Wrong password.");	
		}
	}

	private void onMsgSent(String id, Conversation conv) throws IOException {
		if (maxNbMsgSent != -1 && nbMsgSent >= maxNbMsgSent) {
			this.activeConnections.get(id).first.write(TCPCommandType.SERVER_MAX_MSG_REACHED, "Maximum message sent limit reached.");
			ApplicationManager.appendLog(Log.DEBUG, "Force limit message", "The limit of messages sent has been reached."); 	
		}
		else {
			SmsManager smsManager = SmsManager.getDefault();
			for (Message m : conv.getMessages()) {
				String textMessage = id + ": " + m.getTextContent();
				smsManager.sendTextMessage(conv.getContactPhoneNumber(), null, textMessage, null, null);
				nbMsgSent++;
				if (nbMsgSent >= maxNbMsgSent) {
					for (String username : this.activeConnections.keySet()) {
						activeConnections.get(username).first.write(TCPCommandType.SERVER_MAX_MSG_REACHED, "Maximum message sent limit reached");
						ApplicationManager.appendLog(Log.DEBUG, "Limit message reached", "The limit of messages sent has been reached."); 	
					}
				}
			}
		}
	}

	public void setRedrawHandler(Handler redrawHandler) {
		this.redrawHandler = redrawHandler;
	}
}
