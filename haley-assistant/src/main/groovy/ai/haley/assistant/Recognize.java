package ai.haley.assistant;


import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.cloud.speech.v1.WordInfo;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;
import net.sourceforge.javaflacencoder.FLACFileWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

public class Recognize {
  public static void main(String... args) throws Exception {
   
	  /*
	  if (args.length < 1) {
      System.out.println("Usage:");
      System.out.printf(
          "\tjava %s \"<command>\" \"<path-to-image>\"\n"
          + "Commands:\n"
          + "\tsyncrecognize | asyncrecognize | streamrecognize | wordoffsets | streamrecognize2 | micrecognize | playaudio \n"
          + "Path:\n\tA file path (ex: ./resources/audio.raw) or a URI "
          + "for a Cloud Storage resource (gs://...)\n",
          Recognize.class.getCanonicalName());
      return;
    }
    */
	  
	  String command = "micrecognize";
	  
	 // String command = "playaudio";
	  
    
    //String command = args[0];
	  
	  
    //String path = args.length > 1 ? args[1] : "";

    String path = "/Users/hadfield/Desktop/speech_20180213172645355.mp3";
    
    
    
    // Use command and GCS path pattern to invoke transcription.
    if (command.equals("syncrecognize")) {
      if (path.startsWith("gs://")) {
        syncRecognizeGcs(path);
      } else {
        syncRecognizeFile(path);
      }
    } else if (command.equals("wordoffsets")) {
      if (path.startsWith("gs://")) {
        asyncRecognizeWords(path);
      } else {
        syncRecognizeWords(path);
      }
    } else if (command.equals("asyncrecognize")) {
      if (path.startsWith("gs://")) {
        asyncRecognizeGcs(path);
      } else {
        asyncRecognizeFile(path);
      }
    } else if (command.equals("streamrecognize")) {
      streamingRecognizeFile(path);
    } else if(command.equals("streamrecognize2")) {
      streamingRecognizeFile2(path);
    } else if(command.equals("micrecognize")) {
      streamingMicrophone();
    } else if(command.equals("playaudio")) {
    	playAudio(path);
    }

  }

  /**
   * Performs speech recognition on raw PCM audio and prints the transcription.
   *
   * @param fileName the path to a PCM audio file to transcribe.
   */
  public static void syncRecognizeFile(String fileName) throws Exception, IOException {
    SpeechClient speech = SpeechClient.create();

    Path path = Paths.get(fileName);
    byte[] data = Files.readAllBytes(path);
    ByteString audioBytes = ByteString.copyFrom(data);

    // Configure request with local raw PCM audio
    RecognitionConfig config = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.LINEAR16)
        .setLanguageCode("en-US")
        .setSampleRateHertz(16000)
        .build();
    RecognitionAudio audio = RecognitionAudio.newBuilder()
        .setContent(audioBytes)
        .build();

    // Use blocking call to get audio transcript
    RecognizeResponse response = speech.recognize(config, audio);
    List<SpeechRecognitionResult> results = response.getResultsList();

