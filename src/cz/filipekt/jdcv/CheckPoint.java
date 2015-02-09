package cz.filipekt.jdcv;

/**
 * Marks an relevant event on the timeline. Can hold three types of information -
 * that a person entered a vehicle, left a vehicle, or is at a specified place at specified 
 * time with the car.
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
class CheckPoint {	
	
	/**
	 * A {@link CheckPoint} can bear three types of information - that a person entered 
	 * the car, left the car, or is at a specified place at specified time with the car. 
	 */
	public static enum Type{
		PERSON_ENTERS, PERSON_LEAVES, POSITION_DEF
	}
	
	/**
	 * Specifies the type of information this {@link CheckPoint} bears.
	 */
	private final CheckPoint.Type type;
	
	/**
	 * @return The type of information this {@link CheckPoint} bears.
	 * @see {@link CheckPoint.Type}
	 */
	public CheckPoint.Type getType() {
		return type;
	}

	/**
	 * x-coordinate of a point in map
	 */
	private final double x;
	
	/**
	 * y-coordinate of a point in map
	 */
	private final double y;
	
	/**
	 * A point in time
	 */
	private final double time;
	
	/**
	 * Who is driving
	 */
	private final String person;
	
	/**
	 * @return Who is driving
	 * @see {@link CheckPoint#person}
	 */
	public String getPerson() {
		return person;
	}
	
	/**
	 * Vehicle used
	 */
	private final String vehicle;

	/**
	 * @return Vehicle used
	 * @see {@link CheckPoint#vehicle}
	 */
	public String getVehicle() {
		return vehicle;
	}

	/**
	 * @return x-coordinate of a point in map
	 * @see {@link CheckPoint#x}
	 */
	public double getX() {
		return x;
	}

	/**
	 * @return y-coordinate of a point in map
	 * @see {@link CheckPoint#y}
	 */
	public double getY() {
		return y;
	}

	/**
	 * @return A point in time
	 * @see {@link CheckPoint#time}
	 */
	public double getTime() {
		return time;
	}

	/**
	 * @param x x-coordinate of a point in map
	 * @param y y-coordinate of a point in map
	 * @param time A point in time
	 * @param person Who is driving
	 * @param type The type of information this {@link CheckPoint} bears.
	 */
	public CheckPoint(double x, double y, double time, String person, String vehicle, Type type) {
		this.x = x;
		this.y = y;
		this.time = time;
		this.person = person;
		this.vehicle = vehicle;
		this.type = type;
	}
	
	/**
	 * Equal to calling {@code CheckPoint(0,0,time,person,type)}. Useful when the type
	 * is one of {@code PERSON_ENTERS} , {@code PERSON_LEAVES}
	 * @param time
	 * @param person
	 * @param type
	 */
	public CheckPoint(double time, String person, String vehicle, Type type) {
		this(0, 0, time, person, vehicle, type);
	}

	/**
	 * Produces a human-readable representation of the object. For debugging purposes.
	 */
	@Override
	public String toString() {
		return "Time: " + time + ", x: " + x + ", y: " + y + ", person: " + person;
	}
	
	
}