package pentos.g2;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

    private Random gen = new Random();
    private Set<Cell> road_cells = new HashSet<Cell>();

    private boolean[][] occupied_cells;

    private int max_i = 0;
    private int max_j = 0;

    private int staging_max_i = 0;
    private int staging_max_j = 0;


    public void init() { // function is called once at the beginning before play is called
        occupied_cells = new boolean[50][50];
    }
    
    public Move play(Building request, Land land) {
        // find all valid building locations and orientations
        ArrayList <Move> moves = new ArrayList <Move> ();
        for (int i = 0 ; i < land.side ; i++)
            for (int j = 0 ; j < land.side ; j++) {
            Cell p = new Cell(i, j);
            Building[] rotations = request.rotations();
            for (int ri = 0 ; ri < rotations.length ; ri++) {
                Building b = rotations[ri];
                if (land.buildable(b, p)) 
                    moves.add(new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
            }
        }
        // choose a building placement at random
        if (moves.isEmpty()) // reject if no valid placements
            return new Move(false);
        else {
            int inc;
            int placement_idx;
            if(request.type == Building.Type.FACTORY) {
                placement_idx = moves.size() - 1;
                inc = -1;
            } else {
                placement_idx = 0;
                inc = 1;
            }

            while (true) {
                // Look at the next possible place to look for 
                Move chosen = moves.get(placement_idx); 

                // Pick a random location for the building
                // Move chosen = moves.get(gen.nextInt(moves.size()));

                // Get coordinates of building placement (position plus local building cell coordinates).
                Set<Cell> shiftedCells = new HashSet<Cell>();
                for (Cell x : chosen.request.rotations()[chosen.rotation]) {
                    if(request.type == Building.Type.RESIDENCE) {
                        staging_max_i = max_i;
                        staging_max_j = max_j;
                        if(x.i+chosen.location.i > staging_max_i) {
                            staging_max_i = x.i+chosen.location.i;
                        }
                        if(x.j+chosen.location.j > staging_max_j) {
                            staging_max_j = x.j+chosen.location.j;
                        }
                    }
                    shiftedCells.add(new Cell(x.i+chosen.location.i, x.j+chosen.location.j));
                }

                // Build a road to connect this building to perimeter.
                Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
                if (roadCells != null) {

                    if(request.type == Building.Type.RESIDENCE) {

                        /*int island_size = checkUnusableIslands(occupied_cells, roadCells, shiftedCells);

                        if (island_size == 4) {

                        }
                        else if(island_size == 0 || island_size > 4) {
                            // Move this to if(roadCells != null) if it's not working
   
                        }*/

                        Set<Cell> markedForConstruction = new HashSet<Cell>();
                        markedForConstruction.addAll(roadCells);
                        chosen.water = randomWalk(shiftedCells, markedForConstruction, land, 4);
                        markedForConstruction.addAll(chosen.water);
                        chosen.park = randomWalk(shiftedCells, markedForConstruction, land, 4);

                        chosen.road = roadCells;
                        road_cells.addAll(roadCells);

                        for(Cell x : roadCells) {
                            occupied_cells[x.i][x.j] = true;
                        }
                        for(Cell x : shiftedCells) {
                            occupied_cells[x.i][x.j] = true;
                        }
                        max_i = staging_max_i;
                        max_j = staging_max_j;

                        System.out.format("max_i: %d\nmax_j: %d\n\n", max_i, max_j);
                        return chosen;

                    } else { // Building.Type.FACTORY
                        // Move this to if(roadCells != null) if it's not working
                        chosen.road = roadCells;
                        road_cells.addAll(roadCells);
                        for(Cell x : roadCells) {
                            occupied_cells[x.i][x.j] = true;
                        }
                        for(Cell x : shiftedCells) {
                            occupied_cells[x.i][x.j] = true;
                        }
                        return chosen;
                    }

                    // for residences, build random ponds and fields connected to it
                    /*if (request.type == Building.Type.RESIDENCE) {
                        Set<Cell> markedForConstruction = new HashSet<Cell>();
                        markedForConstruction.addAll(roadCells);
                        chosen.water = randomWalk(shiftedCells, markedForConstruction, land, 4);
                        markedForConstruction.addAll(chosen.water);
                        chosen.park = randomWalk(shiftedCells, markedForConstruction, land, 4);
                    }*/
                    
                }
                else { // reject placement if building cannot be connected by road
                    placement_idx += inc;
                    if(placement_idx < 0 || placement_idx >= moves.size()) {
                        return new Move(false);  
                    }
                }
            }

            //return new Move(false);

        }
    }

    private int checkUnusableIslands(Set<Cell> roads, Set<Cell> buildings) {
        
        boolean[][] new_occupied_cells = new boolean[50][50];
        for (int i = 0; i < 50; ++i) {
            new_occupied_cells[i] = occupied_cells[i].clone();
        }
        for(Cell x : roads) {
            new_occupied_cells[x.i][x.j] = true;
        }
        for(Cell x : buildings) {
            new_occupied_cells[x.i][x.j] = true;
        }

        // Minimize the number of blobs with less than 4 unoccupied spots.
        for(int i = 0; i <= staging_max_i; ++i) {
            for(int j = 0; j <= staging_max_j; ++j) {

            }
        }

        return 0;

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
