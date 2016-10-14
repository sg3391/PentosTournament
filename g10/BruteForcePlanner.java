package pentos.g10;

import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;

public class BruteForcePlanner implements Planner {
	static int maxTries = 20;

	@Override
	public Action makeAPlan(Player player, Building request, Land land) {
		if (request.type == Building.Type.RESIDENCE)
			return bruteForceResidenceSolution(player, request, land);
		else if (request.type == Building.Type.FACTORY)
			return bruteForceFactorySolution(player, request, land);
		else {
			System.out.println("Unknown building type: " + request.type);
			return new Action();
		}
	}

	public Action bruteForceResidenceSolution(Player player, Building request, Land land) {
		int tries = 0;
		double score = -100.0;
		Action toTake = new Action();
		for (int i = 0; i < land.side; i++) {
			for (int j = 0; j < land.side; j++) {
				Cell c = new Cell(i, j);
				Building[] rotations = request.rotations();
				for (int k = 0; k < rotations.length; k++) {
					Building b = rotations[k];
					if (land.buildable(b, c)) {
						Set<Cell> shiftedCells = ToolBox.shiftCells(b, c);
						// Set<Cell> roads =
						// ToolBox.findShortestRoad(shiftedCells, land,
						// player.roadcells);
						Action toCheck = new Action(request, c, k);
						Set<Cell> roads = RoadFinder.findRoad(player, toCheck, land);
						if (roads == null) {
							System.out.println("Cannot plan roads. This building will not work.");
							continue;
						} else {
							System.out.println("One solution found");
							toCheck.setRoadCells(roads);
//							/* Build parks and ponds */
//							Set<Cell> parkCells = ParkAndWaterFinder.findPark(player, toCheck, land);
//							toCheck.setParkCells(parkCells);
//							Set<Cell> water = ParkAndWaterFinder.findWater(player, toCheck, land);
//							toCheck.setWaterCells(water);

							/* Validate here */
							boolean ok = PlanEvaluator.validateMove(toCheck, player, land);
							if (!ok) {
								System.out.println("Actin rejected in planner");
								continue;
							}

							/* Optimization: Try only limited solutions. */
							if (tries >= maxTries) {
								System.out.println("Tried " + tries + " solutions already. Choose the best one.");
								System.out.println("Optimal solution has score: " + score);
								return toTake;
							}

							double thisScore = PlanEvaluator.evaluateLastMinutePlan(player, toCheck, land);
							if (thisScore > score) {
								score = thisScore;
								toTake = toCheck;
							}
							tries++;
						}
					}
				}
			}
		}
		System.out.println("Optimal solution has score: " + score);
		return toTake;
	}

	public Action bruteForceFactorySolution(Player player, Building request, Land land) {
		int tries = 0;
		double score = -100.0;
		Action toTake = new Action();
		for (int i = land.side - 1; i >= 0; i--) {
			for (int j = land.side - 1; j >= 0; j--) {
				Cell c = new Cell(i, j);
				Building[] rotations = request.rotations();
				for (int k = 0; k < rotations.length; k++) {
					Building b = rotations[k];
					if (land.buildable(b, c)) {
						Set<Cell> shiftedCells = ToolBox.shiftCells(b, c);
						// Set<Cell> roads =
						// ToolBox.findShortestRoad(shiftedCells, land,
						// player.roadcells);
						Action toCheck = new Action(request, c, k);
						Set<Cell> roads = RoadFinder.findRoad(player, toCheck, land);
						if (roads == null) {
							System.out.println("Cannot plan roads. This building will not work.");
							continue;
						} else {
							System.out.println("One solution found");
							toCheck.setRoadCells(roads);
							/* Validate here */
							boolean ok = PlanEvaluator.validateMove(toCheck, player, land);
							if (!ok) {
								System.out.println("Actin rejected in planner");
								continue;
							}

							/* Optimization: Try only limited solutions. */
							if (tries >= maxTries) {
								System.out.println("Tried " + tries + " solutions already. Choose the best one.");
								System.out.println("Optimal solution has score: " + score);
								return toTake;
							}

							double thisScore = PlanEvaluator.evaluateLastMinutePlan(player, toCheck, land);
							if (thisScore > score) {
								score = thisScore;
								toTake = toCheck;
							}
							tries++;
						}
					}

				}
			}
		}
		System.out.println("Optimal solution has score: " + score);
		return toTake;
	}
}
