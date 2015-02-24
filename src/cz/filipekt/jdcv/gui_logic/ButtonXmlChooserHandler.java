package cz.filipekt.jdcv.gui_logic;

import java.io.File;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Listener for the {@link Event} that the user clicks a button that allows to choose a
 * file from the local filesystem. Shows the standard {@link FileChooser} and records the
 * selected file into a {@link TextField} specified by {@link ButtonXmlChooserHandler#field}. 
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class ButtonXmlChooserHandler implements EventHandler<Event>{

	/**
	 * The main {@link Stage} of the application
	 */
	private final Stage stage;
	
	/**
	 * After the file selection process is completed, the absolute path to the
	 * selected file is put inside this {@link TextField}.
	 */
	private final TextField field;
	
	/**
	 * @param stage The main {@link Stage} of the application
	 * @param field After the file selection process is completed, the absolute path to the
	 * selected file is put inside this {@link TextField}.
	 */
	public ButtonXmlChooserHandler(Stage stage, TextField field) {
		this.stage = stage;
		this.field = field;
	}

	/**
	 * When the button, to which this handler is appointed, is clicked, this method is run.
	 * Shows the standard JavaFX {@link FileChooser} allowing for selection of a single file. 
	 */
	@Override
	public void handle(Event arg0) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open XML File");
		File res = fileChooser.showOpenDialog(stage);
		if (res != null){
			field.setText(res.toPath().toAbsolutePath().toString());
		}
	}
	
}