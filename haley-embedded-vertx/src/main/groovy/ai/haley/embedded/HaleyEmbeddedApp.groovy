package ai.haley.embedded


import ai.haley.embedded.webserver.StatusHandler
import ai.haley.embedded.wemo.HaleyWemoManager
import ai.haley.embedded.temphumiditysensor.HaleyTempHumiditySensorManager

import groovy.json.JsonSlurper
import io.vertx.groovy.ext.web.handler.StaticHandler


//import io.vertx.core.DeploymentOptions
//import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpMethod

import java.util.Set;

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
import ai.vital.domain.Edge_hasUserLogin;
import ai.vital.service.vertx3.websocket.VitalServiceAsyncWebsocketClient
import ai.vital.vitalservice.query.ResultList
import ai.vital.vitalsigns.VitalSigns
import ai.vital.vitalsigns.model.GraphMatch
import ai.vital.vitalsigns.model.VITAL_GraphContainerObject
import ai.vital.vitalsigns.model.VitalApp
import ai.vital.vitalsigns.model.property.GeoLocationProperty
import ai.vital.vitalsigns.model.property.URIProperty
import com.vitalai.aimp.domain.AIMPMessage
import com.vitalai.aimp.domain.Channel
import com.vitalai.aimp.domain.UserTextMessage
import com.vitalai.aimp.domain.AssetConditionMessage
import com.vitalai.aimp.domain.AssetLocationMessage
import com.vitalai.aimp.domain.Edge_hasAssetNode



//import ai.vital.vitalsigns.model.xsd

public class HaleyEmbeddedApp {

	static final ALL_ASSETS = 'all'
	
	
	static HaleyAPI haleyAPI

	static HaleySession haleySession

	static Channel channel

	static Channel assetsChannel
	
	static Channel loginChannel
	
	static String username

	static String password

	static VitalApp app

	static String wemoDeviceName = "Outlet1" // "Wemo Insight"

	static String endpointURL

	static String channelName = 'devices'
	
	static Boolean assetsEnabled = null
	
	static Boolean verifyAssets = true
	
	static Boolean sendAssetConditionMessage = true
	
	static Boolean sendAssetLocationMessage = false
	
	static String assetsChannelName = 'assets'
	
	static Integer assetsIntervalSeconds = null
	
	static String assetURI = ALL_ASSETS

	static Set<String> assetURIs = new HashSet<String>()
	
	static Vertx vertx 
	
	
	static Long lastAssetMessageTimestamp = null
	
	static Long lastAssetReceivedTimestamp = null
	
	static Boolean wakeUpWordEnabled = null
	
	private final static Logger log = LoggerFactory.getLogger(HaleyEmbeddedApp.class)

