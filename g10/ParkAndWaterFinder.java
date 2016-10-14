package pentos.g10;

import java.util.Set;
import java.util.*;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;

public class ParkAndWaterFinder {
	public static Set<Cell> removeExistingFields(Set<Cell> fields, Land land) {
		Set<Cell> result = new HashSet<>();
		for (Cell f : fields) {
			if (land.isField(f))
				continue;
			else
				result.add(f);
		}
		return result;
	}

	public static Set<Cell> removeExistingWater(Set<Cell> fields, Land land) {
		Set<Cell> result = new HashSet<>();
		for (Cell f : fields) {
			if (land.isPond(f))
				continue;
			else
				result.add(f);
		}
		return result;
	}
	/*
	 * 3 scenarios: 
	 *  1. If there is a whitespace that cannot put anything in, put a park
	 *  2. Ponds or parks around, connect itself to it and put another
	 * on the other side 3. Factory around, put a thing between residence and
	 * factory 4. Border around, put a thing opposite to the border
	 */
	public static Set<Cell> findPark(Player player, Action action, Land land) {
		Set<Cell> cells = action.getAbsoluteBuildingCells();
		Set<Cell> toCover = ToolBox.combineSets(cells, action.getRoadCells());

		// Set<Cell> allNeighbors = findTwoLevelNeighbors(cells, land);
		Set<Cell> allNeighbors = findThreeLevelNeighbors(cells, land);
		boolean decided = false;
		Set<Cell> fieldsToBuild = new HashSet<>();

		// If the building is adjacent to park, use it
		Set<Cell> buildingNeighbors = ToolBox.allNeighbors(cells);
		System.out.println("All neighbors: " + buildingNeighbors);
		Set<Cell> parkNeighbor = ToolBox.setInterception(player.parkCells, buildingNeighbors);
		parkNeighbor.removeAll(toCover);
		if (parkNeighbor.size() > 0) {
			fieldsToBuild = new HashSet<>();
			decided = true;
			System.out.println("Available field neighbors there already: " + parkNeighbor);
			return removeExistingFields(fieldsToBuild, land);
		}

		// Else look for other situations
		for (Cell c : allNeighbors) {
			if (decided)
				break;
			// Find a field, connect to it
			if (land.isField(c)) {
				Set<Cell> midField = connectToField(c, cells, toCover, land);
				if (midField != null) {
					fieldsToBuild.addAll(midField);
					decided = true;
				}
			}
			// Find a factory, put things in between
			else if (land.getCellType(c) == Cell.Type.FACTORY) {
				Set<Cell> midField = betweenFactory(c, cells, toCover, land);
				if (midField != null) {
					fieldsToBuild.addAll(midField);
					decided = true;
				}
			} else {
				// If there is border, put the field to the opposite
				int border = ToolBox.findClosestBorder(c, action, land);
				System.out.println("Border is:"+border);
				if (border == -1)// not border
					continue;
				Set<Cell> oppoBorder;
				Cell start;
				if (border == 0)// upper border met, put to bottom
					start = ToolBox.findBottomLeft(cells);
				else if (border == 1)
					start = action.getStartPoint();
				else if (border == 2)
					start = action.getStartPoint();
				else
					start = ToolBox.findTopRight(cells);
				oppoBorder = growFieldsFrom(start, toCover, land);
				if (oppoBorder != null) {
					fieldsToBuild.addAll(oppoBorder);
					decided = true;
				}

			}
		}
		if (decided) {
			Set<Cell> toReturn=removeExistingFields(fieldsToBuild, land);
			System.out.println("Parks found by meeting criteria:"+toReturn);
			return toReturn;
		}
		
//		//If there is a whitespace that cannot be used for anything else, put a park
//		Set<Cell> firstLevelSur=ToolBox.findFirstLevelSurroundings(toCover,land);
//		System.out.println("Checking through first level neighbors: "+firstLevelSur);
//		Set<Cell> fields=null;
//		//find the connected whitespace
//		for(Cell f:firstLevelSur){
//			if(land.unoccupied(f)){
//				System.out.println(f+" is not occupied yet!");
//				Set<Cell> connectedNeighbors=findConnectedNeighbors(f,toCover,land);
//				if(connectedNeighbors.size()>4){
//					System.out.println("Vacant neighbors available for residence. Don't build parks.");
//					Cell[] imaginaryCells=connectedNeighbors.toArray(new Cell[connectedNeighbors.size()]);
//					Building imaginaryBuilding=new Building(imaginaryCells,Building.Type.RESIDENCE);
//					Action newAction=new Action(imaginaryBuilding,new Cell(0,0),0);
//					Set<Cell> roadsToConnect=RoadFinder.findRoad(player, newAction, land);
//					if(roadsToConnect.size()==0){
//						//it can be adjacent to roads
//						Set<Cell> roadNeighbors=ToolBox.setInterception(player.roadNeighbors, connectedNeighbors);
//						if(roadNeighbors.size()>0){
//							System.out.println("White space connected to road already. Leave it.");
//							fields=new HashSet<Cell>();
//							decided=true;
//						}else{
//							System.out.println("White space cannot connect to a road. Build a park.");
//							fields=growFieldsFrom(f, toCover, land);
//							decided=true;
//						}
//					}
//				}else{
//					System.out.println("The whitespace cannot be used to anything else. Build a park.");
//					fields=connectedNeighbors;
//					decided=true;
//				}
//			}
//		}
//		if (decided) {
//			Set<Cell> toReturn=removeExistingFields(fieldsToBuild, land);
//			System.out.println("Parks found by filling whitespace:"+toReturn);
//			return toReturn;
//		}

		// If no situation is met, build fields to the right
		Cell start = action.getStartPoint();
		Set<Cell> fields = growFieldsFrom(start, toCover, land);
		System.out.println("The fields to build are:" + fields);
		if (fields == null) {
			start = ToolBox.findTopRight(cells);
			fields = growFieldsFrom(start, toCover, land);
			System.out.println("The fields to build are:" + fields);
		}
		Set<Cell> toReturn=removeExistingFields(fieldsToBuild, land);
		System.out.println("Parks found by filling whitespace:"+toReturn);
		return toReturn;
	}
	public static Set<Cell> findConnectedNeighbors(Cell start,Set<Cell> toOccupy,Land land){
		System.out.println("Looking for connected vacant neighbor cells from "+start);
		Set<Cell> vacant=new HashSet<>();
		Set<Cell> toCheck=new HashSet<>();
		toCheck.add(start);
		Set<Cell> checked=new HashSet<>();
		checked.add(start);
		while(vacant.size()<5){
			Set<Cell> vacantNeighbors=ToolBox.vacantNeighbors(toCheck, toOccupy, land);
			vacantNeighbors.removeAll(checked);
			if(vacantNeighbors.size()==0){
				System.out.println("No vacant neighbors. Stop.");
				break;
			}
			vacant.addAll(vacantNeighbors);
			toCheck.clear();
			toCheck.addAll(vacantNeighbors);
			checked.addAll(vacantNeighbors);
		}
		System.out.println("Found connected vacant neighbor cells: "+vacant);
		return vacant;
	}

