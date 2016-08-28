package ai.haley.embedded


import ai.haley.embedded.wemo.HaleyWemoManager


//import io.vertx.groovy.ext.web.handler.StaticHandler


//import java.net.URI
//import javax.websocket.ClientEndpoint

//import ai.vital.domain.*

//import com.vitalai.aimp.domain.*

//import ai.vital.vitalsigns.model.property.URIProperty

//import javax.websocket.CloseReason
//import javax.websocket.ContainerProvider
//import javax.websocket.OnClose
//import javax.websocket.OnMessage
//import javax.websocket.OnOpen
//import javax.websocket.Session
//import javax.websocket.WebSocketContainer

//import WebsocketClientEndpoint.MessageHandler

//import java.net.URI 
//import java.net.URISyntaxException


import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.core.Vertx

//import io.vertx.ext.web.handler.sockjs.BridgeEventType
//import io.vertx.ext.web.handler.sockjs.BridgeOptions
//import io.vertx.ext.web.handler.sockjs.PermittedOptions
//import io.vertx.ext.web.handler.sockjs.SockJSHandler

//import io.vertx.groovy.ext.web.handler.sockjs.SockJSHandler


//import ai.haley.embedded.message.MessageUtils

import ai.haley.api.HaleyAPI
import ai.haley.api.session.HaleySession
import ai.haley.api.session.HaleyStatus
import ai.haley.embedded.config.HaleyEmbeddedAppConfig
import ai.vital.service.vertx3.websocket.VitalServiceAsyncWebsocketClient
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.VitalApp
import com.vitalai.aimp.domain.AIMPMessage
import com.vitalai.aimp.domain.Channel
import com.vitalai.aimp.domain.UserTextMessage


//import ai.haley.embedded.json.JSONUtils

//import ai.vital.vitalsigns.model.VitalApp;
//import ai.vital.vitalsigns.VitalSigns
//import ai.vital.vitalsigns.model.VITAL_Node
//import ai.vital.vitalsigns.model.*
//import ai.vital.vitalsigns.rdf.RDFFormat
//import ai.vital.vitalsigns.model.xsd

public class HaleyEmbeddedApp {

	static HaleyAPI haleyAPI
	
	static HaleySession haleySession
	
	static Channel channel
	
	static String username
	
	static String password
	
	
	
	
public static void main(String[] args) {


println "Hello!"

try {
	
	// config file
	
	HaleyEmbeddedAppConfig.init()
	
	
	// start http server, use for local UI and rest interface
	
	
	 def router = Router.router( Vertx.vertx() )
	
	router.route().handler({ routingContext ->
	  routingContext.response().putHeader("content-type", "text/html").end("Hello World!")
	})
	
	Vertx.vertx().createHttpServer().requestHandler(router.&accept).listen(8888)
	
	
	
	VitalSigns vs = VitalSigns.get()
	
	
	String endpointURL = vs.getConfig("haleyEndpoint")
	println "Endpoint: ${endpointURL}"
	String appID =  vs.getConfig("haleyApp")
	println "AppID: ${appID}"
	username =  vs.getConfig("haleyLogin")
	println "Username: ${username}"
	password =  vs.getConfig("haleyPassword")
	println "Password length: ${password.length()}"
	
	
	VitalApp app = VitalApp.withId(appID)
	
	VitalServiceAsyncWebsocketClient websocketClient = new VitalServiceAsyncWebsocketClient(Vertx.vertx(), app, 'endpoint.', endpointURL)
	
	websocketClient.connect() { Throwable exception ->
		
		if(exception) {
			exception.printStackTrace()
			return
		}
		
		haleyAPI = new HaleyAPI(websocketClient)
		
		println "Sessions: " + haleyAPI.getSessions().size()
		
		haleyAPI.openSession() { String errorMessage,  HaleySession session ->
			
			haleySession = session
			
			if(errorMessage) {
				throw new Exception(errorMessage)
			}
			
			println "Session opened ${session.toString()}"
			
			println "Sessions: " + haleyAPI.getSessions().size()
			
			onSessionReady()
			
		}
		
	}
	
	def devices = HaleyWemoManager.listDevices()
	
	devices.each { println "Device: " + it }
	
	def stat = HaleyWemoManager.deviceStatus("WeMo Insight")
	
	println "WeMo Insight Status: " + stat
	

	
	//def options = [
	//	heartbeatInterval:2000
	//  ]
	/*
	
	def sockJSHandler = SockJSHandler.create(Vertx.vertx() )

	sockJSHandler.socketHandler({ sockJSSocket ->
		
		  // Just echo the data back
		  sockJSSocket.handler(sockJSSocket.&write)
		})
	
	
	def inboundPermitted1 = [
		addressRegex:".*"
	  ]
	
	def outboundPermitted1 = [
		addressRegex:".*"
	  ]
	
	
	//def options = [:]
	
	def options = [
		inboundPermitteds:[
		  inboundPermitted1
		  
		],
		outboundPermitteds:[
		  outboundPermitted1
		 
		]
	  ]
	
	
	
	sockJSHandler.bridge(options)
	
	router.route("/eventbus/*").handler(sockJSHandler)
	 
	 
	router.route("/*").handler(StaticHandler.create())
	 
	
	Vertx.vertx().createHttpServer().requestHandler(router.&accept).listen(8888)
	
	*/
	
	
	
	
	
	
	
	//println "Sending Message!"

	// send message to websocket
  	/*
   clientEndPoint.sendMessage("hello!")

 BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

 while(true) {

 String line = reader.readLine();

 
 ChatMessage chat_msg = new ChatMessage().generateURI()
 
 
 chat_msg.chatMessage = line
 
 
 def msg = MessageUtils.messageTemplate("session1234", chat_msg.toJSON(), "payload")
 */
	
 
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
 
 
 //clientEndPoint.sendMessage(msg)

 //}
 
 
} catch (InterruptedException ex) {
println("InterruptedException exception: " + ex.getMessage())
} catch (URISyntaxException ex) {
println("URISyntaxException exception: " + ex.getMessage())
}
 




}


static void onSessionReady() {
	
	haleyAPI.authenticateSession(haleySession, username, password) { HaleyStatus status ->
		
		println "auth status: ${status}"

		println "session: ${haleySession.toString()}"
		
		
		onAuthenticated()
		
	}
	
}

static void onAuthenticated() {
		
	println "listing channels"
	
	haleyAPI.listChannels(haleySession) { String error, List<Channel> channels ->

		if(error) {
			System.err.println("Error when listing channels")
			return
		}

		
		println "channels count: ${channels.size()}"
		
		channel = channels[0]
		
		onChannelObtained()
		
	}
		
		
}

static void onChannelObtained() {
	
	
	HaleyStatus rs = haleyAPI.registerCallback(AIMPMessage.class, true, { ResultList msg ->
		
		//HaleyTextMessage m = msg.first()
		
		AIMPMessage m = msg.first()
		
		println "HALEY SAYS: ${m?.text}"
		
		
		String message = m?.text
		
		if(message.equals("list devices")) {
			
			def devices = HaleyWemoManager.listDevices()
			
			UserTextMessage utm = new UserTextMessage()
			utm.text = "I found these devices: " + devices
			utm.channelURI = channel.URI
			
			haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
				
				println "send text message status: ${sendStatus}"
				
			}
			
			
		}
		
		
		
		if(message.equals("turn on the light")) {

			HaleyWemoManager.turnOnDevice("WeMo Insight")
			
			
			UserTextMessage utm = new UserTextMessage()
			utm.text = "I turned on the light."
			utm.channelURI = channel.URI
			
			haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
				
				println "send text message status: ${sendStatus}"
				
			}
			
			
		}
		
		if(message.equals("turn off the light")) {
			
			HaleyWemoManager.turnOffDevice("WeMo Insight")
			
			
			UserTextMessage utm = new UserTextMessage()
			utm.text = "I turned off the light."
			utm.channelURI = channel.URI
			
			haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
				
				println "send text message status: ${sendStatus}"
				
			}
			
		}
		
