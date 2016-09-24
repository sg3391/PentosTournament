package pentos.g6;

import java.util.HashSet;
import java.util.Set;

import pentos.sim.Building;

public class Row {
	
	private int start, end;


	private int currentLocation;
	private int roadLocation;


	private Set<Building> buildings;
	
	public Row(int start, int end,int roadLocation) {
		this.start = start;
		this.end = end;
		this.currentLocation = 0;
		this.buildings = new HashSet<Building>();
		this.roadLocation = roadLocation;
	}
	
	public Row(int start, int end,int roadLocation, int currentLocation) {
		this.start = start;
		this.end = end;
		this.currentLocation = currentLocation;
		this.buildings = new HashSet<Building>();
		this.roadLocation = roadLocation;
	}
	
	public int size() {
		return this.end - this.start;
	}
	
	public int getStart(){
		return start;
	}
	
	public int getEnd(){
		return end;
	}
	
	public int getCurrentLocation() {
		return currentLocation;
	}

	public int getRoadLocation(){
		return roadLocation;
	}
	
	public void setCurrentLocation(int currentLocation) {
		this.currentLocation = currentLocation;
	}

}
