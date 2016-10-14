package pentos.g6;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Building.Type;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class DummyPlayer implements pentos.sim.Player {

	private Random gen = new Random();
	private Set<Cell> road_cells = new HashSet<Cell>();

	public void init() { // function is called once at the beginning before play
							// is called

	}

	public void initializeRoadCells(Land land) {
		for (int i = 0; i < land.side; i++) {
			for (int j = 0; j < land.side; j++) {
				if (land.getCellType(i,j) == Cell.Type.ROAD) {
					road_cells.add(new Cell(i, j));
				}
			}
		}
	}

	public Move leastRoadMove(Building request, Land land) {
		// find all valid building locations and orientations
		ArrayList<Move> moves = new ArrayList<Move>();
		if (request.type == Building.Type.FACTORY) {
			// searching top-down, left-right
			for (int j = 0; j < land.side; j++) {
				for (int i = 0; i < land.side; i++) {
					Cell p = new Cell(i, j);
					Building[] rotations = request.rotations();
					for (int ri = 0; ri < rotations.length; ri++) {
						Building b = rotations[ri];
						if (land.buildable(b, p))
							moves.add(new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(),
									new HashSet<Cell>()));
					}
				}
			}
		} else if (request.type == Building.Type.RESIDENCE) {
			// Searching top-down, right-left
			for (int j = land.side; j >= 0; --j) {
				for (int i = 0; i < land.side; ++i) {
					Cell p = new Cell(i, j);
					Building[] rotations = request.rotations();
					for (int ri = 0; ri < rotations.length; ri++) {
						Building b = rotations[ri];
						if (land.buildable(b, p))
							moves.add(new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(),
									new HashSet<Cell>()));
					}
				}
			}
		}
		// Reject is no valid placements exist
		if (moves.isEmpty())
			return new Move(false);

		ArrayList<Move> moves2 = new ArrayList<Move>();
		boolean checkEmptyNeighborRoadAdj = true;
		
		while(true){
			// Iterate through the moves and find their shortest roads for each
			for (Move mc : moves) {
				// get coordinates of building placement 
				// (position plus local building cell coordinates)
				Set<Cell> shiftedCells = new HashSet<Cell>();
				for (Cell x : mc.request.rotations()[mc.rotation])
					shiftedCells.add(new Cell(x.i + mc.location.i, x.j + mc.location.j));
				// build a road to connect this building to perimeter
				Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
//				if (roadCells.size()==0) {
//					if (!isNextToRoad(mc.location.i, mc.location.j, mc.request, land)) {
//						continue;
//					}
//				}
				if(roadCells == null){
					continue;
				}
				else if(checkEmptyNeighborRoadAdj && !areNeighborsNextToRoad(mc.location.i, mc.location.j, mc.request.rotations()[mc.rotation], land, roadCells)){
					continue;
				}
				else {
					mc.road = roadCells;
					moves2.add(mc);
				}
			}
			if(checkEmptyNeighborRoadAdj && moves2.isEmpty()){
				checkEmptyNeighborRoadAdj=false;
				//System.out.println("Failed using neighborsRoadChecking");
			}
			else{
				break;
			}
		}
		
		
		moves = moves2;

		// Reject if no valid placements exist
		if (moves.isEmpty())
			return new Move(false);

		// Find minimum road length move
		int minimumIndex = 0;
		for (int i = 0; i < moves.size(); i++) {
			if (moves.get(i).road.size() < moves.get(minimumIndex).road.size())
				minimumIndex = i;
		}

		Move chosenMove = moves.get(minimumIndex);
		road_cells.addAll(chosenMove.road); // Adding this road to the set of
											// roads
		return chosenMove;
	}

	public Move play(Building request, Land land) {
		return leastRoadMove(request, land);
	}

	// build shortest sequence of road cells to connect to a set of cells b
	private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
		for (Cell cell : b) {
			if (isNextToRoad(cell.i, cell.j, land))
				return new HashSet<Cell>();
		}
		
		Set<Cell> output = new HashSet<Cell>();
		boolean[][] checked = new boolean[land.side][land.side];
		Queue<Cell> queue = new LinkedList<Cell>();
		// add border cells that don't have a road currently
		// dummy cell to serve as road connector to perimeter cells
		Cell source = new Cell(Integer.MAX_VALUE, Integer.MAX_VALUE);
		for (int z = 0; z < land.side; z++) {
			if (b.contains(new Cell(0, z)) || b.contains(new Cell(z, 0)) || b.contains(new Cell(land.side - 1, z))
					|| b.contains(new Cell(z, land.side - 1)))
				return output;
			if (land.unoccupied(0, z))
				queue.add(new Cell(0, z, source));
			if (land.unoccupied(z, 0))
				queue.add(new Cell(z, 0, source));
			if (land.unoccupied(z, land.side - 1))
				queue.add(new Cell(z, land.side - 1, source));
			if (land.unoccupied(land.side - 1, z))
				queue.add(new Cell(land.side - 1, z, source));
		}
		// add cells adjacent to current road cells
		for (Cell p : road_cells) {
			for (Cell q : p.neighbors()) {
				if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q))
					queue.add(new Cell(q.i, q.j, p)); // use tail field of cell
														// to keep track of
														// previous road cell
														// during the search
			}
		}
		while (!queue.isEmpty()) {
			Cell p = queue.remove();
			checked[p.i][p.j] = true;
			for (Cell x : p.neighbors()) {
				if (b.contains(x)) { // trace back through search tree to find
										// path
					Cell tail = p;
					while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
						output.add(new Cell(tail.i, tail.j));
						tail = tail.previous;
					}
					if (!output.isEmpty())
						return output;
				} else if (!checked[x.i][x.j] && land.unoccupied(x.i, x.j)) {
					x.previous = p;
					queue.add(x);
				}

			}
		}
		if (output.isEmpty() && queue.isEmpty())
			return null;
		else
			return output;
	}

	private boolean areNeighborsNextToRoad(int locI, int locJ, Building request, Land land, Set<Cell> road){
		Iterator<Cell> cIt = request.iterator();
		Set<Cell> emptyNeighbors = new HashSet<Cell>();
		while (cIt.hasNext()) {
			Cell c = cIt.next();
			if (locJ + c.j + 1 < land.side && land.getCellType(locI + c.i, locJ + c.j + 1) == Cell.Type.EMPTY)
				emptyNeighbors.add(new Cell(locI + c.i, locJ + c.j + 1));
			if (locJ + c.j - 1 >= 0 && land.getCellType(locI + c.i, locJ + c.j - 1) == Cell.Type.EMPTY)
				emptyNeighbors.add(new Cell(locI + c.i, locJ + c.j - 1));
			if (locI + c.i + 1 < land.side && land.getCellType(locI + c.i + 1, locJ + c.j) == Cell.Type.EMPTY)
				emptyNeighbors.add(new Cell(locI + c.i + 1, locJ + c.j));
			if (locI + c.i - 1 >= 0 && land.getCellType(locI + c.i - 1, locJ + c.j) == Cell.Type.EMPTY)
				emptyNeighbors.add(new Cell(locI + c.i - 1, locJ + c.j));
		}
//		for(Cell c : emptyNeighbors){
//			if(!road.contains(c) && isNextToRoad(c.i, c.j, land)){
//				if(	road.isEmpty() || isNextToSet(c.i, c.j, land, road) ){
//					return true;
//				}
//			}
//		}
		for(Cell c : emptyNeighbors){
			if(road.isEmpty() && isNextToRoad(c.i, c.j, land))
				return true;
			else if(!road.isEmpty() && setHas2EmptyNeighbors(road,land))
				return true;
		}
		return false;
	}
	
	private boolean setHas2EmptyNeighbors(Set<Cell> set, Land land){
		Iterator<Cell> cIt = set.iterator();
		int count=0;
		while(cIt.hasNext()){
			Cell c = cIt.next();
			Cell c1, c2, c3, c4;
			if(c.j+1 <= land.side){
				c1 = new Cell(c.i, c.j+1);
				if(!(c1.i < 0 || c1.i >= land.side || c1.j < 0 || c1.j >= land.side) && !set.contains(c1) && land.getCellType(c1)==Cell.Type.EMPTY)
					count++;//return true;
			}
			if(c.j-1 > 0){
				c2 = new Cell(c.i, c.j-1);
				if(!(c2.i < 0 || c2.i >= land.side || c2.j < 0 || c2.j >= land.side) && !set.contains(c2) && land.getCellType(c2)==Cell.Type.EMPTY)
					count++;//return true;
			}
			if(c.i + 1 <= land.side){
				c3 = new Cell(c.i+1, c.j);
				if(!(c3.i < 0 || c3.i >= land.side || c3.j < 0 || c3.j >= land.side) && !set.contains(c3) && land.getCellType(c3)==Cell.Type.EMPTY)
					count++;//return true;
			}
			if(c.i-1 > 0){
				c4 = new Cell(c.i-1, c.j);
				if(!(c4.i < 0 || c4.i >= land.side || c4.j < 0 || c4.j >= land.side) && !set.contains(c4) &&land.getCellType(c4)==Cell.Type.EMPTY)
					count++;//return true;
			}

		}
		//System.out.println("Count is: " + count);
		return (count>=2);//return false;
	}
	
	private boolean isNextToRoad(int locI, int locJ, Building request, Land land) {
		Iterator<Cell> cIt = request.iterator();
		while (cIt.hasNext()) {
			Cell c = cIt.next();
			if (locJ + c.j+ 1 >= land.side || land.getCellType(locI + c.i, locJ + c.j + 1) == Cell.Type.ROAD)
				return true;
			if (locJ + c.j - 1 < 0 || land.getCellType(locI + c.i, locJ + c.j - 1) == Cell.Type.ROAD)
				return true;
			if (locI + c.i + 1 >= land.side || land.getCellType(locI + c.i + 1, locJ + c.j) == Cell.Type.ROAD)
				return true;
			if (locI + c.i - 1 < 0 || land.getCellType(locI + c.i - 1, locJ + c.j) == Cell.Type.ROAD)
				return true;
		}
		return false;
	}
	
	private boolean isNextToSet(int locI, int locJ, Land land, Set<Cell> set){
		if (!(locJ < land.side) || !(locJ >= 0) || !(locI < land.side) || !(locI >= 0)	)
			throw new RuntimeException("Bad cell");
		else{ //Don't need to check if neighbor is a valid cell as long as set is only valid cells
			Cell c1 = new Cell(locI, locJ+1);
			Cell c2 = new Cell(locI, locJ-1);
			Cell c3 = new Cell(locI+1, locJ);
			Cell c4 = new Cell(locI-1, locJ);
			if(set.contains(c1) || set.contains(c2) || set.contains(c3) || set.contains(c4)){
				return true;
			}
			else{
				return false;
			}
		}
	}
	
	private boolean isNextToRoad(int locI, int locJ, Land land) {
		if (locJ + 1 >= land.side || land.getCellType(locI, locJ + 1) == Cell.Type.ROAD)
			return true;
		if (locJ - 1 < 0 || land.getCellType(locI, locJ - 1) == Cell.Type.ROAD)
			return true;
		if (locI + 1 >= land.side || land.getCellType(locI + 1, locJ) == Cell.Type.ROAD)
			return true;
		if (locI - 1 < 0 || land.getCellType(locI - 1, locJ) == Cell.Type.ROAD)
			return true;
		return false;
	}

}
