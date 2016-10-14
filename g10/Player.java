package pentos.g10;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;

public class Player extends pentos.g0.Player {

	/* Keep the land's size */
	public int landSize = 0;
	public static int staticLandSize=0;
	public boolean initialized = false;

	/*
	 * These are the possible points for a building to start to be built. Loop
	 * through this set to decide where exactly to put the building.
	 * 
	 * In order to pack buildings closer to the corner, whenever a new building
	 * is settled, the set will grow to contain the cells on this building's
	 * margin, in order to make further moves to pack new buildings closer to
	 * the existing one.
	 */
	public Set<Cell> factoryStart;
	public Set<Cell> residenceStart;
	public Set<Cell> borders;

	public Set<Cell> roadcells;// Just changed the name so it will not overwrite
								// the road_cells field in g0.Player
	public Set<Cell> roadNeighbors;

	public Set<Cell> parkCells = new HashSet<>();
	public Set<Cell> waterCells = new HashSet<>();
	public Set<Cell> parkNeighbors = new HashSet<>();
	public Set<Cell> waterNeighbors = new HashSet<>();

	public Set<Cell> vacantBorders = new HashSet<>();

	/* A couple of planners working for the player */
	public Planner packToCornerPlanner = new PackToCornerPlanner();
	public Planner bruteForcePlanner = new BruteForcePlanner();
	public Planner dispatchingPlanner=new DispatchingPlanner();
	
	public int[] factoryRows;
	
	public OutputStream writer;
	

	@Override
	public void init() { 
//		 System.out.println("Do not really do things in init()");
	}

	public void learnLand(Land land) {
		this.landSize = land.side;
		if(staticLandSize==0)
			staticLandSize=land.side;
//		System.out.println("Initiating a player with strategy to start from two corners.");
		factoryStart = new HashSet<>();
		factoryStart.add(new Cell(landSize-1, landSize-1));
		
		residenceStart = new HashSet<>();
		residenceStart.add(new Cell(0, 0));
		
		/* Change: Add more cells as candidates */
		for(int i=0;i<land.side;i++){
//			residenceStart.add(new Cell(i,0));
			residenceStart.add(new Cell(0,i));
//			factoryStart.add(new Cell(land.side-i,land.side-1));
			factoryStart.add(new Cell(land.side-1,land.side-i));
		}

		/* Initiate border cells */
		borders = new HashSet<>();
		for (int i = 0; i < landSize; i++) {
			borders.add(new Cell(0, i));
			borders.add(new Cell(i, 0));
			borders.add(new Cell(landSize-1 - i, landSize-1));
			borders.add(new Cell(landSize-1, landSize-1 - i));
		}

		roadcells = new HashSet<Cell>();
		roadNeighbors = new HashSet<>();
		roadNeighbors.addAll(borders);

		vacantBorders.addAll(borders);

		initialized = true;
		
		factoryRows = new int[land.side];
        for (int i = 0; i < land.side; i++) {
            factoryRows[i] = 0;
        }
	}

