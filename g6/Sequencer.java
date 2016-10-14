/**
 * 
 */
package pentos.g6;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Cell.Type;

/**
 * @author rajan
 *
 */
public class Sequencer implements pentos.sim.Sequencer {

	private Random rand;
	private double threshold = 0.0;
	
	@Override
	public void init(Long seed) {
		if (seed != null)
			rand = new Random(seed);
		else
			rand = new Random();
	}

	
	@Override
	public Building next() {
		double sample = rand.nextDouble();
		Set<Cell> residence = new HashSet<>();
		//if (sample > threshold) {
			residence.add(new Cell(0,0));
			residence.add(new Cell(0,1));
			residence.add(new Cell(0,2));
			residence.add(new Cell(0,3));
			residence.add(new Cell(0,4));
		/*} else {
			residence.add(new Cell(0,0));
			residence.add(new Cell(1,0));
			residence.add(new Cell(1,1));
			residence.add(new Cell(2,1));
			residence.add(new Cell(3,1));
		}*/
		return new Building(residence.toArray(new Cell[residence.size()]), Building.Type.RESIDENCE);
	}

}
