package cz.filipekt.jdcv.prefs;

import java.util.HashMap;
import java.util.Map;

import cz.filipekt.jdcv.ensembles.MembershipRelation;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

/**
 * Preferences object associated with a given ensemble membership relation
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class MembershipPrefs {
	
	/**
	 * Name of the ensemble
	 */
	private final String ensembleName;
	
	/**
	 * @return Name of the ensemble
	 * @see {@link MembershipPrefs#ensembleName}
	 */
	public String getEnsembleName() {
		return ensembleName;
	}

	/**
	 * ID of the coordinator
	 */
	private final String coordinator;
	
	/**
	 * @return ID of the coordinator
	 * @see {@link MembershipPrefs#coordinator}
	 */
	public String getCoordinator() {
		return coordinator;
	}

	/**
	 * ID of the member 
	 */
	private final String member;
	
	/**
	 * @return ID of the member
	 * @see {@link MembershipPrefs#member}
	 */
	public String getMember() {
		return member;
	}
	
	/**
	 * @return Representation of the membership relation for which 
	 * this object sets preferences
	 */
	public MembershipRelation getMembershipRelation(){
		return new MembershipRelation(ensembleName, coordinator, member);
	}
	
	/**
	 * For each {@link Line} representing a membership relation, this map holds
	 * a corresponding {@link ChangeListener} that ensures the {@link Line} is not
	 * shown in the visualization.
	 * The {@link Line} objects can't be simply marked as invisible, because the
	 * visible property is manipulated continuously by the visualization timeline. 
	 * Thus, this listener-approach is used.
	 */
	private static final Map<Line,ChangeListenerWithMemory> visibilityListeners = new HashMap<>();
	

	/**
	 * The geometric shape representing the membership relation
	 */
	private final Line line;

	/**
	 * @param ensembleName Name of the ensemble
	 * @param coordinator ID of the coordinator
	 * @param member ID of the member 
	 * @param line The geometric shape representing the membership relation
	 */
	public MembershipPrefs(String ensembleName, String coordinator,
			String member, Line line) {
		this.ensembleName = ensembleName;
		this.coordinator = coordinator;
		this.member = member;
		this.line = line;
		initVisibilityListener();
	}
	
	/**
	 * Blocks any attempt to make the {@link MembershipPrefs#line} visible.
	 * Moreover, the class remembers the value of "newValue" in the last call 
	 * of the {@link ChangeListenerWithMemory#changed} 
	 */
	private class ChangeListenerWithMemory implements ChangeListener<Boolean>{
		
		/**
		 * Value of "newValue" in the last call of the 
		 * {@link ChangeListenerWithMemory#changed}
		 */
		private boolean lastValue;

		/**
		 * @return Value of "newValue" in the last call of the 
		 * {@link ChangeListenerWithMemory#changed}
		 * @see {@link ChangeListenerWithMemory#lastValue}
		 */
		public boolean getLastValue() {
			return lastValue;
		}

		/**
		 * Sets the last encountered value of the visibility property of
		 * {@link MembershipPrefs#line}. It is usually set when this listener
		 * is activated, and the value is the property value at that time.
		 * @param lastValue The last encountered value of the visibility property of
		 * {@link MembershipPrefs#line}
		 */
		public void setLastValue(boolean lastValue) {
			this.lastValue = lastValue;
		}

		@Override
		public void changed(ObservableValue<? extends Boolean> arg0,
				Boolean oldValue, Boolean newValue) {
			lastValue = newValue;
			if (newValue == true){
				line.setVisible(false);
			}
		}
		
	}
	
	/**
	 * Initializes the value corresponding to {@link MembershipPrefs#line} in
	 * {@link MembershipPrefs#visibilityListeners}.
	 */
	private void initVisibilityListener(){
		if (!visibilityListeners.containsKey(line)){
			ChangeListenerWithMemory listener = new ChangeListenerWithMemory();
			visibilityListeners.put(line, listener);
		}
	}
	
	/**
	 * @return The current color of the membership relation visualization
	 */
	public Paint getColor(){
		return line.getStroke();
	}
	
	/**
	 * Sets the color of the membership relation visualization
	 * @param color The new color
	 */
	public void setColor(Paint color){
		line.setStroke(color);
	}
	
	/**
	 * Specifies, whether the membership relation should be shown in the visualization
	 * @param enabled If false, the membership relation won't be shown in the visualization.
	 */
	public void setEnabled(boolean enabled){
		ChangeListenerWithMemory listener = visibilityListeners.get(line);
		line.visibleProperty().removeListener(listener);
		if (!enabled){
			boolean wasVisible = line.isVisible();
			line.visibleProperty().addListener(listener);
			line.setVisible(false);
			listener.setLastValue(wasVisible);
		} else {
			if (!line.isVisible()){
				line.setVisible(listener.getLastValue());
			}
		}
	}
}
