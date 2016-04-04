package ai.haley.local

import java.net.URI
import javax.websocket.ClientEndpoint

import ai.vital.domain.*

import com.vitalai.aimp.domain.*

import ai.vital.vitalsigns.model.property.URIProperty

import javax.websocket.CloseReason
import javax.websocket.ContainerProvider
import javax.websocket.OnClose
import javax.websocket.OnMessage
import javax.websocket.OnOpen
import javax.websocket.Session
import javax.websocket.WebSocketContainer

import WebsocketClientEndpoint.MessageHandler

import java.net.URI 
import java.net.URISyntaxException


import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.core.Vertx

import ai.haley.local.message.MessageUtils

import ai.haley.local.config.HaleyLocalServerConfig


import ai.haley.local.plugins.HaleyPluginManager



import ai.haley.local.json.JSONUtils

import ai.vital.vitalsigns.model.VitalApp;
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.VITAL_Node
import ai.vital.vitalsigns.model.*
import ai.vital.vitalsigns.rdf.RDFFormat
import ai.vital.vitalsigns.model.xsd

public class HaleyLocalServer {

	
public static void main(String[] args) {


println "Hello!"

try {
	
	// config file
	
	HaleyLocalServerConfig.init()
	
	
	HaleyPluginManager.init()
	
	
	
	def router = Router.router( Vertx.vertx() )
	
	router.route().handler({ routingContext ->
	  routingContext.response().putHeader("content-type", "text/html").end("Hello World!")
	})
	
	 Vertx.vertx().createHttpServer().requestHandler(router.&accept).listen(8888)
	
	
	
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
	
	
	
	VitalSigns vs = VitalSigns.get()
	
	VitalApp app = new VitalApp(name: 'haley-local-server-app', appID: 'haley-local-server-app')
	
	vs.setCurrentApp(app)
	
	
	println "Sending Message!"

	// send message to websocket
  	
   clientEndPoint.sendMessage("hello!")

 BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

 while(true) {

 String line = reader.readLine();

 
 ChatMessage chat_msg = new ChatMessage().generateURI()
 
 
 chat_msg.chatMessage = line
 
 
 def msg = MessageUtils.messageTemplate("session1234", chat_msg.toJSON(), "payload")
 
 
 /*
 def msg_obj = JSONUtils.parseMessage(msg)
 
 boolean valid = MessageUtils.validateMessage(msg_obj)
 
 
 if(valid) {
	 println "Message is valid."
 }
 else { 
	 println "Message is invalid!" 
 }
 
 
 //def msg_string = JSONUtils.encodeMessage(msg)
 
 */
 
 
 clientEndPoint.sendMessage(msg)

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


