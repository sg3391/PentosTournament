package pentos.g6;

import java.util.HashSet;
import java.util.Set;

import pentos.sim.Building;

public class Row {
	
	private int start, end;

	private int currentLocation;
	private int roadLocation;
	private int parkLocation;
	private int parkSize;
	
	private boolean recentlyPadded;

	private Set<Building> buildings;
	
	public Row(int start, int end, int roadLocation) {
		this.start = start;
		this.end = end;
		this.currentLocation = 0;
		this.buildings = new HashSet<Building>();
		this.roadLocation = roadLocation;
		this.parkLocation = Integer.MAX_VALUE; // Never used
		this.parkSize = 0;
	}
	
	public Row(int start, int end,int roadLocation, int parkLocation, int currentLocation) {
		this.start = start;
		this.end = end;
		this.currentLocation = currentLocation;
		this.buildings = new HashSet<Building>();
		this.roadLocation = roadLocation;
		this.parkLocation = parkLocation;
		this.parkSize = 0;
	}
	
	public int size() {
		return this.end - this.start;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getEnd() {
		return end;
	}
	
	public int getCurrentLocation() {
		return currentLocation;
	}

	public int getRoadLocation() {
		return roadLocation;
	}
	
	public int getParkLocation() {
		return parkLocation;
	}
	public int getParkSize(){
		return parkSize;
	}
	public void setParkSize(int size){
		parkSize = size;
	}
	
	public void setCurrentLocation(int currentLocation) {
		this.currentLocation = currentLocation;
	}

	public String toString() {
		return "Row from " + start + " to " + end
				+ ". Road at " + roadLocation
				+ " and park at " + parkLocation;
	}
	
	public void setWasRecentlyPadded() {
		this.recentlyPadded = true;
	}
	
	public void setWasNotRecentlyPadded() {
		this.recentlyPadded = false;
	}
	
	public boolean getRecentlyPadded() {
		return this.recentlyPadded;
	}
}
