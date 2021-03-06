package org.arma.guillotine;

import java.util.ArrayList;
import org.apache.log4j.Logger;

public class SQMClass {
	private static Logger logger = Logger.getLogger(SQMClass.class);
	private String type;
	//TODO: This is being casted all the time -> Bad programming.
	//This can be either Vehicle, Markers, Triggers or Item.
	//Figure out how to replace
	private Object object;
	private ArrayList<SQMClass> childs = new ArrayList<SQMClass>();
	private SQMClass parent;
	private Waypoints waypoints;
	public SQMClass(String string, SQMClass parent) {
		this.type = string;
		this.parent = parent;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	//TODO: rename this function
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	public ArrayList<SQMClass> getChilds() {
		return childs;
	}
	public void setChilds(ArrayList<SQMClass> childs) {
		this.childs = childs;
	}
	
	public boolean equals(String s) {
		if(this.type.equals(s)) {
			return true;
		}
		return false;
	}
	
	public String toString() {
		return this.type;
	}

	public void span() {
		span(0);
	}

	private void span(int i) {
		String str = "";
		for(int n = 0; n < i; n++) { str += "\t"; }
		logger.debug(str + "CLASS: " + type);
		for(SQMClass tc : childs) {
			tc.span(i+1);
		}
	}


	public SQMClass getParent() {
		return parent;
	}


	public void setParent(SQMClass parent) {
		this.parent = parent;
	}


	public int getFullCount() {
		int res = 0;
		res += this.getChilds().size();
		
		for(SQMClass tc : getChilds()) {
			res += tc.getFullCount();
		}
		return res;
	}


	public void addWaypoints(Waypoints waypoints) {
		setWaypoints(waypoints);
	}


	public Waypoints getWaypoints() {
		return waypoints;
	}


	public void setWaypoints(Waypoints waypoints) {
		this.waypoints = waypoints;
	}
	
	
	
}