    for (SpeechRecognitionResult result: results) {
      // There can be several alternative transcripts for a given chunk of speech. Just use the
      // first (most likely) one here.
      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      System.out.printf("Transcription: %s%n", alternative.getTranscript());
    }
    speech.close();
  }

  /**
   * Performs sync recognize and prints word time offsets.
   *
   * @param fileName the path to a PCM audio file to transcribe get offsets on.
   */
  public static void syncRecognizeWords(String fileName) throws Exception, IOException {
    SpeechClient speech = SpeechClient.create();

    Path path = Paths.get(fileName);
    byte[] data = Files.readAllBytes(path);
    ByteString audioBytes = ByteString.copyFrom(data);

    // Configure request with local raw PCM audio
    RecognitionConfig config = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.LINEAR16)
        .setLanguageCode("en-US")
        .setSampleRateHertz(16000)
        .setEnableWordTimeOffsets(true)
        .build();
    RecognitionAudio audio = RecognitionAudio.newBuilder()
        .setContent(audioBytes)
        .build();

    // Use blocking call to get audio transcript
    RecognizeResponse response = speech.recognize(config, audio);
    List<SpeechRecognitionResult> results = response.getResultsList();

    for (SpeechRecognitionResult result: results) {
      // There can be several alternative transcripts for a given chunk of speech. Just use the
      // first (most likely) one here.
      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      System.out.printf("Transcription: %s%n", alternative.getTranscript());
      for (WordInfo wordInfo: alternative.getWordsList()) {
        System.out.println(wordInfo.getWord());
        System.out.printf("\t%s.%s sec - %s.%s sec\n",
            wordInfo.getStartTime().getSeconds(),
            wordInfo.getStartTime().getNanos() / 100000000,
            wordInfo.getEndTime().getSeconds(),
            wordInfo.getEndTime().getNanos() / 100000000);
      }
    }
    speech.close();
  }


  /**
   * Performs speech recognition on remote FLAC file and prints the transcription.
   *
   * @param gcsUri the path to the remote FLAC audio file to transcribe.
   */
  public static void syncRecognizeGcs(String gcsUri) throws Exception, IOException {
    // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
    SpeechClient speech = SpeechClient.create();

    // Builds the request for remote FLAC file
    RecognitionConfig config = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.FLAC)
        .setLanguageCode("en-US")
        .setSampleRateHertz(16000)
        .build();
    RecognitionAudio audio = RecognitionAudio.newBuilder()
        .setUri(gcsUri)
        .build();

    // Use blocking call for getting audio transcript
    RecognizeResponse response = speech.recognize(config, audio);
    List<SpeechRecognitionResult> results = response.getResultsList();

    for (SpeechRecognitionResult result: results) {
      // There can be several alternative transcripts for a given chunk of speech. Just use the
      // first (most likely) one here.
      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      System.out.printf("Transcription: %s%n", alternative.getTranscript());
    }
    speech.close();
  }

  /*
  /**
   * Performs non-blocking speech recognition on raw PCM audio and prints
   * the transcription. Note that transcription is limited to 60 seconds audio.
   *
   * @param fileName the path to a PCM audio file to transcribe.
   */
  public static void asyncRecognizeFile(String fileName) throws Exception, IOException {
    // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
    SpeechClient speech = SpeechClient.create();

    Path path = Paths.get(fileName);
    byte[] data = Files.readAllBytes(path);
    ByteString audioBytes = ByteString.copyFrom(data);

    // Configure request with local raw PCM audio
    RecognitionConfig config = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.LINEAR16)
        .setLanguageCode("en-US")
        .setSampleRateHertz(16000)
        .build();
    RecognitionAudio audio = RecognitionAudio.newBuilder()
        .setContent(audioBytes)
        .build();

    // Use non-blocking call for getting file transcription
    OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
        speech.longRunningRecognizeAsync(config, audio);

    while (!response.isDone()) {
      System.out.println("Waiting for response...");
      Thread.sleep(10000);
    }

    List<SpeechRecognitionResult> results = response.get().getResultsList();

    for (SpeechRecognitionResult result: results) {
      // There can be several alternative transcripts for a given chunk of speech. Just use the
      // first (most likely) one here.
      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      System.out.printf("Transcription: %s%n", alternative.getTranscript());
    }
    speech.close();
  }

  /**
   * Performs non-blocking speech recognition on remote FLAC file and prints
   * the transcription as well as word time offsets.
   *
   * @param gcsUri the path to the remote LINEAR16 audio file to transcribe.
   */
  public static void asyncRecognizeWords(String gcsUri) throws Exception, IOException {
    // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
    SpeechClient speech = SpeechClient.create();

    // Configure remote file request for Linear16
    RecognitionConfig config = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.FLAC)
        .setLanguageCode("en-US")
        .setSampleRateHertz(16000)
        .setEnableWordTimeOffsets(true)
        .build();
    RecognitionAudio audio = RecognitionAudio.newBuilder()
        .setUri(gcsUri)
        .build();

    // Use non-blocking call for getting file transcription
    OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
        speech.longRunningRecognizeAsync(config, audio);
    while (!response.isDone()) {
      System.out.println("Waiting for response...");
      Thread.sleep(10000);
    }

    List<SpeechRecognitionResult> results = response.get().getResultsList();

    for (SpeechRecognitionResult result: results) {
      // There can be several alternative transcripts for a given chunk of speech. Just use the
      // first (most likely) one here.
      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      System.out.printf("Transcription: %s\n",alternative.getTranscript());
      for (WordInfo wordInfo: alternative.getWordsList()) {
        System.out.println(wordInfo.getWord());
        System.out.printf("\t%s.%s sec - %s.%s sec\n",
            wordInfo.getStartTime().getSeconds(),
            wordInfo.getStartTime().getNanos() / 100000000,
            wordInfo.getEndTime().getSeconds(),
            wordInfo.getEndTime().getNanos() / 100000000);
      }
    }
    speech.close();
  }

  /**
   * Performs non-blocking speech recognition on remote FLAC file and prints
   * the transcription.
   *
   * @param gcsUri the path to the remote LINEAR16 audio file to transcribe.
   */
  public static void asyncRecognizeGcs(String gcsUri) throws Exception, IOException {
    // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
    SpeechClient speech = SpeechClient.create();

    // Configure remote file request for Linear16
    RecognitionConfig config = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.FLAC)
        .setLanguageCode("en-US")
        .setSampleRateHertz(16000)
        .build();
    RecognitionAudio audio = RecognitionAudio.newBuilder()
        .setUri(gcsUri)
        .build();

    // Use non-blocking call for getting file transcription
    OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
        speech.longRunningRecognizeAsync(config, audio);
    while (!response.isDone()) {
      System.out.println("Waiting for response...");
      Thread.sleep(10000);
    }

    List<SpeechRecognitionResult> results = response.get().getResultsList();

    for (SpeechRecognitionResult result: results) {
      // There can be several alternative transcripts for a given chunk of speech. Just use the
      // first (most likely) one here.
      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      System.out.printf("Transcription: %s\n",alternative.getTranscript());
    }
    speech.close();
  }


  /**
   * Performs streaming speech recognition on raw PCM audio data.
   *
   * @param fileName the path to a PCM audio file to transcribe.
   */
  public static void streamingRecognizeFile(String fileName) throws Exception, IOException {
    Path path = Paths.get(fileName);
    byte[] data = Files.readAllBytes(path);

    // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
    SpeechClient speech = SpeechClient.create();

    // Configure request with local raw PCM audio
    RecognitionConfig recConfig = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.LINEAR16)
        .setLanguageCode("en-US")
        .setSampleRateHertz(16000)
        .build();
    StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder()
        .setConfig(recConfig)
        .build();

    class ResponseApiStreamingObserver<T> implements ApiStreamObserver<T> {
      private final SettableFuture<List<T>> future = SettableFuture.create();
      private final List<T> messages = new java.util.ArrayList<T>();

      @Override
      public void onNext(T message) {
        messages.add(message);
      }

      @Override
      public void onError(Throwable t) {
        future.setException(t);
      }

      @Override
      public void onCompleted() {
        future.set(messages);
      }

      // Returns the SettableFuture object to get received messages / exceptions.
      public SettableFuture<List<T>> future() {
        return future;
      }
    }

    ResponseApiStreamingObserver<StreamingRecognizeResponse> responseObserver =
        new ResponseApiStreamingObserver<StreamingRecognizeResponse>();

    BidiStreamingCallable<StreamingRecognizeRequest,StreamingRecognizeResponse> callable =
        speech.streamingRecognizeCallable();

    ApiStreamObserver<StreamingRecognizeRequest> requestObserver =
        callable.bidiStreamingCall(responseObserver);

    // The first request must **only** contain the audio configuration:
    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
        .setStreamingConfig(config)
        .build());

    // Subsequent requests must **only** contain the audio data.
    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
        .setAudioContent(ByteString.copyFrom(data))
        .build());

    // Mark transmission as completed after sending the data.
    requestObserver.onCompleted();

    List<StreamingRecognizeResponse> responses = responseObserver.future().get();

    for (StreamingRecognizeResponse response: responses) {
      // For streaming recognize, the results list has one is_final result (if available) followed
      // by a number of in-progress results (if iterim_results is true) for subsequent utterances.
      // Just print the first result here.
      StreamingRecognitionResult result = response.getResultsList().get(0);
      // There can be several alternative transcripts for a given chunk of speech. Just use the
      // first (most likely) one here.
      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
      System.out.println(alternative.getTranscript());
    }
    speech.close();
  }

  
  /**
   * Performs streaming speech recognition on raw PCM audio data.
   *
   * @param fileName the path to a PCM audio file to transcribe.
   */
  public static void streamingRecognizeFile2(String fileName) throws Exception, IOException {
//    Path path = Paths.get(fileName);
//    byte[] data = Files.readAllBytes(path);

    // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
    SpeechClient speech = SpeechClient.create();

    // Configure request with local raw PCM audio
    RecognitionConfig recConfig = RecognitionConfig.newBuilder()
        .setEncoding(AudioEncoding.LINEAR16)
        .setLanguageCode("en-US")
        .setSampleRateHertz(22050)
        .build();
    StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder()
    	.setInterimResults(true)
        .setConfig(recConfig)
        .build();

    class ResponseApiStreamingObserver implements ApiStreamObserver<StreamingRecognizeResponse> {
      private final SettableFuture<List<StreamingRecognizeResponse>> future = SettableFuture.create();
      private final List<StreamingRecognizeResponse> messages = new java.util.ArrayList<StreamingRecognizeResponse>();

      @Override
      public void onNext(StreamingRecognizeResponse message) {
    	  System.out.print("ON NEXT MESSAGE " + message);
    	  
//    	  for(StreamingRecognitionResult r: message.getResultsList() ) {
//    		  if(r.getIsFinal()
//    	  }
    	  
    	  messages.add(message);
      }

      @Override
      public void onError(Throwable t) {
    	  System.out.print("ON ERROR " + t.getLocalizedMessage() + " " + t.getClass().getCanonicalName());
        future.setException(t);
      }

      @Override
      public void onCompleted() {
    	  System.out.print("ON COMPLETED");
        future.set(messages);
      }

    }

    ResponseApiStreamingObserver responseObserver =
        new ResponseApiStreamingObserver();

    BidiStreamingCallable<StreamingRecognizeRequest,StreamingRecognizeResponse> callable =
        speech.streamingRecognizeCallable();

    ApiStreamObserver<StreamingRecognizeRequest> requestObserver =
        callable.bidiStreamingCall(responseObserver);

    // The first request must **only** contain the audio configuration:
    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
        .setStreamingConfig(config)
        .build());
    
