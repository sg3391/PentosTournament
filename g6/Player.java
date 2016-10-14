package pentos.g6;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import pentos.g10.RoadFinder;
import pentos.sim.Building;
import pentos.sim.Building.Type;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;

public class Player implements pentos.sim.Player {

	private static int rejectNum = 0;
	
	private static int FACTORY_THRESHOLD = 22;
	private static int RESIDENCE_THRESHOLD = 28;

	private enum ResidenceType {
		LINE, L_FACE, R_FACE, INV_L, L, R_BLK, L_BLK, INV_LIGHTNING, LIGHTNING, T, U, RANGLE, STEPS, PLUS, L_TOTEM, R_TOTEM, INV_Z, Z
	};

	@Override
	public void init() {
		// Grid.initializeFactoryRows();
		// Grid.initializeResidenceRows();
	}

	@Override
	public Move play(Building request, Land land) {

		if (rejectNum == 2) {
			Move move;
			if (request.getType() == Type.FACTORY) {
				move = generateFactoryMove(request, land);
			} else {
				move = generateResidenceMove(request, land);
			}
			if (move.accept == true) {
				return move;
			}
			
			
			//System.out.println("Request forwarded to dummy player");
			DummyPlayer dummyplayer = new DummyPlayer();
			dummyplayer.initializeRoadCells(land);
			return dummyplayer.leastRoadMove(request, land);
		}
		if (request.getType() == Type.FACTORY) {
			Move move = generateFactoryMove(request, land);
			if (move.accept == false)
				rejectNum++;
			return move;

		} else {
			// Received request for residence
			Move move = generateResidenceMove(request, land);
			if (move.accept == false)
				rejectNum++;
			return move;
		}
	}

