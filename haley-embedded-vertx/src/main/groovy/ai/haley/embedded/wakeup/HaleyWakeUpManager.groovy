package ai.haley.embedded.wakeup

class HaleyWakeUpManager {

	
	// spawn external process in thread
	// redirect stdio input
	// when receive a line, send wakeup signal
	// wakeup signal puts webapp into listening mode
	
	// trigger recording and sending to service for full speech-to-text
	
	// or turn on the google web speech-to-text, like so:
	// https://www.google.com/intl/en/chrome/demos/speech.html
	// if we can make the web browser and local server not fight over audio hardware
	// potentially we could disable the wake-word process while the 
	// primary speech-to-text is engaged.
	// (test this on raspberry pi + chromium)
	
	
	// recording occurs on the "server" side to control hardware?
	// (don't want web browser and server side to fight over audio input)
	
	// potentially the external native process will need to handle all audio recording
	// to present conflicts
	// in this case, the stdin/stdout could be used to control the external process
	// and a temp file name could be communicated to indicate what should be sent
	// to the server-side speech-to-text system
	// or at that point it may be worthwhile to embedded the process fully with jni
	// and thus it wouldnt be an external process anymore
	
	// we'd want to embed it eventually to fully support streaming speech-to-text
	 
	
	
	String wake_cmd = "bin/haleyWakePhrase.sh"
	
	public static void init() {
		
		
		
		
		
	}
	
	
	
	
	
}
