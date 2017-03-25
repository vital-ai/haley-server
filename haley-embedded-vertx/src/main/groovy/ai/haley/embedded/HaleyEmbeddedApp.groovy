package ai.haley.embedded


import ai.haley.embedded.wemo.HaleyWemoManager


import io.vertx.groovy.ext.web.handler.StaticHandler


//import io.vertx.core.DeploymentOptions
//import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpMethod

import org.slf4j.Logger
import org.slf4j.LoggerFactory;

import com.vitalai.aimp.domain.*

//import ai.vital.vitalsigns.model.property.URIProperty


import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.core.Vertx


import ai.haley.api.HaleyAPI
import ai.haley.api.session.HaleySession
import ai.haley.api.session.HaleyStatus
import ai.haley.embedded.HaleyEmbeddedApp.ConnectionFailedException
import ai.haley.embedded.HaleyEmbeddedApp.ReconnectionFailedException;
import ai.haley.embedded.config.HaleyEmbeddedAppConfig
import ai.vital.service.vertx3.websocket.VitalServiceAsyncWebsocketClient
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.VitalApp
import com.vitalai.aimp.domain.AIMPMessage
import com.vitalai.aimp.domain.Channel
import com.vitalai.aimp.domain.UserTextMessage



//import ai.vital.vitalsigns.model.xsd

public class HaleyEmbeddedApp {

	static HaleyAPI haleyAPI

	static HaleySession haleySession

	static Channel channel

	static String username

	static String password

	static VitalApp app

	static String wemoDeviceName = "Outlet1" // "Wemo Insight"

	static String endpointURL

	static String channelName = 'devices'

	private final static Logger log = LoggerFactory.getLogger(HaleyEmbeddedApp.class)

