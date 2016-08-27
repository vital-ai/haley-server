package ai.haley.embedded.message


import groovy.json.JsonBuilder


class MessageUtils {

	
	public static boolean validateMessage(Object msg) {
		
		String theauth = msg.message.authentication
		
		String themessage = msg.message.message
		
		String thepayload = msg.message.payload
		
		
		println "auth: $theauth"
		println "message: $themessage"
		println "payload: $thepayload"
		
		return true
		
	}
	
	
	
	
	public static Object messageTemplate(String theauth, String themessage, String thepayload) {
		
		def builder = new groovy.json.JsonBuilder()
		
		def msg = builder.message {
			"authentication" "$theauth"
			"message" "$themessage"
			"payload" "$thepayload"
			
		}
		
		return builder.toPrettyString()
		
		
	}
	
	
	
}