	public static Set<Cell> growFieldsFrom(Cell start, Set<Cell> toOccupy, Land land) {
		Set<Cell> result;
		if(land.unoccupied(start)&&(!toOccupy.contains(start))){
			System.out.println("The cell is vacant, build parks from here.");
			Set<Cell> vacantNeighbors=new HashSet<>();
			vacantNeighbors.add(start);
			result = growField(vacantNeighbors, toOccupy, land);
		}else{
			Set<Cell> startPoints = new HashSet<>();
			startPoints.add(start);
			Set<Cell> vacantNeighbors = ToolBox.vacantNeighbors(startPoints, toOccupy, land);
			if (vacantNeighbors.size() == 0) {
				System.out.println("No vacant starting points for fields.");
			}
			result = growField(vacantNeighbors, toOccupy, land);
		}
		
		return result;
	}

	public static Set<Cell> growField(Set<Cell> startPoint, Set<Cell> toCover, Land land) {
		System.out.println("Growing fields from "+startPoint);
		boolean found = false;
		Set<Cell> bestFields = new HashSet<>();
		for (Cell c : startPoint) {
			if (found)
				break;
			Set<Cell> fields = new HashSet<>();
			fields.add(c);
			while (fields.size() < 4) {
				Set<Cell> vacantNeighbors = ToolBox.vacantNeighbors(fields, toCover, land);
				// This means this is the limit of the field and a park cannot
				// be made
				if (vacantNeighbors.size() == 0) {
					if (bestFields.size() < fields.size()) {
						bestFields = fields;
						found = true;
					}
					break;
				}
				// Else we add the neighbors till we have a park
				for (Cell n : vacantNeighbors) {
					fields.add(n);
					if (fields.size() == 4)
						break;
				}
			}
			if (bestFields.size() < fields.size()) {
				bestFields = fields;
				found = true;
			}
		}

		System.out.println("The best fields is:" + bestFields);
		return bestFields;
	}

