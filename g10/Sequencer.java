package pentos.g10;

import java.util.*;
import pentos.sim.Building;
import pentos.sim.Cell;

public class Sequencer implements pentos.sim.Sequencer {

    private Random gen;
    private double ratio = 0.2; // ratio of residences to total number of buildings
    private int lastSideLength = 1;

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

    private Building randomResidence() { // random walk of length 5
	Set<Cell> residence = new HashSet<Cell>();
	residence.add(new Cell(1,1));
	residence.add(new Cell(0,1));
	residence.add(new Cell(1,0));
	residence.add(new Cell(2,1));
	residence.add(new Cell(1,2));
	return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
    }    

    private Building randomFactory() { // random rectangle
	Set<Cell> factory = new HashSet<Cell>();
	int new_length = (lastSideLength + 1) % 5;
	if (new_length == 0)
	{
		new_length = new_length + 1;
	}
	int width = new_length;
	int height = new_length;
	lastSideLength = new_length; //set equal for next turn
	for (int i=0; i<width; i++) {
	    for (int j=0; j<height; j++) {
		factory.add(new Cell(i,j));
	    }
	}
	return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }
    
}