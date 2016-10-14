package pentos.misfits;

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
	if (gen.nextDouble() > ratio)
	    return randomFactory();
	else
	    return randomResidence();
    }

    private Building randomResidence() {
	Set<Cell> residence = new HashSet<Cell>();
	if (gen.nextDouble() > 0.5) { // stars
	    residence.add(new Cell(1,1));
	    residence.add(new Cell(0,1));
	    residence.add(new Cell(1,0));
	    residence.add(new Cell(2,1));
	    residence.add(new Cell(1,2));
	}
	else {
	    if (gen.nextDouble() > 0.5) { // t-shapes
		residence.add(new Cell(0,0));
		residence.add(new Cell(0,1));
		residence.add(new Cell(0,2));
		residence.add(new Cell(1,1));
		residence.add(new Cell(2,1));
	    }
	    else {
		residence.add(new Cell(0,0));
		residence.add(new Cell(1,0));
		residence.add(new Cell(2,0));
		residence.add(new Cell(1,1));
		residence.add(new Cell(1,2));
	    }
	}
	return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);	
    }    

    private Building randomFactory() { // 1x5 lines
	Set<Cell> factory = new HashSet<Cell>();
	int width = 1;
	int height = 5;
	if (gen.nextDouble() > 0.5) {
	    width = 5;
	    height = 1;
	}	    
	for (int i=0; i<width; i++) {
	    for (int j=0; j<height; j++) {
		factory.add(new Cell(i,j));
	    }
	}
	return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }    
    
}
