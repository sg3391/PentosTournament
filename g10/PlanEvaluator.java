package pentos.g10;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;

public class PlanEvaluator {
	/*
	 * If the action require building roads, penalize by buildRoadPenalty. If
	 * the action requires building more than roadThreshold road cells, penalty
	 * becomes constant roadAboveThresholdPrice. If the action requires building
	 * less than roadThreshold road cells, penalty becomes size *
	 * roadUnderThresholdPrice
	 */
	static double buildRoadPenalty = -8.0;
	
	static int roadThreshold = 16;
	static double roadUnderThresholdPrice = -0.0;
	static double roadAboveThresholdPrice = -0.0;

	/*
	 * The price of a road neighbor is different when there are or are not
	 * enough road neighbors.
	 */
	static int roadNeighborRadius = 10;
	static int roadNeighborThreshold = 5;
	static double enoughRoadNeighborsPrice = 0;
	static double notEnoughRoadNeighborsPrice = -0.0;

	/*
	 * The price of a road neighbor is different when there are or are not
	 * enough vacant border neighbors.
	 */
	static int borderRadius = 10;
	static int borderThreshold = 3;
	static double enoughBorderPrice = 0;
	static double notEnoughBorderPrice = -16.0;

	/*
	 * If the residence is adjacent to park, give a bonus. If the residence
	 * builds park cells, give a penalty. If the residence occupies park cells,
	 * give a penalty.
	 */
	static double adjacentToParkBonus = 3.0;
	static double buildParkPrice = -0.5;
	static double occupyParkNeighborPrice = -0.5;

	/*
	 * If the residence is adjacent to water, give a bonus. If the residence
	 * builds water cells, give a penalty. If the residence occupies water
	 * cells, give a penalty.
	 */
	static double adjacentToWaterBonus = 3.0;
	static double buildWaterPrice = -0.5;
	static double occupyWaterNeighborPrice = -0.5;

	/*
	 * If the building plan occupies cells on the border of the cluster, give a
	 * bonus.
	 */
	static double packedToClusterBonus = 1;

	/*
	 * If the building is far from the border it starts from, give a penalty.
	 */
	static double distancePrice = -0.5;

	/*
	 * If the building will make the map neat with a straight border, give a
	 * bonus.
	 */
	static int neatSquareSize = 3;
	static double neatBorderBonus = 0.1;

	/*
	 * If the building blocks an existing road, give a serious penalty.
	 */
	static double blockRoadPenalty = -64.0;

	/*
	 * If the building leaves unreachable whitespace, give a serious penalty.
	 */
	static double breakSpacePenalty = -0.0;
	
	/*
	 * Penalize whitespace above this building
	 */
	static double whiteSpacePenalty=-0.0;
	
	/* Setters */
	public static void setBuildRoadPenalty(double buildRoadPenalty) {
		PlanEvaluator.buildRoadPenalty = buildRoadPenalty;
	}

	public static void setRoadThreshold(int roadThreshold) {
		PlanEvaluator.roadThreshold = roadThreshold;
	}

	public static void setRoadUnderThresholdPrice(double roadUnderThresholdPrice) {
		PlanEvaluator.roadUnderThresholdPrice = roadUnderThresholdPrice;
	}

	public static void setRoadAboveThresholdPrice(double roadAboveThresholdPrice) {
		PlanEvaluator.roadAboveThresholdPrice = roadAboveThresholdPrice;
	}

	public static void setRoadNeighborRadius(int roadNeighborRadius) {
		PlanEvaluator.roadNeighborRadius = roadNeighborRadius;
	}

	public static void setRoadNeighborThreshold(int roadNeighborThreshold) {
		PlanEvaluator.roadNeighborThreshold = roadNeighborThreshold;
	}

	public static void setEnoughRoadNeighborsPrice(double enoughRoadNeighborsPrice) {
		PlanEvaluator.enoughRoadNeighborsPrice = enoughRoadNeighborsPrice;
	}

	public static void setNotEnoughRoadNeighborsPrice(double notEnoughRoadNeighborsPrice) {
		PlanEvaluator.notEnoughRoadNeighborsPrice = notEnoughRoadNeighborsPrice;
	}

	public static void setBorderRadius(int borderRadius) {
		PlanEvaluator.borderRadius = borderRadius;
	}

	public static void setBorderThreshold(int borderThreshold) {
		PlanEvaluator.borderThreshold = borderThreshold;
	}

