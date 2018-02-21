package ai.haley.assistant

import ai.haley.api.HaleyAPI
import ai.haley.api.session.HaleySession;
import ai.haley.api.session.HaleyStatus
import ai.haley.assistant.SpeechRecognitionThread.SpeechRecognitionListener;
import ai.vital.domain.FileNode
import ai.vital.service.vertx3.binary.ResponseMessage;
import ai.vital.service.vertx3.websocket.VitalServiceAsyncWebsocketClient
import ai.vital.vitalservice.VitalStatus;
import ai.vital.vitalservice.query.ResultList;
import ai.vital.vitalsigns.model.VitalApp

import com.vitalai.aimp.domain.AIMPMessage
import com.vitalai.aimp.domain.AudioObject;
import com.vitalai.aimp.domain.Channel
import com.vitalai.aimp.domain.HaleyAudioMessage
import com.vitalai.aimp.domain.UserTextMessage;

//import io.vertx.groovy.core.Vertx
import io.vertx.core.Vertx
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener
import jline.TerminalFactory;
import jline.TerminalFactory.Type
import jline.console.ConsoleReader;

import java.io.FileInputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger
import org.slf4j.LoggerFactory; 

class HaleyAssistant implements SpeechRecognitionListener {

	private final static Logger log = LoggerFactory.getLogger(HaleyAssistant.class)  
	
	String appID
	String endpointURL
	String username
	String password
	String channelName
	
	private VitalApp app
	
	private Vertx vertx 
	
	private VitalServiceAsyncWebsocketClient websocketClient
	
	private HaleyAPI haleyAPI
	
	private HaleySession haleySession
	
	private Channel channel
	
	private ConsoleReader reader
	
	private PrintWriter out
	
	private SpeechRecognitionThread speechRecognitionThread
	
	public HaleyAssistant(String appID, String endpointURL, String username, String password, String channelName) {
		super();
		this.appID = appID;
		this.endpointURL = endpointURL;
		this.username = username;
		this.password = password;
		this.channelName = channelName
	}


	public void onError(String error) {
		System.err.println(error)
		System.exit(1)
	}

	public void run() {
		
		app = VitalApp.withId(appID)
			
		vertx = Vertx.vertx()
					
		websocketClient = new VitalServiceAsyncWebsocketClient(vertx, app, 'endpoint.', endpointURL)
			
		websocketClient.connect({ Throwable exception ->
				
			if(exception) {
				exception.printStackTrace()
				onError(exception.localizedMessage)
				return
			}
				
			haleyAPI = new HaleyAPI(websocketClient)
				
				onHaleyAPIConnected()
				
		}, {Integer attempts ->
			onError("FAILED, attempts: ${attempts}")
		})
		
	}

	private void onHaleyAPIConnected() {
	
		haleyAPI.openSession() { String errorMessage,  HaleySession session ->
			
			haleySession = session
			
			if(errorMessage) {
				onError(errorMessage)
				return
			}
			
			println "Session opened ${session.toString()}"
			
			println "Sessions: " + haleyAPI.getSessions().size()
			
			onSessionReady()
			
		}
			
	}
	
	private void onSessionReady() {
		
		haleyAPI.authenticateSession(haleySession, username, password) { HaleyStatus status ->
			
			println "auth status: ${status}"

			if(!status.ok()) {
				onError(status.errorMessage)
				return
			}
			
			println "session: ${haleySession.toString()}"
			
			onHaleyReady()
			
		}
		
	}
	
	private void onHaleyReady() {
		
		haleyAPI.listChannels(haleySession) { String error, List<Channel> channels ->
			
			for(Channel ch : channels) {
				
				if(ch.name?.toString() == channelName) {
					channel = ch
				}
				
			}
			
			if(channel == null) {
				onError("Channel not found: ${channelName}")
				return
			}
			
			onChannelReady()
			
		}
	}

	private void onChannelReady() {
		
		HaleyStatus registerStatus = haleyAPI.registerDefaultCallback({ResultList msgRL -> 
			
			AIMPMessage msg = msgRL.first()
			
			String channelURI = msg.channelURI
			
			if(channelURI == channel.URI) {
				onAIMPMessage(msgRL)
			} else {
				println "Ignoring message to other channel ${channelURI}"
			}
			
		})
		
		if(!registerStatus.isOk()) {
			onError("Couldn't register default callback: " + registerStatus.errorMessage)
			return
		}
		
		startTerminalThread()
		
	}
	
