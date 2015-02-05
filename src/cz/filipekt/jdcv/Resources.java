package cz.filipekt.jdcv;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * A utility class simplifying the access to the resources of the application.
 */
public class Resources {

		/**
		 * Opens and returns an {@link InputStream} on the resource specified
		 * by the parameter.
		 * @param resourceName A resource to be loaded
		 * @return An {@link InputStream} instance reading from the specified resource.
		 * <br>If the resource could not be found or is inaccessible, null is returned.
		 * @throws IOException When the specified resource could not be found or opened
		 */
		public static InputStream getResourceInputStream(String resourceName) {
			URI resource = getResourceAsURI(resourceName);
			if (resource == null){
				return null;
			} else {
				try {
					URL urlResource = resource.toURL();
					return urlResource.openStream();
				} catch (IOException ex){
					return null;
				}
			}
		}
		
		/**
		 * Constructs an URI pointing to the resource given by its
		 * name in the method parameter.
		 * @param resourceName The resource name
		 * @return URI pointing to the resource given by its name in the method parameter.
		 * <br>If the resource could not be found, null is returned.
		 */
		public static URI getResourceAsURI(String resourceName) {
			try {
				if (Visualizer.debug){
					return Paths.get(resourcesDir, resourceName).toUri();
				} else {
					return Visualizer.class.getResource("/resources/" + resourceName).toURI();
				}
			} catch (URISyntaxException | NullPointerException ex){
				return null;
			}
		}
		
		/**
		 * When in debug mode (specified by {@link Visualizer#debug}), this is
		 * used to specify the directory with application resources.
		 */
		private static final String resourcesDir = "C:/Users/Tom/Documents/diplomka/JDEECoVisualizer-master/resources";

		/**
		 * Loads an image specified by its resource name and returns it
		 * wrapped in an instance of {@link ImageView}.
		 * When the image could not be loaded, an empty {@link ImageView}
		 * is returned.  
		 * @param resourceName Name of the desired image
		 * @param size Preferred width and height of the {@link ImageView} 
		 * @return The desired image wrapped in a {@link ImageView}
		 */
		public static ImageView getImageView(String resourceName, double size){
			try {
				if (resourceName == null){
					throw new NullPointerException();
				}
				InputStream stream = getResourceInputStream(resourceName);
				if (stream == null){
					throw new NullPointerException();
				} else {
					return new ImageView(new Image(stream, size, size, true, true));
				}
			} catch (NullPointerException ex) { // if the specified image is not found
				return new ImageView();
			}
		}

}
