package pentos.g8;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Building.Type;
import pentos.sim.Land;
import pentos.sim.Move;
import pentos.sim.Cell;

public class Player implements pentos.sim.Player {

	private Random gen = new Random();
	private Set<Cell> road_cells = new HashSet<Cell>();
	private ArrayList<ArrayList<Move>> optimized = new ArrayList<ArrayList<Move>>();
	private ArrayList<Building> buildingTypes = new ArrayList<Building>();

	private ArrayList<Set<Cell>> parks = new ArrayList<Set<Cell>>();
	private ArrayList<Set<Cell>> allRoads = new ArrayList<Set<Cell>>();
	private ArrayList<Set<Cell>> allPonds = new ArrayList<Set<Cell>>();

	// The boundary of optimized region
	private static final int OPT_TOP = 22;
	private static final int OPT_BUT = 49;
	private static final int OPT_RIGHT = 20;
	private static final int OPT_LEFT = 0;
	private boolean pond = true;

	// function is called once at the beginning before play is called
	public void init() { 
		addParks();
		addWater();
		addRoads();
		
		addShape1();
		addShape2();
		addShape3();
		addShape4();
		addShape5();
		addShape6();
		addShape7();
		addShape8();
		addShape9();
		addShape10();
		addShape11();
		addShape12();
		addShape13();
		addShape14();
		addShape15();
		addShape16();
		addShape17();
		addShape18();
	}

	public Move play(Building request, Land land) {

		ArrayList<Move> moves = new ArrayList<Move>();
		ArrayList<Move> moves_opt = new ArrayList<Move>();
		ArrayList<Integer> largestArea = new ArrayList<Integer>();

		Move move_opt = getOptMove(request, land);
		if (move_opt != null) {
			return move_opt;
		}

		// find all valid building locations and orientations
		for (int i = 0; i < land.side; i++)
			for (int j = 0; j < land.side; j++) {
				Cell p = new Cell(i, j);
				Building[] rotations = request.rotations();
				for (int ri = 0; ri < rotations.length; ri++) {
					Building b = rotations[ri];
					if (land.buildable(b, p)) {
						Move m = new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(),
								new HashSet<Cell>());
						if (!isInOptimizedArea(m)) {
							moves.add(m);
						} else {
							moves_opt.add(m);
						}

					}
				}
			}

		Move move_normal = getNormalMove(moves, land, request, largestArea);
		if (move_normal != null) {
			return move_normal;
		}

		Move move_normal_in_optimized = getNormalMoveInOpt(moves_opt, land, request, largestArea);
		if (move_normal_in_optimized != null) {
			return move_normal_in_optimized;
		}

