package pentos.g2;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.lang.reflect.Array;
import java.util.*;

public class Player implements pentos.sim.Player {

    private Random gen = new Random();
    private Set<Cell> road_cells = new HashSet<Cell>();
    private Set<Cell> road_cells_on_board = new HashSet<Cell>();

    private Set<Cell> pond_cells = new HashSet<>();
    private Set<Cell> field_cells = new HashSet<>();

    private static int POND_SIZE=4;
    private static int PARK_SIZE=4;


    public void init() { // function is called once at the beginning before play is called
        // For blob detection:
        //occupied_cells = new boolean[50][50];
    }

    private boolean isborderRoad(int i, int j) {
        return ( (i == -1) ||
                (i == 51) ||
                (j == -1) ||
                (j == 51)
        );
    }

    private ArrayList<Move> getAllValidMoves(Building request, Land land) {
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
        return moves;
    }


    private Set<Cell> getCells(Move current) {
        Set<Cell> cells = new HashSet<>();
        if(current.request == null) return cells;
        for (Cell x : current.request.rotations()[current.rotation]) {
            cells.add(new Cell(x.i+current.location.i, x.j+current.location.j));
        }
        return cells;
    }


    private Move handleFactory(Building request, Land land, ArrayList<Move> possibleMoves) {
        int numAdj = 0;
        int counter = 0;
        int internal_counter = 0;
        int inc = -1;
        int placement_idx = possibleMoves.size() - 1;

        //Chosen moves?
        Move chosen_final = new Move(false);

        // while loop (upper bound)
        while (counter < possibleMoves.size()) {

            // Look at the next possible place to look for
            Move chosen = possibleMoves.get(placement_idx);

            // Get coordinates of building placement (position plus local building cell coordinates).
            Set<Cell> shiftedCells = getCells(chosen);

            // Lets find teh highest scoring bulding placement (i.e.)
            // a placement that touches the most desirable sides (Other
            // buldings, parks, ponds, roads)
            int curr_sides = 0;
            boolean alreadyConnectedToRoad = false;
            for (Cell x : shiftedCells) {
                for (int i_seg = -1; i_seg <= 1; i_seg++) {
                    for (int j_seg = -1; j_seg <= 1; j_seg++) {
                        if (Math.abs(i_seg) == Math.abs(j_seg)) continue;
                        int curr_i = x.i + i_seg;
                        int curr_j = x.j + j_seg;
                        int curr_i_actual = curr_i;
                        int curr_j_actual = curr_j;
                        curr_i = (int) Math.max((double) curr_i, 0.0);
                        curr_i = (int) Math.min((double) curr_i, land.side - 1);
                        curr_j = (int) Math.max((double) curr_j, 0.0);
                        curr_j = (int) Math.min((double) curr_j, land.side - 1);
                        Cell curr = new Cell(curr_i, curr_j);
                        if (((!land.unoccupied(curr)) && (!shiftedCells.contains(curr))) ||
                            (isborderRoad(curr_i_actual, curr_j_actual))) curr_sides++;
                        if ((road_cells_on_board.contains(curr)) && !alreadyConnectedToRoad) curr_sides--;


                        // see if you are already connected to a road
                        alreadyConnectedToRoad = (road_cells_on_board.contains(curr) || alreadyConnectedToRoad);
                    }
                }
            }

            // update the score
            if (curr_sides <= numAdj) {
                placement_idx -= 1;
                if (placement_idx < 0 || placement_idx >= possibleMoves.size()) {
//                    printRejectedRequest(shiftedCells);
//                    return new Move(false);
                    counter = possibleMoves.size();
                }
                internal_counter++;
                double weight = 0.5-(possibleMoves.size() / 10000);
                if (internal_counter > (weight * possibleMoves.size()) && chosen_final.accept) {
                    counter = possibleMoves.size();
                    internal_counter = 0;
                }
                continue;
            } else {
                numAdj = curr_sides;
                internal_counter = 0;
            }

            // Build a road to connect this building to perimeter.
            if (!alreadyConnectedToRoad) {
//                    if (!alreadyConnectedToRoad) {
                Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
                if (roadCells != null) {

                    // For the existing road algorithm
                    chosen.road = roadCells;
                    chosen_final = chosen;

                } else { // Reject placement if building cannot be connected by road
                    placement_idx += inc;
                    if (placement_idx < 0 || placement_idx >= possibleMoves.size()) {
                        //printRejectedRequest(shiftedCells);
                        //System.out.println("REJECTION ON FACTORY BECAUSE placement_idx IS INVALID");
                        counter = possibleMoves.size();
                    }
                }
            } else {
                chosen_final = chosen;
            }
            counter++;
        }

        // Could not find any moves for this factory with main strategy, so just placing in next available slot.
        if (chosen_final.accept == false) {
            for (Move m : possibleMoves) {
                Set<Cell> shiftedCells = getCells(m);
                Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
                if(roadCells != null) {
                    chosen_final = m;
                    chosen_final.road = roadCells;
                }
            }  
        }

        if (chosen_final.road != null) {
            road_cells.addAll(chosen_final.road);
            road_cells_on_board.addAll(chosen_final.road);
        }

        return chosen_final;
    }