//    // Subsequent requests must **only** contain the audio data.
//    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
//        .setAudioContent(ByteString.copyFrom(data))
//        .build());

    
    
    //read stream and wait
    InputStream stream = new FileInputStream(fileName);
    
    byte[] buffer = new byte[44100];
    int read = -1;
    int p = 0;
    
    boolean first = true;
    
    while((read = stream.read(buffer)) >= 0) {

    	if(first) {
    		first = false;
    	} else {
    		System.out.println("Waiting 1 second");
       		Thread.sleep(1000);
    	}
    	
    	System.out.println("Sending packet " + ++p + " length " + read + " bytes");
    	
    	requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
    		.setAudioContent(ByteString.copyFrom(buffer, 0, read))
    		.build());
    	
    }
    
    requestObserver.onCompleted();
    
    System.out.println("No more data to send");

//    // Subsequent requests must **only** contain the audio data.
//    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
//        .setAudioContent(ByteString.copyFrom(data))
//        .build());
//
//    // Mark transmission as completed after sending the data.
//    requestObserver.onCompleted();
//
//    List<StreamingRecognizeResponse> responses = responseObserver.future().get();
//
//    for (StreamingRecognizeResponse response: responses) {
//      // For streaming recognize, the results list has one is_final result (if available) followed
//      // by a number of in-progress results (if iterim_results is true) for subsequent utterances.
//      // Just print the first result here.
//      StreamingRecognitionResult result = response.getResultsList().get(0);
//      // There can be several alternative transcripts for a given chunk of speech. Just use the
//      // first (most likely) one here.
//      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
//      System.out.println(alternative.getTranscript());
//    }
    
    Thread.sleep(1000);
    
    speech.close();
    stream.close();
  }
  

  /**
   * Performs streaming speech recognition on raw PCM audio data.
   *
   * @param fileName the path to a PCM audio file to transcribe.
   */
  public static void streamingMicrophone() throws Exception, IOException {
//    Path path = Paths.get(fileName);
//    byte[] data = Files.readAllBytes(path);

    // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
    SpeechClient speech = SpeechClient.create();

    //little endian!
    AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
    
    
    boolean useFLAC = true;
    
    // Configure request with local raw PCM audio
    RecognitionConfig recConfig = RecognitionConfig.newBuilder()
        .setEncoding(useFLAC ? AudioEncoding.FLAC : AudioEncoding.LINEAR16)
        .setLanguageCode("en-US")
        .setSampleRateHertz(16000)
        .build();
    StreamingRecognitionConfig config = StreamingRecognitionConfig.newBuilder()
    	.setInterimResults(true)
        .setConfig(recConfig)
        .build();

    class ResponseApiStreamingObserver implements ApiStreamObserver<StreamingRecognizeResponse> {
      private final SettableFuture<List<StreamingRecognizeResponse>> future = SettableFuture.create();
      private final List<StreamingRecognizeResponse> messages = new java.util.ArrayList<StreamingRecognizeResponse>();

      @Override
      public void onNext(StreamingRecognizeResponse message) {
    	  System.out.print("ON NEXT MESSAGE " + message);
    	  messages.add(message);
      }

      @Override
      public void onError(Throwable t) {
    	  System.out.print("ON ERROR " + t.getLocalizedMessage() + " " + t.getClass().getCanonicalName());
        future.setException(t);
      }

      @Override
      public void onCompleted() {
    	  System.out.print("ON COMPLETED");
        future.set(messages);
      }

    }

    ResponseApiStreamingObserver responseObserver =
        new ResponseApiStreamingObserver();

    BidiStreamingCallable<StreamingRecognizeRequest,StreamingRecognizeResponse> callable =
        speech.streamingRecognizeCallable();

    ApiStreamObserver<StreamingRecognizeRequest> requestObserver =
        callable.bidiStreamingCall(responseObserver);

    // The first request must **only** contain the audio configuration:
    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
        .setStreamingConfig(config)
        .build());
    
    
