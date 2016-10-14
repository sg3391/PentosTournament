package pentos.g8;

import pentos.sim.Building;
import pentos.sim.Cell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/*
 * Sequencer starts off with buildings then 
 * moves on to factories
 */
public class Sequencer implements pentos.sim.Sequencer {

    private Random gen = new Random();
    private final double ratio = 0.5; // ratio of residences to total number of buildings
    private int factnum = 1;
    
    public static int [][][] indices = {
    {{0,0},{0,1},{0,2},{0,3},{0,4}},
    
    {{0,1},{0,2},{1,0},{1,1},{2,1}},
    {{0,0},{0,1},{1,1},{1,2},{2,1}},
    
    {{0,0},{0,1},{1,0},{2,0},{3,0}},
    {{0,0},{0,1},{1,1},{2,1},{3,1}},
    
    {{0,0},{0,1},{1,0},{1,1},{2,1}},
    {{0,0},{0,1},{1,0},{1,1},{2,0}},
    
    {{0,1},{1,1},{2,0},{2,1},{3,0}},
    {{0,0},{1,0},{2,0},{2,1},{3,1}},
    
    {{0,0},{0,1},{0,2},{1,1},{2,1}},
    
    {{0,0},{1,0},{1,1},{1,2},{0,2}},
    
    {{0,0},{0,1},{0,2},{1,2},{2,2}},
    
    {{0,0},{0,1},{1,1},{1,2},{2,2}},
    
    {{0,1},{1,0},{1,1},{1,2},{2,1}},
    
    {{0,1},{1,0},{1,1},{2,1},{3,1}},
    {{0,0},{1,0},{1,1},{2,0},{3,0}},
    
    {{0,0},{0,1},{1,1},{2,1},{2,2}},
    {{0,1},{0,2},{1,1},{2,1},{2,0}}
    };
    
    
    private int count=0;

    public void init() {

    }

    public void init(Long seed) {

    }

    public Building next() {
        if (count < 105) {
            return randomRes();
        }else {
            return randomFactory();
        }
    }

    private Building randomFactory() { // random rectangle
    Set<Cell> factory = new HashSet<Cell>();
    int width = gen.nextInt(5);
    int height = gen.nextInt(5);
    for (int i=0; i<width+1; i++) {
        for (int j=0; j<height+1; j++) {
        factory.add(new Cell(i,j));
        }
    }
    return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }

    private Building randomRes(){
    	Set<Cell> res = new HashSet<Cell>();
        for(int[] shape : indices[count%18]) {
            res.add(new Cell(shape[0], shape[1]));
        }
        ++count;
        return new Building(res.toArray(new Cell[res.size()]), Building.Type.RESIDENCE);
    }    
}
