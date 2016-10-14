package pentos.g1;

import java.util.*;
import pentos.sim.Building;
import pentos.sim.Cell;

public class Sequencer implements pentos.sim.Sequencer {

    private Random gen;
    private int count;
    private final double ratio = 0.8; // ratio of residences to total number of buildings

    public void init(Long seed) {
		if (seed == null) gen = new Random();
		else gen = new Random(seed);
		count = 0;
    }
    
    public Building next() {
    	++ count;
		if (gen.nextDouble() < ratio)
		    if (gen.nextDouble() < 0.6) return starsResidence();
		    else return barResidence();
		else {
			if (count <= 140)
		    	return randomSmallFactory();
		    else return largeFactory();
		}
    }

    private Building starsResidence() {
		Set<Cell> residence = new HashSet<Cell>();
		residence.add(new Cell(1,1));
		residence.add(new Cell(0,1));
		residence.add(new Cell(1,0));
		residence.add(new Cell(2,1));
		residence.add(new Cell(1,2));
		return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
    }

    private Building barResidence() {
		Set<Cell> residence = new HashSet<Cell>();
		residence.add(new Cell(0,0));
		residence.add(new Cell(0,1));
		residence.add(new Cell(0,2));
		residence.add(new Cell(0,3));
		residence.add(new Cell(0,4));
		return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
    }

    private Building randomSmallFactory() {
		Set<Cell> factory = new HashSet<Cell>();
		int width = 1;
		int height = gen.nextInt(2) + 1;
		for (int i=0; i<width; i++) {
		    for (int j=0; j<height; j++) {
				factory.add(new Cell(i,j));
		    }
		}
		return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }

    private Building largeFactory() {
		Set<Cell> factory = new HashSet<Cell>();
		int width = 5;
		int height = 5;
		for (int i=0; i<width; i++) {
		    for (int j=0; j<height; j++) {
			factory.add(new Cell(i,j));
		    }
		}
		return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }    
    
}
