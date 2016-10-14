package pentos.g10;

import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Cell;

import java.util.*;
import java.lang.*;

public class FactoryPlanner implements Planner
{

    @Override
    public Action makeAPlan(Player player, Building request, Land land)
    {
        return packFactory(player, request, land);
    }


    private Action packFactory(Player player, Building request, Land land)
    {
        int[] rows              = player.factoryRows;
        List<Integer> availRows = new ArrayList<Integer>();
        int height              = getHeight(request, land);
        int length              = getLength(request, height);
        Building[] rotations    = request.rotations();

        //System.out.println(height);
        //System.out.println(length);

        for (int i = 0; i < land.side; i++) {
            if (rows[i] == height) {
                availRows.add(i);
            }
            /* no rows with this height yet */
            else if (rows[i] == 0) {
                availRows.add(i);
                break;
            }
        }

        for (int row : availRows) {
            int playerRow = row;
            row = land.side - (row + height);
            System.out.println(playerRow);
            System.out.println(row);
            int j = 0;
            /* search for unoccupied cell in row */
            while (!land.unoccupied(row, j) && j < land.side) {
                j++;
            }

            /* check if enought space to build */
            if (land.side - j >= length) {
                Cell start = new Cell(row, j);
                System.out.println(start);

                if (!land.buildable(request, start)) {
                    continue;
                }
                Action action = new Action(request, start);

                /* reserve rows */
                if (rows[playerRow] == 0) {
                    player.updateFactoryRows(playerRow, height);
                    int blockedRows = playerRow;
                    while (blockedRows++ < playerRow + height - 1
                           && playerRow < land.side) {
                        player.updateFactoryRows(blockedRows, land.side);
                    }
                    if (playerRow > 0 && rows[playerRow-1] != land.side+ 1) {
                        player.updateFactoryRows(blockedRows, land.side + 1);
                    }
                }
                /* dynamically add roads above */
                Set<Cell> roads = new HashSet<>();
                Set<Cell> currentRoads = player.roadcells;
                if (row - 1 >= 0 && playerRow > 0 && rows[playerRow - 1] != land.side + 1) {
                    for (int k = j; k < length + j; k++) {
                        System.out.println(k);
                        Cell roadCandidate = new Cell(row - 1, k);
                        if (!currentRoads.contains(roadCandidate)) {
                            roads.add(roadCandidate);
                        }
                    }
                }
                /* dynamically add roads below */
                if (playerRow > 0 && rows[playerRow - 1] == land.side + 1) {
                    for (int k = j; k < length + j; k++) {
                        Cell roadCandidate = new Cell(row + height, k);
                        if (!currentRoads.contains(roadCandidate)) {
                            roads.add(roadCandidate);
                        }
                    }
                }
                action.setRoadCells(roads);
                System.out.println(roads);

                return action;
            }
        }

        /* could not build factory using this mechanism */
        return null;
    }

    private int getLength(Building request, int height)
    {
        return request.size() / height;
    }

    private int getHeight(Building request, Land land)
    {
        int iMax = 0;
        int iMin = land.side - 1;
        int jMax = 0;
        int jMin = land.side - 1;

        Iterator<Cell> it = request.iterator();

        /* find min and max coordinates */
        while (it.hasNext()) {
            Cell c = it.next();
            //System.out.println(c);

            if (c.i > iMax) {
                iMax = c.i;
            }
        }

        return iMax + 1;
    }
}