	public static void setEnoughBorderPrice(double enoughBorderPrice) {
		PlanEvaluator.enoughBorderPrice = enoughBorderPrice;
	}

	public static void setNotEnoughBorderPrice(double notEnoughBorderPrice) {
		PlanEvaluator.notEnoughBorderPrice = notEnoughBorderPrice;
	}

	public static void setAdjacentToParkBonus(double adjacentToParkBonus) {
		PlanEvaluator.adjacentToParkBonus = adjacentToParkBonus;
	}

	public static void setBuildParkPrice(double buildParkPrice) {
		PlanEvaluator.buildParkPrice = buildParkPrice;
	}

	public static void setOccupyParkNeighborPrice(double occupyParkNeighborPrice) {
		PlanEvaluator.occupyParkNeighborPrice = occupyParkNeighborPrice;
	}

	public static void setAdjacentToWaterBonus(double adjacentToWaterBonus) {
		PlanEvaluator.adjacentToWaterBonus = adjacentToWaterBonus;
	}

	public static void setBuildWaterPrice(double buildWaterPrice) {
		PlanEvaluator.buildWaterPrice = buildWaterPrice;
	}

	public static void setOccupyWaterNeighborPrice(double occupyWaterNeighborPrice) {
		PlanEvaluator.occupyWaterNeighborPrice = occupyWaterNeighborPrice;
	}

	public static void setPackedToClusterBonus(double packedToClusterBonus) {
		PlanEvaluator.packedToClusterBonus = packedToClusterBonus;
	}

	public static void setDistancePrice(double distancePrice) {
		PlanEvaluator.distancePrice = distancePrice;
	}

	public static void setNeatSquareSize(int neatSquareSize) {
		PlanEvaluator.neatSquareSize = neatSquareSize;
	}

	public static void setNeatBorderBonus(double neatBorderBonus) {
		PlanEvaluator.neatBorderBonus = neatBorderBonus;
	}

	public static void setBlockRoadPenalty(double blockRoadPenalty) {
		PlanEvaluator.blockRoadPenalty = blockRoadPenalty;
	}

	public static void setBreakSpacePenalty(double breakSpacePenalty) {
		PlanEvaluator.breakSpacePenalty = breakSpacePenalty;
	}

	public static void setWhiteSpacePenalty(double whiteSpacePenalty) {
		PlanEvaluator.whiteSpacePenalty = whiteSpacePenalty;
	}

	public static double evaluatePlan(Player player, Action action, Land land) {
		Building b = action.getBuilding();
		if (b == null) {
			System.out.println("The plan to evaluate is empty. No building action to evaluate.");
			return -100.0;
		} else if (b.type == Building.Type.RESIDENCE) {
			return evaluateResidence(player, action, land);
		} else {
			return evaluateFactory(player, action, land);
		}
	}

	public static double evaluateFactory(Player player, Action action, Land land) {
		double score = 0;
		if (action.getBuilding() == null) {
			System.out.println("Error: Residence has no building!");
			return -100.0;
		}

		/* Find all the cells this building plan will occupy */
		Set<Cell> toOccupy = ToolBox.combineSets(action.getAbsoluteBuildingCells(), action.getRoadCells(),
				action.getParkCells(), action.getWaterCells());

		/* Road related score calculation */
		score += calculateRoadScore(toOccupy, player, action, land);

		/* Road neighbors related score */
		score += calculateRoadNeighborScore(toOccupy, player, action, land);

		/* Border related score */
		score += calculateBorderScore(toOccupy, player, action, land);

		/* Park and water related score calculation */
		// double parkScore = calculateParkScore(toOccupy, player, action,
		// land);
		// score += parkScore;
		// double waterScore = calculateWaterScore(toOccupy, player, action,
		// land);
		// score += waterScore;

		/* How packed is the building to the existing cluster */
		double packedToCluster = compactnessScore(toOccupy, player, action, land);
		score += packedToCluster;

		// /* The tidiness of the land map */
		// double tidiness = tidinessScore(toOccupy, land);
		// score += tidiness;
		//
		// /* The distance to the starting border. */
		// double distancePenalty = distanceScore(toOccupy, player, action,
		// land);
		// score += distancePenalty;

		/* Check if the building will block a road */
		boolean blockRoad = checkBlockRoads(action, player, land);
		if (blockRoad) {
			System.out.println("==================");
			System.out.println("Blocking the road!");
			score += blockRoadPenalty;
			System.out.println("==================");
		}

		/* Check if the building will make an unreachable space */
		boolean preserveConnectivity = evaluateWhiteSpace(action, player, land);
		if (!preserveConnectivity) {
			System.out.println("This action will break the connected whitespace.");
			score += breakSpacePenalty;
		}

		return score;
	}

