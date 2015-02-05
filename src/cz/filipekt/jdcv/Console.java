package cz.filipekt.jdcv;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import cz.filipekt.jdcv.Dialog.Type;
import cz.filipekt.jdcv.prefs.LinkPrefs;
import cz.filipekt.jdcv.prefs.MembershipPrefs;

/**
 * A console providing the user with the option to change some of the
 * settings and visualization parameters with an ECMA script.
 * This is a singleton class. To retrieve the only instance, call
 * {@link Console#getInstance()}.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>, in Bad Hofgastein 2015
 * 
 */
public class Console {
	
	/**
	 * Width (in pixels) of the buttons provided in the console window
	 */
	private final double buttonWidth = 150;
	
	/**
	 * The amount of horizontal space between the buttons
	 */
	private final double buttonsSpacing = 10;
	
	/**
	 * Width and height of the icons placed inside the control buttons
	 */
	private final double buttonIconSize = 20.0;
	
	/**
	 * The standard output of the JavaScript code is redirected here. 
	 */
	private final StringWriter writer = new StringWriter();
	
	/**
	 * @return The writer that consumes the standard output of the scripts
	 * @see {@link Console#writer}
	 */
	public Writer getWriter(){
		return writer;
	}
	
	/**
	 * Clears the underlying {@link StringBuffer} of the {@link Console#writer}
	 */
	private void clearWriterBuffer(){
		if (writer != null){
			writer.getBuffer().setLength(0);
		}
	}
	
	/**
	 * This engine is used to run the ECMA script code entered by the user.
	 */
	private final ScriptEngine engine;
	
	/**
	 * The only instance of this singleton class.
	 * @see {@link Console#getInstance()}
	 */
	private static final Console INSTANCE = new Console();
	
	/**
	 * @return The only instance of this singleton class.
	 * @see {@link Console#INSTANCE}
	 */
	public static Console getInstance(){
		return INSTANCE;
	}
	
	/**
	 * This is a singleton class. 
	 * The only instance can be retrieved by {@link Console#getInstance()}.
	 */
	private Console(){
		initCharsets();
		ScriptEngineManager manager = new ScriptEngineManager();
		engine = manager.getEngineByName("ecmascript");
		engine.getContext().setWriter(writer);
		initializeEngineEnvironment();
	}
	
	/**
	 * Name of the resource file that contains a script that is run
	 * before the console is ready for user input.
	 */
	private final String initScript = "init.js";
	
	/**
	 * Text encoding of the script file specified by {@link Console#initScript}
	 */
	private final String initScriptEncoding = "UTF-8";
	
	/**
	 * The script files can be loaded using one of these charsets.
	 */
	private final Set<String> charsets = new HashSet<>(); 
	
