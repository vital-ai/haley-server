package ai.haley.local.plugins

import ro.fortsoft.pf4j.DefaultPluginManager

class PluginManager {

	public void init() {
		
		File f = new File(".")
		
		
		PluginManager pluginManager = new DefaultPluginManager(f)
		pluginManager.loadPlugins()
		pluginManager.startPlugins()
		
		
		
		
	}
	
	
	
	
}
