package pentos.dots;

import java.util.*;
import pentos.sim.Building;
import pentos.sim.Cell;

public class Sequencer implements pentos.sim.Sequencer {

    private Random gen;
    private int turn = 0;
    private double ratio = 0.5; // ratio of residences to total number of buildings

    public void init(Long seed) {
	if (seed != null) 
	    gen = new Random(seed.longValue());
	else
	    gen = new Random();
    }
    
    public Building next() {
	if (gen.nextDouble() > ratio) {
	    return randomFactory();
	}
	else {
	    return randomResidence();
	}
    }

    private Building randomResidence() {
	Set<Cell> residence = new HashSet<Cell>();
	residence.add(new Cell(1,1));
	residence.add(new Cell(0,1));
	residence.add(new Cell(1,0));
	residence.add(new Cell(2,1));
	residence.add(new Cell(1,2));
	return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);	
    }    

    private Building randomFactory() { // 1x1 dots
	Set<Cell> factory = new HashSet<Cell>();
	factory.add(new Cell(0,0));
	return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }    
    
}
