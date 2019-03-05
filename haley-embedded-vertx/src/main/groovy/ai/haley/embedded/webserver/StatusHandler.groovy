package ai.haley.embedded.webserver

import groovy.json.JsonOutput
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

import java.text.DecimalFormat;

import ai.haley.embedded.HaleyEmbeddedApp

class StatusHandler implements Handler<RoutingContext> {

	public final static String pingScript = 'commons/scripts/ServicesAccessScript.groovy'
	
	static double mib = 1024d*1024d;
	
	static double gib = 1024d*1024d*1024d;
	
	static DecimalFormat df = new DecimalFormat("0.0");
	
	@Override
	public void handle(RoutingContext ctx) {

		boolean ok = true
		
		String message = ''
		
		def sendResponse = { ->
		Map r = [ok: ok, message: message]
			ctx.response().end(JsonOutput.toJson(r), 'UTF-8')
		}
		
		if(HaleyEmbeddedApp.sendAssetConditionMessage || HaleyEmbeddedApp.sendAssetLocationMessage) {
			
			int interval = HaleyEmbeddedApp.assetsIntervalSeconds
			int margin = 2 * interval
			
			Long ts = HaleyEmbeddedApp.lastAssetMessageTimestamp
			if(ts == null) ts = 0
			
			int ago = (int) ( ( System.currentTimeMillis() - ts ) / 1000L ) 
			
			if(ago > margin) {
				ok = false
				message = "Last temp/location message was sent ${ago} seconds ago, interval ${interval}"
				sendResponse()
				return
			}

			ts = HaleyEmbeddedApp.lastAssetReceivedTimestamp
			if(ts == null) ts = 0
			ago = (int) ( ( System.currentTimeMillis() - ts ) / 1000L )
			
			if(ago > margin) {
				ok = false
				message = "Last temp/location message data was received ${ago} seconds ago, interval ${interval}"
				sendResponse()
				return
			} 
			
		}
		
		message = 'Data OK'
		
		Runtime runtime = Runtime.getRuntime();
		
		double maxMemory = (double) runtime.maxMemory();
	
		double usedMemory = (double) ( runtime.totalMemory() - runtime.freeMemory() );
	
		double memUsage = ( usedMemory / maxMemory ) * 100d;
	
		if(memUsage > 90d) {
			ok = false;
			message += ( "\n available memory below 10% (" + df.format(memUsage) + "%) - used " + df.format(usedMemory/mib) + "MiB, max: " + df.format(maxMemory/mib) + "MiB" );
		} else {
			message += ( "\n mem usage OK: " + df.format(memUsage) + "% - used " + df.format(usedMemory/mib) + "MiB, max: " + df.format(maxMemory/mib) + "MiB" );
		}

	
		File file = new File(".");
		double totalSpace = file.getTotalSpace();
//		double freeSpace = file.getFreeSpace();
		double usableSpace = file.getUsableSpace();
		double usedSpace = totalSpace - usableSpace;

		double diskUsage = (usedSpace / totalSpace) * 100d;
		
		if(diskUsage > 90d) {
			ok = false;
			message += ( "\n available disk space below 10% (" + df.format(diskUsage) + "%) - used " + df.format(usedSpace/gib) + "GiB, total: " + df.format(totalSpace/gib) + "GiB");
		} else {
			message += ( "\n disk usage OK: " + df.format(diskUsage) + "% - used " + df.format(usedSpace/gib) + "GiB, total: " + df.format(totalSpace/gib) + "GiB");
		}
		
		
		sendResponse()
		
	}

}