	private Move generateFactoryMove(Building request, Land land) {
		if (request.getType() != Type.FACTORY) {
			throw new RuntimeException("Calling factory play on non-factory");
		}

		int[] factoryDimensions = getBuildingDimensions(request);

		//System.out.println("Factory Request: " + factoryDimensions[0] + " " + factoryDimensions[1]);

		Row bestRow = null;
		int minLength = -1;
		boolean rotate = false;

		int promotionBump = -1;
		while (bestRow == null && promotionBump <= 3) {
			promotionBump++;
			
			// This is checking for the best row if you use the first dimension of the factory
			if (factoryDimensions[0] + promotionBump <= 5
					&& Grid.getFactoryRows().containsKey(factoryDimensions[0] + promotionBump) ) {
				
				for (Row row : Grid.getFactoryRows().get(factoryDimensions[0] + promotionBump)) {
					if (!factoryRowExtendable(row, land, request.rotations()[0], promotionBump)) {	
						continue;
					} else {
						if (bestRow == null) {
							bestRow = row;
							minLength = bestRow.getCurrentLocation() + factoryDimensions[1];
							rotate = false;
						} else {
							if (row.getCurrentLocation() + factoryDimensions[1] < minLength) {
								bestRow = row;
								minLength = bestRow.getCurrentLocation() + factoryDimensions[1];
								rotate = false;
							}
						}
					}
				}
				
			}
			
			// This is checking for the best row if you use the second dimension
			// of the factory
			if (factoryDimensions[0] != factoryDimensions[1]) {
				// This makes sure that Dim2 isn't the same as Dim1
				if (factoryDimensions[1] + promotionBump <= 5
						&& Grid.getFactoryRows().containsKey(factoryDimensions[1] + promotionBump) ) {
					
					for (Row row : Grid.getFactoryRows().get(factoryDimensions[1] + promotionBump)) {
						if (!factoryRowExtendable(row, land, request.rotations()[1], promotionBump)) {
							continue;
						} else {
							if (bestRow == null) {
								bestRow = row;
								minLength = bestRow.getCurrentLocation() + factoryDimensions[0];
								rotate = true;
							} else {
								if (row.getCurrentLocation() + factoryDimensions[0] < minLength) {
									bestRow = row;
									minLength = bestRow.getCurrentLocation() + factoryDimensions[0];
									rotate = true;
								}
							}
						}
					}
				}
			}
			
			///////////////////////////////////////////////////////////////////////////////////////////
			if(bestRow==null && promotionBump==0){
			//if (promotionBump==0&&Grid.getFactoryRows().get(factoryDimensions[1] + promotionBump) == null) {
				int max = Math.max(factoryDimensions[0], factoryDimensions[1]);
				int min = Math.min(factoryDimensions[0], factoryDimensions[1]);
				
				if (Grid.generatable(max, 1)) {
					Grid.generateFactoryRow(max);
					promotionBump=-1; //This is to make sure the loop does it's first initial run again.
				}
				else if(Grid.generatable(min, 1)){
					Grid.generateFactoryRow(min);
					promotionBump=-1; //This is to make sure the loop does it's initial run again.
				}
			}
			///////////////////////////////////////////////////////////////////////////////////////////
		}

		// Suppose no best row was found, you should reject the request
		if (bestRow == null) {
			//System.out.println("Rejecting because no bestRow was found.");
			return new Move(false);
		} else {
			// If threshold is exceeded, build a new row of the same size
			if (bestRow.getCurrentLocation() > FACTORY_THRESHOLD) {
				if (Grid.generatable(bestRow.size(), 1)) {
					//System.out.println("Building new factory row of size " + bestRow.size());
					Grid.generateFactoryRow(bestRow.size());
				}
			}
		}

		boolean accept = true;
		int yLoc = (bestRow.getRoadLocation() > bestRow.getStart()) ? bestRow.getStart() + promotionBump
				: bestRow.getStart();
		// int yLoc = bestRow.getStart();
		//System.out.println("Building factory at " + bestRow.getCurrentLocation() + ", " + yLoc);
		if (!land.buildable(request, new Cell(yLoc, bestRow.getCurrentLocation()))) {
			//System.out.println("Cannot build this factory.");
		}
		Cell location = new Cell(yLoc, bestRow.getCurrentLocation());
		int rotation = (rotate) ? 1 : 0;

		Set<Cell> road = new HashSet<Cell>();
		if (bestRow.getStart() == 0) {
			// This means bestRow is on the top edge and needs no roads
			// Do nothing
		} else if (bestRow.getEnd() == 50) {
			// This means bestRow is on the bottom edge and needs no roads
			// Do nothing
		} else {
			int extension = (rotate) ? factoryDimensions[0] : factoryDimensions[1];
			for (int i = 0; i < extension; i++) {
				if (!land.unoccupied(bestRow.getRoadLocation(), bestRow.getCurrentLocation() + i)) {
					
				} else {
					road.add(new Cell(bestRow.getRoadLocation(), bestRow.getCurrentLocation() + i));
				}
			}
		}

		Set<Cell> water = new HashSet<Cell>(); // This stays empty. No water
		Set<Cell> park = new HashSet<Cell>(); // This stays empty. No parks

		// Update currentLocation!
		bestRow.setCurrentLocation((rotate) ? bestRow.getCurrentLocation() + factoryDimensions[0]
				: bestRow.getCurrentLocation() + factoryDimensions[1]);
		/*if(land.buildable(request, location)) return new Move(accept, request, location, rotation, road, water, park);
		else return new Move(false);*/
		return new Move(accept, request, location, rotation, road, water, park);
	}

