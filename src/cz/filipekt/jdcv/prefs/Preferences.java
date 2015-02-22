package cz.filipekt.jdcv.prefs;

import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.shape.Line;
import cz.filipekt.jdcv.Console;
import cz.filipekt.jdcv.ensembles.MembershipRelation;
import cz.filipekt.jdcv.network.MyLink;

/**
 * Contains methods to retrieve preferences objects for various elements of simulated situation
 * 
 * @author Tomas Filipek <tom.filipek@seznam.cz>
 */
public class Preferences {
	
	/**
	 * Maps visual representations of links to the corresponding parsed link XML elements.
	 */
	private final Map<Node,MyLink> lines;
	
	/**
	 * Maps each ensemble membership relation to the graphical representation of this relation.
	 */
	private final Map<MembershipRelation,Node> ensembleShapes;
	
	/**
	 * @return Mapping of link IDs to the preferences objects corresponding to the given link.
	 */
	public Map<String,LinkPrefs> linkPrefs(){
		Map<String,LinkPrefs> res = new HashMap<>();
		for (Node shape : lines.keySet()){
			MyLink link = lines.get(shape);
			Line line = (Line)shape;
			Writer writer = Console.getInstance().getWriter();
			LinkPrefs prefs = new LinkPrefs(link.getId(), link.getFrom().getId(), link.getTo().getId(), line, writer);
			res.put(link.getId(), prefs);
		}
		return res;
	}
	
	/**
	 * @return Collection of preferences objects associated with each membership relation
	 */
	public Set<MembershipPrefs> membershipPrefs(){
		Set<MembershipPrefs> res = new HashSet<>();
		for (MembershipRelation mr : ensembleShapes.keySet()){
			Line line = (Line)ensembleShapes.get(mr);
			MembershipPrefs pref = new MembershipPrefs(mr.getEnsembleName(), mr.getCoordinator(), mr.getMember(), line);
			res.add(pref);
		}
		return res;
	}

	/**
	 * @param lines Maps visual representations of links to the corresponding parsed link XML elements.
	 * @param ensembleShapes Maps each ensemble membership relation to the graphical representation 
	 * of this relation.
	 */
	public Preferences(Map<Node, MyLink> lines,
			Map<MembershipRelation, Node> ensembleShapes) {
		this.lines = lines;
		this.ensembleShapes = ensembleShapes;
	}
}