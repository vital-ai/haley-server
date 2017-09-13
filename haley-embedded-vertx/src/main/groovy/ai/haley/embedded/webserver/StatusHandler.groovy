package ai.haley.embedded.webserver

import groovy.json.JsonOutput
import io.vertx.core.Handler
import io.vertx.groovy.ext.web.RoutingContext
import ai.haley.embedded.HaleyEmbeddedApp

class StatusHandler implements Handler<RoutingContext> {

	public final static String pingScript = 'commons/scripts/ServicesAccessScript.groovy'
	
	
	
	@Override
	public void handle(RoutingContext ctx) {

		Map r = [ok: true, message: 'OK']
		
		if(HaleyEmbeddedApp.sendAssetConditionMessage || HaleyEmbeddedApp.sendAssetLocationMessage) {
			
			int interval = HaleyEmbeddedApp.assetsIntervalSeconds
			int margin = 2 * interval
			
			Long ts = HaleyEmbeddedApp.lastAssetMessageTimestamp
			if(ts == null) ts = 0
			
			int ago = (int) ( ( System.currentTimeMillis() - ts ) / 1000L ) 
			
			if(ago > margin) {
				r.ok = false
				r.message = "Last temp/location message was sent ${ago} seconds ago, interval ${interval}"
			}

			ts = HaleyEmbeddedApp.lastAssetReceivedTimestamp
			if(ts == null) ts = 0
			ago = (int) ( ( System.currentTimeMillis() - ts ) / 1000L )
			
			if(ago > margin) {
				r.ok = false
				r.message = "Last temp/location message data was received ${ago} seconds ago, interval ${interval}"
			} 
						
		} 
		
		
		ctx.response().end(JsonOutput.toJson(r), 'UTF-8')
		
	}

}
