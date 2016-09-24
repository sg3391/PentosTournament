package pentos.g6;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Building.Type;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;

public class Player implements pentos.sim.Player {

	private static int[] numFactoryRowsPerSize = { 5, 4, 3, 2 };
	private static int factoryRowSizeShift = 2;
	private static int[] numResidenceRowsPerSize = { 14, 4 };
	private static int residenceRowSizeShift = 2;
	
	private HashMap<Integer, Set<Row>> factoryRows = new HashMap<Integer, Set<Row>>();
	private HashMap<Integer, Set<Row>> residenceRows = new HashMap<Integer, Set<Row>>();
	
	private enum ResidenceType {LINE, L_FACE, R_FACE, INV_L, L, R_BLK, L_BLK, INV_LIGHTNING,
								LIGHTNING, T, U, RANGLE, STEPS, PLUS, L_TOTEM, R_TOTEM, INV_Z, Z};
	
	@Override
	public void init() {
		int currentRow = 0;
		int rowNumber= 0;
		for (int i = 0; i < numFactoryRowsPerSize.length; ++i) {
			for (int j = 0; j < numFactoryRowsPerSize[i]; j++) {
				int roadLocation;
				if(rowNumber % 2 == 0) {
					roadLocation = currentRow - 1;
				} else {
					roadLocation = currentRow + i + factoryRowSizeShift;
				}
				Row row = new Row(currentRow, currentRow + i + factoryRowSizeShift, roadLocation);
				if (!factoryRows.containsKey(i + factoryRowSizeShift)) {
					factoryRows.put(i + factoryRowSizeShift, new HashSet<Row>());
				}
				factoryRows.get(i + factoryRowSizeShift).add(row);

				if (rowNumber % 2 == 0) {
					currentRow = currentRow + i + factoryRowSizeShift;
				} else {
					currentRow = currentRow + i + factoryRowSizeShift + 1;
				}
				rowNumber++;
			}
		}
		currentRow = 0 ;
		rowNumber = 0;
		for (int i = 0; i < numResidenceRowsPerSize.length; ++i) {
			for (int j = 0; j < numResidenceRowsPerSize[i]; j++) {
				int roadLocation;
				if(rowNumber % 2 == 0) {
					roadLocation = currentRow - 1;
				} else {
					roadLocation = currentRow + i + residenceRowSizeShift;
				}
				Row row = new Row(currentRow, currentRow + i + residenceRowSizeShift, roadLocation, 49);
				if (!residenceRows.containsKey(i + residenceRowSizeShift)) {
					residenceRows.put(i + residenceRowSizeShift, new HashSet<Row>());
				}
				residenceRows.get(i + residenceRowSizeShift).add(row);
				
				if (rowNumber % 2 == 0) {
					currentRow = currentRow + i + residenceRowSizeShift;
				} else {
					currentRow = currentRow + i + residenceRowSizeShift + 1;
				}
				rowNumber++;
			}
		}
	}