	public static double evaluateResidence(Player player, Action action, Land land) {
		double score = 0;
		if (action.getBuilding() == null) {
			System.out.println("Error: Residence has no building!");
			return -100.0;
		}

		/* Find all the cells this building plan will occupy */
		Set<Cell> toOccupy = ToolBox.combineSets(action.getAbsoluteBuildingCells(), action.getRoadCells(),
				action.getParkCells(), action.getWaterCells());

		/* Road related score calculation */
		score += calculateRoadScore(toOccupy, player, action, land);

		/* Road neighbors related score */
		score += calculateRoadNeighborScore(toOccupy, player, action, land);

		/* Border related score */
		score += calculateBorderScore(toOccupy, player, action, land);

		 /* Park and water related score calculation */
		 double parkScore = calculateParkScore(toOccupy, player, action,
		 land);
		 score += parkScore;
		 double waterScore = calculateWaterScore(toOccupy, player, action,
		 land);
		 score += waterScore;

		/* How packed is the building to the existing cluster */
		double packedToCluster = compactnessScore(toOccupy, player, action, land);
		score += packedToCluster;
		
		/* How much whitespace it leaves above it */
		double whiteSpacePenalty=countWhiteSpace(toOccupy,player,action,land);
		System.out.println("White space penalty is: "+whiteSpacePenalty);
		score+=whiteSpacePenalty;

		// /* The tidiness of the land map */
		// double tidiness = tidinessScore(toOccupy, land);
		// score += tidiness;
		//
		 /* The distance to the starting border. */
		 double distancePenalty = distanceScore(toOccupy, player, action,land);
		 System.out.println("The distance penalty for this residence is "+distancePenalty);
		 score += distancePenalty;

		/* Check if the building will block a road */
		boolean blockRoad = checkBlockRoads(action, player, land);
		if (blockRoad) {
			System.out.println("==================");
			System.out.println("Blocking the road!");
			score += blockRoadPenalty;
			System.out.println("==================");
		}

		/* Check if the building will make an unreachable space */
		boolean preserveConnectivity = evaluateWhiteSpace(action, player, land);
		if (!preserveConnectivity) {
			System.out.println("This action will break the connected whitespace.");
			score += breakSpacePenalty;
		}
		return score;
	}
	public static double countWhiteSpace(Set<Cell> toOccupy,Player player,Action action,Land land){
		Set<Cell> total=ToolBox.combineSets(action.getAbsoluteBuildingCells(),action.getRoadCells(),action.getParkCells(),action.getWaterCells());
		Set<Cell> allNeighbors=ParkAndWaterFinder.findTwoLevelNeighbors(total, land);
		Cell topLeft=ToolBox.findTopLeft(action.getAbsoluteBuildingCells());
		double score=0.0;
		int count=0;
		for(Cell c:allNeighbors){
			if(c.i<=topLeft.i)
				count++;
		}
		score=whiteSpacePenalty*count;
		System.out.println(count+" white space cells above.");
		return score;
	}
	
