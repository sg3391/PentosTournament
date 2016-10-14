package pentos.g10;

import pentos.sim.Building;
import pentos.sim.Land;

public interface Planner {
	public Action makeAPlan(Player player,Building request,Land land);
}
