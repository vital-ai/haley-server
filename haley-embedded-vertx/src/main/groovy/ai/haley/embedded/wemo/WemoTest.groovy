package ai.haley.embedded.wemo

import static groovy.io.FileType.FILES
import java.util.regex.Matcher
import java.util.regex.Pattern


class WemoTest {

	static main(args) {
	
		println "List:"
		
		List cmd = ["/anaconda/bin/wemo", "list"]
			
		def process=new ProcessBuilder(cmd).redirectErrorStream(true).start()
		process.inputStream.eachLine {println it}
		
		println "Status:"
		
		cmd  = ["/anaconda/bin/wemo", "-v", "switch", "WeMo Insight", "status"]
		
		
		process=new ProcessBuilder(cmd).redirectErrorStream(true).start()
		process.inputStream.eachLine {println it}
		
		Thread.sleep(1000)
		
		println "Off:"
		
		
		cmd  = ["/anaconda/bin/wemo", "switch", "WeMo Insight", "off"]
		
		process=new ProcessBuilder(cmd).redirectErrorStream(true).start()
		process.inputStream.eachLine {println it}
		
		Thread.sleep(1000)
		
		
		println "On:"
		
		
		cmd  = ["/anaconda/bin/wemo", "switch", "WeMo Insight", "on"]
		
		process=new ProcessBuilder(cmd).redirectErrorStream(true).start()
		process.inputStream.eachLine {println it}
		
	}

}