		return new Move(false);
	}

	private boolean isInOptimizedArea(Move chosen) {
		if (chosen == null) {
			return false;
		}
		Set<Cell> shiftedBuilding = shiftCells(chosen);
		for (Cell c : shiftedBuilding) {
			if (isInOptimizedArea(c) == true) {
				return true;
			}
		}
		return false;
	}

	private boolean isInOptimizedArea(Set<Cell> cells) {
		if (cells == null) {
			return false;
		}
		for (Cell c : cells) {
			if (isInOptimizedArea(c) == true) {
				return true;
			}
		}
		return false;
	}

	private boolean isInOptimizedArea(Cell c) {
		if (c == null) {
			return false;
		}
		int x = c.i;
		int y = c.j;
		if (x >= Player.OPT_TOP && x <= Player.OPT_BUT && y >= OPT_LEFT && y <= OPT_RIGHT) {
			return true;
		}
		return false;
	}

	private Set<Cell> shiftCells(Move move) {
		Set<Cell> res = new HashSet<Cell>();
		Building[] rotations = move.request.rotations();
		Building b = rotations[move.rotation];
		for (Cell c : b) {
			res.add(new Cell(c.i + move.location.i, c.j + move.location.j));
		}
		return res;
	}

	private Move getNormalMove(List<Move> moves, Land land, Building request, List<Integer> areas) {

		PriorityQueue<int[]> pq = getCompactMoveIndex(moves, land, request);

		Set<Cell> shiftedCells = new HashSet<Cell>();
		Set<Cell> roadCells = new HashSet<Cell>();

		Move chosen = new Move(false);
		while (!pq.isEmpty()) {
			int[] pair = pq.poll();
			int index = pair[1];
			chosen = moves.get(index);
			shiftedCells = shiftCells(chosen);
			roadCells = findShortestRoad(shiftedCells, land);
			// find the road and it's not inside the opt region
			if (roadCells != null && !isInOptimizedArea(roadCells)) { 
				break;
			}
		}

		if (roadCells != null) {
			chosen.road = roadCells;
			road_cells.addAll(roadCells);
			// for residences, build random ponds and fields connected to it
			if (request.type == Building.Type.RESIDENCE) { 
				Set<Cell> markedForConstruction = new HashSet<Cell>();
				markedForConstruction.addAll(roadCells);

				Set<Cell> ponds = randomWalk(shiftedCells, markedForConstruction, land, 4);
				if (!isInOptimizedArea(ponds))
					chosen.water = ponds;

				markedForConstruction.addAll(chosen.water);

				Set<Cell> parks = randomWalk(shiftedCells, markedForConstruction, land, 4);
				if (!isInOptimizedArea(parks))
					chosen.park = parks;

			}

			if (chosen.request == null) {
				return null;
			}

			if (!land.buildable(chosen.request.rotations()[chosen.rotation], chosen.location)) {
				return null;
			}
			return chosen;
		}

		return null;
	}

	private Move getNormalMoveInOpt(List<Move> moves, Land land, Building request, List<Integer> areas) {

		PriorityQueue<int[]> pq = getCompactMoveIndex(moves, land, request);

		Set<Cell> shiftedCells = new HashSet<Cell>();
		Set<Cell> roadCells = new HashSet<Cell>();

		Move chosen = new Move(false);
		while (!pq.isEmpty()) {
			int[] pair = pq.poll();
			int index = pair[1];
			chosen = moves.get(index);
			shiftedCells = shiftCells(chosen);
			roadCells = findShortestRoad(shiftedCells, land);
			if (roadCells != null) {
				break;
			}
		}

		if (roadCells != null) {
			chosen.road = roadCells;
			road_cells.addAll(roadCells);
			//for residences, build random ponds and fields connected to it
			if (request.type == Building.Type.RESIDENCE) { 
				Set<Cell> markedForConstruction = new HashSet<Cell>();
				markedForConstruction.addAll(roadCells);
				chosen.water = randomWalk(shiftedCells, markedForConstruction, land, 4);
				markedForConstruction.addAll(chosen.water);
				chosen.park = randomWalk(shiftedCells, markedForConstruction, land, 4);
			}

			if (chosen.request == null) {
				return null;
			}

			if (!land.buildable(chosen.request.rotations()[chosen.rotation], chosen.location)) {
				return null;
			}
			return chosen;
		}

		return null;
	}

	private boolean connectedToRoad(Move chosen){
		
		Set<Cell> shiftedCells = new HashSet<Cell>();
		for (Cell x : chosen.request.rotations()[chosen.rotation])
			shiftedCells.add(new Cell(x.i + chosen.location.i, x.j + chosen.location.j));

		for (Cell p : shiftedCells) {
			for (Cell q : p.neighbors()) {
				if(road_cells.contains(q)){
					return true;
				}
			}
		}
		return false;		
	}

	private int getWeight(Set<Cell> shiftedCells, Land land, Building request) {
		int weight = 0; 
		for (Cell shiftedCell : shiftedCells) {
			for (Cell neighbor : shiftedCell.neighbors()) {
				if (!shiftedCells.contains(neighbor) && land.unoccupied(neighbor)) { 
					weight += 10; 
				}
				
				if (request.type == Building.Type.RESIDENCE) {
					if (land.isPond(neighbor) || land.isField(neighbor)) {
						weight -= 3;
					}
				}

				if (request.type == Building.Type.FACTORY) {
					if (land.getCellType(neighbor) == Cell.Type.FACTORY) {
						weight -= 5;
					}
					if (land.isPond(neighbor) || land.isField(neighbor)) {
						weight += 100000;
					}
				}
			}
		}
		return weight;
	}

	private PriorityQueue<int[]> getCompactMoveIndex(List<Move> moves, Land land, Building request) {
		PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);

		for (int i = 0 ; i < moves.size() ; ++i) {
			int index = i;
			index = request.type == Building.Type.FACTORY ? index : moves.size() - index -1 ;
			Set<Cell> shiftedCells = shiftCells(moves.get(index));
			int weight = getWeight(shiftedCells, land, request);
			int[] pair = {weight, index};
			pq.offer(pair);

		}
		return pq;
	}

	private Move getOptMove(Building request, Land land) {

		for (int i = 0; i < optimized.size(); i++) {
			if (request.equals(buildingTypes.get(i))) {
				if (!optimized.get(i).isEmpty()) {
					Move m = optimized.get(i).get(0);
					optimized.get(i).remove(0);
					
					int nextToParkOrWater = nextToWaterOrPark(m, land);
					
					if(nextToParkOrWater == 3){
						m.park = new HashSet<Cell>();
						m.water = new HashSet<Cell>();
					}else if(nextToParkOrWater == 2){
						m.water = new HashSet<Cell>();
					}else if(nextToParkOrWater == 1){
						m.park = new HashSet<Cell>();
					}
					
					Building hardCoded = m.request;
					
					m.rotation = getCorrectRotation(request, hardCoded, m.rotation);
					m.request = request;

					if(connectedToRoad(m)){
						m.road = new HashSet<Cell>();
					}else{
						road_cells.addAll(m.road);
					}

					if(!land.buildable(m.request.rotations()[m.rotation], m.location)){
						Building[] buildings = m.request.rotations();
						for(int j = 0; j < buildings.length;j++){
							if(land.buildable(buildings[j],m.location)){
								m.rotation = j;
								return m;
							}
						}
						return null;
					}
					return m;
				}
			}
		}

		return null;
	}

	private int nextToWaterOrPark(Move chosen, Land land) {
		boolean park = false;
		boolean water = false;
		
		Set<Cell> shiftedCells = new HashSet<Cell>();
		for (Cell x : chosen.request.rotations()[chosen.rotation])
			shiftedCells.add(new Cell(x.i + chosen.location.i, x.j + chosen.location.j));

		for (Cell p : shiftedCells) {
			for (Cell q : p.neighbors()) {
				if (land.isField(q)) {
					park = true;
				}
				if (land.isPond(q)) {
					water = true;
				}
			}
		}
		
		if(park && water){
			return 3;
		}else if(park){
			return 1;
		}else if(water){
			return 2;
		}else{
			return 0;
		}
	}

	private int getCorrectRotation(Building request, Building hardCoded, int rotationOfHardCoded) {
		Building newHardCoded = hardCoded.rotations()[rotationOfHardCoded];

		Building[] rotationsOfRequest = request.rotations();
		for (int i = 0; i < rotationsOfRequest.length; i++) {
			Building newRequest = rotationsOfRequest[i];
			if (newRequest.hash == newHardCoded.hash) {
				return i;
			}

		}
		return -1;
	}


	private boolean isReached(Set<Cell> cells, Land land) {
		for (int z=0; z<land.side; z++) {
			if (
				cells.contains(new Cell(0, z)) || 
				cells.contains(new Cell(z, 0)) || 
				cells.contains(new Cell(land.side-1, z)) || 
				cells.contains(new Cell(z, land.side-1))
			) {
				return true;
			}		
		}
		for (Cell p : road_cells) {
			for (Cell q : p.neighbors()) {
				if (cells.contains(q)) return true;
			}
		}
		return false;
	}



	private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
		Set<Cell> output = new HashSet<Cell>();
		boolean[][] checked = new boolean[land.side][land.side];
		Queue<Cell> queue = new LinkedList<Cell>();

		// if cells are on the boarder or are connected to current roads
		if (isReached(b, land)) return output;

		// add border cells that don't have a road currently
		Cell source = new Cell(Integer.MAX_VALUE, Integer.MAX_VALUE); 

		for (int z = 0; z < land.side; z++) {
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
				// use tail field of cell to keep track of previous road cell during the search
				if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q))
					queue.add(new Cell(q.i, q.j, p)); 
			}
		}
		
		while (!queue.isEmpty()) {
			Cell p = queue.remove();
			checked[p.i][p.j] = true;
			for (Cell x : p.neighbors()) {
				// trace back through search tree to find path
				if (b.contains(x)) { 
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

	/*
	 * Walk n consecutive cells starting from a building.
	 * Used to build a random field or pond.
	 */
	private Set<Cell> randomWalk(Set<Cell> b, Set<Cell> marked, Land land, int n) {
		ArrayList<Cell> adjCells = new ArrayList<Cell>();
		Set<Cell> output = new HashSet<Cell>();
		for (Cell p : b) {
			for (Cell q : p.neighbors()) {
				if (land.isField(q) || land.isPond(q))
					return new HashSet<Cell>();
				if (!b.contains(q) && !marked.contains(q) && land.unoccupied(q))
					adjCells.add(q);
			}
		}
		if (adjCells.isEmpty())
			return new HashSet<Cell>();
		Cell tail = adjCells.get(gen.nextInt(adjCells.size()));
		for (int ii = 0; ii < n; ii++) {
			ArrayList<Cell> walk_cells = new ArrayList<Cell>();
			for (Cell p : tail.neighbors()) {
				if (!b.contains(p) && !marked.contains(p) && land.unoccupied(p) && !output.contains(p))
					walk_cells.add(p);
			}
			if (walk_cells.isEmpty()) {
				return new HashSet<Cell>();
			}
			output.add(tail);
			tail = walk_cells.get(gen.nextInt(walk_cells.size()));
		}
		pond = !pond;
		return output;
	}
	
	/*
	 * Creates all the parks that are 
	 * attached to optimized buildings
	 */
	private void addParks() {

		Set<Cell> parkSet = new HashSet<Cell>();

		//0
		parkSet.add(new Cell(49, 3));
		parkSet.add(new Cell(48, 3));
		parkSet.add(new Cell(48, 2));
		parkSet.add(new Cell(47, 2));

		parks.add(parkSet);
		parkSet = new HashSet<Cell>();
		
		//1
		parkSet.add(new Cell(44, 0));
		parkSet.add(new Cell(44, 1));
		parkSet.add(new Cell(44, 2));
		parkSet.add(new Cell(44, 3));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//2
		parkSet.add(new Cell(47, 6));
		parkSet.add(new Cell(46, 6));
		parkSet.add(new Cell(46, 8));
		parkSet.add(new Cell(46, 7));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//3
		parkSet.add(new Cell(47, 12));
		parkSet.add(new Cell(47, 13));
		parkSet.add(new Cell(46, 12));
		parkSet.add(new Cell(46, 13));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//4
		parkSet.add(new Cell(45, 17));
		parkSet.add(new Cell(45, 18));
		parkSet.add(new Cell(46, 17));
		parkSet.add(new Cell(46, 18));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//5
		parkSet.add(new Cell(39, 4));
		parkSet.add(new Cell(39, 5));
		parkSet.add(new Cell(38, 5));
		parkSet.add(new Cell(37, 5));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//6
		parkSet.add(new Cell(39, 8));
		parkSet.add(new Cell(39, 9));
		parkSet.add(new Cell(38, 9));
		parkSet.add(new Cell(38, 10));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//7
		parkSet.add(new Cell(40, 12));
		parkSet.add(new Cell(40, 13));
		parkSet.add(new Cell(41, 12));
		parkSet.add(new Cell(42, 12));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//8
		parkSet.add(new Cell(37, 16));
		parkSet.add(new Cell(38, 16));
		parkSet.add(new Cell(39, 16));
		parkSet.add(new Cell(40, 16));
		parkSet.add(new Cell(40, 17));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();
		
		//9
		parkSet.add(new Cell(33, 2));
		parkSet.add(new Cell(33, 3));
		parkSet.add(new Cell(33, 4));
		parkSet.add(new Cell(33, 5));
		parkSet.add(new Cell(32, 4));
		parkSet.add(new Cell(31, 4));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();
		
		//10
		parkSet.add(new Cell(33, 9));
		parkSet.add(new Cell(32, 9));
		parkSet.add(new Cell(32, 10));
		parkSet.add(new Cell(31, 10));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();
		
		//11
		parkSet.add(new Cell(31, 14));
		parkSet.add(new Cell(31, 15));
		parkSet.add(new Cell(31, 16));
		parkSet.add(new Cell(32, 16));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//12
		parkSet.add(new Cell(24, 3));
		parkSet.add(new Cell(23, 3));
		parkSet.add(new Cell(23, 4));
		parkSet.add(new Cell(23, 5));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//13
		parkSet.add(new Cell(26, 3));
		parkSet.add(new Cell(26, 4));
		parkSet.add(new Cell(27, 3));
		parkSet.add(new Cell(27, 4));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//14
		parkSet.add(new Cell(26, 10));
		parkSet.add(new Cell(25, 11));
		parkSet.add(new Cell(24, 11));
		parkSet.add(new Cell(26, 11));
		
		parks.add(parkSet);
		parkSet = new HashSet<Cell>();

		//15
		parkSet.add(new Cell(26, 14));
		parkSet.add(new Cell(25, 14));
		parkSet.add(new Cell(25, 15));
		parkSet.add(new Cell(24, 15));
		
		parks.add(parkSet);
	}

	/*
	 * Creates all the ponds that are 
	 * attached to optimized buildings
	 */
	private void addWater() {
		
		Set<Cell> ponds = new HashSet<Cell>();
		
		//0
		ponds.add(new Cell(47, 0));
		ponds.add(new Cell(47, 1));
		ponds.add(new Cell(46, 1));
		ponds.add(new Cell(46, 2));
		ponds.add(new Cell(46, 3));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//1
		ponds.add(new Cell(36, 1));
		ponds.add(new Cell(36, 2));
		ponds.add(new Cell(37, 1));
		ponds.add(new Cell(37, 2));
		ponds.add(new Cell(38, 2));
		ponds.add(new Cell(38, 3));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//2
		ponds.add(new Cell(37, 6));
		ponds.add(new Cell(38, 6));
		ponds.add(new Cell(39, 6));
		ponds.add(new Cell(38, 7));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//3
		ponds.add(new Cell(48, 14));
		ponds.add(new Cell(48, 15));
		ponds.add(new Cell(49, 14));
		ponds.add(new Cell(49, 15));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//4
		ponds.add(new Cell(43, 12));
		ponds.add(new Cell(43, 13));
		ponds.add(new Cell(43, 14));
		ponds.add(new Cell(44, 13));
		ponds.add(new Cell(45, 13));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//5
		ponds.add(new Cell(47, 5));
		ponds.add(new Cell(48, 5));
		ponds.add(new Cell(48, 6));
		ponds.add(new Cell(49, 6));
		ponds.add(new Cell(49, 7));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//6
		ponds.add(new Cell(36, 15));
		ponds.add(new Cell(37, 15));
		ponds.add(new Cell(36, 16));
		ponds.add(new Cell(36, 17));
		ponds.add(new Cell(36, 18));
		ponds.add(new Cell(36, 19));
		ponds.add(new Cell(37, 19));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//7
		ponds.add(new Cell(42, 18));
		ponds.add(new Cell(42, 19));
		ponds.add(new Cell(43, 18));
		ponds.add(new Cell(43, 19));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//8
		ponds.add(new Cell(33, 1));
		ponds.add(new Cell(32, 1));
		ponds.add(new Cell(32, 2));
		ponds.add(new Cell(32, 3));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//9
		ponds.add(new Cell(31, 6));
		ponds.add(new Cell(31, 7));
		ponds.add(new Cell(32, 7));
		ponds.add(new Cell(32, 8));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//10
		ponds.add(new Cell(33, 11));
		ponds.add(new Cell(33, 12));
		ponds.add(new Cell(32, 12));
		ponds.add(new Cell(32, 13));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//11
		ponds.add(new Cell(31, 17));
		ponds.add(new Cell(31, 18));
		ponds.add(new Cell(32, 17));
		ponds.add(new Cell(32, 18));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//12
		ponds.add(new Cell(40, 11));
		ponds.add(new Cell(39, 11));
		ponds.add(new Cell(39, 12));
		ponds.add(new Cell(38, 12));
		
		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//13
		ponds.add(new Cell(24, 1));
		ponds.add(new Cell(24, 2));
		ponds.add(new Cell(25, 2));
		ponds.add(new Cell(25, 3));

		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//14
		ponds.add(new Cell(24, 7));
		ponds.add(new Cell(24, 8));
		ponds.add(new Cell(25, 7));
		ponds.add(new Cell(25, 8));

		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//15
		ponds.add(new Cell(26, 12));
		ponds.add(new Cell(26, 13));
		ponds.add(new Cell(27, 11));
		ponds.add(new Cell(27, 12));

		allPonds.add(ponds);
		ponds = new HashSet<Cell>();

		//16
		ponds.add(new Cell(25, 16));
		ponds.add(new Cell(25, 17));
		ponds.add(new Cell(25, 18));
		ponds.add(new Cell(26, 16));

		allPonds.add(ponds);
	}

	/*
	 * Creates all the roads that are 
	 * attached to optimized buildings
	 */
	private void addRoads() {
		
		Set<Cell> roads = new HashSet<Cell>();
		
		//0
		roads.add(new Cell(49, 20));
		roads.add(new Cell(48, 20));
		roads.add(new Cell(47, 20));
		roads.add(new Cell(46, 20));
		roads.add(new Cell(45, 20));
		roads.add(new Cell(44, 20));
		roads.add(new Cell(43, 20));
		roads.add(new Cell(42, 20));
		roads.add(new Cell(41, 20));
		roads.add(new Cell(40, 20));
		roads.add(new Cell(39, 20));
		roads.add(new Cell(38, 20));
		roads.add(new Cell(37, 20));
		roads.add(new Cell(36, 20));
		roads.add(new Cell(35, 20));
		
		roads.add(new Cell(41, 19));
		roads.add(new Cell(41, 18));
		roads.add(new Cell(41, 17));
		roads.add(new Cell(41, 16));
		roads.add(new Cell(42, 16));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//1
		roads.add(new Cell(35, 19));
		roads.add(new Cell(35, 18));
		roads.add(new Cell(35, 17));
		roads.add(new Cell(35, 16));
		roads.add(new Cell(35, 15));
		roads.add(new Cell(35, 14));
		roads.add(new Cell(35, 13));
		roads.add(new Cell(35, 12));
		roads.add(new Cell(35, 11));
		roads.add(new Cell(35, 10));
		roads.add(new Cell(35, 9));
		roads.add(new Cell(35, 8));
		roads.add(new Cell(35, 7));
		roads.add(new Cell(35, 6));
		roads.add(new Cell(35, 5));
		roads.add(new Cell(35, 4));
		roads.add(new Cell(35, 3));
		roads.add(new Cell(35, 2));
		roads.add(new Cell(35, 1));
		roads.add(new Cell(35, 0));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//2
		roads.add(new Cell(49, 9));
		roads.add(new Cell(48, 9));
		roads.add(new Cell(47, 9));
		roads.add(new Cell(46, 9));
		roads.add(new Cell(45, 9));
		roads.add(new Cell(44, 9));
		roads.add(new Cell(43, 9));
		roads.add(new Cell(42, 9));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//3
		roads.add(new Cell(42, 8));
		roads.add(new Cell(42, 7));
		roads.add(new Cell(42, 6));
		roads.add(new Cell(42, 5));
		roads.add(new Cell(42, 4));
		roads.add(new Cell(42, 3));
		roads.add(new Cell(42, 2));
		roads.add(new Cell(42, 1));
		roads.add(new Cell(42, 0));

		roads.add(new Cell(43, 5));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//4
		roads.add(new Cell(49, 16));
		roads.add(new Cell(48, 16));
		roads.add(new Cell(47, 16));
		roads.add(new Cell(46, 16));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//5
		roads.add(new Cell(29, 0));
		roads.add(new Cell(29, 1));
		roads.add(new Cell(29, 2));
		roads.add(new Cell(29, 3));
		roads.add(new Cell(29, 4));
		roads.add(new Cell(29, 5));
		roads.add(new Cell(29, 6));
		roads.add(new Cell(29, 7));
		roads.add(new Cell(29, 8));
		roads.add(new Cell(29, 9));
		roads.add(new Cell(29, 10));
		roads.add(new Cell(29, 11));
		roads.add(new Cell(29, 12));
		roads.add(new Cell(29, 13));
		roads.add(new Cell(29, 14));
		roads.add(new Cell(29, 15));
		roads.add(new Cell(29, 16));
		roads.add(new Cell(29, 17));
		roads.add(new Cell(29, 18));
		roads.add(new Cell(29, 19));
		roads.add(new Cell(29, 20));
		roads.add(new Cell(30, 20));
		roads.add(new Cell(31, 20));
		roads.add(new Cell(32, 20));
		roads.add(new Cell(33, 20));
		roads.add(new Cell(34, 20));

		roads.add(new Cell(26,7));
		roads.add(new Cell(27,7));
		roads.add(new Cell(28,7));
		
		allRoads.add(roads);
		roads = new HashSet<Cell>();

		//6
		roads.add(new Cell(22,0));
		roads.add(new Cell(22,1));
		roads.add(new Cell(22,2));
		roads.add(new Cell(22,3));
		roads.add(new Cell(22,4));
		roads.add(new Cell(22,5));
		roads.add(new Cell(22,6));
		roads.add(new Cell(22,7));
		roads.add(new Cell(22,8));
		roads.add(new Cell(22,9));
		roads.add(new Cell(22,10));
		roads.add(new Cell(22,11)); 
		roads.add(new Cell(22,12));
		roads.add(new Cell(22,13));
		roads.add(new Cell(22,14));
		roads.add(new Cell(22,15));
		roads.add(new Cell(22,16));
		roads.add(new Cell(22,17));
		roads.add(new Cell(22,18));
		roads.add(new Cell(22,19));
		roads.add(new Cell(22,20));

		roads.add(new Cell(23,20));
		roads.add(new Cell(24,20));
		roads.add(new Cell(25,20));
		roads.add(new Cell(26,20));
		roads.add(new Cell(27,20));
		roads.add(new Cell(28,20));

		allRoads.add(roads);
	}

	/* . .
	 * . . .
	 * 
	 */
	private void addShape1() {
		ArrayList<Move> shape1 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(49, 0);
		building[1] = new Cell(48, 0);
		building[2] = new Cell(48, 1);
		building[3] = new Cell(49, 1);
		building[4] = new Cell(49, 2);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(48, 0), 0, new HashSet<Cell>(),
			allPonds.get(0), parks.get(0)));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(43, 6), 2, allRoads.get(2),
			new HashSet<Cell>(), new HashSet<Cell>()));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(47, 10), 0, allRoads.get(2),
			new HashSet<Cell>(), parks.get(3)));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(45, 14), 3, allRoads.get(4),
			allPonds.get(3), parks.get(3)));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(23, 13), 1, allRoads.get(6),
			new HashSet<Cell>(), parks.get(15)));
		shape1.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(23, 15), 2, allRoads.get(5),
			new HashSet<Cell>(), parks.get(13)));
		optimized.add(shape1);
	}

	/*
	 * . . . .
	 * .
	 */
	private void addShape2() {
		ArrayList<Move> shape2 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(46, 0);
		building[1] = new Cell(45, 0);
		building[2] = new Cell(45, 1);
		building[3] = new Cell(45, 2);
		building[4] = new Cell(45, 3);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(45, 0), 0, new HashSet<Cell>(),
			allPonds.get(0), parks.get(1)));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(45, 5), 0, allRoads.get(2),
			allPonds.get(5),parks.get(2)));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(48, 10), 2, new HashSet<Cell>(),
			allPonds.get(3), parks.get(3)));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 12), 1, allRoads.get(0),
			allPonds.get(12), parks.get(7)));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(33, 7), 2, allRoads.get(1),
			allPonds.get(10), parks.get(10)));
		shape2.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(27, 3), 2, allRoads.get(5),
			new HashSet<Cell>(), parks.get(13)));
		optimized.add(shape2);
	}

	/*
	 *   .
	 * . . .
	 *   .
	 */
	private void addShape3() {
		ArrayList<Move> shape3 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(36, 11);
		building[1] = new Cell(37, 10);
		building[2] = new Cell(37, 11);
		building[3] = new Cell(37, 12);
		building[4] = new Cell(38, 11);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape3.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 10), 0, allRoads.get(1),
			allPonds.get(12), parks.get(6)));
		shape3.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(25, 0), 0, new HashSet<Cell>(),
			allPonds.get(13), parks.get(13)));
		shape3.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(26, 16), 0, allRoads.get(5),
			allPonds.get(16), new HashSet<Cell>()));
		optimized.add(shape3);
	}

	/*
	 * . .
	 * .
	 * . .
	 */
	private void addShape4() {
		ArrayList<Move> shape4 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(49, 5);
		building[1] = new Cell(49, 6);
		building[2] = new Cell(48, 5);
		building[3] = new Cell(47, 5);
		building[4] = new Cell(47, 6);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape4.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(44, 14), 1, allRoads.get(4),
			allPonds.get(4), parks.get(4)));
		shape4.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(39, 2), 0, allRoads.get(3),
			allPonds.get(1), parks.get(5)));
		shape4.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(32, 14), 2, allRoads.get(1),
			allPonds.get(10), parks.get(11)));
		shape4.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(27, 0), 3, new HashSet<Cell>(),
			new HashSet<Cell>(), parks.get(13)));
		shape4.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(27, 8), 3, allRoads.get(5),
			allPonds.get(15), parks.get(14)));
		shape4.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(26, 18), 2, allRoads.get(5),
			allPonds.get(16), new HashSet<Cell>()));
		optimized.add(shape4);
	}

	/*   . .
	 * . . .
	 */
	private void addShape5() {
		ArrayList<Move> shape5 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(45, 6);
		building[1] = new Cell(45, 7);
		building[2] = new Cell(45, 8);
		building[3] = new Cell(44, 7);
		building[4] = new Cell(44, 8);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(47, 7), 3, new HashSet<Cell>(),
			allPonds.get(5), parks.get(2)));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(45, 10), 2, allRoads.get(2),
			allPonds.get(4), parks.get(3)));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 3), 3, allRoads.get(1),
			allPonds.get(1),parks.get(5)));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(33, 16), 2, allRoads.get(1),
			allPonds.get(11), parks.get(11)));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 12), 2, allRoads.get(5),
			allPonds.get(10), parks.get(11)));
		shape5.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(23, 18), 3, allRoads.get(6),
			allPonds.get(16), new HashSet<Cell>()));
		optimized.add(shape5);
	}

	/*
	 * . . . . .
	 */
	private void addShape6() {
		ArrayList<Move> shape6 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(43, 0);
		building[1] = new Cell(43, 1);
		building[2] = new Cell(43, 2);
		building[3] = new Cell(43, 3);
		building[4] = new Cell(43, 4);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(43, 0), 0, new HashSet<Cell>(),
			new HashSet<Cell>(), parks.get(1)));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 0), 1, new HashSet<Cell>(),
			allPonds.get(1), new HashSet<Cell>()));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 14), 1, allRoads.get(1),
			allPonds.get(6),parks.get(7)));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(34, 0), 0, new HashSet<Cell>(),
			allPonds.get(8), parks.get(9)));
		shape6.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 15), 0, allRoads.get(5),
			allPonds.get(11), parks.get(11)));
		optimized.add(shape6);
	}

	/* . .
	 *   .
	 *   . .
	 */
	private void addShape7() {
		ArrayList<Move> shape7 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(44, 4);
		building[1] = new Cell(44, 5);
		building[2] = new Cell(45, 5);
		building[3] = new Cell(46, 5);
		building[4] = new Cell(46, 6);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape7.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(47, 3), 0, new HashSet<Cell>(),
			allPonds.get(5), parks.get(0)));
		shape7.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(39, 8), 1, allRoads.get(3),allPonds.get(12), parks.get(6)));
		shape7.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 4), 0, allRoads.get(5),allPonds.get(9), parks.get(9)));
		optimized.add(shape7);
	}

	/*   .
	 *   .
	 *   .
	 * . . 
	 */
	private void addShape8() {
		ArrayList<Move> shape8 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(47, 18);
		building[1] = new Cell(47, 19);
		building[2] = new Cell(46, 19);
		building[3] = new Cell(45, 19);
		building[4] = new Cell(44, 19);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(44, 18), 0, allRoads.get(0),
			allPonds.get(7), parks.get(4)));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(38, 14), 0, allRoads.get(0),
			allPonds.get(6), parks.get(8)));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(38, 0), 0, new HashSet<Cell>(),
			allPonds.get(1), new HashSet<Cell>()));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(31, 18), 0, allRoads.get(1),
			allPonds.get(11), new HashSet<Cell>()));
		shape8.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 0), 3, new HashSet<Cell>(),
			allPonds.get(8), parks.get(9)));
		optimized.add(shape8);
	}

	/*   
	 *   . . .
	 *   .
	 *   .
	 */
	private void addShape9() {
		ArrayList<Move> shape9 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(44, 4);
		building[1] = new Cell(44, 5);
		building[2] = new Cell(44, 6);
		building[3] = new Cell(45, 4);
		building[4] = new Cell(46, 4);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(44, 4), 0, allRoads.get(3),
			allPonds.get(0), parks.get(1)));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(39, 5), 2, allRoads.get(3),
			allPonds.get(2), parks.get(6)));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(31, 0), 0, new HashSet<Cell>(),
			allPonds.get(8), new HashSet<Cell>()));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 9), 1, allRoads.get(5),
			allPonds.get(10), parks.get(10)));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(23, 0), 0, allRoads.get(6),
			allPonds.get(13), parks.get(12)));
		shape9.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(23, 10), 2, allRoads.get(6),
			allPonds.get(15), parks.get(14)));
		optimized.add(shape9);
	}

	/*
	 *   . .
	 * . .
	 * .
	 */
	private void addShape10() {
		ArrayList<Move> shape10 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(38, 8);
		building[1] = new Cell(37, 8);
		building[2] = new Cell(37, 9);
		building[3] = new Cell(36, 9);
		building[4] = new Cell(36, 10);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape10.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 8), 0, allRoads.get(1),
			allPonds.get(2), parks.get(6)));
		shape10.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(38, 17), 3, allRoads.get(0),
			new HashSet<Cell>(), parks.get(8)));
		shape10.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(37, 17), 1, allRoads.get(0),
			allPonds.get(6), parks.get(8)));
		optimized.add(shape10);
	}

	/*
	 * . . .
	 *     . .
	 * 
	 */
	private void addShape11() {
		ArrayList<Move> shape11 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(30, 6);
		building[1] = new Cell(30, 7);
		building[2] = new Cell(30, 8);
		building[3] = new Cell(31, 8);
		building[4] = new Cell(31, 9);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape11.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(30, 6), 0, allRoads.get(5),
			allPonds.get(9), parks.get(10)));
		shape11.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(23, 7), 0, allRoads.get(6),
			allPonds.get(14), parks.get(14)));
		optimized.add(shape11);
	}

	/*
	 *    . . .
	 *  . .
	 */
	private void addShape12() {
		ArrayList<Move> shape12 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(34, 5);
		building[1] = new Cell(34, 6);
		building[2] = new Cell(33, 6);
		building[3] = new Cell(33, 7);
		building[4] = new Cell(33, 8);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape12.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(33, 5), 0, allRoads.get(1),
			allPonds.get(9),parks.get(9)));
		shape12.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(33, 11), 2, allRoads.get(1),
			allPonds.get(10), new HashSet<Cell>()));
		shape12.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(27, 11), 2, allRoads.get(5),
			allPonds.get(15), parks.get(15)));
		optimized.add(shape12);
	}

	/*
	 * .
	 * . . .
	 *     .
	 * 
	 */
	private void addShape13() {
		ArrayList<Move> shape13 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(41, 13);
		building[1] = new Cell(42, 13);
		building[2] = new Cell(42, 14);
		building[3] = new Cell(42, 15);
		building[4] = new Cell(43, 15);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape13.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(41, 13), 0, allRoads.get(0),
			allPonds.get(4), parks.get(7)));
		optimized.add(shape13);
	}

	/*
	 * . . . .
	 *   .   
	 * 
	 */
	private void addShape14() {
		ArrayList<Move> shape14 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(40, 3);
		building[1] = new Cell(40, 4);
		building[2] = new Cell(40, 5);
		building[3] = new Cell(40, 6);
		building[4] = new Cell(41, 4);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape14.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(40, 3), 0, allRoads.get(3),
			allPonds.get(2), parks.get(5)));
		optimized.add(shape14);
	}

	/*
	 * . . . .
	 *     .   
	 * 
	 */
	private void addShape15() {
		ArrayList<Move> shape15 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(36, 5);
		building[1] = new Cell(36, 6);
		building[2] = new Cell(36, 7);
		building[3] = new Cell(36, 8);
		building[4] = new Cell(37, 7);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape15.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(36, 5), 0, allRoads.get(1),
			allPonds.get(2), parks.get(5)));
		optimized.add(shape15);
	}

	/*
	 * . . . 
	 *   .   
	 *   .
	 */
	private void addShape16() {
		ArrayList<Move> shape16 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(41, 9);
		building[1] = new Cell(41, 10);
		building[2] = new Cell(41, 11);
		building[3] = new Cell(42, 10);
		building[4] = new Cell(43, 10);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape16.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(41, 9), 0, allRoads.get(2),
			allPonds.get(12), parks.get(7)));
		shape16.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(42, 10), 2, allRoads.get(2),
			allPonds.get(4), parks.get(7)));
		shape16.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(23, 4), 1, allRoads.get(6),
			allPonds.get(14), parks.get(12)));
		shape16.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(26, 14), 2, allRoads.get(5),
			allPonds.get(16), parks.get(15)));
		optimized.add(shape16);
	}

	/*
	 * .  
	 * . . .   
	 *   .
	 */
	private void addShape17() {
		ArrayList<Move> shape17 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(47, 17);
		building[1] = new Cell(48, 17);
		building[2] = new Cell(48, 18);
		building[3] = new Cell(48, 19);
		building[4] = new Cell(49, 18);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape17.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(47, 17), 0, allRoads.get(0),
			new HashSet<Cell>(), parks.get(4)));
		shape17.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(25, 8), 1, allRoads.get(5),
			allPonds.get(14), parks.get(14)));
		optimized.add(shape17);
	}

	/*
	 *   .  
	 * . .   
	 *   . .
	 */
	private void addShape18() {
		ArrayList<Move> shape18 = new ArrayList<Move>();
		Cell[] building = new Cell[5];
		building[0] = new Cell(43, 16);
		building[1] = new Cell(42, 17);
		building[2] = new Cell(43, 17);
		building[3] = new Cell(44, 17);
		building[4] = new Cell(44, 18);

		buildingTypes.add(new Building(building, Type.RESIDENCE));
		shape18.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(42, 16), 0, allRoads.get(0),
			allPonds.get(7), parks.get(4)));
		shape18.add(new Move(true, new Building(building, Type.RESIDENCE), new Cell(25, 4), 2, allRoads.get(5),
			allPonds.get(13), parks.get(13)));
		optimized.add(shape18);
	}
}
