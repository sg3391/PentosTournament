package pentos.g1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;

public class Player implements pentos.sim.Player {

	private static int INF = (int)1e9;

	private Random gen = new Random();
	final int ITERATION_COUNT = 200;
	final int side = 50;
	private Set<Cell> road_cells = new HashSet<Cell>();
	static int count = 0;

	public void init() { // function is called once at the beginning before play is called

	}

    public Move getBestMove(Building request, Land land) {

	    Building[] rotations = request.rotations();
	    int best_i = land.side + 1,
		best_j = land.side + 1;
	    int best_perimeter = -1;
		// Remove this feature: no difference in score and slows down the computation
	    //int best_changes = 1000;
	    Move best_move = null;

	    //find first free location row by row
	    if(request.type == Building.Type.RESIDENCE) {
			best_i = land.side + 1;
			best_j = land.side + 1;

			for (int i = 0; i < land.side; ++i) 
				for (int j = 0; j < land.side; ++j) {
					Cell p = new Cell(i,j);
					for (int ri = 0; ri < rotations.length; ++ri)
						if(land.buildable(rotations[ri], p)) {

							Move temp = new Move(true, 
									request, 
									p, 
									ri, 
									new HashSet<Cell>(), 
									new HashSet<Cell>(), 
									new HashSet<Cell>());
							Set<Cell> shiftedCells = new HashSet<Cell>();
							for (Cell x : temp.request.rotations()[temp.rotation]) {
								shiftedCells.add(
										new Cell(x.i+temp.location.i,
											x.j+temp.location.j));
							}

							// Get the number of "perimeter cells"
							int perimeter = 0;
							for(Cell x : shiftedCells) {
								for(Cell y : x.neighbors()) {
									if(!land.unoccupied(y)) {
										++perimeter;
									}
								}
								if(x.i == 0 || x.i == land.side - 1) ++ perimeter;
								if(x.j == 0 || x.j == land.side - 1) ++ perimeter;						
							}

							// Remove this feature, statistics shows no difference in score
							//int changes = best_changes;
//							if (perimeter == best_perimeter)
//								changes = getChangesOfEmptySpaces(temp, land);

							// Only compare the perimeter change and the place (from corner to corner)
							if((/*(changes < best_changes)
										||*/ (perimeter > best_perimeter /*&& changes == best_changes*/)
										||(perimeter==best_perimeter /*&& changes == best_changes*/ && (i  + j) < best_i +  best_j)
										|| (perimeter==best_perimeter /*&& changes == best_changes */ && (i  + j) ==  best_i + best_j) && Math.abs(i-j) < Math.abs(best_i - best_j))) {
								Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
								if(roadCells != null) {
									best_move = temp;
									best_i = i;
									best_j = j;
									best_perimeter = perimeter;
									//best_changes = changes;
								}
							}				
						}
				}
		//find closest free location to end
		} else if(request.type == Building.Type.FACTORY) {
			best_i = -1;
			best_j = -1;
			best_perimeter = -1;
			for (int i = land.side - 1; i >= 0; --i) 
				for ( int j = land.side - 1; j >= 0; --j) {
					Cell p = new Cell(i,j);
					for (int ri = 0; ri < rotations.length; ++ri)
						if(land.buildable(rotations[ri], p)) {
							Move temp = new Move(true, 
									request, 
									p, 
									ri, 
									new HashSet<Cell>(), 
									new HashSet<Cell>(), 
									new HashSet<Cell>());

							Set<Cell> shiftedCells = new HashSet<Cell>();
							for (Cell x : temp.request.rotations()[temp.rotation]) {
								shiftedCells.add(
										new Cell(x.i+temp.location.i,
											x.j+temp.location.j));
							}

							int perimeter = 0;
							for(Cell x : shiftedCells) {
								for(Cell y : x.neighbors()) {
									if(!land.unoccupied(y)) {
										++perimeter;
									}
								}
								if(x.i == 0 || x.i == land.side - 1) ++ perimeter;
								if(x.j == 0 || x.j == land.side - 1) ++ perimeter;						
							}

							// builda road to connect this building to perimeter
							//int changes = best_changes;
			//				changes = getChangesOfEmptySpaces(temp, land);

							// Only compare the perimeter change and the place (from corner to corner)
							if( (perimeter > best_perimeter
										|| (perimeter == best_perimeter /*&& changes < best_changes*/)
										|| (perimeter==best_perimeter /*&& changes == best_changes*/ && (i  + j) >  best_i + best_j))
									||(perimeter==best_perimeter /*&& changes == best_changes*/ && (i  + j) ==  best_i + best_j) && Math.abs(i-j) < Math.abs(best_i - best_j)) {
								Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
								if(roadCells != null) {
									best_move = temp;
									best_i = i;
									best_j = j;
									best_perimeter = perimeter;
									//best_changes = changes;
								}
							}				    
						}
				}
		}
		return best_move;
	}