	public static void main(String[] args) {


		println "Hello!"

		try {

			// config file

			//HaleyEmbeddedAppConfig.init()


			// start http server, use for local UI and rest interface


			/*
			 * tried a bunch of things, but get occasional warnings and exceptions like:
			 * Aug 28, 2016 12:43:40 PM io.vertx.core.impl.BlockedThreadChecker
			 * WARNING: Thread Thread[vert.x-eventloop-thread-0,5,main] has been blocked for 20
			 * 
			 * io.vertx.core.VertxException: Thread blocked
			 * 
			 * but it seems we still get a response
			 * 
			 */


			/*
			 * added -Dvertx.options.blockedThreadCheckInterval=12345
			 * which didn't seem to help
			 * 
			 */


			//new DeploymentOptions().setWorker(true)

			def vertx = Vertx.vertx()

			def server = vertx.createHttpServer()

			def router = Router.router(vertx)


			//def router = Router.router( Vertx.vertx(
			// [
			// maxEventLoopExecuteTime:60000,
			// maxWorkerExecuteTime:60000,
			// blockedThreadCheckPeriod:60000
			// ]
			//) )


			def route = router.route(HttpMethod.POST, "/devices/:devicename/:deviceaction")

			route.blockingHandler({ routingContext ->

				def deviceName = routingContext.request().getParam("devicename")
				def deviceAction= routingContext.request().getParam("deviceaction")

				println "Device: " + deviceName
				println "Action: " + deviceAction

				if(deviceAction == "on") {


					HaleyWemoManager.turnOnDevice(deviceName)


				}

				if(deviceAction == "off") {

					HaleyWemoManager.turnOffDevice(deviceName)

				}

				if(deviceAction == "status") {



				}

				def stat = HaleyWemoManager.deviceStatus(deviceName)

				routingContext.response().putHeader("content-type", "text/html").end("$deviceName $deviceAction Current: $stat")

			})



			def route_list = router.route(HttpMethod.GET, "/devices")

			route_list.blockingHandler({ routingContext ->


				println "listing devices"

				def devices = HaleyWemoManager.listDevices()

				def device_list = ""

				devices.each{ device_list += ( it + "\n") ; println "device: " + it}

				//device_list = "<html><body>" + device_list + "</body></html>"

				// routingContext.response().putHeader("content-type", "text/html")

				// routingContext.response().setChunked(true)

				// routingContext.response().write(device_list)

				routingContext.response().putHeader("content-type", "text/html").end(device_list)


			})



			// add static handler


			//def router2 = Router.router( Vertx.vertx(
			// [
			// maxEventLoopExecuteTime:60000,
			// maxWorkerExecuteTime:60000,
			// blockedThreadCheckPeriod:60000
			// ]
			//) )


			router.route().blockingHandler(StaticHandler.create())

			/*
			 def route_home = router.route(HttpMethod.GET, "/")
			 route_home.handler({ routingContext ->
			 routingContext.response().putHeader("content-type", "text/html").end("Hello World!")
			 })
			 */




			/*
			 router.route().handler({ routingContext ->
			 routingContext.response().putHeader("content-type", "text/html").end("Hello World!")
			 })
			 */

			server.requestHandler(router.&accept).listen(8888)

			//Vertx.vertx().createHttpServer().requestHandler(router2.&accept).listen(8080)



			VitalSigns vs = VitalSigns.get()


			endpointURL = vs.getConfig("haleyEndpoint")
			println "Endpoint: ${endpointURL}"
			String appID =  vs.getConfig("haleyApp")
			println "AppID: ${appID}"
			username =  vs.getConfig("haleyLogin")
			println "Username: ${username}"
			password =  vs.getConfig("haleyPassword")
			println "Password length: ${password.length()}"


			String wemoBinary = vs.getConfig("wemoBinary")

			HaleyWemoManager.wemoBinary = wemoBinary

			println "wemoBinary:" + HaleyWemoManager.wemoBinary


			app = VitalApp.withId(appID)


			vs.setCurrentApp(app)

			def devices = HaleyWemoManager.listDevices()

			devices.each { println "Device: " + it }

			def stat = HaleyWemoManager.deviceStatus(wemoDeviceName)

			println wemoDeviceName + " Status: " + stat


			while(true) {

				try {

					connectHaleyAPI(vertx)

				} catch(Exception e) {
					log.error("Haley connection failed, resuming immedatiely", e)
				}
			}



		} catch (InterruptedException ex) {
			println("InterruptedException exception: " + ex.getMessage())
		} catch (URISyntaxException ex) {
			println("URISyntaxException exception: " + ex.getMessage())
		}






	}


	static class ConnectionFailedException extends Exception {

		public ConnectionFailedException(Throwable arg0) {
			super(arg0);
		}

	}

	static class ReconnectionFailedException extends Exception {

		int attempts = 0

		ReconnectionFailedException(int attempts) {
			this.attempts = attempts
		}

	}

	/**
	 * throws exception when either the client couldn't connect to endpoint or websocket reconnection failed
	 * @param vertx
	 */
	static void connectHaleyAPI(Vertx vertx) throws ConnectionFailedException, ReconnectionFailedException {


		VitalServiceAsyncWebsocketClient websocketClient = new VitalServiceAsyncWebsocketClient(vertx, app, 'endpoint.', endpointURL, 10, 3000)


		Object lock = new Object()

		Exception returnedException = null

		websocketClient.connect({ Throwable exception ->

			if(exception) {
				log.error("Error when connecting to endpoint: ${endpointURL}", exception)
				returnedException = new ConnectionFailedException(exception)
				synchronized(lock) {
					lock.notifyAll()
				}
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

		}, {Integer attempts ->
			log.error("websocket reconnect failed ${attempts} time(s)")
			returnedException = new ReconnectionFailedException(attempts)
			synchronized(lock) {
				lock.notifyAll()
			}
		})

		synchronized (lock) {
			lock.wait()
		}

		if(returnedException != null) throw returnedException


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

			for(Channel ch : channels) {

				if(ch.name == channelName) {
					channel = ch
					break
				}

			}

			if(channel == null) {
				System.err.println("Channel not found: ${channelName}")
				return
			}
			
			println "CHANNEL: ${channel}"

			onChannelObtained()

		}


	}