    private Move handleResidence(Building request, Land land, ArrayList<Move> possibleMoves) {
        // some counters to keep track of where we are and for scores
        // if residence, build from top. if factory, buld from bottom
        int inc = 1;
        int placement_idx = 0;

        int numAdj = 0;
        int counter = 0;
        int internal_counter = 0;

        //shifted cells
        Move chosen_final = new Move(false);
        Set<Cell> finalCells = null;

        //some booleans to keep track of whether we are already next to
        boolean buildParkPonds = false;
        boolean alreadyConnectedToPond = false;
        boolean alreadyConnectedToPark = false;

        while(counter < possibleMoves.size()) {

            Move chosen = possibleMoves.get(placement_idx);

            // Get coordinates of building placement (position plus local building cell coordinates).
            Set<Cell> shiftedCells = getCells(chosen);
            int curr_sides = 0;
            boolean alreadyConnectedToRoad = false;
            alreadyConnectedToPond = false;
            alreadyConnectedToPark = false;

            // same story about the score, but with slightly different criteria
            for (Cell x : shiftedCells) {
                for (int i_seg = -1; i_seg <= 1; i_seg++) {
                    for (int j_seg = -1; j_seg <= 1; j_seg++) {
                        if (Math.abs(i_seg) == Math.abs(j_seg)) continue;
                        int curr_i = x.i + i_seg;
                        int curr_j = x.j + j_seg;
                        int curr_i_actual = curr_i;
                        int curr_j_actual = curr_j;
                        curr_i = (int)Math.max((double)curr_i, 0.0);
                        curr_i = (int)Math.min((double)curr_i, land.side-1);
                        curr_j = (int)Math.max((double)curr_j, 0.0);
                        curr_j = (int)Math.min((double)curr_j, land.side-1);
                        Cell curr = new Cell(curr_i, curr_j);
                        if ((!land.unoccupied(curr)) && (!shiftedCells.contains(curr))) curr_sides++;
                        if (isborderRoad(curr_i_actual, curr_j_actual)) curr_sides++;
                        //if ((road_cells_on_board.contains(curr))) curr_sides--;
                        if ((land.isField(curr)) && !alreadyConnectedToPond) curr_sides++;
                        if ((land.isPond(curr)) && !alreadyConnectedToPark) curr_sides++;

                        alreadyConnectedToRoad = (road_cells_on_board.contains(curr) || alreadyConnectedToRoad);
                        alreadyConnectedToPond = (land.isPond(curr) || alreadyConnectedToPond);
                        alreadyConnectedToPark = (land.isField(curr) || alreadyConnectedToPark);
                    }
                }
            }

            // update score
            if (curr_sides <= numAdj) {
                placement_idx += inc;
                if(placement_idx < 0 || placement_idx >= possibleMoves.size()) {
//                    printRejectedRequest(shiftedCells);
//                    return new Move(false);
                    counter = possibleMoves.size();
                }
                internal_counter++;
                double limit = 0.6-(possibleMoves.size() / 10000);
                if (internal_counter > (limit * possibleMoves.size())) {
                    counter = possibleMoves.size();
                    internal_counter = 0;
                }
                continue;
            } else {
                numAdj = curr_sides;
                internal_counter = 0;
            }

            // Build a road to connect this building to perimeter.
            if (!alreadyConnectedToRoad) {
//                    if (!alreadyConnectedToRoad) {
                Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
                if (roadCells != null) {

                    // For the existing road algorithm
                    chosen.road = roadCells;
                    buildParkPonds = true;
                    finalCells = shiftedCells;
                    chosen_final = chosen;

                }
                else { // Reject placement if building cannot be connected by road
                    placement_idx += inc;
                    if(placement_idx < 0 || placement_idx >= possibleMoves.size()) {
                        counter = possibleMoves.size();
                        break;
                    }
                }
            } else {
                buildParkPonds = true;
                finalCells = shiftedCells;
                chosen_final = chosen;
            }
            counter++;
        }


        // check if you are supposed to build a park or pond
        if (buildParkPonds) {

            // Generate pseudo-random parks/ponds
            Set<Cell> markedForConstruction = new HashSet<>();
            markedForConstruction.addAll(chosen_final.road);
            markedForConstruction.addAll(getCells(chosen_final));

            Set<Cell> waterAttempt = findShortestPath(Cell.Type.WATER, finalCells, land, markedForConstruction);
//            if(pond_cells.size() > 0 && waterAttempt.size() > 3) {


            // find best shape for pond
            int topscore = 0;
            if (alreadyConnectedToPond) topscore = 100;
            boolean pondConnected = false;
            if ((waterAttempt != null) && (waterAttempt.size() < 5)) {
                int score = 0;
                for (Cell PPn : waterAttempt) {
                    for (Cell c : PPn.neighbors()) {
                        if (waterAttempt.contains(c)) continue;
                        if (finalCells.contains(c)) score++;
                        if (chosen_final.road.contains(c)) score--;
                        if (land.isField(c)) score++;
                        if (!land.unoccupied(c) &&
                                !land.isPond(c) &&
                                !land.isField(c) &&
                                !finalCells.contains(c)) score--;
                        pondConnected = (pondConnected | land.isPond(c));
                    }
                }
                if (score > topscore) {
                    topscore = score;
                    chosen_final.water = waterAttempt;
                }
            }
            if (!pondConnected) topscore = 0;

            Set<Cell> PP;
            for (int tryidx = 0; tryidx < 10; tryidx++) {
                PP = randomShape(finalCells, markedForConstruction, land, POND_SIZE);
                int score = 0;
                for (Cell PPn : PP) {
                    for (Cell c : PPn.neighbors()) {
                        if (PP.contains(c)) continue;
                        if (finalCells.contains(c)) score++;
                        if (chosen_final.road.contains(c)) score--;
                        if (land.isPond(c)) score++;
                        if (!land.unoccupied(c) &&
                                !land.isPond(c) &&
                                !land.isField(c) &&
                                !finalCells.contains(c)) score--;

                    }
                }
                if (score > topscore) {
                    topscore = score;
                    chosen_final.water = PP;
                }
            }
//            } else {
//                chosen_final.water = waterAttempt;
//            }

            //chosen_final.water = randomWalk(shiftedCells_final, markedForConstruction, land, 4);
            markedForConstruction.addAll(chosen_final.water);

            Set<Cell> fieldAttempt = findShortestPath(Cell.Type.PARK, finalCells, land, markedForConstruction);

            // find best shape for park
//            if(field_cells.size() > 0 && fieldAttempt.size() > 3) {
            topscore = 0;
            if (alreadyConnectedToPark) topscore = 100;
            boolean fieldConnected = false;
            if ((fieldAttempt != null) && (fieldAttempt.size() < 5)) {
                int score = 0;
                for (Cell PPn : fieldAttempt) {
                    for (Cell c : PPn.neighbors()) {
                        if (fieldAttempt.contains(c)) continue;
                        if (finalCells.contains(c)) score++;
                        if (chosen_final.road.contains(c)) score--;
                        if (land.isField(c)) score++;
                        if (!land.unoccupied(c) &&
                                !land.isPond(c) &&
                                !land.isField(c) &&
                                !finalCells.contains(c)) score--;
                        fieldConnected = (fieldConnected | land.isField(c));
                    }
                }
                if (score > topscore) {
                    topscore = score;
                    chosen_final.park = fieldAttempt;
                }
            }

            if (!fieldConnected) topscore = 0;

            for (int tryidx = 0; tryidx < 10; tryidx++) {
                PP = randomShape(finalCells, markedForConstruction, land, PARK_SIZE);
                int score = 0;
                for (Cell PPn : PP) {
                    for (Cell c : PPn.neighbors()) {
                        if (PP.contains(c)) continue;
                        if (finalCells.contains(c)) score++;
                        if (chosen_final.road.contains(c)) score--;
                        if (land.isField(c)) score++;
                        if (!land.unoccupied(c) &&
                                !land.isPond(c) &&
                                !land.isField(c) &&
                                !finalCells.contains(c)) score--;
                    }
                }
                if (score > topscore) {
                    topscore = score;
                    chosen_final.park = PP;
                }
            }
//            } else {
//                chosen_final.park = fieldAttempt;
//            }

            //chosen_final.park = randomWalk(shiftedCells_final, markedForConstruction, land, 4);
        }

        // Could not find any moves for this residence with main strategy, so just placing in next available slot.
        if (chosen_final.accept == false) {
            for (Move m : possibleMoves) {
                Set<Cell> shiftedCells = getCells(m);
                Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
                if(roadCells != null) {
                    chosen_final = m;
                    chosen_final.road = roadCells;
                }
            }  
        }

        if (chosen_final.road != null) {
            road_cells.addAll(chosen_final.road);
            road_cells_on_board.addAll(chosen_final.road);
        }

        if(chosen_final.park != null) {
            field_cells.addAll(chosen_final.park);
        }

        if(chosen_final.water != null) {
            pond_cells.addAll(chosen_final.water);
        }

        // Return the final chosen move
        return chosen_final;
    }

