package pentos.g3;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

	private static final int RESIDENCESIZE = 5;
	private Random gen = new Random();
	private Set<Cell> road_cells = new HashSet<Cell>();
	private final int NUMBER_OF_RETRY = 20;


	public void init() { // function is called once at the beginning before play is called
	}

	public Move play(Building request, Land land) {
		// find all valid building locations and orientations
		ArrayList<Move> moves = findBuildableMoves(request, land);
		// if there is not any buildable place, reject it
		if (moves.isEmpty()) return new Move(false);	
		// find all objective function values of each move, means "how bad the move is"
		ArrayList<Integer> objs = findObjectiveOfMoves(moves, land, request);
		// find the best 20 buildable moves in case some of them is detached from any raod
		ArrayList<Integer> indexes = findSmallestObjs(objs, NUMBER_OF_RETRY);
		// pick the best one among these candidates, this time include more time-consuming computation
		Move chosen = findBestMove(moves, objs, indexes, land);
		// record the place of chosen and its corresponding road
		Set<Cell> shiftedCells = shiftedCellsFromMove(chosen);
		Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
		// check if there is no reachable place
		if (roadCells == null) return new Move(false);
		// start finding reasonable pond and field for resident
		chosen.road = roadCells;
		road_cells.addAll(roadCells);
		if (request.type == Building.Type.RESIDENCE)
			setWaterAndParkToResidence(chosen, shiftedCells, roadCells, land);
		return chosen;
	}
	
	/* helper functions begin */
	// after calculating the objective functions, findSmallestObjs return the indexes of smallest n objs in order
	private ArrayList<Integer> findSmallestObjs(ArrayList<Integer> objs, int n) {
		int index = 0;
		int j = 0;
		int[] objIndex = new int[n];
		int[] objValue = new int[n];
		ArrayList<Integer> objList = new ArrayList<Integer> ();
		for (j = 0; j < n; j++) {
			objIndex[j] = 0;
			objValue[j] = Integer.MAX_VALUE;
		}
		for (Integer obj : objs) {
			for (j = n-1; j >= 0; j--) {
				if (obj < objValue[j]) {
					if (j != n-1) {
						objValue[j+1] = objValue[j];
						objIndex[j+1] = objIndex[j];
					}
					objValue[j] = obj;
					objIndex[j] = index;
				}
			}
			index += 1;
		}
		for (j = 0; j < n; j ++) {
			if (objValue[j] != Integer.MAX_VALUE) objList.add(objIndex[j]);
		}
		return objList;
	}
	// check if any of the cell in a set is direct connected to the road
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
	// check if a cell is detached from any road after some cells are marked (occupied)
	private boolean detachedFromRoad(Cell cell, Set<Cell> marked, Land land) {
		Queue<Cell> queue = new LinkedList<Cell>();
		Set<Cell> visited = new HashSet<Cell>();
		queue.add(cell);
		visited.add(cell);
		while (!queue.isEmpty()) {
			Cell p = queue.remove();
			Set<Cell> cells = new HashSet<Cell>();
			cells.add(p);
			if (isReached(cells, land)) return true;
			for (Cell q : p.neighbors()) {
				if (!land.unoccupied(q) || visited.contains(q) || marked.contains(q)) continue;
				visited.add(q);
				queue.add(q);
			}
		}
		return false;
	}
	// find the area of connected unoccupied area that contains q;
	private int findConnectedArea(Cell q, Set<Cell> marked, Land land, int upperBound) {
		if (!land.unoccupied(q)) return 0;
		Queue<Cell> queue = new LinkedList<Cell>();
		Set<Cell> record = new HashSet<Cell>();
		int area = 1;
		int upper = upperBound <= 0 ? Integer.MAX_VALUE : upperBound;
		queue.add(q);
		record.add(q);
		while (!queue.isEmpty()) {
			Cell r = queue.remove();
			for (Cell s : r.neighbors()) {
				if (record.contains(s) || marked.contains(s) || !land.unoccupied(s)) continue;
				queue.add(s);
				record.add(s);
				area += 1;
				if (area >= upper) return upper;
			}
		}
		return area;
	}
	/* helper functions end */

	/* methods on buildings begin */
	// enumarate all possible buildable moves (consider rotations)
	private ArrayList<Move> findBuildableMoves(Building request, Land land) {
		ArrayList<Move> moves = new ArrayList<Move> ();
		for (int i = 0 ; i < land.side ; i++) {
			for (int j = 0 ; j < land.side ; j++) {
				Cell p = new Cell(i, j);
				Building[] rotations = request.rotations();
				for (int ri = 0 ; ri < rotations.length ; ri++) {
					Building b = rotations[ri];
					if (land.buildable(b, p)) {
						moves.add(new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
					}
				}
			}
		}
		return moves;
	}
	// corresponding shiftedCells determined by the move
	private Set<Cell> shiftedCellsFromMove(Move move) {
		Set<Cell> shiftedCells = new HashSet<Cell>();
		for (Cell x : move.request.rotations()[move.rotation])
			shiftedCells.add(new Cell(x.i+move.location.i,x.j+move.location.j));
		return shiftedCells;
	}
	// objective function element: punish when increase new edges (too much edges means you probably have holes)
	private int edgeIncrease(Set<Cell> shiftedCells, Land land) {
		int edge_increase = 0; // the increase of new edge: the less, the better
		for (Cell shiftedCell : shiftedCells) {
			for (Cell adjCell : shiftedCell.neighbors()) {
				if (!shiftedCells.contains(adjCell) && land.unoccupied(adjCell))
					edge_increase += 1;
				if (!shiftedCells.contains(adjCell) && !land.unoccupied(adjCell))
					edge_increase -= 2;
			}
		}
		return edge_increase;
	}
	// objective function element: punish when increase detached slot (the slot that can never be used by other buildings)
	private int detachedNearbySlots(Set<Cell> potential, Set<Cell> marked, Land land) {
		int slots = 0;
		Set<Cell> oldMarked = new HashSet<Cell>(marked);
		Set<Cell> newMarked = new HashSet<Cell>(marked);
		newMarked.addAll(potential);

		for (Cell p : potential) {
			for (Cell q : p.neighbors()) {
				if (!land.unoccupied(q) || potential.contains(q) || marked.contains(q)) continue;
				if (detachedFromRoad(q, newMarked, land) && !detachedFromRoad(q, oldMarked, land)) slots += 1;
			}
		}
		return slots;
	}
	// objective function of buildings
	private ArrayList<Integer> findObjectiveOfMoves(ArrayList<Move> moves, Land land, Building request) {
		ArrayList<Integer> objs = new ArrayList<Integer> ();
		int i = 0;
		for (Move move : moves) {
			Set<Cell> shiftedCells = shiftedCellsFromMove(move);
			int obj = 0;
			obj += (request.type == Building.Type.FACTORY ? i : -i); // seperate factory and resident from top to bottom
			obj += 100 * edgeIncrease(shiftedCells, land); // punishment
			// TODO: remove hardcode
			objs.add(obj);
			i += 1;
		}
		return objs;
	}
	// given a list of good indexes, we use more computing resources to find the best one among them
	private Move findBestMove(ArrayList<Move> moves, ArrayList<Integer> objs, ArrayList<Integer> indexes, Land land) {
		Move chosen = new Move(false);
		Set<Cell> shiftedCells = new HashSet<Cell>();
		Set<Cell> roadCells = new HashSet<Cell>();
		Set<Cell> marked = new HashSet<Cell>(); // always empty in this case
		int minimalValue = Integer.MAX_VALUE;
		int miniIndex = 0;

		for (Integer index : indexes) {
			int tmpObj = objs.get(index);
			chosen = moves.get(index);
			shiftedCells = shiftedCellsFromMove(chosen);
			roadCells = findShortestRoad(shiftedCells, land);
			if (roadCells == null) {
				continue;
			} else { 
				// long road punishment
				tmpObj += 20 * roadCells.size();
				// TODO: remove hardcode here
			}
			// cut the space punishment
			tmpObj += 20 * detachedNearbySlots(shiftedCells, marked, land);
			// TODO: remove hardcode here
			if (tmpObj < minimalValue) {
				minimalValue = tmpObj;
				miniIndex = index;
			}
		}
		return moves.get(miniIndex);
	}
	/* methods on buildings end */

	/* methods on road begin */
	// build shortest sequence of road cells to connect to a set of cells b
	private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
		Set<Cell> output = new HashSet<Cell>();
		boolean[][] checked = new boolean[land.side][land.side];
		Queue<Cell> queue = new LinkedList<Cell>();

		if (isReached(b, land)) return output;

		Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); 
		for (int z=0; z<land.side; z++) {
			if (land.unoccupied(0, z))
				queue.add(new Cell(0, z, source));
			if (land.unoccupied(z, 0))
				queue.add(new Cell(z, 0, source));
			if (land.unoccupied(z, land.side-1))
				queue.add(new Cell(z, land.side-1, source));
			if (land.unoccupied(land.side-1, z))
				queue.add(new Cell(land.side-1, z, source));
		}
		// add cells adjacent to current road cells
		for (Cell p : road_cells) {
			for (Cell q : p.neighbors()) {
				if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q)) 
					queue.add(new Cell(q.i, q.j, p)); 
			}
		}
		while (!queue.isEmpty()) {
			Cell p = queue.remove();
			checked[p.i][p.j] = true;
			for (Cell x : p.neighbors()) {		
				if (b.contains(x)) { 
					Cell tail = p;
					while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
						output.add(new Cell(tail.i,tail.j));
						tail = tail.previous;
					}
					if (!output.isEmpty())
						return output;
				}
				else if (!checked[x.i][x.j] && land.unoccupied(x.i,x.j)) {
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
    /* methods on road ends */

    /* methods on park or water begin */
    private void setWaterAndParkToResidence(Move chosen, Set<Cell> shiftedCells, Set<Cell> roadCells, Land land) {
		Set<Cell> markedForConstruction = new HashSet<Cell>();
		markedForConstruction.addAll(roadCells);

		// find smaller set strategy
		Set<Cell> potentialWater = walkAndBuild(shiftedCells, markedForConstruction, land, 4, true);
		Set<Cell> potentialPark = walkAndBuild(shiftedCells, markedForConstruction, land, 4, false);

		if (potentialWater.size() == potentialPark.size()) {
			// cannot build either
			if (potentialWater.size() == 0) {
				return ;
			}
			chosen.water = potentialWater;
			markedForConstruction.addAll(chosen.water);
			potentialPark = walkAndBuild(shiftedCells, markedForConstruction, land, 4, false);
			chosen.park = potentialPark;
			markedForConstruction.addAll(chosen.park);
		} else if (potentialWater.size() < potentialPark.size() && potentialWater.size() != 0) {
			chosen.water = potentialWater;
			markedForConstruction.addAll(chosen.water);
			potentialPark = walkAndBuild(shiftedCells, markedForConstruction, land, 4, false);
			chosen.park = potentialPark;
			markedForConstruction.addAll(chosen.park);	
		} else {
			chosen.park = potentialPark;
			markedForConstruction.addAll(chosen.park);
			potentialWater = walkAndBuild(shiftedCells, markedForConstruction, land, 4, true);
			chosen.water = potentialWater;
			markedForConstruction.addAll(chosen.water);
		}
		/*
		int waterPunish = punishment(shiftedCells, markedForConstruction, land, potentialWater);
		int parkPunish = punishment(shiftedCells, markedForConstruction, land, potentialPark);

		if (waterPunish <= parkPunish) {
			chosen.water = potentialWater;
			markedForConstruction.addAll(chosen.water);
			potentialPark = walkAndBuild(shiftedCells, markedForConstruction, land, 4, false);
			chosen.park = potentialPark;
		} else {
			chosen.park = potentialPark;
			markedForConstruction.addAll(chosen.park);
			potentialWater = walkAndBuild(shiftedCells, markedForConstruction, land, 4, true);
			chosen.water = potentialWater;
		} 
		*/
	}

	private Set<Cell> walkAndBuild(Set<Cell> b, Set<Cell> marked, Land land, int n, boolean pond) {
		Set<Cell> potentialPondOrPark = new HashSet<Cell>();
		ArrayList<Set<Cell>> possibleChoices = new ArrayList<Set<Cell>>();
		int distance  = findNearbyPondOrParkDistance(b, marked, land, pond); 

		if (distance == 0) {
			return potentialPondOrPark;
		} else if (distance < 4 && distance > 0) {	
			possibleChoices = givenShortLengthWalks(b, marked, land, distance, pond);
		} else {
			possibleChoices.addAll(frankWalk(b, marked, land, n));
			/*
			possibleChoices.addAll(shardenduWalk(b, marked, land, n));
			for (int i=1; i<50; i++) {
				possibleChoices.add(randomWalk(b, marked, land, n));
			}
			*/
		}

		if (!possibleChoices.isEmpty()) {
			ArrayList<Integer> objs = new ArrayList<Integer>();
			for (Set<Cell> choice : possibleChoices) {
				objs.add(punishment(b, marked, land, choice));
			}
			ArrayList<Integer> index = findSmallestObjs(objs, 1);

			if (objs.get(index.get(0)) < 10) // TODO: remove this hardcode
				potentialPondOrPark = possibleChoices.get(index.get(0));
		}
		return potentialPondOrPark;
	}

	private int punishment(Set<Cell> b, Set<Cell> marked, Land land, Set<Cell> potential) {
		int punish = 0;
		// punish when take too much space
		punish += potential.size();

		// punish when useless holes appear
		int oldConnectedArea = 0;
		int newConnectedArea = 0;
		Set<Cell> oldMarked = new HashSet<Cell>(marked);
		Set<Cell> newMarked = new HashSet<Cell>(marked);
		oldMarked.addAll(b);
		newMarked.addAll(b); newMarked.addAll(potential);
		for (Cell p : potential) {
			for (Cell q : p.neighbors()) {
				if (b.contains(q) || marked.contains(q) || !land.unoccupied(q)) continue;
				oldConnectedArea = findConnectedArea(q, oldMarked, land, this.RESIDENCESIZE);
				newConnectedArea = findConnectedArea(q, newMarked, land, this.RESIDENCESIZE);
				punish += (oldConnectedArea == 5 && newConnectedArea < 5) ? 3 : 0;
			}
			if (detachedFromRoad(p, oldMarked, land)) punish -= 1;
		}
		// punish when ...

		return punish;
	}

	private int findNearbyPondOrParkDistance(Set<Cell> b, Set<Cell> marked, Land land, boolean pond) {
		// return 0,1,2,3 to indicate we can add 0,1,2,3 water to reach the pond or park, return -1 to indicate no nearby facility exist
		Set<Cell> distance1 = new HashSet<Cell>();
		Set<Cell> distance2 = new HashSet<Cell>();
		Set<Cell> distance3 = new HashSet<Cell>();
		for (Cell p : b) {
			for (Cell q : p.neighbors()) {
				if ((pond && land.isPond(q)) || (!pond && land.isField(q))) return 0;
				if (!b.contains(q) && !marked.contains(q) && land.unoccupied(q)) 
					distance1.add(q);
			}
		}
		for (Cell p : distance1) {
			for (Cell q : p.neighbors()) {
				if ((pond && land.isPond(q)) || (!pond && land.isField(q))) return 1;
				if (!b.contains(q) && !marked.contains(q) && land.unoccupied(q) && !distance1.contains(q)) 
					distance2.add(q);
			}
		}
		for (Cell p : distance2) {
			for (Cell q : p.neighbors()) {
				if ((pond && land.isPond(q)) || (!pond && land.isField(q))) return 2;
				if (!b.contains(q) && !marked.contains(q) && land.unoccupied(q) && !distance2.contains(q)) 
					distance3.add(q);
			}
		}		
		for (Cell p : distance3) {
			for (Cell q : p.neighbors()) {
				if ((pond && land.isPond(q)) || (!pond && land.isField(q))) return 3;
			}
		}
		return -1;
	}

	private ArrayList<Set<Cell>> givenShortLengthWalks(Set<Cell> b, Set<Cell> marked, Land land, int walks, boolean pond) {
		ArrayList<Set<Cell>> traces = new ArrayList<Set<Cell>>();
		Set<Cell> traceQueue = new HashSet<Cell>();
		Set<Cell> updatedMarked = new HashSet<Cell>(marked); updatedMarked.addAll(b);
		if (walks != 1 && walks != 2) {
			return traces;
		} else if (walks == 1) {
			for (Cell p : b) {
				for (Cell q : p.neighbors()) {
					if (b.contains(q) || marked.contains(q) || !land.unoccupied(q)) continue;
					traceQueue.add(q);
					if (pond && findNearbyPondOrParkDistance(traceQueue, updatedMarked, land, true) == 0) {
						Set<Cell> trace = new HashSet<Cell>(traceQueue);
						traces.add(trace);
					}
					else if (!pond && findNearbyPondOrParkDistance(traceQueue, updatedMarked, land, false) == 0) {
						Set<Cell> trace = new HashSet<Cell>(traceQueue);
						traces.add(trace);
					}	
					traceQueue.remove(q);
				}
			}
			return traces;
		} else {
			for (Cell p : b) {
				for (Cell q : p.neighbors()) {
					if (b.contains(q) || marked.contains(q) || !land.unoccupied(q)) continue;
					traceQueue.add(q);
					for (Cell r : q.neighbors()) {
						if (b.contains(r) || marked.contains(r) || !land.unoccupied(r)) continue;
						traceQueue.add(r);
						if (pond && findNearbyPondOrParkDistance(traceQueue, updatedMarked, land, true) == 0) {
							Set<Cell> trace = new HashSet<Cell>(traceQueue);
							traces.add(trace);
						}
						else if (!pond && findNearbyPondOrParkDistance(traceQueue, updatedMarked, land, false) == 0) {
							Set<Cell> trace = new HashSet<Cell>(traceQueue);
							traces.add(trace);
						}	
						traceQueue.remove(r);
					}
					traceQueue.remove(q);
				}
			}
			return traces;
		}
	}

	// a bunch of possible good shapes of water or park
	private ArrayList<Set<Cell>> frankWalk(Set<Cell> b, Set<Cell> marked, Land land, int n) {
		ArrayList<Set<Cell>> output = new ArrayList<Set<Cell>>();
		ArrayList<Cell> adjCells = new ArrayList<Cell>();
		Set<Cell> set = new HashSet<Cell>();
		for (Cell p : b) {
			for (Cell q : p.neighbors()) {
				if (land.isField(q) || land.isPond(q))
					return new ArrayList<Set<Cell>>();
				if (!b.contains(q) && !marked.contains(q) && land.unoccupied(q))
					adjCells.add(q); 
			}
		}
		if (adjCells.isEmpty()) {
			return new ArrayList<Set<Cell>>();
		}
		for (Cell tail : adjCells) {
			ArrayList<Cell> walk_cells = new ArrayList<Cell>();
			for (int ii=0; ii<n; ii++) {
				if (!b.contains(tail) && !marked.contains(tail) && land.unoccupied(tail) && !set.contains(tail)) {
					boolean crowded = false;
					for (Cell c : tail.neighbors()) {
						if (c.isPark() || c.isWater() || marked.contains(c)) {
							crowded = true;
						}
					}
					if (crowded) { 
						set = new HashSet<Cell>();
						break; 
					}
					walk_cells.add(tail);
					set.add(tail);	
				}
				else {
					set = new HashSet<Cell>();
					break;
				}
				if (tail.i-1 < 0) {
					// Only break if not a long enough pond or field yet
					if (walk_cells.size() < 4) {
						set = new HashSet<Cell>();
						break;
					}
				}
				else {
					Cell above = new Cell(tail.i-1, tail.j);
					for (Cell p : tail.neighbors()) {
						if (above.equals(p)) {
							tail = p;
						}
					}
				}
			}
			if (walk_cells.size() == 4) {
				output.add(set);
				set = new HashSet<Cell>();
			}
		}
		return output;	
	}
	private ArrayList<Set<Cell>> shardenduWalk(Set<Cell> b, Set<Cell> marked, Land land, int n) {
		ArrayList<Cell> adjCells = new ArrayList<Cell>();
		for (Cell p : b) {
			for (Cell q : p.neighbors()) {
				if (land.isField(q) || land.isPond(q))
					return new ArrayList<Set<Cell>>();
				if (!b.contains(q) && !marked.contains(q) && land.unoccupied(q))
					adjCells.add(q); 
			}
		}
		if (adjCells.isEmpty()) {
			return new ArrayList<Set<Cell>>();
		}
		ArrayList<Set<Cell>> WeightCheck=new ArrayList<Set<Cell>>();
		for (Cell squarewalk : adjCells) {
			Set<Cell> output = new HashSet<Cell>();

			if (squarewalk.i+1 < land.side && squarewalk.i-1 > 0 && squarewalk.j-1 > 0 && squarewalk.j+1 < land.side){
				Cell a = new Cell(squarewalk.i,squarewalk.j);
				Cell q = new Cell(squarewalk.i-1,squarewalk.j-1);
				Cell c = new Cell(squarewalk.i,squarewalk.j-1);
				Cell d = new Cell(squarewalk.i-1,squarewalk.j);
				if (land.unoccupied(squarewalk.i,squarewalk.j) && !b.contains(a) && !b.contains(q) && !b.contains(c) && !b.contains(d) && !marked.contains(a)  && !marked.contains(q) && !marked.contains(c) && !marked.contains(d)  && land.unoccupied(squarewalk.i-1,squarewalk.j-1) && land.unoccupied(squarewalk.i,squarewalk.j-1) && land.unoccupied(squarewalk.i-1,squarewalk.j)){
					output.add(a);
					output.add(q);
					output.add(c);
					output.add(d);
					WeightCheck.add(output);
				}
			
			}
		}
		return WeightCheck;
	}
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
		if (adjCells.isEmpty()) {
			return new HashSet<Cell>();
		}
		Cell tail = adjCells.get(gen.nextInt(adjCells.size()));
		for (int ii=0; ii<n; ii++) {
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
		return output;
	}
	/* methods on park and water end */
}