	private Move generateResidenceMove(Building request, Land land) {
		/*
		 * Use rotation that has least number of cells in the leftmost row for
		 * now If one of the dimensions is size 4 or more, prefer that as the
		 * height
		 */
		boolean is4 = false;
		boolean is5 = false;
		LinkedList<Integer> validRotations = new LinkedList<>();
		Building[] rotations = request.rotations();
		for (int i = 0; i < rotations.length; ++i) {
			int[] residenceDimensions = getBuildingDimensions(rotations[i]);
			if (residenceDimensions[0] == 5) {
				// If size 5, use this rotation
				is5 = true;
				validRotations = new LinkedList<>();
				validRotations.add(i);
				break;
			} else if (residenceDimensions[0] == 4) {
				// Size 4
				if (!is4) {
					is4 = true;
					validRotations = new LinkedList<>();
				}
				validRotations.add(i);
			} else if (residenceDimensions[0] == 3) {
				// Size 3 or less
				if (!is4) {
					validRotations.add(i);
				}
			}
		}
		
		// Pointer to set of possible rows
		Set<Row> possibleRows;
		if (is5) {
			possibleRows = Grid.getResidenceRows().get(5);
		} else if (is4) {
			possibleRows = Grid.getResidenceRows().get(4);
		} else {
			possibleRows = Grid.getResidenceRows().get(3);
		}
		
		int rowSize = 3;
		if (is5)
			rowSize = 5;
		else if (is4)
			rowSize = 4;

		// Row doesn't exist for this size, generate new set
		if (possibleRows == null) {
			if (Grid.generatable(rowSize, 2)) {
				Grid.generateResidenceRow(rowSize);
				possibleRows = Grid.getResidenceRows().get(rowSize);
			}
		}

		/*
		 * Now we have the set of rotations we can use, 
		 * and the set of rows we can put it in
		 */

		int bestOffSet = 0;
		Row bestRow = null;
		int bestLocation = Integer.MIN_VALUE;
		int bestRotation = -1;
		int leastLeftCells = Integer.MAX_VALUE;
		for (int rotation : validRotations) {
			Building rotatedRequest = request.rotations()[rotation];
			for (Row row : possibleRows) {
				int offSet = 0;
				int positionInRow = residenceRowExtendPosition(land, row, rotatedRequest, offSet);
				if (positionInRow >= 0) {
					if (positionInRow > bestLocation) {
						bestLocation = positionInRow;
						bestRow = row;
						bestOffSet = offSet;
						bestRotation = rotation;
						leastLeftCells = countCellsOnLeft(rotatedRequest);
					} else if (positionInRow == bestLocation) {
						int leftCells = countCellsOnLeft(rotatedRequest);
						if (leftCells < leastLeftCells) {
							bestLocation = positionInRow;
							bestRow = row;
							bestOffSet = offSet;
							bestRotation = rotation;
							leastLeftCells = leftCells;
						}
					}
				}
			}
		}
		
		// Row exits, but it is unbuildable, generate another row if possible
		if (bestRow == null) {
			if (Grid.generatable(rowSize, 2)) {
				Grid.generateResidenceRow(rowSize);
				possibleRows = Grid.getResidenceRows().get(rowSize);
			}
		}

		// try same thing again
		for (int rotation : validRotations) {
			Building rotatedRequest = request.rotations()[rotation];
			for (Row row : possibleRows) {
				int offSet = 0;
				int positionInRow = residenceRowExtendPosition(land, row, rotatedRequest, offSet);
				if (positionInRow >= 0) {
					if (positionInRow > bestLocation) {
						bestLocation = positionInRow;
						bestRow = row;
						bestOffSet = offSet;
						bestRotation = rotation;
						leastLeftCells = countCellsOnLeft(rotatedRequest);
					} else if (positionInRow == bestLocation) {
						int leftCells = countCellsOnLeft(rotatedRequest);
						if (leftCells < leastLeftCells) {
							bestLocation = positionInRow;
							bestRow = row;
							bestOffSet = offSet;
							bestRotation = rotation;
							leastLeftCells = leftCells;
						}
					}
				}
			}
		}
		
		// This is changed to true if there is no promotion
		boolean padWithWater = false;
		
		// If you still didn't find anything and you were length 3, promote to 4
		if (bestRow == null && !is4 && !is5) {
			// Promoting to 4
			possibleRows = Grid.getResidenceRows().get(4);
			if (possibleRows != null) {
				for (int rotation : validRotations) {
					Building rotatedRequest = request.rotations()[rotation];
					for (Row row : possibleRows) {
						int offSet = (row.getRoadLocation() > row.getStart()) ? 1 : 0;
						int positionInRow = residenceRowExtendPosition(land, row, rotatedRequest, offSet);
						if (positionInRow >= 0) {
							if (positionInRow > bestLocation) {
								bestLocation = positionInRow;
								bestRow = row;
								bestOffSet = offSet;
								bestRotation = rotation;
								leastLeftCells = countCellsOnLeft(rotatedRequest);
							} else if (positionInRow == bestLocation) {
								int leftCells = countCellsOnLeft(rotatedRequest);
								if (leftCells < leastLeftCells) {
									bestLocation = positionInRow;
									bestRow = row;
									bestOffSet = offSet;
									bestRotation = rotation;
									leastLeftCells = leftCells;
								}
							}
						}
					}
				}
			}

			// Still nothing, promote to 5
			if (bestRow == null) {
				// Promoting to 5
				possibleRows = Grid.getResidenceRows().get(5);
				if (possibleRows != null) {
					for (int rotation : validRotations) {
						Building rotatedRequest = request.rotations()[rotation];
						for (Row row : possibleRows) {
							int offSet = (row.getRoadLocation() > row.getStart()) ? 2 : 0;
							int positionInRow = residenceRowExtendPosition(land, row, rotatedRequest, offSet);
							if (positionInRow >= 0) {
								if (positionInRow > bestLocation) {
									bestLocation = positionInRow;
									bestRow = row;
									bestOffSet = offSet;
									bestRotation = rotation;
									leastLeftCells = countCellsOnLeft(rotatedRequest);
								} else if (positionInRow == bestLocation) {
									int leftCells = countCellsOnLeft(rotatedRequest);
									if (leftCells < leastLeftCells) {
										bestLocation = positionInRow;
										bestRow = row;
										bestOffSet = offSet;
										bestRotation = rotation;
										leastLeftCells = leftCells;
									}
								}
							}
						}
					}
				}
			}
		} else if (bestRow == null && is4) {
			// If you still didn't find anything and you were length 4
			possibleRows = Grid.getResidenceRows().get(5);
			if (possibleRows != null) {
				for (int rotation : validRotations) {
					Building rotatedRequest = request.rotations()[rotation];
					for (Row row : possibleRows) {
						int offSet = (row.getRoadLocation() > row.getStart()) ? 1 : 0;
						int positionInRow = residenceRowExtendPosition(land, row, rotatedRequest, offSet);
						if (positionInRow >= 0) {
							if (positionInRow > bestLocation) {
								bestLocation = positionInRow;
								bestRow = row;
								bestOffSet = offSet;
								bestRotation = rotation;
								leastLeftCells = countCellsOnLeft(rotatedRequest);
							} else if (positionInRow == bestLocation) {
								int leftCells = countCellsOnLeft(rotatedRequest);
								if (leftCells < leastLeftCells) {
									bestLocation = positionInRow;
									bestRow = row;
									bestOffSet = offSet;
									bestRotation = rotation;
									leastLeftCells = leftCells;
								}
							}
						}
					}
				}
			}
		} else {
			// No promotion, so should pad
			padWithWater = true;
		}
		
		// Checking which rotation was selected
		if (bestRotation == -1) {
			bestRotation = validRotations.get(0);
		}

		// If it is still null, it means we didn't find the row to 
		// place it (even after all the effort). Otherwise, check
		// if we want to build a new row at this point
		if (bestRow == null) {
			return new Move(false);
		} else {
			// If threshold is exceeded, build a new row of the same size
			if (bestRow.getCurrentLocation() < RESIDENCE_THRESHOLD) {
				if (Grid.generatable(bestRow.size(), 2)) {
					//System.out.println("Building new residence row of size " + bestRow.size());
					Grid.generateResidenceRow(bestRow.size());
				}
			}
		}

		// Everything is decided, now generate the complete move
		Padding padding = new MyPadding();
		if (bestRow.getRecentlyPadded() || !padWithWater) {
			padWithWater = false;
			bestRow.setWasNotRecentlyPadded();
		} else {
			padWithWater = true;
			bestRow.setWasRecentlyPadded();
		}
		Move move = padding.getPadding(request, bestRotation, land, bestRow, bestLocation, padWithWater, bestOffSet);
		
		if (!land.buildable(move.request.rotations()[move.rotation], move.location)) {
			//System.out.println("***Cannot build***" + move.location.i + "," + move.location.j);
		}
		//System.out.println("Building residence at " + move.location.i + "," + move.location.j);
		// System.out.println("***Can build***");
		return move;
	}

