package cz.filipekt.jdcv.prefs;

import java.io.IOException;
import java.io.Writer;

import cz.filipekt.jdcv.MapScene;

/**
 * Provides the option to change some of the global preferences of the
 * application or visualization.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class GlobalPrefs {
	
	/**
	 * The simulated situation 
	 */
	private final MapScene scene;
	
	/**
	 * Used to write to scripting console output
	 */
	private final Writer outputWriter;
	
	/**
	 * @param scene The simulated situation 
	 * @param writer Used to write to scripting console output
	 */
	public GlobalPrefs(MapScene scene, Writer writer) {
		this.scene = scene;
		this.outputWriter = writer;
	}

	/**
	 * Given a path to an image, each person will be represented by this
	 * image in the visualization.
	 * @param path The image to use for visualizing people
	 */
	public void setPersonImage(String path) {
		if (scene == null){
			printNoOp();
		} else {
			try {
				scene.changePeopleImage(path, false);
				write("Image for persons has been successfully changed.");
			} catch (Exception ex){
				write("Image for persons couldn't be changed.");
			}
		}
	}
	
	/**
	 * Prints a "no operation" log to the output
	 */
	private void printNoOp() {
		write("No simulation scene has been specified.");
	}
	
	/**
	 * Writes the specified text to the console output, defined by {@link GlobalPrefs#outputWriter}
	 * @param text The text to be written
	 */
	private void write(String text){
		try {
			if (outputWriter != null){
				outputWriter.append(text);
				outputWriter.append("\n");
				outputWriter.flush();
			}
		} catch (IOException ex){
			System.out.println("Couldn't write to the console output.");
		}
	}
}
