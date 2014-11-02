package cz.filipekt.jdcv.network;

/**
 * Represents a node in the map.
 * @author Tom
 *
 */
public class MyNode {
	
	private final String id;
	private final double x;
	private final double y;
	
	public MyNode(String id, double x, double y) {
		super();
		this.id = id;
		this.x = x;
		this.y = y;
	}

	public String getId() {
		return id;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MyNode){
			MyNode that = (MyNode)obj;
			return this.id.equals(that.id);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

}
