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
	private Random gen = new Random();
	final int side = 50;
	private boolean[][] isDisconnected;
    private Set<Cell> road_cells = new HashSet<Cell>();
    static int count = 0;
    public void init() { // function is called once at the beginning before play is called
    	
    	isDisconnected = new boolean[side][side];
    }
    
    public Move play(Building request, Land land) {
    	
    	Building[] rotations = request.rotations();
    	int best_i = land.side + 1,
    		best_j = land.side + 1;
    	Move best_move = null;
    	
    	//find first free location row by row
	    if(request.type == Building.Type.RESIDENCE) {
	    	best_i = land.side + 1;
	        best_j = land.side + 1;
	    	for (int i = 0; i < land.side; ++i) 
	    	for ( int j = 0; j < land.side; ++j) {
    			Cell p = new Cell(i,j);
    			for (int ri = 0; ri < rotations.length; ++ri)
				if(land.buildable(rotations[ri], p) 
						&& (best_i > i || (best_i == i && best_j > j))
						&& !isDisconnected[i][j]) {
					Move temp = new Move(true, 
								request, 
								p, 
								ri, 
								new HashSet<Cell>(), 
								new HashSet<Cell>(), 
								new HashSet<Cell>());
					boolean disconnected = false;
					Set<Cell> shiftedCells = new HashSet<Cell>();
					for (Cell x : temp.request.rotations()[temp.rotation]) {
				    	shiftedCells.add(
				    			new Cell(x.i+temp.location.i,
				    					x.j+temp.location.j));
				    	disconnected |= 
				    			isDisconnected[x.i + temp.location.i][x.j + temp.location.j];
					}
				    // builda road to connect this building to perimeter

					if(!disconnected) {
						Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
					    if(roadCells != null) {
					    	best_move = temp;
					    	best_i = i;
					    	best_j = j;
					    } else {
					    	for(Cell x : shiftedCells) {
					    		isDisconnected[x.i][x.j] = true;
					    	}
					    }
					}				
				}
    		}
	    //find closest free location to end
	    } else if(request.type == Building.Type.FACTORY) {
	    	best_i = -1;
	    	best_j = -1;
	    	for (int i = land.side - 1; i >= 0; --i) 
	    	for ( int j = land.side - 1; j >= 0; --j) {
    			Cell p = new Cell(i,j);
    			for (int ri = 0; ri < rotations.length; ++ri)
				if(land.buildable(rotations[ri], p) 
						&& (best_i < i || (best_i == i && best_j < j))) {
					Move temp = new Move(true, 
								request, 
								p, 
								ri, 
								new HashSet<Cell>(), 
								new HashSet<Cell>(), 
								new HashSet<Cell>());
					
					boolean disconnected = false;
					Set<Cell> shiftedCells = new HashSet<Cell>();
					for (Cell x : temp.request.rotations()[temp.rotation]) {
				    	shiftedCells.add(
				    			new Cell(x.i+temp.location.i,
				    					x.j+temp.location.j));
				    	disconnected |= 
				    			isDisconnected[x.i + temp.location.i][x.j + temp.location.j];
					}
				    // builda road to connect this building to perimeter
					
					if(!disconnected) {
						Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
					    if(roadCells != null) {
					    	best_move = temp;
					    	best_i = i;
					    	best_j = j;
					    } else {
					    	for(Cell x : shiftedCells) {
					    		isDisconnected[x.i][x.j] = true;
					    	}
					    }
					}				    
				}
    		}
	    }
	    
	    //no move
		if (best_move == null) {
			//System.out.println("no moves");
			return new Move(false);
		}
	    // get coordinates of building placement (position plus local building cell coordinates)
	    Set<Cell> shiftedCells = new HashSet<Cell>();
	    //System.out.println(request.type == request.type.RESIDENCE);
	    //System.out.println();
	    for (Cell x : best_move.request.rotations()[best_move.rotation])
	    	shiftedCells.add(
	    			new Cell(x.i+best_move.location.i,
	    					x.j+best_move.location.j));
	    
	    // builda road to connect this building to perimeter
	    Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
	    if (roadCells != null) {
			best_move.road = roadCells;
			road_cells.addAll(roadCells);
			int x = gen.nextInt()%2;
			if(request.type == request.type.RESIDENCE && x==0) {
				
				Set<Cell> markedForConstruction = new HashSet<Cell>();
			    markedForConstruction.addAll(roadCells);
			    best_move.water = randomWalk(shiftedCells, markedForConstruction, land, 4);
			    markedForConstruction.addAll(best_move.water);
			    best_move.park = randomWalk(shiftedCells, markedForConstruction, land, 4);
			}
			
		    
			return best_move;
	    }
	    else {// reject placement if building cannot be connected by road
	    	//Kailash: This should never happen now.
	    	System.out.println("No Road" + best_i + " " + best_j);
	    	Cell p = new Cell(best_i, best_j);
	    	System.out.println(land.buildable(best_move.request.rotations()[best_move.rotation], p));
	    	return new Move(false);
    
	    }
    }
    
    
    // build shortest sequence of road cells to connect to a set of cells b
    private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
	Set<Cell> output = new HashSet<Cell>();
	boolean[][] checked = new boolean[land.side][land.side];
	Queue<Cell> queue = new LinkedList<Cell>();
	// add border cells that don't have a road currently
	Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
	for (int z=0; z<land.side; z++) {
	    if (b.contains(new Cell(0,z)) || b.contains(new Cell(z,0)) || b.contains(new Cell(land.side-1,z)) || b.contains(new Cell(z,land.side-1))) //if already on border don't build any roads
		return output;
	    if (land.unoccupied(0,z))
		queue.add(new Cell(0,z,source));
	    if (land.unoccupied(z,0))
		queue.add(new Cell(z,0,source));
	    if (land.unoccupied(z,land.side-1))
		queue.add(new Cell(z,land.side-1,source));
	    if (land.unoccupied(land.side-1,z))
		queue.add(new Cell(land.side-1,z,source));
	}
	// add cells adjacent to current road cells
	for (Cell p : road_cells) {
	    for (Cell q : p.neighbors()) {
		if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q)) 
		    queue.add(new Cell(q.i,q.j,p)); // use tail field of cell to keep track of previous road cell during the search
	    }
	}	
	while (!queue.isEmpty()) {
	    Cell p = queue.remove();
	    checked[p.i][p.j] = true;
	    for (Cell x : p.neighbors()) {		
		if (b.contains(x)) { // trace back through search tree to find path
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

    // walk n consecutive cells starting from a building. Used to build a random field or pond. 
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
	for (int ii=0; ii<n; ii++) {
	    ArrayList<Cell> walk_cells = new ArrayList<Cell>();
	    for (Cell p : tail.neighbors()) {
		if (!b.contains(p) && !marked.contains(p) && land.unoccupied(p) && !output.contains(p))
		    walk_cells.add(p);		
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