    public Move play(Building request, Land land) {

        ArrayList<Move> moves = getAllValidMoves(request, land);

        // choose a building placement at random
        if (moves.isEmpty()) { // reject if no valid placements
            return new Move(false);
        }

        //shifted cells
        Set<Cell> shiftedCells_final = new HashSet<Cell>();

        if(request.type == Building.Type.FACTORY)
            return handleFactory(request, land, moves);
        else
            return handleResidence(request, land, moves);
    }


    private Set<Cell> findShortestPath(Cell.Type type, Set<Cell> b, Land land, Set<Cell> markedForConstruction) {
        Set<Cell> output = new HashSet<Cell>();
        boolean[][] checked = new boolean[land.side][land.side];
        Queue<Cell> queue = new LinkedList<Cell>();
        Set<Cell> cells = null;
        if(type == Cell.Type.WATER) cells = pond_cells;
        else if(type == Cell.Type.PARK) cells = field_cells;
        else if(type == Cell.Type.ROAD) cells = road_cells;

        // add border cells that don't have a road currently
        Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
        for(Cell x : b) {
            if(x.i==0 || x.i==land.side-1 || x.j==0 || x.j==land.side-1) return new HashSet<Cell>();
            for(Cell y : x.neighbors()) {
                if(cells.contains(y)) {
                    return new HashSet<Cell>();
                }
            }
        }
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

        for (Cell p : cells) {
            for (Cell q : p.neighbors()) {
                if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q) && !markedForConstruction.contains(q))
                    queue.add(new Cell(q.i,q.j,p)); // use tail field of cell to keep track of previous road cell during the search
            }
        }
        while (!queue.isEmpty()) {
            Cell p = queue.remove();
            checked[p.i][p.j] = true;
            for (Cell x : p.neighbors()) {
                if (b.contains(x)) { // trace back through search tree to find path
                    Cell tail = p;
                    while (!b.contains(tail) && !cells.contains(tail) && !tail.equals(source)) {
                        if (markedForConstruction.contains(tail)) return null;
                        output.add(new Cell(tail.i,tail.j));
                        tail = tail.previous;
                    }
                    if (!output.isEmpty())
                        return output;
                }
                else if (!checked[x.i][x.j] && land.unoccupied(x.i,x.j)) {
                    x.previous = p;
                    checked[x.i][x.j] = true;
                    queue.add(x);
                }

            }
        }
        if (output.isEmpty() && queue.isEmpty())
            return null;
        else
            return output;
    }


    // build shortest sequence of road cells to connect to a set of cells b
    private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
        Set<Cell> output = new HashSet<Cell>();
        boolean[][] checked = new boolean[land.side][land.side];
        Queue<Cell> queue = new LinkedList<Cell>();
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
                    checked[x.i][x.j] = true;
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
                if (!b.contains(p) &&
                        !marked.contains(p) &&
                        land.unoccupied(p) &&
                        !output.contains(p)) {
                    walk_cells.add(p);
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

    // shape n consecutive cells starting from a building. Used to build a random field or pond.
    // different than walk in that any shape can be formed, not just those based off walks
    // which can exclude certain shapes based off of starting location
    private Set<Cell> randomShape(Set<Cell> b, Set<Cell> marked, Land land, int n) {

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

        // if there aren't any open cells, return empty set
        if (adjCells.isEmpty())
            return new HashSet<Cell>();

        // get a random Tail
        Cell tail = adjCells.get(gen.nextInt(adjCells.size()));

        // no create a ranodm shape, not even a random walk
        Set<Cell> shape = new HashSet<Cell>();
        shape.add(tail);
        for (int ii=0; ii<n; ii++) {
            ArrayList<Cell> walk_cells = new ArrayList<Cell>();
            for (Cell x : shape) {
                for (Cell p : x.neighbors()) {
                    if (!b.contains(p) && !marked.contains(p) && land.unoccupied(p) && !output.contains(p) && !shape.contains(p))
                        walk_cells.add(p);
                }
            }
            if (walk_cells.isEmpty()) {
                //return output; //if you want to build it anyway
                continue;
            }
//            String s = String.format("The shape size is currently %d and the walk_cells size is currently %d", shape.size(), walk_cells.size());
//            System.out.println(s);
            output.add(tail);
            tail = walk_cells.get(gen.nextInt(walk_cells.size()));
            shape.add(tail);

        }
        return output;
    }

    private void printRejectedRequest(Set<Cell> cells) {
        String s = "The coordinates of the rejected request are ";
        for (Cell c : cells) {
            s = s + c.toString() + " ";
        }
        System.out.println(s);
    }
}
