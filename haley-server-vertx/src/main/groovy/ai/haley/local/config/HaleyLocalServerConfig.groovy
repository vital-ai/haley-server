package ai.haley.local.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory


class HaleyLocalServerConfig {

	// plugin directory
	
	// websocket endpoint
	
	// SSL?  (confirm WSS secure endpoint?)
	// (avoid man-in-middle, DNS redirection, etc.)
	
	
	public static Config conf = null
	
	
	// rest port
	
	
	public static void init() {
		
		conf = ConfigFactory.parseFile(new File("./config/haley-local-server.config"))
		
		
		
	}
	
	
	
	
	
}