//    // Subsequent requests must **only** contain the audio data.
//    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
//        .setAudioContent(ByteString.copyFrom(data))
//        .build());

    
	DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);
    
	TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
	targetLine.open(format);
	targetLine.start();
    
    //read stream and wait
    
    byte[] buffer = new byte[targetLine.getBufferSize()];
    int read = -1;
    int p = 0;
    
//    AudioInputStream ais = new AudioInputStream(targetLine);
    FLACFileWriter ffw = useFLAC ? new FLACFileWriter() : null;
    
    while((read = targetLine.read(buffer, 0, buffer.length)) >= 0) {

    	System.out.println(new Date().toString() + " sending packet " + ++p + " length " + read + " bytes");

    	byte[] flacData = null; 
    	
    	if(useFLAC) {
    		
    		AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(buffer, 0 , read), format, read / 2);
    		ByteArrayOutputStream os = new ByteArrayOutputStream();
    		ffw.write(ais, FLACFileWriter.FLAC, os);
    		flacData = os.toByteArray();
    		System.out.println(new Date().toString() + " compressed packet " + p + " length " + flacData.length + " bytes");
    		
    	}
    	
    	requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
    		.setAudioContent(flacData != null ? ByteString.copyFrom(flacData) : ByteString.copyFrom(buffer, 0, read))
    		.build());
    	
    }
    
    targetLine.close();
    
    requestObserver.onCompleted();
    
    System.out.println("No more data to send");

