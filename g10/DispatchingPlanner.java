package pentos.g10;

import pentos.sim.Building;
import pentos.sim.Land;

public class DispatchingPlanner implements Planner {
	FactoryPlanner fp = new FactoryPlanner();
	ResidencePlanner rp = new ResidencePlanner();

	@Override
	public Action makeAPlan(Player player, Building request, Land land) {
		Action toTake;
		if (request.type == Building.Type.RESIDENCE)
			toTake = rp.makeAPlan(player, request, land);
		else if (request.type == Building.Type.FACTORY)
			toTake = fp.makeAPlan(player, request, land);
		else {
			System.out.println("Error: request type " + request.type);
			toTake = new Action();
		}
		ToolBox.reportAction(toTake);
		return toTake;
	}

}
