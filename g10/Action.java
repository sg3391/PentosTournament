package pentos.g10;

import java.util.HashSet;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Cell;

public class Action {
	private Building building;
	private Cell startPoint;
	private int rotation;
	
	private boolean recomputeShift=true;
	
	private Set<Cell> roadCells = new HashSet<>();
	private Set<Cell> waterCells = new HashSet<>();
	private Set<Cell> parkCells = new HashSet<>();

	public Set<Cell> shifted;

	public Action() {
		building = null;
		startPoint = null;
		rotation = 0;
	}

	public Action(Building b, Cell c) {
		building = b;
		startPoint = c;
		rotation = 0;
	}

	public Action(Building b, Cell c, int r) {
		building = b;
		startPoint = c;
		rotation = r;
	}
	
	/* Short hand for changing building and start point tgt */
	public void changePlan(Building building,Cell start){
		this.building=building;
		this.startPoint=start;
		this.recomputeShift=true;
	}

	/*
	 * Use this to get the exact location of building cells! Optimized aiming at
	 * repetitive usage.
	 * 
	 */
	public Set<Cell> getAbsoluteBuildingCells() {
		if(recomputeShift==false&&shifted != null){
			return shifted;
		}
		Building[] rotations = this.building.rotations();
		Building building = rotations[this.rotation];
		Cell start = this.startPoint;
		Set<Cell> shiftedCells = ToolBox.shiftCells(building, start);
		this.shifted = shiftedCells;
		this.recomputeShift=false;
		
		return this.shifted;
	}
	
	/* Getters and setter */
	public Set<Cell> getRoadCells() {
		return roadCells;
	}

	public void setRoadCells(Set<Cell> roadCells) {
		this.roadCells = roadCells;
	}

	public Set<Cell> getWaterCells() {
		return waterCells;
	}

	public void setWaterCells(Set<Cell> waterCells) {
		this.waterCells = waterCells;
	}

	public Set<Cell> getParkCells() {
		return parkCells;
	}

	public void setParkCells(Set<Cell> parkCells) {
		this.parkCells = parkCells;
	}
	
	public int getRotation(){
		return this.rotation;
	}
	
	public void setRotation(int r){
		this.rotation=r;
	}
	
	public Building getBuilding(){
		return this.building;
	}
	
	public Cell getStartPoint(){
		return this.startPoint;
	}
}