	public static void main(String[] args) {


		println "Hello!"

		try {

			// config file
			
			VitalSigns vs = VitalSigns.get()
			
			
			endpointURL = vs.getConfig("haleyEndpoint")
			println "Endpoint: ${endpointURL}"
			String appID =  vs.getConfig("haleyApp")
			println "AppID: ${appID}"
			username =  vs.getConfig("haleyLogin")
			println "Username: ${username}"
			password =  vs.getConfig("haleyPassword")
			println "Password length: ${password.length()}"

			
			Object assetsEnabledParam = vs.getConfig("assetsEnabled")
			if(assetsEnabledParam == null) throw new Exception("No assetsEnabled boolean param")
			if(!(assetsEnabledParam instanceof Boolean)) throw new Exception("assetsEnabled param must be a boolean")
			
			assetsEnabled = assetsEnabledParam
			println "assetsEnabled: ${assetsEnabled}"
			
			Object assetURIParam = vs.getConfig("assetURI")
			if(assetURIParam != null) {
				if(!(assetURIParam instanceof String)) throw new Exception("assetURI param must be a string")
				assetURI = assetURIParam
				println "assetURI: ${assetURI}"
			} else {
				println "assetURI: ${assetURI} (default)"
			}
			
			
			Object assetsIntervalSecondsParam = vs.getConfig("assetsIntervalSeconds")
			if(assetsIntervalSecondsParam == null) throw new Exception("No assetsIntervalSeconds integer param")
			if(!(assetsIntervalSecondsParam instanceof Number)) throw new Exception("assetsIntervalSeconds must be an integer")
			
			assetsIntervalSeconds = assetsIntervalSecondsParam.intValue()
			println "assetsIntervalSeconds: ${assetsIntervalSeconds}"

			lastAssetMessageTimestamp = System.currentTimeMillis()
			lastAssetReceivedTimestamp = System.currentTimeMillis()
			
//			Object wakeUpWordEnabledParam = vs.getConfig("wakeUpWordEnabled");
//			if(wakeUpWordEnabledParam == null) throw new Exception("No wakeUpWordEnabled boolean param")
//			if(!(wakeUpWordEnabledParam instanceof Boolean)) throw new Exception("wakeUpWordEnabled must be a boolean")
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
			Map haleyEmbeddedWebserverCfg = vs.getConfig("haleyEmbeddedWebserver")
			
			if(haleyEmbeddedWebserverCfg == null) {
				throw new Exception("No haleyEmbeddedWebserver vitalsigns config object")
			}
			
			println "haleyEmbeddedWebserver config: ${haleyEmbeddedWebserverCfg}"
			
			//config is just passed
			
			vertx = Vertx.vertx()

			def server = vertx.createHttpServer(haleyEmbeddedWebserverCfg)

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

			router.get('/status').handler(new StatusHandler())

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

			server.requestHandler(router.&accept).listen()

			//Vertx.vertx().createHttpServer().requestHandler(router2.&accept).listen(8080)



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


		VitalServiceAsyncWebsocketClient websocketClient = new VitalServiceAsyncWebsocketClient(vertx, app, 'endpoint.', endpointURL, 1000, 5000)


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

			haleyAPI.addReconnectListener({HaleySession session ->
				
				log.info("websocket client reconnected, sending join channel message")
					
				if(channel == null) {
					log.warn("No devices channel, cannot proceed")
					return
				}
				
				HaleyEmbeddedApp.sendMessage(new JoinChannel(channelURI: channel.URI)) { HaleyStatus sendStatus ->
						
					log.info("Send join channel message status: ${sendStatus}")
						
				}
						
			})
			
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
				}
				
				if(ch.name == assetsChannelName) {
					assetsChannel = ch
				}
				
				if(ch.name.toString() == username) {
					loginChannel = ch
				}				

			}

			if(channel == null) {
				System.err.println("Channel not found: ${channelName}")
				return
			}
			
			if(loginChannel == null) {
				System.err.println("Login channel not found: ${username}")
				return
			}
			
			
			println "CHANNEL: ${channel}"
			
			if(assetsEnabled) {
				
				if(assetsChannel == null) {
					System.err.println("Assets channel not found: ${assetsChannelName}")
					return
				}
				
				println "ASSETS CHANNEL: ${assetsChannel}"
				
				println "Querying for assets..."
				
				queryForAssets()
				
			} else {
			
				onChannelObtained()
				
			}


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
					dscm.channelsHistory = m.channelsHistory
					if(m.requestURI) {
						dscm.requestURI = m.requestURI
					} else {
						dscm.requestURI = m.URI
					}


					//UserTextMessage utm = new UserTextMessage()
					//utm.text = "I turned on the light."
					//utm.channelURI = channel.URI

