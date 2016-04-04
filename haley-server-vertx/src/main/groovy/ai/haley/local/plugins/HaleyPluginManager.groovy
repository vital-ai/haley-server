package ai.haley.local.plugins

import ro.fortsoft.pf4j.DefaultPluginManager

import ro.fortsoft.pf4j.PluginManager

class HaleyPluginManager {

	
	static PluginManager pluginManager = null
	
	
	public static void init() {
		
		// plugin directory in config
		
		
		File f = new File("./plugins/")
		
		
		pluginManager = new DefaultPluginManager(f)
		pluginManager.loadPlugins()
		pluginManager.startPlugins()
		
		
	}
	
	
	
	
}
