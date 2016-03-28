package ai.haley.local.json

import groovy.json.JsonSlurper

class JSONUtils {

	
	static public Object parseMessage(String msg) {
		
		def slurper = new JsonSlurper()
				
		return slurper.parseText(msg)
	}
	
	public static String encodeMessage(Object obj) {
		
		// obj should be an object built with JSON builder
		
		return obj.toString()
		
		
	}
	
	
	
}