	private static int num = 0;
	static Set<Cell> prev_water=null;

	public Move play(Building request, Land land) {

		//first find a placement for buildings
		Move best_move = getBestMove(request, land);

		//no move
		if (best_move == null) {
			return new Move(false);
		}

		// get coordinates of building placement (position plus local building cell coordinates)
		Set<Cell> shiftedCells = new HashSet<Cell>();
		for (Cell x : best_move.request.rotations()[best_move.rotation]) {
			shiftedCells.add(
					new Cell(x.i+best_move.location.i,
						x.j+best_move.location.j));
		}

		// build a road to connect this building to perimeter
		Set<Cell> roadCells = findShortestRoad(shiftedCells, land);

		if (roadCells != null) {
			best_move.road = roadCells;
			road_cells.addAll(roadCells);
			if(request.type == request.type.RESIDENCE) {
				Set<Cell> bcells = new HashSet<Cell>();
				for (Cell x : best_move.request.rotations()[best_move.rotation]) {
					bcells.add(new Cell(x.i + best_move.location.i, x.j + best_move.location.j));
				}
				optimize_water_and_park(roadCells, land, best_move, shiftedCells, bcells);
			}

			return best_move;
		}
		else {// reject placement if building cannot be connected by road
			//Kailash: This should never happen now.
			return new Move(false);    
		}
	}

	private boolean adjToParkOrWater(Cell.Type type, Set<Cell> bcells, Land land) {
		for (Cell b : bcells) {
			for (Cell nei : b.neighbors()) {
				if (type == Cell.Type.WATER && land.isPond(nei)) return true;
				if (type == Cell.Type.PARK && land.isField(nei)) return true;
			}
		}
		return false;
	}

	// trying to build parks/ponds. candidates consists of all bar-shaped buildings and random walks
	void optimize_water_and_park(Set<Cell> roadCells, Land land, Move best_move, Set<Cell> shiftedCells, Set<Cell> bcells) {

		Set<Cell> markedForConstruction = new HashSet<Cell>();
		markedForConstruction.addAll(roadCells);
		Set<Set<Cell>> park_options = getStraightParksAndPonds(shiftedCells, roadCells, land, best_move);

		if (!adjToParkOrWater(Cell.Type.PARK, bcells, land)) {
			Set<Cell> best_park = getBestRandomParkOrWater(shiftedCells, markedForConstruction, land, 1);

			best_park = getActualBestParkOrWater(new HashSet<Cell>(), park_options, markedForConstruction, land, best_park);
			best_move.park = best_park;
			markedForConstruction.addAll(best_move.park);
		}

		if (!adjToParkOrWater(Cell.Type.WATER, bcells, land)) {
			Set<Cell> best_water = getBestRandomParkOrWater(shiftedCells, markedForConstruction, land, 2);
			best_water = getActualBestParkOrWater(best_move.park, park_options, markedForConstruction, land, best_water);
			best_move.water = best_water;
		}
	}

	// pick the best option from poll, compare the size first and then perimeter change
	private Set<Cell> getActualBestParkOrWater(Set<Cell> used, Set<Set<Cell>> park_options, Set<Cell> markedForConstruction, Land land, Set<Cell> best_park) {
		// best of random walks
		int best_perimeter = getPerimeter(land, markedForConstruction, best_park);
		for(Set<Cell> park_option: park_options) {
			boolean flag = true;
			for (Cell cell : park_option)
				if (used.contains(cell)) {
					flag = false;
					break;
				}
			if (!flag) continue;

			// the number of occupied cells in the perimeter
			// fewer, better ( for future placement )
			int perimeter = getPerimeter(land, markedForConstruction, park_option);

			int size = park_option.size();
			if(size < best_park.size() || (size == best_park.size() && perimeter < best_perimeter)) {
				best_perimeter = perimeter;
				best_park = park_option;
			}
		}
		return best_park;
	}

