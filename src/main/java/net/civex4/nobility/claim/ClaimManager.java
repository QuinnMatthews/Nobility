package net.civex4.nobility.claim;

import java.util.HashMap;

import io.github.kingvictoria.nodes.Node;
import net.civex4.nobility.development.Camp;
import net.civex4.nobility.estate.Estate;

public class ClaimManager {
	
	public HashMap<Node,Estate> claims;
	
	public ClaimManager() {
		claims = new HashMap<Node,Estate>();

	}
	
	public boolean claim(Node n, Estate e) {
		if(claims.containsKey(n)) {
			if(claims.get(n) != null) {
				return false;
			}else {
				if(underNodeLimit(n,e)) {
					claims.put(n, e);
					return true;
				}else {return false; }
			}
		}else {
			if(underNodeLimit(n,e)) {
				claims.put(n, e);
				return true;
			}else { return false;}
		}
	}
	
	public boolean underNodeLimit(Node n, Estate e) {
		Camp camp = e.getCamp(n.type);
		int limit = camp.getNodeLimit();
		int counter = 0;
		for(Node node : this.claims.keySet()) {
			if(claims.get(node) == e && node.type == n.type) {
				counter++;
			}
		}
		return counter < limit;
	}
	
	public void unclaim(Node n) {
		claims.put(n, null);
	}
}