	private void startTerminalThread() {
		
		Thread t = new Thread(){
			
			@Override
			void run() {
				
				println "type / or /help for help"
				
				Type terminalType = Type.UNIX
				TerminalFactory.configure(terminalType)
				
				reader = new ConsoleReader()
				reader.setPrompt("haley> ")
				
				out = new PrintWriter(reader.getOutput());
				
				String line = null
				
				while ((line = reader.readLine()) != null) {
					
					line = line.trim()
					
					if(line.isEmpty()) continue
					
					out.println("======> " + line);
					out.flush();
					
					if(line.equals('/') || line.equals('/help')) {
						out.println("\ncommands:")
						out.println("/            displays help");
						out.println("/help        displays help");
						out.println("/quit        exit the assistant");
						out.println("/micstart    start capturing phrase from microphone")
						out.println("/micstop     stop capturing phrase from microphone")
						out.flush();
						continue;
					}
					
					if(line.equals("/quit")) {
						break
					}
					
					if(line.equals("/micstart")) {
						startMicrophone()
						continue
					}
					
					if(line.equals("/micstop")) {
						stopMicrophone()
						continue
					}
					
					
					UserTextMessage utm = new UserTextMessage()
					utm.channelURI = channel.URI
					utm.text = line
					
					//send the message now
					haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
						
						if(!sendStatus.isOk()) {
							System.err.println "Error when sending the text message: ${sendStatus.errorMessage}"
						}
						
					}
					
					
				}
				
				println "quiting now..."
		
				websocketClient.close ({ ResponseMessage closeRes ->
					
				})
				
				vertx.close()
				
				Thread.sleep(1000)
		
				reader.close()
				
			};
		}
		
		t.start()
		

		
	}
	
	private void onAIMPMessage(ResultList msgRL) {
		
		AIMPMessage msg = msgRL.first()
		
		
		
		reader.redrawLine();
		reader.getOutput().flush();
		
		println ""
		
		println msg.getClass().getSimpleName() + " " + ( msg.text != null ? msg.text : '')
		
		
//		reader.setPrompt(newPrompt);
		//an actor that sets prompt
		reader.redrawLine();
		reader.moveCursor(reader.getCursorBuffer().length());
		reader.getOutput().flush();
		
		
		if(msg instanceof HaleyAudioMessage) {
			
			List<AudioObject> audioObjects = msgRL.iterator(AudioObject.class).toList()
			List<FileNode> audioFileNodes = msgRL.iterator(FileNode.class).toList()
			
			if(audioObjects.size() > 0) {
				
				//just play the first audio object
				AudioObject audioObject = audioObjects.get(0)
				FileNode fileNode = audioFileNodes.size() > 0 ? audioFileNodes.get(0) : null
				
				String sourceURL = fileNode != null ? haleyAPI.getFileNodeDownloadURL(haleySession, fileNode) : audioObject.url
				
				log.info("Playing audio from URL: ${sourceURL}")
				
				
				Thread playerThread = new Thread(){
					
					@Override
					public void run(){
						
						InputStream inputStream = null
						
						try {
							
							inputStream = new URL(sourceURL).openStream()
							
							AdvancedPlayer player = new AdvancedPlayer(inputStream);
							
							final Object mutex = new Object();
							AtomicBoolean playing = new AtomicBoolean(false);
							
							/*
							player.setPlayBackListener(new PlaybackListener() {
					  
							  @Override
							  public void playbackFinished(PlaybackEvent evt) {
								  System.out.println(new Date() + " Playback finished");
								  synchronized (mutex) {
									  mutex.notifyAll();
								  }
							  }
					  
							  @Override
							  public void playbackStarted(PlaybackEvent evt) {
								  System.out.println(new Date() + " Playback started");
								  playing.set(true);
							  }
								
							});
							*/
							player.play();
							
							player.close();
//							log.info("Played the sample from ${sourceURL}")
							
						} catch(Exception e) {
							log.error(e.localizedMessage)
						}
										
					}
				}
				
				playerThread.start()
				
			} else {
			
				log.warn("No audio objects in haley audio message")
			
			}
			
			
		} else {
		
//			println "Ignoring other messages on this channel: " + msg.getClass().canonicalName
		
		}
		
	}

	
	void startMicrophone() {
		
		if(this.speechRecognitionThread != null) {
			out.println("RECOGNITION ALREADY IN PROGRESS")
			out.flush()
			return
		}
		
		this.speechRecognitionThread = new SpeechRecognitionThread(this)
		this.speechRecognitionThread.start()
		
	}	

	void stopMicrophone() {

		if(this.speechRecognitionThread != null) {
			this.speechRecognitionThread.cancel()
		} else {
			out.println("NO RECOGNITION IN PROGRESS")
		}
		
				
	}

	@Override
	public void onStarted() {
		//nop
	}


	@Override
	public void onComplete(String phrase) {
		
		if(phrase) {
			out.println("PHRASE: " + phrase)
			
			UserTextMessage utm = new UserTextMessage()
			utm.channelURI = channel.URI
			utm.text = phrase
			
			//send the message now
			haleyAPI.sendMessage(haleySession, utm) { HaleyStatus sendStatus ->
				
				if(!sendStatus.isOk()) {
					System.err.println "Error when sending the text message: ${sendStatus.errorMessage}"
				}
				
			}
			
		} else {
			out.println("NO PHRASE DETECTED")
		}
		out.flush();
		this.speechRecognitionThread = null
	}


	@Override
	public void onError(Throwable error) {

		out.println("SPEECH RECOGNITION ERROR")
		out.flush()
		
		// TODO Auto-generated method stub
		System.err.println("Speech recognition error: " + error.getLocalizedMessage())
		error.printStackTrace()
		
		this.speechRecognitionThread = null
		
		
		
	}
	
	
}