	private boolean outside(int i, int j, Land land) {
		if (i < 0 || i >= land.side || j < 0 || j >= land.side) return true;
		return false;
	}

	// Return all possible bar-shaped parks/ponds
	private Set<Set<Cell>> getStraightParksAndPonds(Set<Cell> shiftedCells, Set<Cell> roadCells, Land land, Move best_move) {
		Set<Set<Cell>> park_options = new HashSet<Set<Cell>>();
		for(Cell x : best_move.request.rotations()[best_move.rotation]) {
			int[] dx = {0,0,1,-1};
			int[] dy = {1,-1,0,0};
			Cell cur = new Cell(best_move.location.i + x.i, best_move.location.j + x.j);
			for (Cell y : cur.neighbors()) {
				int i = y.i;
				int j = y.j;
				for(int k=0;k<4;++k) {
					Set<Cell> option = new HashSet<Cell>();
					boolean empty = true;
					for(int cellnum = 0;cellnum<4;++cellnum) {
						if (outside(i + dx[k] * cellnum, j + dy[k] * cellnum, land)) {
							empty = false;
							continue;
						}
						Cell tmp = new Cell(i + dx[k] * cellnum, j + dy[k] * cellnum);
						if(!land.unoccupied(tmp) || shiftedCells.contains(tmp) || roadCells.contains(tmp)) {
							empty = false;
						} else {
							option.add(new Cell(i+dx[k]*cellnum, j + dy[k]*cellnum));
						}
					}
					if(empty) {
						park_options.add(option);
					}
				}
			}
		}
		return park_options;
	}
    
	// removed feature
    private int getChangesOfEmptySpaces(Move move, Land land) {
        boolean[][] vis = new boolean[side][side];
        int ret = -1;	// If no empty cells nearby then it fills an empty hole
        
        Building[] rotations = move.request.rotations();
        Set<Cell> builds = new HashSet<Cell>();
        for (Cell cell : rotations[move.rotation])
            builds.add(new Cell(cell.i + move.location.i, cell.j + move.location.j));
        builds.addAll(move.road);
        builds.addAll(move.water);
        builds.addAll(move.park);
        Set<Cell> perimeter = new HashSet<Cell>();
        for (Cell cell : builds) {
            for (Cell p : cell.neighbors())
                if (land.unoccupied(p))
                    perimeter.add(p);
        }
        
        for (Cell p : perimeter) {
            if (vis[p.i][p.j]) continue;
            
            // found a new connected empty region
            ++ ret;
            Queue<Cell> queue = new LinkedList<Cell>();
            queue.add(p);
            vis[p.i][p.j] = true;
            while (!queue.isEmpty()) {
                Cell x = queue.poll();
                for (Cell y : x.neighbors()) {
                    if (vis[y.i][y.j] || builds.contains(y) || !land.unoccupied(y)) continue;
                    vis[y.i][y.j] = true;
                    queue.add(y);
                }
            }
        }
        return ret;
    }

	// Return the number of occupied cells of given set of cells
	private int getPerimeter(Land land, Set<Cell> markedForConstruction, Set<Cell> cells) {
		int perimeter = 0;
		for(Cell x : cells) {
			for(Cell y: x.neighbors()) {
				if(!land.unoccupied(y) || markedForConstruction.contains(y)) {
					++perimeter;
				}
			}
		}
		return perimeter;
	}

	// Generate a certain random samples of park/ponds, pick the best one
	private Set<Cell> getBestRandomParkOrWater(Set<Cell> shiftedCells, Set<Cell> markedForConstruction, Land land, int type) {
		Set<Cell> best_park = new HashSet<Cell>();
		int best_perimeter = 100;
		int best_size = 100;
		for(int i = 0; i < ITERATION_COUNT; ++i) {
			int perimeter = 0;
			int size = 0;
			Set<Cell> park_option = randomWalk(shiftedCells, markedForConstruction, land, 4, type);
			size = park_option.size()>0?park_option.size():110;
			perimeter = getPerimeter(land, markedForConstruction, park_option);
			if(size < best_size || (size == best_size && perimeter < best_perimeter)) {
				best_perimeter = perimeter;
				best_park = park_option;
				best_size = size;
			}
		}
		return best_park;
	}

