package ai.haley.local

import java.net.URI
import javax.websocket.ClientEndpoint
import javax.websocket.CloseReason
import javax.websocket.ContainerProvider
import javax.websocket.OnClose
import javax.websocket.OnMessage
import javax.websocket.OnOpen
import javax.websocket.Session
import javax.websocket.WebSocketContainer

import WebsocketClientEndpoint.MessageHandler;

import java.net.URI
import java.net.URISyntaxException



public class HaleyLocalServer {


public static void main(String[] args) {


println "Hello!"

try {
	// open websocket
	final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI("ws://localhost:8887/websocket/"))

	// add listener
	clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
		public void handleMessage(String message) {
			println(message)
		}
	})


	
	// wait for session to become valid
	// and for it to be Open
	
	Session mysession = clientEndPoint.userSession
	
	def max = 10
	
	boolean open = false
	
	def t = 0
	
	while(t < max ) {
		
		mysession = clientEndPoint.userSession
		
		if(mysession == null) {
			
			// do nothing
			
		}
		else {
		
		if(mysession.isOpen() == true) {
			
			open = true
			break
		}
		
		}
		
		Thread.sleep(1000)
		t++
		
	}
	
	if(open == false) {
		
		println "WebSocket is not open! Exiting..."
		System.exit(-1)
		
		
	}
	
	println "Sending Message!"

	// send message to websocket
  
	
   clientEndPoint.sendMessage("hello!")


 BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

 while(true) {

 String line = reader.readLine();

 clientEndPoint.sendMessage(line)

 }
 
 
} catch (InterruptedException ex) {
println("InterruptedException exception: " + ex.getMessage())
} catch (URISyntaxException ex) {
println("URISyntaxException exception: " + ex.getMessage())
}
 




}



}


@ClientEndpoint
public class WebsocketClientEndpoint {

	Session userSession = null
	private MessageHandler messageHandler

	public WebsocketClientEndpoint(URI endpointURI) {
		try {
			WebSocketContainer container = ContainerProvider.getWebSocketContainer()
			container.connectToServer(this, endpointURI)
		} catch (Exception e) {
			throw new RuntimeException(e)
		}
	}

	/**
	 * Callback hook for Connection open events.
	 *
	 * @param userSession the userSession which is opened.
	 */
	@OnOpen
	public void onOpen(Session userSession) {
		System.out.println("opening websocket")
		this.userSession = userSession
	}

	/**
	 * Callback hook for Connection close events.
	 *
	 * @param userSession the userSession which is getting closed.
	 * @param reason the reason for connection close
	 */
	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
		System.out.println("closing websocket")
		this.userSession = null
	}

	/**
	 * Callback hook for Message Events. This method will be invoked when a client send a message.
	 *
	 * @param message The text message
	 */
	@OnMessage
	public void onMessage(String message) {
		if (this.messageHandler != null) {
			this.messageHandler.handleMessage(message)
		}
	}

	/**
	 * register message handler
	 *
	 * @param msgHandler
	 */
	public void addMessageHandler(MessageHandler msgHandler) {
		this.messageHandler = msgHandler
	}

	/**
	 * Send a message.
	 *
	 * @param message
	 */
	public void sendMessage(String message) {
		this.userSession.getAsyncRemote().sendText(message)
	}

	/**
	 * Message handler.
	 *
	 * @author Jiji_Sasidharan
	 */
	public static interface MessageHandler {

		public void handleMessage(String message)
	}
}


