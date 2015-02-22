package cz.filipekt.jdcv.plugins;

import cz.filipekt.jdcv.prefs.GlobalPrefs;
import cz.filipekt.jdcv.prefs.Preferences;

/**
 * Base class for all plugins that need to access some of the visualizations preferences and parameters
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public abstract class PanelWithPreferences implements Plugin {
	
	/**
	 * Generates the preferences objects for visualization elements
	 */
	private Preferences preferences;
	
	/**
	 * Allows for setting the global preferences of the visualization
	 */
	private GlobalPrefs generalPrefs;
	
	/**
	 * Sets the preferences wrapper object
	 * @param preferences Generates the preferences objects for visualization elements
	 */
	public final void setPreferences(Preferences preferences) {
		this.preferences = preferences;
	}

	/**
	 * @return Generates the preferences objects for visualization elements
	 */
	public final Preferences getPreferences() {
		return preferences;
	}
	
	/**
	 * Sets the global preferences object
	 * @param generalPrefs Allows for setting the global preferences of the visualization
	 */
	public final void setGeneralPrefs(GlobalPrefs generalPrefs) {
		this.generalPrefs = generalPrefs;
	}
	
	/**
	 * @return Allows for setting the global preferences of the visualization
	 */
	public final GlobalPrefs getGeneralPrefs() {
		return generalPrefs;
	}
	
	
}
