package pentos.g6;

import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

public interface Padding {
	public Move getPadding(Building request, int rotation, Land land, Row row, int location, boolean buildWater, int offSet);
}