	@Override
	public Move play(Building request, Land land) {
		/* Redirect output stream */
		PrintStream stream=System.out;
		System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
		    @Override public void write(int b) {}
		}) {
		    @Override public void flush() {}
		    @Override public void close() {}
		    @Override public void write(int b) {}
		    @Override public void write(byte[] b) {}
		    @Override public void write(byte[] buf, int off, int len) {}
		    @Override public void print(boolean b) {}
		    @Override public void print(char c) {}
		    @Override public void print(int i) {}
		    @Override public void print(long l) {}
		    @Override public void print(float f) {}
		    @Override public void print(double d) {}
		    @Override public void print(char[] s) {}
		    @Override public void print(String s) {}
		    @Override public void print(Object obj) {}
		    @Override public void println() {}
		    @Override public void println(boolean x) {}
		    @Override public void println(char x) {}
		    @Override public void println(int x) {}
		    @Override public void println(long x) {}
		    @Override public void println(float x) {}
		    @Override public void println(double x) {}
		    @Override public void println(char[] x) {}
		    @Override public void println(String x) {}
		    @Override public void println(Object x) {}
		    @Override public java.io.PrintStream printf(String format, Object... args) { return this; }
		    @Override public java.io.PrintStream printf(java.util.Locale l, String format, Object... args) { return this; }
		    @Override public java.io.PrintStream format(String format, Object... args) { return this; }
		    @Override public java.io.PrintStream format(java.util.Locale l, String format, Object... args) { return this; }
		    @Override public java.io.PrintStream append(CharSequence csq) { return this; }
		    @Override public java.io.PrintStream append(CharSequence csq, int start, int end) { return this; }
		    @Override public java.io.PrintStream append(char c) { return this; }
		});
		
		
		if (initialized == false) {
			learnLand(land);
		}

		boolean valid = false;
		Action willDo = new Action();
		try {
//			willDo = packToCornerPlanner.makeAPlan(this, request, land);
			willDo = dispatchingPlanner.makeAPlan(this, request, land);
			if (willDo==null||willDo.getStartPoint() == null) {
				System.out.println("Empty move");
			} else {
				//Compromise: Remove unbuildable parks and ponds
				Set<Cell> combined=ToolBox.combineSets(willDo.getAbsoluteBuildingCells(),willDo.getRoadCells());
				Set<Cell> keepParks=new HashSet<>();
				for(Cell c:willDo.getParkCells()){
					if(!land.unoccupied(c)||combined.contains(c)){
						System.out.println("Error: Park "+c+" is not available! Need to check out why.");
					}else{
						keepParks.add(c);
					}
				}
				willDo.setParkCells(keepParks);
				
				combined.addAll(willDo.getParkCells());
				Set<Cell> keepWater=new HashSet<>();
				for(Cell c:willDo.getWaterCells()){
					if(!land.unoccupied(c)||combined.contains(c)){
						System.out.println("Error: Water "+c+" is not available! Need to check out why.");
					}else{
						keepWater.add(c);
					}
				}
				willDo.setWaterCells(keepWater);
				
				valid = PlanEvaluator.validateMove(willDo, this, land);
			}
		} catch (Exception e) {
			System.out.println("Error: Exception is thrown from packToCornerPlanner:");
			e.printStackTrace();
		}

		/* If no valid move is to be performed, fall back to brute force. */
		if (!valid) {
			try {
				System.out.println("Fall back to brute force solution");
				willDo = bruteForcePlanner.makeAPlan(this, request, land);
				/* Validate this action as well? */
				valid = PlanEvaluator.validateMove(willDo, this, land);
			} catch (Exception e) {
				System.out.println("Error: Exception thrown in bruteForcePlanner:");
				e.printStackTrace();
			}
		}

		/* If brute force cannot do, return empty action */
		if (!valid) {
			return new Move(false);
		}
		
		/* Pre-planned roads */