	public int[] getBuildingDimensions(Building factory) {
		int rowMin = Integer.MAX_VALUE, rowMax = Integer.MIN_VALUE, colMin = Integer.MAX_VALUE,
				colMax = Integer.MIN_VALUE;
		Iterator<Cell> iter = factory.iterator();

		while (iter.hasNext()) {
			Cell temp = iter.next();
			rowMin = (temp.i < rowMin) ? temp.i : rowMin;
			rowMax = (temp.i > rowMax) ? temp.i : rowMax;
			colMin = (temp.j < colMin) ? temp.j : colMin;
			colMax = (temp.j > colMax) ? temp.j : colMax;
		}

		int height = rowMax - rowMin + 1;
		int width = colMax - colMin + 1;

		return new int[] { height, width };
	}

	public boolean factoryRowExtendable(Row row, Land land, Building factory, int promotion) {
		if (factory.getType() != Type.FACTORY) {
			throw new RuntimeException("Incorrect building type inputted.");
		}

		int topCell = row.getStart();
		if (row.getRoadLocation() > row.getStart()) {
			topCell += promotion;
		}
		int leftCell = row.getCurrentLocation();

		if (row.getRoadLocation() != -1 
				&& row.getRoadLocation() != 50
				&& row.getCurrentLocation() < 50
				&& !land.unoccupied(row.getRoadLocation(), row.getCurrentLocation())
				&& land.getCellType(row.getRoadLocation(), row.getCurrentLocation()) != Cell.Type.ROAD) {
			// If road position is occupied and isn't a road already, can't extend
			return false;
		}
		
		return land.buildable(factory, new Cell(topCell, leftCell));

	}

