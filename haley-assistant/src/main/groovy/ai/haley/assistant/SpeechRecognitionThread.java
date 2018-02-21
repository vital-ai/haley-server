package ai.haley.assistant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

import net.sourceforge.javaflacencoder.FLACFileWriter;

public class SpeechRecognitionThread extends Thread {

	SpeechRecognitionListener listener;

	boolean active = true;
	
	private ApiStreamObserver<StreamingRecognizeRequest> requestObserver;

	private TargetDataLine targetLine = null;
	
	private SpeechClient speech = null;
	
	public void cleanup() {
		if(targetLine != null) {
			try {
				targetLine.close();
			} catch(Exception e) {
				
			}
			targetLine = null;
		}
		
		if(requestObserver != null) {
			try {
				requestObserver.onCompleted();
			} catch(Exception e) {
				
			}
			requestObserver = null;
		}
		
		if(speech != null) {
			try {
				speech.close();
			} catch(Exception e){
				
			}
			
			speech = null;
		}
		
	}
	
	public void cancel() {
		this.active = false;
	}
	
	
	public SpeechRecognitionThread(SpeechRecognitionListener listener) {
		super();
		if (listener == null)
			throw new NullPointerException("Listener is required");
		this.listener = listener;
	}

	public static interface SpeechRecognitionListener {

		public void onStarted();

		/**
		 * @param phrase
		 *            or null if not phrase / stopped
		 */
		public void onComplete(String phrase);

		public void onError(Throwable error);

	}
	
	@Override
	public void run() {

		try {

			speech = SpeechClient.create();

			// little endian!

			boolean useFLAC = true;
			boolean interimResults = false;
			
			int sampleRate = 16000;
			int sampleSizeBits = 16;
			int channelsCount = 1;
			
			AudioFormat format = new AudioFormat(sampleRate, sampleSizeBits, channelsCount, true, false);

			// Configure request with local raw PCM audio
			RecognitionConfig recConfig = RecognitionConfig.newBuilder()
					.setEncoding(useFLAC ? AudioEncoding.FLAC : AudioEncoding.LINEAR16).setLanguageCode("en-US")
					.setSampleRateHertz(sampleRate).build();
			StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder().setInterimResults(interimResults)
					.setConfig(recConfig).build();

			class ResponseApiStreamingObserver implements ApiStreamObserver<StreamingRecognizeResponse> {
				private final SettableFuture<List<StreamingRecognizeResponse>> future = SettableFuture.create();
				private final List<StreamingRecognizeResponse> messages = new java.util.ArrayList<StreamingRecognizeResponse>();

				@Override
				public void onNext(StreamingRecognizeResponse message) {
					System.out.print("ON NEXT MESSAGE " + message);
					messages.add(message);
					
					for( StreamingRecognitionResult result : message.getResultsList() ) {
						if(result.getIsFinal()) {
							List<SpeechRecognitionAlternative> alternativesList = result.getAlternativesList();
							if(alternativesList.size() > 0) {
								SpeechRecognitionAlternative alternative = alternativesList.get(0);
								String transcript = alternative.getTranscript();
								if(listener != null) {
									listener.onComplete(transcript);
									listener = null;
								}
								active = false;
							}
						}
					}
				}

				@Override
				public void onError(Throwable t) {
					System.out.flush();
					System.out.println("ON ERROR " + t.getLocalizedMessage() + " " + t.getClass().getCanonicalName());
					future.setException(t);
					if(listener != null) {
						listener.onError(t);
						listener = null;
					}
					active = false;
//					cleanup();
				}

				@Override
				public void onCompleted() {
					System.out.print("ON COMPLETED");
					future.set(messages);
					if(listener != null) {
						listener.onComplete(null);
						listener = null;
					}
					active = false;
				}

			}

			ResponseApiStreamingObserver responseObserver = new ResponseApiStreamingObserver();

			BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable = speech
					.streamingRecognizeCallable();

			requestObserver = callable.bidiStreamingCall(responseObserver);

			// The first request must **only** contain the audio configuration:
			requestObserver.onNext(StreamingRecognizeRequest.newBuilder().setStreamingConfig(config).build());

			// // Subsequent requests must **only** contain the audio data.
			// requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
			// .setAudioContent(ByteString.copyFrom(data))
			// .build());

			DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);

			targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
			targetLine.open(format);
			targetLine.start();

			// read stream and wait

			//this buffer size == sampleRate, this should send data every quarter of a second
			
			
			byte[] buffer = new byte[targetLine.getBufferSize() / 2];
			int read = -1;
			int p = 0;
			
			//TODO timeout, use partial results to reset counter etc
			long millisPerBatch = 250L;
			long timeout = 30000;
			
			// AudioInputStream ais = new AudioInputStream(targetLine);
			FLACFileWriter ffw = useFLAC ? new FLACFileWriter() : null;

			while (active && (read = targetLine.read(buffer, 0, buffer.length)) >= 0) {

				System.out.println(new Date().toString() + " sending packet " + ++p + " length " + read + " bytes");

				byte[] flacData = null;

				if (useFLAC) {

					AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(buffer, 0, read), format,
							read / 2);
					ByteArrayOutputStream os = new ByteArrayOutputStream();
					ffw.write(ais, FLACFileWriter.FLAC, os);
					flacData = os.toByteArray();
					System.out.println(new Date().toString() + " compressed packet " + p + " length " + flacData.length
							+ " bytes");

				}

				if(requestObserver != null) {
				requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
						.setAudioContent(
								flacData != null ? ByteString.copyFrom(flacData) : ByteString.copyFrom(buffer, 0, read))
						.build());
				}

			}


			System.out.println("No more data to send");
			
			if(listener != null) {
				listener.onComplete(null);
				listener = null;
			}
			
			cleanup();

			
		} catch (Exception e) {
			e.printStackTrace();
			if(this.listener != null) {
				this.listener.onError(e);
				this.listener = null;
			} 
		}

	}

}