	public static Set<Cell> findWater(Player player, Action action, Land land) {
		Set<Cell> cells = action.getAbsoluteBuildingCells();
		Set<Cell> toCover = ToolBox.combineSets(cells, action.getRoadCells(), action.getParkCells());

		// Set<Cell> allNeighbors = findTwoLevelNeighbors(cells, land);
		Set<Cell> allNeighbors = findThreeLevelNeighbors(cells, land);
		boolean decided = false;
		Set<Cell> waterToBuild = new HashSet<>();

		// If the building is adjacent to park, use it
		Set<Cell> buildingNeighbors = ToolBox.allNeighbors(cells);
		System.out.println("All neighbors: " + buildingNeighbors);
		Set<Cell> waterNeighbor = ToolBox.setInterception(player.waterCells, buildingNeighbors);
		waterNeighbor.removeAll(toCover);
		if (waterNeighbor.size() > 0) {
			waterToBuild = new HashSet<>();
			decided = true;
			System.out.println("Available water neighbors there already: " + waterNeighbor);
			return removeExistingWater(waterToBuild, land);
		}

		// Else look for other situations
		for (Cell c : allNeighbors) {
			if (decided)
				break;
			// Find a pond, connect to it
			if (land.isPond(c)) {
				Set<Cell> midWater = connectToWater(c, cells, toCover, land);
				if (midWater != null) {
					waterToBuild.addAll(midWater);
					decided = true;
				}
			}
			// Find a factory, put things in between
			else if (land.getCellType(c) == Cell.Type.FACTORY) {
				System.out.println("Leave the factories to parks.");
				 Set<Cell> midWater = betweenFactory(c, cells, toCover, land);
				 if (midWater != null) {
				 waterToBuild.addAll(midWater);
				 decided = true;
				 }
			} else {
				System.out.println("Leave the borders to the parks.");
				// // If there is border, put the field to the opposite
				// int border = ToolBox.findClosestBorder(c, action, land);
				// if (border == -1)// not border
				// continue;
				// Set<Cell> oppoBorder;
				// Cell start;
				// if (border == 0)// upper border met, put to bottom
				// start = ToolBox.findBottomLeft(cells);
				// else if (border == 1)
				// start = action.getStartPoint();
				// else if (border == 2)
				// start = action.getStartPoint();
				// else
				// start = ToolBox.findTopRight(cells);
				// oppoBorder = growFieldsFrom(start, toCover, land);
				// if (oppoBorder != null) {
				// fieldsToBuild.addAll(oppoBorder);
				// decided = true;
				// }

			}
		}
		if (decided) {
			return removeExistingWater(waterToBuild, land);
		}

		// If no situation is met, build fields to the right
		Cell start = ToolBox.findBottomLeft(cells);
		Set<Cell> water = growWaterFrom(start, toCover, land);
		System.out.println("The water to build are:" + water);
		if (water == null) {
			start = ToolBox.findBottomRight(cells);
			water = growWaterFrom(start, toCover, land);
			System.out.println("The water to build are:" + water);
		}

		return removeExistingWater(water, land);
	}

	// Find the way to put the fields in deadzone
	public static Set<Cell> betweenFactory(Cell c, Set<Cell> toBuild, Set<Cell> toCover, Land land) {
		Cell[] fieldNeighbor = c.neighbors();
		Set<Cell> neighborCell = new HashSet<>(Arrays.asList(fieldNeighbor));

		Set<Cell> buildingNeighbor = ToolBox.allNeighbors(toBuild);

		Set<Cell> cross = ToolBox.setInterception(buildingNeighbor, neighborCell);
		cross.removeAll(toCover);
		System.out.println("Cells in between residence and factory: " + cross);
		if (cross.size() == 0)
			return null;

		// If the cell will not be covered, build a field there
		return growField(cross, toCover, land);
	}

