package pentos.g10;

import java.util.*;
import pentos.sim.Cell;
import pentos.sim.Land;


public class RoadFinder
{
    public static Set<Cell> findRoad(Player player, Action action,
                                     Land land)
    {
        Queue<Cell> queue   = new LinkedList<Cell>();
        Set<Cell>   visited = new HashSet<>();
        Set<Cell>   newRoad = new HashSet<>();

        Set<Cell> building      = action.getAbsoluteBuildingCells();
        Set<Cell> roadNeighbors = player.roadNeighbors;
        Set<Cell> roads         = player.roadcells;

        //initializeRoad(newRoad, land);

        //if already on border don't build any roads
        for (int i = 0; i < land.side; i++) {
            if (   building.contains(new Cell(0, i))
                || building.contains(new Cell(i, 0))
                || building.contains(new Cell(land.side-1, i))
                || building.contains(new Cell(i, land.side-1))) {
                action.setRoadCells(newRoad);
                return newRoad;
            }
        }

        // populate queue with empty neighbor cells of the building
        // also check if any neighboring cells are roads
        for (Cell c : building) {
            if (roadNeighbors.contains(c)) {
                action.setRoadCells(newRoad);
                return newRoad;
            }
            Set<Cell> neighbors = findNeighbors(c, building, land);
            for (Cell cn : neighbors) {
                queue.add(cn);
            }
        }

        while (!queue.isEmpty()) {
            Cell roadCandidate = queue.remove();

            if (roadNeighbors.contains(roadCandidate)) {
                newRoad = buildRoad(roadCandidate, building); 
                break;
            }
            Set<Cell> neighbors = findNeighbors(roadCandidate, building, land);
            for (Cell cn : neighbors) {
                if (!visited.contains(cn)) {
                    queue.add(cn);
                    visited.add(cn);
                }
            }
        }

        action.setRoadCells(newRoad);
        return newRoad;
    }

    public static boolean findClearPath(Cell c, Set<Cell> roadCells,
                                        Player player, Action action,
                                        Land land)
    {
//        System.out.println("MY FUNCTION CALLED");
        Queue<Cell> queue     = new LinkedList<Cell>();
        Set<Cell>   visited   = new HashSet<>();
        Set<Cell>   clearPath = new HashSet<>();

        Set<Cell> roadNeighbors = player.roadNeighbors;
        Set<Cell> roads         = player.roadcells;
        Set<Cell> building=action.getAbsoluteBuildingCells();

        // populate queue with empty neighbor cells of the road cells 
        // also check if any neighboring cells are roads
        for (Cell r : roadCells) {
            Set<Cell> neighbors = findNeighbors(r, roadCells, land);
            for (Cell rn : neighbors) {
            	if(!building.contains(rn))
            		queue.add(rn);
            }
        }

        while (!queue.isEmpty()) {
            Cell pathCandidate = queue.remove();

            if (pathCandidate.equals(c)) {
                return true;
            }
            Set<Cell> neighbors = findNeighbors(pathCandidate, roadCells,
                                                land);
            for (Cell rn : neighbors) {
                if (!visited.contains(rn) && !building.contains(rn)) {
                    queue.add(rn);
                    visited.add(rn);
                }
            }
        }

        return false;
    }

    private static boolean isBorderCell(Cell c, Land land)
    {
        for (int i = 0; i < land.side; i++) {
            if (   c.equals(new Cell(0, i))
                || c.equals(new Cell(i, 0))
                || c.equals(new Cell(land.side-1, i))
                || c.equals(new Cell(i, land.side-1))) {
                return true;
            }
        }
        return false;

    }
      
    private static Set<Cell> findNeighbors(Cell c, Set<Cell> building,
                                           Land land)
    /* returns neighbors who are both orthogonal to cell c and empty */
    {
        Set<Cell> neighbors = new HashSet<>();
        Cell      neighbor;

        if (c.i < land.side) {
            neighbor = new Cell(c.i + 1, c.j, c);
            if (land.unoccupied(neighbor) && !building.contains(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        if (c.i > 0) {
            neighbor = new Cell(c.i - 1, c.j, c);
            if (land.unoccupied(neighbor) && !building.contains(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        if (c.j < land.side) {
            neighbor = new Cell(c.i, c.j + 1, c);
            if (land.unoccupied(neighbor) && !building.contains(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        if (c.j > 0) {
            neighbor = new Cell(c.i, c.j - 1, c);
            if (land.unoccupied(neighbor) && !building.contains(neighbor)) {
                neighbors.add(neighbor);
            }
        }

        return neighbors;
        
        /* may use this later
        if (roads.contains(roadCandidate)) {
            action.setRoadCells(null);
            return null;
        }
        else if ( ! building.contains(roadCandidate)
            && roadNeighbors.contains(roadCandidate)) {
            break;
        } */
    }

    private static Set<Cell> buildRoad(Cell c, Set<Cell> building)
    /* builds a road using trail of previous cells leading back to building */
    {
        Set<Cell> newRoad = new HashSet<>();

        do {
            newRoad.add(c);
            c = c.previous;
        } while (!building.contains(c));

        return newRoad;
    }

    private static void initializeRoad(Set<Cell> newRoad, Land land)
    {
        if (land.unoccupied(land.side - 1, land.side/2 - 1)) {
            for (int i = land.side - 1; i >= 0; i--) {
                newRoad.add(new Cell(i, land.side/2 - 1));
            }
            for (int i = land.side - 1; i >= 0; i--) {
                newRoad.add(new Cell(land.side/2 - 1, i));
            }
            for (int i = land.side - 1; i >= 0; i--) {
                newRoad.add(new Cell(i, land.side/4 - 1));
            }
            for (int i = land.side - 1; i >= 0; i--) {
                newRoad.add(new Cell(i, 3*land.side/4 - 1));
            }
        }
    }
}