	// build shortest sequence of road cells to connect to a set of cells b
	private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
		Set<Cell> output = new HashSet<Cell>();
		Set<Set<Cell>> cand = new HashSet<Set<Cell>>();

		boolean[][] checked = new boolean[land.side][land.side];
		Queue<Cell> queue = new LinkedList<Cell>();
		Queue<Integer> step = new LinkedList<Integer>();
		// add border cells that don't have a road currently
		Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
		for(Cell x : b) {
			if(x.i==0 || x.i==land.side-1 || x.j==0 || x.j==land.side-1) return new HashSet<Cell>();
			for(Cell y : x.neighbors()) {
				if(road_cells.contains(y)) {
					return new HashSet<Cell>();
				}
			}
		}
		for (int z=0; z<land.side; z++) {
			if (b.contains(new Cell(0,z)) || b.contains(new Cell(z,0)) || b.contains(new Cell(land.side-1,z)) || b.contains(new Cell(z,land.side-1))) //if already on border don't build any roads
				return output;
			if (land.unoccupied(0,z)) {
				queue.add(new Cell(0,z,source));
				step.add(1);
			}
			if (land.unoccupied(z,0)) {
				queue.add(new Cell(z,0,source));
				step.add(1);
			}
			if (land.unoccupied(z,land.side-1)) {
				queue.add(new Cell(z,land.side-1,source));
				step.add(1);
			}
			if (land.unoccupied(land.side-1,z)) {
				queue.add(new Cell(land.side-1,z,source));
				step.add(1);
			}
		}
		// add cells adjacent to current road cells

		for (Cell p : road_cells) {
			for (Cell q : p.neighbors()) {
				if (b.contains(q)) return output;

				if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q)) {
					queue.add(new Cell(q.i,q.j,p)); // use tail field of cell to keep track of previous road cell during the search
					step.add(1);
				}
			}
		}

		while (!queue.isEmpty()) {
			Cell p = queue.remove();
			int dis = step.remove();
			checked[p.i][p.j] = true;
			for (Cell x : p.neighbors()) {		
				if (b.contains(x)) { // trace back through search tree to find path
					Set<Cell> tmp = new HashSet<Cell>();
					Cell tail = p;
					while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
						tmp.add(new Cell(tail.i,tail.j));
						tail = tail.previous;
					}
					if (!tmp.isEmpty())
						cand.add(tmp);
				}
				else if (!checked[x.i][x.j] && land.unoccupied(x.i,x.j)) {
					x.previous = p;
					checked[x.i][x.j] = true;
					queue.add(x);
					step.add(dis + 1);
				} 

			}
			if (!queue.isEmpty() && step.peek() > dis && cand.size() > 0) break;
		}

		if (output.isEmpty() && queue.isEmpty())
			return null;
		else {
			int minP = INF;
			for (Set<Cell> road : cand) {
				int p = getPerimeter(land, road_cells, road);
				if (p < minP) {
					minP = p;
					output = road;
				}
			}
			return output;
		}
	}

	// walk n consecutive cells starting from a building. Used to build a random field or pond. 
	private Set<Cell> randomWalk(Set<Cell> b, Set<Cell> marked, Land land, int n, int type) {
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
		for (int ii=0; ii<n; ii++) {
			ArrayList<Cell> walk_cells = new ArrayList<Cell>();
			for (Cell p : tail.neighbors()) {
				if (!b.contains(p) && !marked.contains(p) && land.unoccupied(p) && !output.contains(p))
					walk_cells.add(p);
				if((land.isField(p) && type == 1) || (land.isPond(p) && type == 2)) {
					//	System.out.println("hi");
					output.add(tail);
					return output;
				}
			}
			if (walk_cells.isEmpty()) {
				//return output; //if you want to build it anyway
				return new HashSet<Cell>();
			}
			output.add(tail);	    
			tail = walk_cells.get(gen.nextInt(walk_cells.size()));
		}
		return output;
	}

}