	@Override
	public Move play(Building request, Land land) {

		if(request.getType() == Type.FACTORY){
			int[] factoryDimensions = getBuildingDimensions(request);
			
			System.out.println("Request: " + factoryDimensions[0] + "\t" + factoryDimensions[1]);
			
			Row bestRow = null;
			int minLength = -1;
			boolean rotate = false;
			
			//This is checking for the best row if you use the first dimension of the factory
			for(Row row : factoryRows.get(factoryDimensions[0])){
				if(!factoryRowExtendable(row, land, request.rotations()[0])){
					continue;
				}
				else{
					if(bestRow == null){
						bestRow = row;
						minLength = bestRow.getCurrentLocation() + factoryDimensions[1];
						rotate = false;
					}
					else{
						if(row.getCurrentLocation() + factoryDimensions[1] < minLength){
							bestRow = row;
							minLength = bestRow.getCurrentLocation() + factoryDimensions[1];
							rotate = false;
						}
					}
				}
			}
			//This is checking for the best row if you use the second dimension of the factory
			if(factoryDimensions[0]!=factoryDimensions[1]){ //This makes sure that Dim2 isn't the same as Dim1
				for(Row row : factoryRows.get(factoryDimensions[1])){
					if(!factoryRowExtendable(row, land, request.rotations()[1])){
						continue;
					}
					else{
						if(bestRow == null){
							bestRow = row;
							minLength = bestRow.getCurrentLocation() + factoryDimensions[0];
							rotate = true;
						}
						else{
							if(row.getCurrentLocation() + factoryDimensions[0] < minLength){
								bestRow = row;
								minLength = bestRow.getCurrentLocation() + factoryDimensions[0];
								rotate = true;
							}
						}
					}
				}
			}

			//Suppose no best row was found, you should reject the request
			if(bestRow==null){
				System.out.println("Rejecting because no bestRow was found.");
				return new Move(false);
			}
			
			
			boolean accept = true;
			Cell location = new Cell(bestRow.getStart(), bestRow.getCurrentLocation());
			int rotation = (rotate) ? 1 : 0;
			
			Set<Cell> road = new HashSet<Cell>();
			if(bestRow.getStart() == 0){
				//This means bestRow is on the top edge and needs no roads
				//Do nothing
			}
			else if(bestRow.getEnd() == 50){
				//This means bestRow is on the bottom edge and needs no roads
				//Do nothing
			}
			else{
				int extension = (rotate) ? factoryDimensions[0] : factoryDimensions[1];
				for(int i=0; i<extension; i++){
					if(!land.unoccupied(bestRow.getRoadLocation(), bestRow.getCurrentLocation() + i)){
						//This means it should already be road, so don't do anything. There might be a logic issue here.
						//return new Move(false);
						continue;
					}
					else{
						road.add(new Cell(bestRow.getRoadLocation(), bestRow.getCurrentLocation() + i));
					}
				}
			}
			
		
			Set<Cell> water = new HashSet<Cell>(); //This stays empty. No water
			Set<Cell> park = new HashSet<Cell>(); //This stays empty. No parks
			
			//Update currentLocation! 
			bestRow.setCurrentLocation((rotate) ? bestRow.getCurrentLocation() + factoryDimensions[0] : bestRow.getCurrentLocation() + factoryDimensions[1]);
			return new Move(accept, request, location, rotation, road, water, park);
			
		}
		else{
			// Received request for residence
			
			// TODO: (future) Figure out building shape
			
			/* Use rotation that has least number of cells in the leftmost row
			 * If one of the dimensions is size 2, prefer that as the height
			 */
			int rotation = 0;
			int minCellsOnLeft = Integer.MAX_VALUE;
			boolean is2 = false;
			Building[] rotations = request.rotations();
			for (int i = 0; i < rotations.length; ++i) {
				int[] residenceDimensions = getBuildingDimensions(rotations[i]);
				if(residenceDimensions[0] <= 2) {
					// Size 2
					int leftCells = countCellsOnLeft(rotations[i]);
					if (is2) {
						if (leftCells < minCellsOnLeft) {
							minCellsOnLeft = leftCells;
							rotation = i;
						}
					} else {
						is2 = true;
						rotation = i;
						minCellsOnLeft = leftCells;
					}
				} else {
					// Size 3 or more
					if (is2) {
						// Forget this rotation
					} else {
						int leftCells = countCellsOnLeft(rotations[i]);
						if (leftCells < minCellsOnLeft) {
							minCellsOnLeft = leftCells;
							rotation = i;
						}
					}
				}
			}
			Set<Row> possibleRows;
			if (is2) {
				possibleRows = residenceRows.get(2);
			} else {
				possibleRows = residenceRows.get(3);
			}
			
			/*
			 *  Now we have the rotation we want, and the set of rows we can put it in
			 */
			
			Building rotatedRequest = request.rotations()[rotation];
			
			Row bestRow = null;
			int bestLocation = Integer.MIN_VALUE;
			for (Row row : possibleRows) {
				if (residenceRowExtendable(land, row, rotatedRequest) && row.getCurrentLocation() > bestLocation) {
					bestLocation = row.getCurrentLocation();
					bestRow = row;
				}
			}
			
			// If it is still null, it means we didn't find the row to place it
			if (bestRow == null) {
				return new Move(false);
			}
			
			// TODO: (Future) align building so that padding is on opposite side of road
			
			// All decided, now generate the complete
			Move move = padding(request, rotation, land, bestRow);
			if(!land.buildable(move.request.rotations()[move.rotation], move.location)) {
				System.out.println("Canot build");
			}
			return move;
		}
	}

	public int[] getBuildingDimensions(Building factory){
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
		
		return new int[]{height, width};
	}
	
	public boolean factoryRowExtendable(Row row, Land land, Building factory){
		if(factory.getType() != Type.FACTORY){
			throw new RuntimeException("Incorrect building type inputted.");
		}
		
		int topCell = row.getStart();
		int leftCell = row.getCurrentLocation();
		
		return land.buildable(factory, new Cell(topCell,leftCell));

	}