	// Connect building to existing field
	public static Set<Cell> connectToField(Cell f, Set<Cell> toBuild, Set<Cell> toCover, Land land) {
		Cell[] fieldNeighbor = f.neighbors();
		Set<Cell> neighborCell = new HashSet<>(Arrays.asList(fieldNeighbor));

		Set<Cell> buildingNeighbor = ToolBox.vacantNeighbors(toBuild, toCover, land);

		Set<Cell> cross = ToolBox.setInterception(buildingNeighbor, neighborCell);
		cross.removeAll(toCover);
		System.out.println("Connect to neighbor field with: " + cross);
		if (cross.size() == 0)
			return null;

		return cross;
	}

	// Connect building to existing water
	public static Set<Cell> connectToWater(Cell f, Set<Cell> toBuild, Set<Cell> toCover, Land land) {
		Cell[] fieldNeighbor = f.neighbors();
		Set<Cell> neighborCell = new HashSet<>(Arrays.asList(fieldNeighbor));

		Set<Cell> buildingNeighbor = ToolBox.vacantNeighbors(toBuild, toCover, land);

		Set<Cell> cross = ToolBox.setInterception(buildingNeighbor, neighborCell);
		cross.removeAll(toCover);
		System.out.println("Connect to neighbor field with: " + cross);
		if (cross.size() == 0)
			return null;

		return cross;
	}

	public static Set<Cell> growWaterFrom(Cell start, Set<Cell> toOccupy, Land land) {
		Set<Cell> startPoints = new HashSet<>();
		startPoints.add(start);
		Set<Cell> vacantNeighbors = ToolBox.vacantNeighbors(startPoints, toOccupy, land);
		if (vacantNeighbors.size() == 0) {
			System.out.println("No vacant starting points for water.");
		}
		Set<Cell> result = growWater(vacantNeighbors, toOccupy, land);
		return result;
	}

	public static Set<Cell> growWater(Set<Cell> startPoint, Set<Cell> toCover, Land land) {
		boolean found = false;
		Set<Cell> bestWater = new HashSet<>();
		for (Cell c : startPoint) {
			if (found)
				break;
			Set<Cell> water = new HashSet<>();
			water.add(c);
			while (water.size() < 4) {
				Set<Cell> vacantNeighbors = ToolBox.vacantNeighbors(water, toCover, land);
				// This means this is the limit of the field and a park cannot
				// be made
				if (vacantNeighbors.size() == 0) {
					if (bestWater.size() < water.size()) {
						bestWater = water;
						found = true;
					}
					break;
				}
				// Else we add the neighbors till we have a park
				for (Cell n : vacantNeighbors) {
					water.add(n);
					if (water.size() == 4)
						break;
				}
			}
			if (bestWater.size() < water.size()) {
				bestWater = water;
				found = true;
			}
		}
		System.out.println("The best fields is:" + bestWater);
		return bestWater;
	}

	public static Set<Cell> findTwoLevelNeighbors(Set<Cell> toBuild, Land land) {
		Set<Cell> directNeighbors = ToolBox.allNeighbors(toBuild);
		Set<Cell> indirectNeighbors = ToolBox.allNeighbors(directNeighbors);
		indirectNeighbors.removeAll(toBuild);
		directNeighbors.addAll(indirectNeighbors);
		System.out.println(directNeighbors.size() + " neighbors in distance of 2");

		return directNeighbors;
	}

	public static Set<Cell> findThreeLevelNeighbors(Set<Cell> toBuild, Land land) {
		Set<Cell> directNeighbors = ToolBox.allNeighbors(toBuild);
		Set<Cell> indirectNeighbors = ToolBox.allNeighbors(directNeighbors);
		Set<Cell> fartherNeighbors = ToolBox.allNeighbors(indirectNeighbors);
		fartherNeighbors.addAll(indirectNeighbors);
		fartherNeighbors.removeAll(toBuild);

		System.out.println(fartherNeighbors.size() + " neighbors in distance of 3");

		return fartherNeighbors;
	}
}
