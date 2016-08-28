package ai.haley.embedded.wemo

class HaleyWemoManager {

	
	
	public static List<String> listDevices() {
		
		def devices = []
		
		
		List cmd = ["/anaconda/bin/wemo", "list"]
		
	def process=new ProcessBuilder(cmd).redirectErrorStream(true).start()
	process.inputStream.eachLine {devices.add( it )}
		
		
	return devices
	
	}
	
	public static boolean turnOnDevice(String name) {
		
		// check status first
		
		
		def cmd  = ["/anaconda/bin/wemo", "switch", name, "on"]
		
		String status
		
		def process=new ProcessBuilder(cmd).redirectErrorStream(true).start()
		process.inputStream.eachLine {status = it}
		
		
		// check status, did it turn on?
		
		
		return true
		
		
	}
	
	public static boolean turnOffDevice(String name) {
		
		// check status first
		
		
		def cmd  = ["/anaconda/bin/wemo", "switch", name, "off"]
		
		String status
		
		def process=new ProcessBuilder(cmd).redirectErrorStream(true).start()
		process.inputStream.eachLine {status = it}
		
		
		// check status, did it turn off?
		
		return true
		
	}
	
	public static String deviceStatus(String name) {
		
		
		List cmd  = ["/anaconda/bin/wemo", "-v", "switch", name, "status"]
		
		String status
		
		def process=new ProcessBuilder(cmd).redirectErrorStream(true).start()
		process.inputStream.eachLine {status = it}
		
		return status
		
	}
	
	
}