					HaleyEmbeddedApp.sendMessage(dscm) { HaleyStatus sendStatus ->

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
					dscm.channelsHistory = m.channelsHistory
					if(m.requestURI) {
						dscm.requestURI = m.requestURI
					} else {
						dscm.requestURI = m.URI
					}

					//UserTextMessage utm = new UserTextMessage()
					//utm.text = "I turned off the light."
					//utm.channelURI = channel.URI

					HaleyEmbeddedApp.sendMessage(dscm) { HaleyStatus sendStatus ->

						println "send text message status: ${sendStatus}"

					}




				}
				if(m.isaType(DeviceStatusRequestMessage)) {


					println "status request"

					def stat = HaleyWemoManager.deviceStatus(wemoDeviceName)
					
					println "state: " + stat
					

					//				if(stat == "on") {

					DeviceStatusMessage dsm = new DeviceStatusMessage()
					dsm.deviceName = wemoDeviceName
					dsm.deviceStatus = stat
					dsm.channelURI = channel.URI
					dsm.channelsHistory = m.channelsHistory
					if(m.requestURI) {
						dsm.requestURI = m.requestURI
					} else {
						dsm.requestURI = m.URI
					}

					//					UserTextMessage utm = new UserTextMessage()
					//					utm.text = "The light is on."
					//					utm.channelURI = channel.URI

					HaleyEmbeddedApp.sendMessage(dsm) { HaleyStatus sendStatus ->

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

					HaleyEmbeddedApp.sendMessage(utm) { HaleyStatus sendStatus ->

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
	
						HaleyEmbeddedApp.sendMessage(utm) { HaleyStatus sendStatus ->
	
							println "send text message status: ${sendStatus}"
	
						}
	
	
					}
					else {
						UserTextMessage utm = new UserTextMessage()
						utm.text = "Yes."
						utm.channelURI = channel.URI
	
						HaleyEmbeddedApp.sendMessage(utm) { HaleyStatus sendStatus ->
	
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

		sendMessage(joinChannelMessage) { HaleyStatus sendStatus ->

			println "join channel message status: ${sendStatus}"

			
			if(assetsEnabled) {
				
				onAssetsListReady()
				
			}
			
		}
		
	}
	
	static void onAssetsListReady() {
		
		if(assetURIs.size() == 0) {
			System.err.println "Assets list is empty"
			return
		}
		
		println "Assets list size: ${assetURIs.size()}"
		
		sendNextMessage()
		
		vertx.setPeriodic(assetsIntervalSeconds * 1000L) { Long timerID ->
			
			sendNextMessage()
			
		}
		
	}
	
	static sendNextMessage() {
		
		println "Sending assets messages..."
		
		for(String assetURI : assetURIs) {
			
			
			
			if(sendAssetLocationMessage.booleanValue()) {
				
				double minLat = 40.72046126415031;
				double maxLat = 40.72176227543701;
				double minLon = -74.00802612304688;
				double maxLon = -73.98433685302734;
				
				double lat = minLat + ( Math.random() * (maxLat - minLat));
				double lon = minLon + ( Math.random() * (maxLon - minLon));
				
				AssetLocationMessage locationMessage = new AssetLocationMessage()
				locationMessage.timestamp = System.currentTimeMillis()
				locationMessage.assetURI = assetURI
				locationMessage.channelURI = assetsChannel.URI
				locationMessage.location = new GeoLocationProperty(lon, lat)
				
				
				
				sendMessage(locationMessage) { HaleyStatus sendStatus ->
					
					println "location ${assetURI} message send status: ${sendStatus}"
					
				}
				
			}
			
			if(sendAssetConditionMessage?.booleanValue()) {
				
				//first obtain the weather condition
				
				def onTemperatureHumidityReady = { Float temperature, Float humidity ->
					
					lastAssetReceivedTimestamp = System.currentTimeMillis()
					
					AssetConditionMessage conditionMessage = new AssetConditionMessage()
					conditionMessage.timestamp = System.currentTimeMillis()
					conditionMessage.assetURI = assetURI
					conditionMessage.channelURI = assetsChannel.URI
					conditionMessage.humidity = humidity
					conditionMessage.temperature = temperature
					
					sendMessage(conditionMessage) { HaleyStatus sendStatus ->
						
						println "condition ${assetURI} message send status: ${sendStatus}"
						
						if(sendStatus.isOk()) {
							lastAssetMessageTimestamp = System.currentTimeMillis()
						}
						
					}
					
					
				}

				if(loginChannel != null) {
					
					IntentMessage intentMessage = new IntentMessage()
					intentMessage.generateURI()
					intentMessage.channelURI = loginChannel.URI
					intentMessage.intent = 'weather'
					
					haleyAPI.sendMessageWithRequestCallback(haleySession, intentMessage, [], { ResultList messageRL ->
						
							println "query results received"

							AIMPMessage res = messageRL.first()
							
							if(!(res instanceof MetaQLResultsMessage)) {
								println("Still waiting for weather results, got " + res.getClass())
								//keep waiting
								return true
							} 
														
							MetaQLResultsMessage resMsg = res
							String status = resMsg.status
							println "weather status: " + status
							if(status != 'ok') {
								System.err.println("Error: " + resMsg.statusMessage)
								return false
							}
						
							List<VITAL_GraphContainerObject> containers = messageRL.iterator(VITAL_GraphContainerObject.class).toList()
							
							if(containers.size() != 1) {
								System.err.println("Error - no weather container object")
								return false
							}
							
							String jsonData = containers.get(0).jsonData
							
							Map weatherObj = new JsonSlurper().parseText(jsonData)

							Map currently = weatherObj.currently
							
							//float t = currently.temperature
							
							//float h = 100f * currently.humidity

														
							float t = HaleyTempHumiditySensorManager.getTemp()
							float h = HaleyTempHumiditySensorManager.getHumidity()		
										
							println "Current NYC temp ${t} humidity ${h}"
							
							onTemperatureHumidityReady(t, h)							
								
						}) { HaleyStatus sendStatus ->
							
							println "weather intent message send status: ${sendStatus}"
							
							if(!sendStatus.isOk()) {
								return
							}
							
						}
					
				} else {
				
					float t = 30f + (float)Math.round(300 * Math.random()) / 10f;
					float h = 60f + (float) Math.round( 400 * Math.random()) / 10f
					println "Using random temp ${t} humidity ${h} values"
					onTemperatureHumidityReady(t, h)
				
				}			
				
				
			}

			
		}

	}
	
	static void sendMessage(AIMPMessage msg, Closure sendStatusClosure) {
		
		synchronized(HaleyEmbeddedApp.class) {
			
			haleyAPI.sendMessage(haleySession, msg, sendStatusClosure)
			
		}
		
	}
			
	
	static void queryForAssets() {
		
		if( ! verifyAssets.booleanValue() ) {
			println "Assets verification skipped - using single asset URI ${assetURI}"
			assetURIs.add(assetURI)
			onChannelObtained()
			return
		}


		
		IntentMessage intent = new IntentMessage().generateURI((VitalApp) null)
		intent.intent = 'details'
		intent.propertyValue = 'device ' + assetURI
		intent.channelURI = loginChannel.URI
		
		haleyAPI.sendMessageWithRequestCallback(haleySession, intent, [], { ResultList messageRL ->
			
				println "query results received"
				
				AIMPMessage msg = messageRL.first()
				
				if(!(msg instanceof MetaQLResultsMessage)) {
					println("Received non metaql results message " + msg.getClass() + " - still waiting for response");
					return true
				}
				
				MetaQLResultsMessage resMsg = msg
				println "device details status: " + resMsg.status
				
				if(resMsg.status?.toString() != 'ok') {
					System.err.println("Error when querying for device: " + resMsg.statusMessage)
					return false;
				}
				
				
				List<Entity> entities = messageRL.iterator(Entity.class).toList()
				if(entities.size() != 1) {
					System.err.println("Received more than 1 device entity")
					return false
				}
				
				Entity deviceEntity = entities.get(0)
				
				println "Device Entity: ${deviceEntity.name}"
				
				assetURIs.add(assetURI)
				
				onChannelObtained()

				return false
									
			}) { HaleyStatus sendStatus ->
				
				println "query message send status: ${sendStatus}"
				
				if(!sendStatus.isOk()) {
					return
				}
				
			}
		
		/*
		if( ! verifyAssets.booleanValue() ) {
			println "Assets verification skipped - using single asset URI ${assetURI}"
			assetURIs.add(assetURI)
			onChannelObtained()
			return
		}
		
		String accountURI = null;
		
		MetaQLMessage queryMessage = new MetaQLMessage().generateURI((VitalApp) null)
		queryMessage.channelURI = assetsChannel.URI

		String uriFilter = '';
		
		if(assetURI != ALL_ASSETS) {
				
			uriFilter = """\
node_constraint { "URI eq ${assetURI}" }
"""
		}
			
			
		queryMessage.queryString =
"""\
GRAPH {

	value segments: '*'
	value offset: 0
	value limit: 1000
	value inlineObjects: false

	ARC {

		node_constraint { "URI eq ${haleySession.authAccount.URI}" }

		ARC {

			value direction: 'reverse'

			edge_constraint { ${Edge_hasUserLogin.class.getCanonicalName()}.class }

			ARC {

				value direction: 'forward'

				edge_constraint { ${Edge_hasAssetNode.class.getCanonicalName()}.class }
	
				node_bind { "asset" }
	
				${uriFilter}
			}

		}
	
	}
					
}
"""

		haleyAPI.sendMessageWithRequestCallback(haleySession, queryMessage, [], { ResultList messageRL ->
		
			println "query results received"
			
			List<MetaQLResultsMessage> resMsgs = messageRL.iterator(MetaQLResultsMessage.class).toList()
			if(resMsgs.size() == 0) {
				System.err.println("No results message object")
				return
			}
			MetaQLResultsMessage resMsg = resMsgs.get(0)
			println "query status: " + resMsg.status
			
			for ( GraphMatch gm : messageRL.iterator(GraphMatch.class) ) {
				
				URIProperty _assetURI = gm.getProperty("asset");
			
				if(_assetURI != null) {
					assetURIs.add(_assetURI.get())
				}
					
			}
			
			if(assetURI != ALL_ASSETS) {

				if(!assetURIs.contains(assetURI)) {
					System.err.println("Asset not found: ${assetURI}")
					return
				}
								
			}
			
			if(assetURIs.size() == 0) {
				System.err.println("No assets found: ${assetURI}")
				return
			}
			
			onChannelObtained()
				
		}) { HaleyStatus sendStatus ->
			
			println "query message send status: ${sendStatus}"
			
			if(!sendStatus.isOk()) {
				return
			}
			
		}
		*/
				
	}


}