		if(message.equals("is the light on?")) {
		
	
			def stat = HaleyWemoManager.deviceStatus("Wemo Insight")
			
			if(stat == "on") {
				
				UserTextMessage utm = new UserTextMessage()
				utm.text = "Yes."
				utm.channelURI = channel.URI
				
				haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
					
					println "send text message status: ${sendStatus}"
					
				}
				
				
			}
			else {
				UserTextMessage utm = new UserTextMessage()
				utm.text = "No."
				utm.channelURI = channel.URI
				
				haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
					
					println "send text message status: ${sendStatus}"
					
				}
				
			}
				
	}
		
		
		if(message.equals("is the light off?")) {
		
			
			
			def stat = HaleyWemoManager.deviceStatus("Wemo Insight")
			
			if(stat == "on") {
				
				UserTextMessage utm = new UserTextMessage()
				utm.text = "No."
				utm.channelURI = channel.URI
				
				haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
					
					println "send text message status: ${sendStatus}"
					
				}
				
				
			}
			else {
				UserTextMessage utm = new UserTextMessage()
				utm.text = "Yes."
				utm.channelURI = channel.URI
				
				haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
					
					println "send text message status: ${sendStatus}"
					
				}
				
			}
			
	}
	
		
	})
	
	println "Register callback status: ${rs}"
	
	UserTextMessage utm = new UserTextMessage()
	utm.text = "Whats your name?"
	utm.channelURI = channel.URI
	
	haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
		
		println "send text message status: ${sendStatus}"
		
	}
	
	
}





}









// remove all websocket stuff, using haley api for that



/*

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

*/

	/**
	 * Callback hook for Connection open events.
	 *
	 * @param userSession the userSession which is opened.
	 */

/*
	@OnOpen
	public void onOpen(Session userSession) {
		System.out.println("opening websocket")
		this.userSession = userSession
	}

*/
	/**
	 * Callback hook for Connection close events.
	 *
	 * @param userSession the userSession which is getting closed.
	 * @param reason the reason for connection close
	 */

/*
	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
		System.out.println("closing websocket")
		this.userSession = null
	}
*/

	/**
	 * Callback hook for Message Events. This method will be invoked when a client send a message.
	 *
	 * @param message The text message
	 */

/*

	@OnMessage
	public void onMessage(String message) {
		if (this.messageHandler != null) {
			this.messageHandler.handleMessage(message)
		}
	}
*/

	/**
	 * register message handler
	 *
	 * @param msgHandler
	 */

/*

	public void addMessageHandler(MessageHandler msgHandler) {
		this.messageHandler = msgHandler
	}
*/

	/**
	 * Send a message.
	 *
	 * @param message
	 */
/*

	public void sendMessage(String message) {
		this.userSession.getAsyncRemote().sendText(message)
	}
*/

	/**
	 * Message handler.
	 *
	 * @author Jiji_Sasidharan
	 */

/*
	public static interface MessageHandler {

		public void handleMessage(String message)
	}
	
	

}
*/