//		Cell anchor=new Cell(0,land.side/2-1);
//		if(land.unoccupied(anchor)){
//			Set<Cell> roadPlan=willDo.getRoadCells();
//			System.out.println("Build pre-planned roads.");
//			for(int i=0;i<10;i++){
//				roadPlan.add(new Cell(i,land.side/2-1));
//				roadPlan.add(new Cell(land.side-1-i,land.side/2-1));
//				roadPlan.add(new Cell(land.side/2-1,i));
//				roadPlan.add(new Cell(land.side/2-1,land.side-1-i));
//			}
//		}

		/* Update related neighbors */

		/* Find overall space to occupy */
		Set<Cell> shifted = willDo.getAbsoluteBuildingCells();
		Set<Cell> overallToOccupy = ToolBox.combineSets(shifted, willDo.getRoadCells(), willDo.getParkCells(),
				willDo.getWaterCells());
		Set<Cell> overallNewNeighbors = ToolBox.vacantNeighbors(overallToOccupy, overallToOccupy, land);

		/* Update road neighbors */
		roadcells.addAll(willDo.getRoadCells());
		Set<Cell> newRoadNeighbors = ToolBox.vacantNeighbors(willDo.getRoadCells(), overallToOccupy, land);
		updateNeighbors(roadNeighbors, newRoadNeighbors, overallToOccupy);

		/* Update water and neighbors */
		waterCells.addAll(willDo.getWaterCells());
		Set<Cell> newWaterNeighbors = ToolBox.vacantNeighbors(willDo.getWaterCells(), overallToOccupy, land);
		updateNeighbors(waterNeighbors, newWaterNeighbors, overallToOccupy);

		/* Update parks and neighbors */
		parkCells.addAll(willDo.getParkCells());
		Set<Cell> newParkNeighbors = ToolBox.vacantNeighbors(willDo.getParkCells(), overallToOccupy, land);
		updateNeighbors(parkNeighbors, newParkNeighbors, overallToOccupy);

		/* Update building type related neighbors */
		Set<Cell> newBuildingNeighbors = ToolBox.vacantNeighbors(shifted, overallToOccupy, land);
		if (willDo.getBuilding().type == Building.Type.RESIDENCE) {
			updateNeighbors(residenceStart, newBuildingNeighbors, overallToOccupy);
		} else if (willDo.getBuilding().type == Building.Type.FACTORY) {
			updateNeighbors(factoryStart, newBuildingNeighbors, overallToOccupy);
		} else {
			System.out.println("Error: Building has type " + willDo.getBuilding().type);
		}

		/* Update borders */
		updateNeighbors(borders, overallNewNeighbors, overallToOccupy);

		/* Update vacant borders */
		vacantBorders.removeAll(overallToOccupy);
		
		
		/* Return print stream to out */
		System.setOut(stream);

		return new Move(true, // accept the move
				willDo.getBuilding(), // building
				willDo.getStartPoint(), // location
				willDo.getRotation(), // rotation
				willDo.getRoadCells(), // road
				willDo.getWaterCells(), // water
				willDo.getParkCells());
	}

	public void updateNeighbors(Set<Cell> toUpdate, Set<Cell> toAdd, Set<Cell> toRemove) {
		toUpdate.addAll(toAdd);
		toUpdate.removeAll(toRemove);
	}

	public void updateRoads(Set<Cell> roads) {
		if (roads != null)
			roadcells.addAll(roads);
		System.out.println(roadcells.size() + " road cells after update.");
	}

	public void updateBorders(Set<Cell> toOccupy, Set<Cell> avail) {
		borders.removeAll(toOccupy);
		borders.addAll(avail);
		// System.out.println("Now the borders are:"+borders);
	}

	public Set<Cell> occupyCells(Set<Cell> buildingCells, Set<Cell> roadCells) {
		Set<Cell> overall = new HashSet<>();
		overall.addAll(buildingCells);
		if (roadCells != null)
			overall.addAll(roadCells);
		return overall;
	}

	public Set<Cell> newSurrounding(Set<Cell> toOccupy, Land land) {
		Set<Cell> avail = new HashSet<>();
		for (Cell c : toOccupy) {
			Cell[] neighbors = c.neighbors();
			// the cell can be a candidate for another new start if not occupied
			for (int i = 0; i < neighbors.length; i++) {
				Cell cc = neighbors[i];
				if (land.unoccupied(cc)) {
					avail.add(cc);
				}
			}
		}
		avail.removeAll(toOccupy);
		return avail;
	}

	public Set<Cell> occupyCells(Action action) {
		Set<Cell> occupied = action.getAbsoluteBuildingCells();
		occupied.addAll(action.getRoadCells());
		occupied.addAll(action.getParkCells());
		occupied.addAll(action.getWaterCells());
		return occupied;
	}

	public Set<Cell> occupyCells(Building building, Cell start) {
		Set<Cell> toOccupy = new HashSet<>();
		Iterator<Cell> iter = building.iterator();
		while (iter.hasNext()) {
			Cell c = iter.next();
			Cell mapped = new Cell(c.i + start.i, c.j + start.j, Cell.Type.RESIDENCE);
			toOccupy.add(mapped);
		}
		return toOccupy;
	}

	public void updateResidenceStart(Set<Cell> toOccupy, Set<Cell> avail) {
		residenceStart.removeAll(toOccupy);
		residenceStart.addAll(avail);
	}

	public void updateFactoryStart(Set<Cell> toOccupy, Set<Cell> avail) {
		factoryStart.removeAll(toOccupy);
		factoryStart.addAll(avail);
	}
	public void updateFactoryRows(int row, int value)
    {
        factoryRows[row] = value;
    }
}