	public static double distanceScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		// calculate how far it's from the corner
		double score = 0.0;
		double distance = 0.0;
		if (action.getBuilding().type == Building.Type.RESIDENCE) {
			Cell start = action.getStartPoint();
			distance = ToolBox.calculateVerticalDistance(start, 0);
		} else if (action.getBuilding().type == Building.Type.FACTORY) {
			Cell start = ToolBox.findBottomRight(action.getAbsoluteBuildingCells());
			distance = ToolBox.calculateVerticalDistance(start, land.side);
		}
		score += distancePrice * distance;
		return score;
	}

	public static double calculateRoadScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		double score = 0.0;
		if (action.getRoadCells() != null) {
			if (action.getRoadCells().size() > 0) {
				score += buildRoadPenalty;
				int siz = action.getRoadCells().size();
				if (siz < roadThreshold)
					score += roadUnderThresholdPrice * action.getRoadCells().size();
				else
					score += roadAboveThresholdPrice;
			}
		}
		return score;
	}

	public static double calculateRoadNeighborScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		double score = 0.0;

		// Find road neighbors in the quarter
		Set<Cell> nearbyRoadNeighbors = new HashSet<>();
		for (Cell n : player.roadNeighbors) {
			if (ToolBox.geoDistance(action.getStartPoint(), n) < roadNeighborRadius) {
				nearbyRoadNeighbors.add(n);
			}
		}

		/*
		 * The fewer the vacant road neighbor cells are, the more expensive to
		 * occupy each.
		 */
		int occupiedRoadNeighbors = ToolBox.setInterception(nearbyRoadNeighbors, toOccupy).size();
		int potentialRoad = nearbyRoadNeighbors.size();
		System.out.println(potentialRoad + " road neighbors around.");
		if (potentialRoad <= roadNeighborThreshold) {
			score += notEnoughRoadNeighborsPrice * occupiedRoadNeighbors;
		} else
			score += enoughRoadNeighborsPrice * occupiedRoadNeighbors;

		return score;
	}

	public static double calculateBorderScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		double score = 0.0;
		// Find road neighbors in the quarter
		Set<Cell> nearbyBorders = new HashSet<>();
		for (Cell n : player.vacantBorders) {
			if (ToolBox.geoDistance(action.getStartPoint(), n) < roadNeighborRadius) {
				nearbyBorders.add(n);
			}
		}

		int vacantBorderSize = nearbyBorders.size();
		System.out.println(vacantBorderSize + " vacant border cells around.");
		int coverBorder = ToolBox.setInterception(toOccupy, nearbyBorders).size();
		if (vacantBorderSize <= borderThreshold) {
			score += notEnoughBorderPrice * coverBorder;
		} else {
			score += enoughBorderPrice * coverBorder;
		}
		return score;
	}

	public static double evaluateLastMinutePlan(Player player, Action action, Land land) {
		double score = 0.0;
		/* Find all the cells this building plan will occupy */
		Set<Cell> toOccupy = ToolBox.combineSets(action.getAbsoluteBuildingCells(), action.getRoadCells(),
				action.getParkCells(), action.getWaterCells());
		double packedToCluster = compactnessScore(toOccupy, player, action, land);
		score += packedToCluster;
		/* Check if the building will block a road */
		boolean blockRoad = checkBlockRoads(action, player, land);
		if (blockRoad) {
			System.out.println("==================");
			System.out.println("Blocking the road!");
			score += blockRoadPenalty;
			System.out.println("==================");
		}
		return score;
	}

	public static double tidinessScore(Set<Cell> toOccupy, Land land) {
		int count = 0;
		for (int i = 0; i < 50; i++) {
			for (int j = 0; j < 50; j++) {
				Cell c = new Cell(i, j);
				if (vacantSquare(c, toOccupy, land, neatSquareSize))
					count++;
			}
		}
		return neatBorderBonus * count;
	}

	public static boolean vacantSquare(Cell c, Set<Cell> toOccupy, Land land, int side) {
		for (int i = 0; i < side; i++) {
			for (int j = 0; j < side; j++) {
				Cell toCheck = new Cell(c.i + i, c.j + j);
				if (!ToolBox.isValid(toCheck))// cell itself has invalid
												// coordinates
					return false;
				if (toOccupy.contains(toCheck))// cell will be occupied
					return false;
				if (!land.unoccupied(toCheck))// cell is already occupied
					return false;
			}
		}
		return true;
	}

	public static double calculateParkScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		double score = 0.0;
		// if (action.getBuilding().type == Building.Type.FACTORY)
		// return score;

		int adjacentToParks = ToolBox.setInterception(player.parkNeighbors, toOccupy).size();
		if (adjacentToParks > 0)
			score += adjacentToParkBonus;

		if (action.getParkCells().size() > 0)
			score += buildParkPrice * action.getParkCells().size();

		int occupiedParkNeighbors = ToolBox.setInterception(player.parkNeighbors, toOccupy).size();
		score += occupiedParkNeighbors * occupyParkNeighborPrice;
		// score += 0.5 * adjacentToParks;

		return score;
	}

	public static double calculateWaterScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		double score = 0.0;
		// if (action.getBuilding().type == Building.Type.FACTORY)
		// return score;

		int adjacentToWater = ToolBox.setInterception(player.waterNeighbors, toOccupy).size();
		if (adjacentToWater > 0)
			score += adjacentToWaterBonus;

		int size = action.getWaterCells().size();
		if (size > 0)
			score += size * buildWaterPrice;

		int occupiedWaterNeighbors = ToolBox.setInterception(player.waterNeighbors, toOccupy).size();
		score += occupiedWaterNeighbors * occupyWaterNeighborPrice;
		// score -= 0.5 * adjacentToWater;

		return score;
	}

	public static double compactnessScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		Set<Cell> itself = action.getAbsoluteBuildingCells();
		Set<Cell> all=ToolBox.combineSets(itself,action.getRoadCells(),action.getParkCells(),action.getWaterCells());

		double score = 0.0;
		int packedToCluster = 0;
		// Calculate how much it's packed to its cluster
		if (action.getBuilding().type == Building.Type.RESIDENCE) {
			// packedToCluster = ToolBox.setInterception(player.residenceStart,
			// itself).size();

			for (Cell c : all) {
				if (player.residenceStart.contains(c)) {
					// check how many neighbors are occupied
					Cell[] nei = c.neighbors();
					for (int i = 0; i < nei.length; i++) {
						if (!land.unoccupied(nei[i]))
							packedToCluster++;
					}
				}
			}
		} else if (action.getBuilding().type == Building.Type.FACTORY) {
			// packedToCluster = ToolBox.setInterception(player.factoryStart,
			// itself).size();

			for (Cell c : all) {
				if (player.factoryStart.contains(c)) {
					// check how many neighbors are occupied
					Cell[] nei = c.neighbors();
					for (int i = 0; i < nei.length; i++) {
						if (!land.unoccupied(nei[i]))
							packedToCluster++;
					}
				}
			}

		} else {
			System.out.println("The type of building is not recognized!");
			return -100.0;
		}
		score = packedToClusterBonus * packedToCluster;

		return score;
	}

	public static boolean validateMove(Action action, Player player, Land land) {
		if (action == null || action.getBuilding() == null)
			return false;
		/* Validate move is buildable */
		Building[] rotations = action.getBuilding().rotations();
		Building building = rotations[action.getRotation()];
		Cell start = action.getStartPoint();
		boolean canDo = land.buildable(building, start);
		if (!canDo) {
			System.out.println("The action cannot be performed?!");
			return false;
		}
		for(Cell c:action.getRoadCells()){
			if(!land.unoccupied(c)){
				System.out.println("Error: Road cell "+c+" is already occupied!");
				return false;
			}
		}
		for(Cell c:action.getParkCells()){
			if(!land.unoccupied(c)){
				System.out.println("Error: Park cell "+c+" is already occupied!");
				return false;
			}
		}
		for(Cell c:action.getWaterCells()){
			if(!land.unoccupied(c)){
				System.out.println("Error: Water cell "+c+" is already occupied!");
				return false;
			}
		}

		/* Validate if there is any overlap of cells */
		boolean noOverlap = validateActionOverlap(action, land);
		if (!noOverlap)
			return false;

		/* Validate roads */
		boolean roadValid = validateRoads(action, land);
		if (!roadValid)
			return false;

		/* Validate the building is adjacent to roads */
		boolean nextToRoads = validateNextToRoads(action, player);
		if (!nextToRoads)
			return false;

		/* The action is valid if all validations have passed */
		return true;
	}

	public static boolean validateNextToRoads(Action action, Player player) {
		Set<Cell> roadsToBuild = action.getRoadCells();
		Set<Cell> roadsBuiltNeighbors = player.roadNeighbors;
		// System.out.println(roadsBuiltNeighbors.size()+" existing road
		// neighbors.");

		Set<Cell> buildingCells = action.getAbsoluteBuildingCells();
		Set<Cell> buildingNeighbors = ToolBox.allNeighbors(buildingCells);

		// The building has cells that are neighbor of existing roads
		Set<Cell> adjacentToRoad = ToolBox.setInterception(action.getAbsoluteBuildingCells(), roadsBuiltNeighbors);
		// The building has neighbors that are in the road plan
		Set<Cell> adjacentToRoadPlan = ToolBox.setInterception(buildingNeighbors, roadsToBuild);
		if (adjacentToRoad.size() == 0 && adjacentToRoadPlan.size() == 0) {
			System.out.println("Error: The action is not adjacent to road!");
			return false;
		}
		return true;
	}

	/*
	 * Validate the building, roads, parks and water do not interfere with each
	 * other
	 */
	public static boolean validateActionOverlap(Action action, Land land) {
		Set<Cell> shifted = action.getAbsoluteBuildingCells();
		Set<Cell> total = ToolBox.combineSets(shifted, action.getRoadCells(), action.getWaterCells(),
				action.getParkCells());
		int totalSize = shifted.size() + action.getRoadCells().size() + action.getWaterCells().size()
				+ action.getParkCells().size();
		if (totalSize != total.size()) {
			System.out.println("There are overlap in the building, roads, water and parks! They in total should occupy "
					+ totalSize + " but now only occupying " + total.size());
			return false;
		}
		return true;
	}

	/* Validate that roads are connected and connected with borders */
	public static boolean validateRoads(Action action, Land land) {
		Set<Cell> existingRoads = ToolBox.copyLandRoads(land);
		if (action.getRoadCells() != null) {
			for (Cell r : action.getRoadCells()) {
				existingRoads.add(new Cell(r.i + 1, r.j + 1));
			}
		}
		Cell[] erc = existingRoads.toArray(new Cell[existingRoads.size()]);
		boolean roadValid = Cell.isConnected(erc, 50 + 2);
		if (!roadValid) {
			System.out.println("The road plan is rejected!");
			return false;
		}
		return true;
	}

	public static boolean checkBlockRoads(Action action, Player player, Land land) {
		Set<Cell> centerCells = new HashSet<>();
		// check if the center is occupied
		Cell center = new Cell(land.side / 2 - 1, land.side / 2 - 1);
		if (land.unoccupied(center))
			centerCells.add(center);
		else {
			boolean found = false;
			Cell[] n = center.neighbors();
			for (int j = 0; j < n.length; j++) {
				Cell toCheck = n[j];
				if (land.unoccupied(toCheck)) {
					found = true;
					centerCells.add(toCheck);
				}
			}
			// 4 other cells
			Cell[] localCenter = new Cell[] { new Cell(land.side / 4, land.side / 4),
					new Cell(3 * land.side / 4, land.side / 4), new Cell(land.side / 4, 3 * land.side / 4),
					new Cell(3 * land.side / 4, 3 * land.side / 4), };
			for (int j = 0; j < localCenter.length; j++) {
				Cell toCheck = localCenter[j];
				if (land.unoccupied(toCheck)) {
					found = true;
					centerCells.add(toCheck);
				}
			}
			if (found == false) {
				System.out.println("No vacant cells in the center. Forget it.");
				return false;
			}
		}

		// Find the closest road to the building
		Set<Cell> cells = action.getAbsoluteBuildingCells();

		/* Debug */
		Set<Cell> canary = new HashSet<>();
		canary.add(new Cell(24, 10));
		canary.add(new Cell(10, 24));
		for (Cell a : canary) {
			if (cells.contains(a))
				System.out.println("Alert! One important cell is to be occupied!");
		}

		Set<Cell> roadPlan = action.getRoadCells();
		Set<Cell> roadClues = new HashSet<>();
		if (roadPlan.size() == 0) {
			System.out.println("No roads in plan. Search the neighbors of this building.");
			Set<Cell> buildingNeighbors = ToolBox.allNeighbors(cells);
			for (Cell c : buildingNeighbors) {
				if (player.roadcells.contains(c)) {
					roadClues.add(c);
				}
			}
			System.out.println("Found " + roadClues.size() + " road cells around the building.");
		} else {
			Set<Cell> roadPlanNeighbors = ToolBox.allNeighbors(roadPlan);
			for (Cell c : roadPlanNeighbors) {
				if (player.roadcells.contains(c)) {
					roadClues.add(c);
				}
			}
			System.out.println("Found " + roadClues.size() + " road cells around the road plan.");
		}

		/* If the building is adjacent to border */
		if (roadClues.size() == 0) {
			System.out.println(
					"If the action is connected to the border, we need to valid future road connectivity to the center.");

			// Check connection to the center cell from the existing roads
			boolean allFailed = true;
			for (Cell c : centerCells) {
				// if road is already found, no need to keep checking others
				if (allFailed == false) {
					continue;
				}
				Building imagineBuilding = new Building(new Cell[] { new Cell(0, 0) }, Building.Type.RESIDENCE);
				Action imagineAction = new Action(imagineBuilding, c, 0);
				Set<Cell> futureRoads = RoadFinder.findRoad(player, imagineAction, land);
				if (futureRoads != null && futureRoads.size() > 0) {
					System.out.println("Center cell i:" + c.i + " j:" + c.j + " can connect to road.");
					allFailed = false;
				}
			}
			if (allFailed) {
				System.out.println("All center cells cannot connect to the road! We cannot let the action happen! ");
				return true;
			} else {
				System.out.println("Still hope for vacant roads. ");
				return false;
			}

		}
		/* Or the building is connected to roads */
		else {
			// //Find the road based on the road cells that we have found
			Set<Cell> roadCells = new HashSet<>();
			for (Cell c : roadClues) {
				Set<Cell> connection = findConnectionToBorder(c, player, land, new HashSet<Cell>());
				if (connection != null)
					roadCells.addAll(connection);
			}
			System.out.println("The road found has " + roadCells.size() + " cells");
			roadClues.addAll(action.getRoadCells());
			roadClues.addAll(roadCells);

			// Check connection to the center cell
			boolean allFailed = true;
			for (Cell c : centerCells) {
				boolean connectToCenter = RoadFinder.findClearPath(c, roadClues, player, action, land);
				if (connectToCenter) {
					System.out.println("Center cell i:" + c.i + " j:" + c.j + " can connect to road.");
					allFailed = false;
				}
			}
			if (allFailed) {
				System.out.println("All center cells cannot connect to the road! We cannot let the action happen! ");
				return true;
			} else {
				System.out.println("Still hope for vacant roads. ");
				return false;
			}
		}

	}

	public static Set<Cell> findConnectionToBorder(Cell c, Player player, Land land, Set<Cell> checked) {
		Set<Cell> path = new HashSet<>();
		if (c.i == 0 || c.i == land.side - 1) {
			// System.out.println("Found the horizontal border at " + c);
			path.add(c);
			// System.out.println("Road found so far " + path);
			return path;
		} else if (c.j == 0 || c.j == land.side - 1) {
			// System.out.println("Found the vertical border at " + c);
			path.add(c);
			// System.out.println("Road found so far " + path);
			return path;
		} else {
			// System.out.println(c + " is not a border cell.");
			Cell[] cells = c.neighbors();
			boolean roadAround = false;
			for (int i = 0; i < cells.length; i++) {
				Cell n = cells[i];
				if (checked.contains(n)) {
					// System.out.println("Cell " + n + " has been checked.");
					// there is another possibility that n is checked and n is
					// road
					if (player.roadcells.contains(n)) {
						// System.out.println(n + " is a checked road cell.
						// Adding it whatsoever.");
						path.add(n);
						roadAround = true;
					}
					continue;
				}
				// System.out.println("Cell " + n + " is not checked. Checking
				// now.");
				checked.add(n);
				if (player.roadcells.contains(n)) {
					// System.out.println(n + " is a road cell");
					roadAround = true;
					path = findConnectionToBorder(n, player, land, checked);
					if (path == null) {
						System.out.println("No path found!");
					}
					path.add(n);
					path.add(c);
				}
			}
			if (roadAround == false) {
				System.out.println("Broken road here i:" + c.i + " j:" + c.j);
				return null;
			} else {
				// System.out.println("Road found so far " + path);
				return path;
			}
		}
	}

	public static boolean evaluateWhiteSpace(Action action, Player player, Land land) {
		Set<Cell> vacantSur = ToolBox.findSurroundingVacantNeighbors(action.getAbsoluteBuildingCells(), land);
		if (vacantSur.size() == 0) {
			return true;
		}
		/* Check whitespace is connected */
		boolean whitespaceConn = Cell.isConnected(vacantSur);
		if (!whitespaceConn) {
			System.out.println("The whitespace is not connected anymore!");
			return false;
		}

		/* Check one cell of whitespace is connected to closest road */
		Iterator<Cell> it = vacantSur.iterator();
		Cell first = it.next();
		Building blankBuilding = new Building(new Cell[] { new Cell(0, 0) }, Building.Type.RESIDENCE);
		Action whitespaceAction = new Action(blankBuilding, first, 0);
		Set<Cell> whitespaceConnectedToRoad = RoadFinder.findRoad(player, whitespaceAction, land);
		if (whitespaceConnectedToRoad != null) {
			System.out.println("Whitespace cell is not connected to road.");
			return true;
		}
		return false;
	}

}
