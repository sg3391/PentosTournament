package pentos.g4;

import java.util.*;
import pentos.sim.Building;
import pentos.sim.Cell;
/*
Random 11 time for residence, then put all cross
11 time 5*5 factory, then random size of square factory
*/
public class Sequencer implements pentos.sim.Sequencer {

    private Random gen;
    private int turn = 0;
    private double ratio = 0.5; // ratio of residences to total number of buildings
    private int factory_count = 0;
    private int residence_count = 0;

    public void init(Long seed) {
	if (seed != null) 
	    gen = new Random(seed.longValue());
	else
	    gen = new Random();
    }
    
    public Building next() {
		if (gen.nextDouble() > ratio){
			if(factory_count < 11){
				factory_count ++;
				return randomFactory(5);
			}
			else{
				return randomFactory(gen.nextInt(5)+1);	
			}
		}
		else{
			if(residence_count < 11){
				residence_count++;
				return randomResidence();
			}
			else{
				return cross();
			}
		    
		}
    }

    private Building randomFactory(int side) { // random rectangle
		Set<Cell> factory = new HashSet<Cell>();
		int width = side;
		int height = side;
		    
		for (int i=0; i<width; i++) {
		    for (int j=0; j<height; j++) {
			factory.add(new Cell(i,j));
		    }
		}
		return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }   


    private Building randomResidence() { // random walk of length 5
	Set<Cell> residence = new HashSet<Cell>();
	Cell tail = new Cell(0,0);
	residence.add(tail);
	for (int i=0; i<4; i++) {
	    ArrayList<Cell> walk_cells = new ArrayList<Cell>();
	    for (Cell p : tail.neighbors()) {
		if (!residence.contains(p))
		    walk_cells.add(p);
	    }
	    tail = walk_cells.get(gen.nextInt(walk_cells.size()));
	    residence.add(tail);
	}
	return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
    }    

    private Building randomFactory() { // random rectangle with side length between 2 and 5 inclusive
		Set<Cell> factory = new HashSet<Cell>();
		int width = gen.nextInt(3);
		int height = gen.nextInt(3);
		for (int i=0; i<width+2; i++) {
		    for (int j=0; j<height+2; j++) {
			factory.add(new Cell(i,j));
		    }
		}
		return new Building(factory.toArray(new Cell[factory.size()]), Building.Type.FACTORY);
    }     

    private Building cross() { // random walk of length 5
		Set<Cell> residence = new HashSet<Cell>();
		residence.add(new Cell(1,1));
		residence.add(new Cell(0,1));
		residence.add(new Cell(1,0));
		residence.add(new Cell(2,1));
		residence.add(new Cell(1,2));
		return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
    }   
    
}
