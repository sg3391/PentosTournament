package pentos.g2;

import pentos.sim.Building;
import pentos.sim.Cell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Sequencer implements pentos.sim.Sequencer {

    private Random gen;
    private final double ratio = 0.7; // ratio of residences to total number of buildings

    private boolean factoryNext = true;

    private static final int[][]
            STAR = {{1,0}, {1,1}, {1,2}, {0,1}, {2,1}},
            LSHAPE = {{0,0}, {0,1}, {0,2}, {0,3}, {1,3}},
            TSHAPE = {{0,0}, {1,0}, {2,0}, {1,1}, {1,2}},
            RSHAPE = {{0,0}, {0,1}, {0,2}, {0,3}, {1,0}},
            USHAPE = {{0,0}, {0,1}, {1,1}, {2,1}, {2,0}},
            SQUASH = {{0,0}, {0,1}, {0,2}, {1,2}, {1,1}},
            RSQUASH = {{1,0}, {1, 1}, {1,2}, {0, 2}, {0,1}},
            LINE = {{0,0}, {0,1}, {0,2}, {0,3}, {0,4}},
            RTSHAPE = {{0,1}, {1,1}, {2,1}, {2,0}, {3,0}},
            BEND = {{0,0}, {0,1}, {0,2}, {1,2}, {2,2}};

    private static final int[][][] shapes = {STAR, LSHAPE, TSHAPE, RSHAPE, USHAPE, SQUASH, RSQUASH, LINE, RTSHAPE, BEND};
    private int current;

    public void init() {
        current = 0;
    }

    @Override
    public void init(Long seed) {

    }

    public Building next() {

        factoryNext = !factoryNext;

        if(factoryNext) {
            Set<Cell> factory = new HashSet<Cell>();
            factory.add(new Cell(0,0));
            return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
        } else {
            Set<Cell> residence = new HashSet<Cell>();
            for(int[] coords : shapes[current]) {
                residence.add(new Cell(coords[0], coords[1]));
            }
            current = (current + 1) % shapes.length;
            return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
        }
    }

}