	public Move padding(Building request, int rotation, Land land, Row currentRow) {
		int rowMin = Integer.MAX_VALUE, rowMax = Integer.MIN_VALUE;
		int colMin = Integer.MAX_VALUE, colMax = Integer.MIN_VALUE;
		Iterator<Cell> iter = request.rotations()[rotation].iterator();
		int paddType = 1;
		// Map<Integer,Map<Integer,Cell>> cellMap = new HashMap<>();
		boolean[][] hasBuildingCell = new boolean[50][50];
		for (boolean[] array : hasBuildingCell) {
			Arrays.fill(array, false);
		}
		while (iter.hasNext()) {
			Cell temp = iter.next();
			//hasBuildingCell[temp.i + currentRow.getStart() - 1][currentRow.getCurrentLocation() - temp.j + 1] = true;
			rowMin = (temp.i < rowMin) ? temp.i : rowMin;
			rowMax = (temp.i > rowMax) ? temp.i : rowMax;
			colMin = (temp.j < colMin) ? temp.j : colMin;
			colMax = (temp.j > colMax) ? temp.j : colMax;
		}
		iter = request.rotations()[rotation].iterator();
		while (iter.hasNext()) {
			Cell temp = iter.next();
			hasBuildingCell[temp.i + currentRow.getStart()][currentRow.getCurrentLocation() - colMax + temp.j] = true;
		}
		for (int i = currentRow.getStart(); i < currentRow.getEnd()
				&& currentRow.getCurrentLocation() != land.side - 1; i++) {
			if (land.isField(i, currentRow.getCurrentLocation() + 1))
				paddType = 1;// build field
			else if (land.isPond(i, currentRow.getCurrentLocation() + 1))
				paddType = 2;// build pond
		}

		// Add water and park by filling blank spaces
		Set<Cell> water = new HashSet<>();
		Set<Cell> park = new HashSet<>();
		Set<Cell> road = new HashSet<>();
		for (int i = currentRow.getStart(); i < currentRow.getEnd(); i++) {
			for (int j = currentRow.getCurrentLocation(); j > currentRow.getCurrentLocation() - colMax + colMin	-1; j--) {
				if (!hasBuildingCell[i][j]) {
					if (paddType == 1)
						water.add(new Cell(i, j));
					else
						park.add(new Cell(i, j));
				}
			}
		}
		
		/*// Adding extra water cells
		if(paddType == 1 && water.size() < 4){
			for(int i = currentRow.getStart();i<currentRow.getEnd();i++){
				if(currentRow.getCurrentLocation() - colMax + colMin + 1 < 50 &&
						currentRow.getCurrentLocation() - colMax + colMin + 1 > 0){
					water.add(new Cell(i,currentRow.getCurrentLocation() - colMax + colMin+ 1));
				}		
			}
				
		}
		// Adding extra park cells
		else if(paddType==2&&park.size()<4){
			for(int i = currentRow.getStart();i<currentRow.getEnd();i++)
				if(currentRow.getCurrentLocation() - colMax + colMin + 1 < 50 &&
						currentRow.getCurrentLocation() - colMax + colMin + 1 > 0){
					park.add(new Cell(i,currentRow.getCurrentLocation() - colMax + colMin+ 1));
				}	
		}*/
		
		// Add road when necessary
		if(currentRow.getRoadLocation()>0 && currentRow.getRoadLocation()<50) {
			for(int i = currentRow.getCurrentLocation();i>currentRow.getCurrentLocation() - colMax + colMin	- 1; 
					i--) {
				if (land.unoccupied(currentRow.getRoadLocation(),i)) {
					road.add(new Cell(currentRow.getRoadLocation(),i));
				}
			}

		}
		
		int width = colMax - colMin + 1;
		currentRow.setCurrentLocation(currentRow.getCurrentLocation() - width);
		
		if (width == 5 && currentRow.getStart() < currentRow.getRoadLocation()) {
			Set<Cell> newWater = new HashSet<>();
			for (Cell cell : water) {
				newWater.add(new Cell(cell.i-1, cell.j));
			}
			
			Set<Cell> newPark = new HashSet<>();
			for (Cell cell : park) {
				newPark.add(new Cell(cell.i-1, cell.j));
			}
			
			return new Move(true,request,new Cell(currentRow.getStart() + 1,
					currentRow.getCurrentLocation() + 1),
					rotation, road, newWater, newPark);
		}
		
		return new Move(true,request,new Cell(currentRow.getStart(),currentRow.getCurrentLocation() + 1),rotation,road,water,park);
	}

	public Move fillCell(Iterator cells, int fillType) {
		Set<Cell> paddedCells = new HashSet<>();

		return null;
	}

	private static boolean residenceRowExtendable(Land land, Row row, Building residence) {
		if (residence.getType() != Type.RESIDENCE) {
			throw new RuntimeException("Incorrect building type inputted.");
		}
		
		int maxWidth = Integer.MIN_VALUE;
		for (Cell cell : residence) {
			if (cell.j > maxWidth) {
				maxWidth = cell.j;
			}
		}
		
		// If row has reached the end of the board, can't build here
		if (row.getCurrentLocation() - maxWidth < 0) {
			
		}
		
		// Check if you can build to the left starting from currentLocation
		if (land.buildable(residence, new Cell(row.getStart(), row.getCurrentLocation() - maxWidth))) {
			return true;
		} else {
			return false;
		}
	}
	
	private static int countCellsOnLeft(Building building) {
		int minRow = Integer.MAX_VALUE;
		int numCellsAtMin = 0;
		Iterator<Cell> iterator = building.iterator();
		while(iterator.hasNext()) {
			Cell cell = iterator.next();
			if (cell.i < minRow) {
				minRow = cell.i;
				numCellsAtMin = 1;
			} else if (cell.i == minRow) {
				numCellsAtMin++;
			}
		}
		return numCellsAtMin;
	}
}
