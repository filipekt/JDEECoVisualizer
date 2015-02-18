package cz.filipekt.jdcv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds the data from the event log in a structured way. The main feature is that
 * it assigns to each person (given by ID) a list of {@link CheckPoint} instances.
 * These checkpoints determine some relevant events regarding the person, such as
 * "entering/leaving a vehicle", or "being at a certain place at a certain time". 
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
class CheckPointDatabase {
	
	/**
	 * To each person ID there is a list of checkpoints (i.e. where & when & etc.) assigned.
	 */
	private final Map<String,List<CheckPoint>> database = new HashMap<>();
	
	/**
	 * Adds the given {@link CheckPoint} instance to the {@link CheckPoint} list of the
	 * specified person.
	 * @param personID To this person the {@link CheckPoint} will be added.
	 * @param checkpoint This {@link CheckPoint} will be added to the specified person
	 */
	public void add(String personID, CheckPoint checkpoint){
		if ((personID != null) && (checkpoint != null) && !personID.isEmpty()){
			if (!database.containsKey(personID)){
				database.put(personID, new ArrayList<CheckPoint>());
			}
			database.get(personID).add(checkpoint);
			if (checkpoint.getTime() < minTime){
				minTime = checkpoint.getTime();
			}
			if (checkpoint.getTime() > maxTime){
				maxTime = checkpoint.getTime();
			}
		}
	}
	
	/**
	 * @param personID A person
	 * @return The list of {@link CheckPoint} instances associated with the person
	 */
	public List<CheckPoint> getList(String personID){		
		if (database.containsKey(personID)){
			return database.get(personID);
		} else {
			return new ArrayList<>();
		}
	}
	
	/**
	 * @return The IDs of all the persons that are recorded in this database.
	 */
	public Set<String> getKeys(){
		return database.keySet();
	}
	
	/**
	 * Minimal value of "time" attribute across all of the recorded {@link CheckPoint} instances.
	 */
	private double minTime = Double.MAX_VALUE;
	
	/**
	 * @return Minimal value of "time" attribute across all of the recorded {@link CheckPoint} instances.
	 * @see {@link CheckPointDatabase#minTime}
	 */
	public double getMinTime() {
		return minTime;
	}
	
	/**
	 * Maximal value of "time" attribute across all of the recorded {@link CheckPoint} instances.
	 */
	private double maxTime = Double.MIN_VALUE;

	/**
	 * @return Maximal value of "time" attribute across all of the recorded {@link CheckPoint} instances.
	 * @see {@link CheckPointDatabase#maxTime}
	 */
	public double getMaxTime() {
		return maxTime;
	}
	
	/**
	 * Associates each person with a vehicle he/she is currently seated in. If the person is
	 * in no vehicle, null value is associated. 
	 * Used when adding a new {@link CheckPoint} instance to the database.
	 */
	private final Map<String, String> inVehicle = new HashMap<>();
	
	/**
	 * @param personID A person
	 * @param vehicleID The vehicle in which the specified person currently travels.
	 * @see {@link CheckPointDatabase#inVehicle}
	 */
	public void setInVehicle(String personID, String vehicleID){
		inVehicle.put(personID, vehicleID);
	}
	
	/**
	 * @param personID A person
	 * @return Vehicle the specified person is currently seated in
	 * @see {@link CheckPointDatabase#inVehicle}
	 */
	public String getInVehicle(String personID){
		return inVehicle.get(personID);
	}
	
	/**
	 * Marks whether the last the last event concerning the person (the key) was
	 * a departure. It is used when adding a new checkpoint.
	 */
	private final Map<String,Boolean> justDeparted = new HashMap<>();
	
	/**
	 * @param personID A person
	 * @return True iff the last event of the specified person was a departure 
	 */
	public boolean getJustDeparted(String personID){
		Boolean val = justDeparted.get(personID);
		if (val == null){
			return false;
		} else {
			return val;
		}
	}
	
	/**
	 * @param personID A person
	 * @param val Whether the last event of the specified person was a departure 
	 */
	public void setJustDeparted(String personID, boolean val){
		justDeparted.put(personID, val);
	}
}