	/**
	 * Fills the {@link Console#charsets} with the available charset names.
	 * The six charsets required by the Java platform are added, plus the
	 * default encoding of the computer.
	 */
	private void initCharsets(){
		if (charsets.size() == 0){
			String defaultCharset = Charset.defaultCharset().name();
			List<String> mandatoryCharsets = Arrays.asList(
					"US-ASCII", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16");
			charsets.add(defaultCharset);
			charsets.addAll(mandatoryCharsets);
		}
	}
	
	/**
	 * Runs the initialization scripts in the scripting environment
	 */
	private void initializeEngineEnvironment(){
		try {
			InputStream initJS = Resources.getResourceInputStream(initScript);
			if (initJS != null){
				Reader initJsReader = new InputStreamReader(initJS, Charset.forName(initScriptEncoding));
				if (engine != null){
					engine.eval(initJsReader);
				}
			}
		} catch (ScriptException e) {
			System.out.println("Engine environment hasn't been properly initialized.");
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * Handler for the event that the user clicks one of the "import" buttons 
	 * in the scripting console window. It shows a file chooser window and
	 * loads the selected file in the text encoding as specified by one of
	 * the constructor parameters.
	 */
	private static class ImportButtonHandler implements EventHandler<ActionEvent>{
		
		/**
		 * The encoding used to read the script file
		 */
		private final Charset encoding;
		
		/**
		 * The main stage of the application
		 */
		private final Stage stage;
		
		/**
		 * The imported script is put inside this {@link TextArea} 
		 */
		private final TextArea inputArea;

		/**
		 * @param encoding The encoding used to read the script file
		 * @param stage The main stage of the application
		 * @param inputArea The imported script is put inside this {@link TextArea}
		 */
		public ImportButtonHandler(String encoding, Stage stage,
				TextArea inputArea) {
			this.encoding = Charset.forName(encoding);
			this.stage = stage;
			this.inputArea = inputArea;
		}

		/**
		 * Shows a file chooser window and loads the selected file in the text 
		 * encoding as specified by {@link ImportButtonHandler#encoding}.
		 */
		@Override
		public void handle(ActionEvent arg0) {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Select a Script File");
			File file = chooser.showOpenDialog(stage);
			if (file != null){
				try {
					byte[] bytes = Files.readAllBytes(file.toPath());
					String script = new String(bytes, encoding);
					inputArea.appendText(script);
				} catch (IOException e) {
					Dialog.show(Type.ERROR, "The file " + file.getAbsolutePath() + 
							" could not be imported.");
				}
			}
		}
		
	}
	
	/**
	 * Opens a console providing the user with the option to change some of 
	 * the settings and parameters of the visualization with an ECMA script.
	 * @param visualizer The parent application
	 */
	public void showScriptingConsole(final Visualizer visualizer){
		if (visualizer == null){
			return;
		}
		int row = 0;
		GridPane gridPane = new GridPane();
		Scene scene = new Scene(gridPane);
		final Stage stage = new Stage();
		final TextArea inputArea = new TextArea();
		ImageView importImage = Resources.getImageView("import.png", buttonIconSize);
		Set<MenuItem> importButtons = new HashSet<>();
		for (String encoding : charsets){
			MenuItem item = new MenuItem();
			item.setText(encoding);
			item.setOnAction(new ImportButtonHandler(encoding, stage, inputArea));
			importButtons.add(item);
		}
		SplitMenuButton importButton = new SplitMenuButton();
		importButton.getItems().addAll(importButtons);
		importButton.setGraphic(importImage);
		importButton.setText("Import File");
		importButton.setOnAction(new ImportButtonHandler(Charset.defaultCharset().name(), stage, inputArea));
		importButton.setPrefWidth(buttonWidth);
		ImageView runImage = Resources.getImageView("run.png", buttonIconSize);
		Button runButton = new Button("Run!", runImage);
		runButton.setPrefWidth(buttonWidth);
		ImageView clearImage = Resources.getImageView("clear.png", buttonIconSize);
		MenuItem clearInputItem = new MenuItem("Clear Input");
		MenuItem clearOutputItem = new MenuItem("Clear Output");
		SplitMenuButton clearButton = new SplitMenuButton(clearInputItem, clearOutputItem);
		clearButton.setGraphic(clearImage);
		clearButton.setText("Clear Input");
		clearButton.setPrefWidth(buttonWidth);
		ImageView helpImage = Resources.getImageView("help.png", buttonIconSize);
		Button helpButton = new Button("Help", helpImage);
		helpButton.setPrefWidth(buttonWidth);
		helpButton.setOnAction(new HelpButtonHandler());
		HBox buttonsBox = new HBox();
		buttonsBox.getChildren().addAll(importButton, runButton, clearButton, helpButton);
		buttonsBox.setAlignment(Pos.TOP_CENTER);
		buttonsBox.setSpacing(buttonsSpacing);
		gridPane.add(buttonsBox, 0, row++);
		Label inputLabel = new Label("INPUT:");
		gridPane.add(inputLabel, 0, row++);
		ScrollPane inputAreaPane = new ScrollPane(inputArea);
		gridPane.add(inputAreaPane, 0, row++);
		Label outputLabel = new Label("OUTPUT:");
		gridPane.add(outputLabel, 0, row++);
		final TextArea outputArea = new TextArea();
		ScrollPane outputAreaPane = new ScrollPane(outputArea);
		outputArea.setText(writer.toString());
		clearWriterBuffer();
		outputArea.setEditable(false);
		gridPane.add(outputAreaPane, 0, row++);
		runButton.setOnMouseClicked(new RunButtonHandler(visualizer, inputArea, outputArea));
		clearButton.setOnAction(new ClearButtonHandler(inputArea));
		clearInputItem.setOnAction(new ClearButtonHandler(inputArea));
		clearOutputItem.setOnAction(new ClearButtonHandler(outputArea));		
		stage.initStyle(StageStyle.DECORATED);
		stage.initModality(Modality.NONE);
		stage.setScene(scene);
		stage.sizeToScene();
		stage.setTitle("Scripting Console");
		InputStream consoleIconStream = Resources.getResourceInputStream("console.png");
		if (consoleIconStream != null){
			Image consoleIcon = new Image(consoleIconStream);
			stage.getIcons().add(consoleIcon);
		}
		stage.show();
	}
	
	/**
	 * Handler for the event that the user clicks the "help" button in
	 * the scripting console window. Makes sure that when clicked, the 
	 * platform default browser is started and it shows the manual page.
	 */
	private static class HelpButtonHandler implements EventHandler<ActionEvent>{
		
		/**
		 * Location of the manual page
		 */
		private final URI helpFile = Resources.getResourceAsURI("scriptingHelp.html");
		
		/**
		 * When thrown, it means that the platform default web browser could not
		 * be found or started.
		 */
		@SuppressWarnings("serial") 
		class BrowserNotAvailableException extends Exception {}

		/**
		 * Makes sure that when clicked, the platform default browser is 
		 * started and it shows the manual page.
		 */
		@Override
		public void handle(ActionEvent arg0) {
			try {
				if (!Desktop.isDesktopSupported()){
					throw new BrowserNotAvailableException();
				}
				Desktop desktop = Desktop.getDesktop();
				if ((desktop == null) || (!desktop.isSupported(Action.BROWSE))){
					throw new BrowserNotAvailableException();
				}
				desktop.browse(helpFile);	
			} catch (IOException | BrowserNotAvailableException ex){
				Dialog.show(Type.ERROR, "Browser could not be started.", 
						"You can find the manual pages on the GitHub repository of this application.");
			}
		}
	}
	
	/**
	 * Handler for the event that the user clicks the "run" button in the
	 * scripting console window. Makes sure that the objects representing
	 * the elements of the visualization are properly imported and the
	 * script is executed.
	 */
	private class RunButtonHandler implements EventHandler<Event>{
		
		/**
		 * The parent application
		 */
		private final Visualizer visualizer;
		
		/**
		 * The users write the scripts into this area.
		 */
		private final TextArea inputArea;
		
		/**
		 * The output from the scripts is shown here
		 */
		private final TextArea outputArea;

		/**
		 * @param visualizer The parent application
		 * @param inputArea The users write the scripts into this area.
		 * @param outputArea The output from the scripts is shown here
		 */
		public RunButtonHandler(Visualizer visualizer, TextArea inputArea, TextArea outputArea) {
			this.visualizer = visualizer;
			this.inputArea = inputArea;
			this.outputArea = outputArea;
		}

		/**
		 * Makes sure that the objects representing the elements of the 
		 * visualization are properly imported and the script is executed.
		 */
		@Override
		public void handle(Event arg0) {
			MapScene scene = visualizer.getScene();
			Map<String,LinkPrefs> linkPrefs;
			Set<MembershipPrefs> membershipPrefs;
			if (scene == null){
				linkPrefs = new HashMap<>();
				membershipPrefs = new HashSet<>();
			} else {
				linkPrefs = scene.getLinkPrefs();
				membershipPrefs = scene.getMembershipPrefs();
			}
			engine.put("links", linkPrefs);
			engine.put("memberships", membershipPrefs);
			try {
				engine.eval(inputArea.getText());
			} catch (ScriptException e) {
				writer.append("Exception occured:\n");
				writer.append(e.getLocalizedMessage() + "\n");
			} finally {
				String output = writer.toString();
				clearWriterBuffer();
				outputArea.appendText(output);
			}
		}
		
	}
	
	/**
	 * Handler for the event that the user clicks one of the "clear" buttons 
	 * in the scripting console window. Makes sure that the input/output area,
	 * as specified by the constructor parameter, is cleared.
	 */
	private static class ClearButtonHandler implements EventHandler<ActionEvent>{
		
		/**
		 * The content of this {@link TextArea} will be cleared. 
		 */
		private final TextArea textArea;

		/**
		 * @param textArea The content of this {@link TextArea} will be cleared.
		 */
		public ClearButtonHandler(TextArea textArea) {
			this.textArea = textArea;
		}

		/**
		 * Makes sure that the input/output area, as specified by 
		 * {@link ClearButtonHandler#textArea}, is cleared.
		 */
		@Override
		public void handle(ActionEvent arg0) {
			textArea.setText("");
		}
	}
	
}
