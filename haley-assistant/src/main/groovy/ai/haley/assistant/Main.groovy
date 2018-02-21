package ai.haley.assistant

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class Main {

	final static String HA = "haleyassistant"
	
	static main(args) {

		if(args.length != 1) {
			println "usage: ${HA} <configfilepath>"
			System.exit(1)
			return
		}	
		
		String gac = System.getenv('GOOGLE_APPLICATION_CREDENTIALS')
		if(!gac) {
			System.err.println("GOOGLE_APPLICATION_CREDENTIALS environment variable not set, cannot start the haley assistent")
			System.exit(1)
			return
		}
		
		println "GOOGLE_APPLICATION_CREDENTIALS: ${gac}"
		
		String confFilePath = args[0]
		File confFile = new File(confFilePath)
		
		println "Config file: ${confFile.absolutePath}"
		
		Config config = ConfigFactory.parseFile(confFile)
		
		String appID = config.getString('appID')
		String endpointURL = config.getString('endpointURL')
		String username = config.getString('username')
		String password = config.getString('password')
		String channelName = config.getString('channelName')
		
		println "appID: ${appID}"
		println "endpointURL: ${endpointURL}"
		println "username: ${username}"
		println "password: ${password.length()} characters"
		println "channelName: ${channelName}"
		
		new HaleyAssistant(appID, endpointURL, username, password, channelName).run()
		
	}

}