	public Move fillCell(Iterator cells, int fillType) {
		Set<Cell> paddedCells = new HashSet<>();

		return null;
	}

	private static int residenceRowExtendPosition(Land land, Row row, Building residence, int offSet) {
		if (residence.getType() != Type.RESIDENCE) {
			throw new RuntimeException("Incorrect building type inputted.");
		}

		int position = row.getCurrentLocation(), roadRow = row.getRoadLocation();
		while (position >= 0) {
			if (land.buildable(residence, new Cell(row.getStart() + offSet, position))) {
				if (roadRow >= 0 && roadRow <= 49) {
					// Checking if roads haven't been blocked
					int to = row.getCurrentLocation();
					if (to + 1 < land.side) {
						to += 1;
					}
					
					Iterator<Cell> it = residence.iterator();
					int from;
					if (roadRow > row.getStart()) {
						// Road is at the bottom
						int maxRow = 0;
						int maxCol = 0;
						while (it.hasNext()) {
							Cell c = it.next();
							if (c.i > maxRow) {
								maxRow = c.i;
								maxCol = 0;
							} else if (c.i == maxRow && c.j > maxCol) {
								maxCol = c.j;
							}
						}
						from = position + maxCol;
					} else {
						// Road is on top
						int maxCol = 0;
						while (it.hasNext()) {
							Cell c = it.next();
							if (c.i == 0 && c.j > maxCol) {
								maxCol = c.j;
							}
						}
						from = position + maxCol;
					}
					
					for (int j = from; j <= to; ++j) {
						// If the road is blocked to this place, cannot build in the row
						if ((!land.unoccupied(roadRow, j)) && land.getCellType(roadRow, j) != Cell.Type.ROAD) {
							return -1;
						}
					}
				}
				
				return position;
			}
			position--;
		}
		
		return -1;
	}

	private static int countCellsOnLeft(Building building) {
		int minCol = Integer.MAX_VALUE;
		int numCellsAtMin = 0;
		Iterator<Cell> iterator = building.iterator();
		while (iterator.hasNext()) {
			Cell cell = iterator.next();
			if (cell.j < minCol) {
				minCol = cell.j;
				numCellsAtMin = 1;
			} else if (cell.j == minCol) {
				numCellsAtMin++;
			}
		}
		return numCellsAtMin;
	}
}