	// these get replaced with handlers/producers of AIMP messages


	static void onChannelObtained() {


		HaleyStatus rs = haleyAPI.registerCallback(AIMPMessage.class, true, { ResultList msg ->

			//HaleyTextMessage m = msg.first()

			AIMPMessage m = msg.first()

			String channelURI = m.channelURI
			if(channel.URI != channelURI) {
				log.warn("Ignoring message not for this channel")
				return
			}

			println "HALEY SAYS: ${m?.text}"


			// the user text messages to be replaced with sending aimp messages back


			if(m.isSubTypeOf(DeviceMessage)) {

				println "got device message"

				if(m.isaType(DeviceActivateMessage)) {
					println "activating"

					HaleyWemoManager.turnOnDevice(wemoDeviceName)


					DeviceStateChangeMessage dscm = new DeviceStateChangeMessage().generateURI()

					dscm.deviceName = wemoDeviceName
					dscm.channelURI = channel.URI
					dscm.deviceNewState = "on"
					dscm.deviceOldState = "off"



					//UserTextMessage utm = new UserTextMessage()
					//utm.text = "I turned on the light."
					//utm.channelURI = channel.URI

					haleyAPI.sendMessage(haleySession, dscm) { HaleyStatus sendStatus ->

						println "send text message status: ${sendStatus}"

					}


				}

				if(m.isaType(DeviceDeactivateMessage)) {

					println "deactivating"

					HaleyWemoManager.turnOffDevice(wemoDeviceName)


					DeviceStateChangeMessage dscm = new DeviceStateChangeMessage().generateURI()

					dscm.deviceName = wemoDeviceName
					dscm.channelURI = channel.URI
					dscm.deviceNewState = "off"
					dscm.deviceOldState = "on"

					//UserTextMessage utm = new UserTextMessage()
					//utm.text = "I turned off the light."
					//utm.channelURI = channel.URI

					haleyAPI.sendMessage(haleySession, dscm) { HaleyStatus sendStatus ->

						println "send text message status: ${sendStatus}"

					}




				}
				if(m.isaType(DeviceStatusRequestMessage)) {

					println "status"


					def stat = HaleyWemoManager.deviceStatus(wemoDeviceName)

					//				if(stat == "on") {

					DeviceStatusMessage dsm = new DeviceStatusMessage()
					dsm.deviceName = wemoDeviceName
					dsm.deviceStatus = stat
					dsm.channelURI = channel.URI

					//					UserTextMessage utm = new UserTextMessage()
					//					utm.text = "The light is on."
					//					utm.channelURI = channel.URI

					haleyAPI.sendMessage(haleySession, dsm) { HaleyStatus sendStatus ->

						println "send text message status: ${sendStatus}"

					}



					//				}
					//				else {
					//					UserTextMessage utm = new UserTextMessage()
					//					utm.text = "The light is off."
					//					utm.channelURI = channel.URI
					//
					//					haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
					//
					//						println "send text message status: ${sendStatus}"
					//
					//					}
					//
					//				}



				}


			} else {


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
	
	
	
	
				}
	
				if(message.equals("turn off the light")) {
	
	
	
				}
	
				if(message.equals("is the light on?")) {
	
	
	
	
				}
	
	
				if(message.equals("is the light off?")) {
	
	
	
					def stat = HaleyWemoManager.deviceStatus(wemoDeviceName)
	
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
			}

		})

		println "Register callback status: ${rs}"

		//	UserTextMessage utm = new UserTextMessage()
		//	utm.text = "Whats your name?"
		//	utm.channelURI = channel.URI
		//
		//	haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
		//
		//		println "send text message status: ${sendStatus}"
		//
		//	}

		JoinChannel joinChannelMessage = new JoinChannel()
		joinChannelMessage.channelURI = channel.URI

		haleyAPI.sendMessage(haleySession, joinChannelMessage) { HaleyStatus sendStatus ->

			println "join channel message status: ${sendStatus}"

		}

	}


}