//    // Subsequent requests must **only** contain the audio data.
//    requestObserver.onNext(StreamingRecognizeRequest.newBuilder()
//        .setAudioContent(ByteString.copyFrom(data))
//        .build());
//
//    // Mark transmission as completed after sending the data.
//    requestObserver.onCompleted();
//
//    List<StreamingRecognizeResponse> responses = responseObserver.future().get();
//
//    for (StreamingRecognizeResponse response: responses) {
//      // For streaming recognize, the results list has one is_final result (if available) followed
//      // by a number of in-progress results (if iterim_results is true) for subsequent utterances.
//      // Just print the first result here.
//      StreamingRecognitionResult result = response.getResultsList().get(0);
//      // There can be several alternative transcripts for a given chunk of speech. Just use the
//      // first (most likely) one here.
//      SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
//      System.out.println(alternative.getTranscript());
//    }
    
    Thread.sleep(1000);
    
    speech.close();
  }

  public static void playAudio(String path) throws FileNotFoundException, JavaLayerException, InterruptedException {
	  
	  AdvancedPlayer player = new AdvancedPlayer(new FileInputStream(path));
	  
	  final Object mutex = new Object();
	  AtomicBoolean playing = new AtomicBoolean(false);
	  
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
	  
	  player.play();

	  /*
	  Thread.sleep(100);
	  
	  if(!playing.get()) {
		  System.err.println("Playback hasn't started soon enough");
		  return;
	  }
	  
	  synchronized (mutex) {
		  System.out.println("Waiting for playback to finish");
		  mutex.wait();
	  }
	  
	  System.out.println(new Date() + " PLAYBACK DONE");
	  
	  */
	  
  }
}
