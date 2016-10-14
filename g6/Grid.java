package pentos.g6;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Grid {
	private static HashMap<Integer, Set<Row>> factoryRows = new HashMap<Integer, Set<Row>>();
	private static HashMap<Integer, Set<Row>> residenceRows = new HashMap<Integer, Set<Row>>();
	private static int[] numFactoryRowsPerSize = { 5, 4, 3, 2 };
	private static int factoryRowSizeShift = 2;
	private static int[] numResidenceRowsPerSize = { 6, 4, 1 };
	private static int residenceRowSizeShift = 3;
	private static int factoryRowCurrentPosition = 0;// starts from bottom
	// private static int factoryRowCurrentPosition = 49;// starts from bottom
	private static int residenceRowCurrentPosition = 0;// starts from top
	private static int previousRowType = 0;// 1 means park, 0 means road
	private static boolean previousIsFactoryRoad = true;// 1 means park, 0 means
														// road

	public static boolean generatable(int rowSize, int rowType) {
		// 1 for factory, 2 for residence

		// if (factoryRowCurrentPosition - residenceRowCurrentPosition <
		// rowSize)
		// return false;/// factory and residence will collide
		if (rowType == 1) {
			// if (factoryRowCurrentPosition < 0)
			// return false; // grid full
			if (factoryRowCurrentPosition + rowSize > 50)
				return false; //

		} else {
			if (residenceRowCurrentPosition + rowSize > 50)
				return false; // grid full
		}
		return true;
	}

	public static HashMap<Integer, Set<Row>> getFactoryRows() {
		return factoryRows;
	}

	public static HashMap<Integer, Set<Row>> getResidenceRows() {
		return residenceRows;
	}

	// This is for factories on the bottom
	public static void generateFactoryRow2(int rowSize) {
		if (generatable(rowSize, 1)) {
			int roadLocation;
			int end = factoryRowCurrentPosition + 1; // exclusive
			int start = factoryRowCurrentPosition - rowSize + 1; // inclusive
			if (previousIsFactoryRoad) {
				roadLocation = end;
				previousIsFactoryRoad = false;
				factoryRowCurrentPosition = start - 1;

			} else {
				roadLocation = start - 1;
				previousIsFactoryRoad = true;
				factoryRowCurrentPosition = roadLocation - 1;

			}
			Row row = new Row(start, end, roadLocation);
			if (!factoryRows.containsKey(rowSize)) {
				factoryRows.put(rowSize, new HashSet<Row>());
			}
			factoryRows.get(rowSize).add(row);
		}
	}

	// This is for left to right
	public static void generateFactoryRow(int rowSize) {
		if (generatable(rowSize, 1)) {
			int roadLocation;
			int start = factoryRowCurrentPosition; // inclusive
			int end = factoryRowCurrentPosition + rowSize; // exclusive

			if (previousIsFactoryRoad) {
				roadLocation = start - 1;
				previousIsFactoryRoad = false;
				factoryRowCurrentPosition = end;
			} else {
				roadLocation = end;
				previousIsFactoryRoad = true;
				factoryRowCurrentPosition = roadLocation + 1;
			}
			Row row = new Row(start, end, roadLocation);
			if (!factoryRows.containsKey(rowSize)) {
				factoryRows.put(rowSize, new HashSet<Row>());
			}
			factoryRows.get(rowSize).add(row);
		}
	}

	public static void generateResidenceRow(int rowSize) {
		if (generatable(rowSize, 2)) {
			int start = residenceRowCurrentPosition;
			int end = residenceRowCurrentPosition + rowSize;
			int roadLocation;
			int parkLocation;
			if (previousRowType == 0) {
				roadLocation = residenceRowCurrentPosition - 1;
				parkLocation = residenceRowCurrentPosition + rowSize;
				previousRowType = 1;
			} else {
				parkLocation = residenceRowCurrentPosition - 1;
				roadLocation = residenceRowCurrentPosition + rowSize;
				previousRowType = 0;
			}
			Row row = new Row(start, end, roadLocation, parkLocation, 49);
			if (!residenceRows.containsKey(rowSize)) {
				residenceRows.put(rowSize, new HashSet<Row>());
			}
			residenceRows.get(rowSize).add(row);
			residenceRowCurrentPosition = end + 1;
		}
	}

	public static void initializeFactoryRows() {
		// Initializing factory rows
		int currentRow = 0;
		int rowNumber = 0;
		for (int i = 0; i < numFactoryRowsPerSize.length; ++i) {
			for (int j = 0; j < numFactoryRowsPerSize[i]; ++j) {
				int roadLocation;
				if (rowNumber % 2 == 0) {
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
	}

	public static void initializeResidenceRows() {
		// Initializing residence rows, accounting for both parks and roads in
		// the middle
		int currentRow = 0;
		int rowNumber = 0;
		for (int i = 0; i < numResidenceRowsPerSize.length; ++i) {
			for (int j = 0; j < numResidenceRowsPerSize[i]; ++j) {
				int roadLocation, parkLocation;
				if (rowNumber % 2 == 0) {
					roadLocation = currentRow - 1;
					parkLocation = currentRow + i + residenceRowSizeShift;
				} else {
					roadLocation = currentRow + i + residenceRowSizeShift;
					parkLocation = currentRow - 1;
				}
				Row row = new Row(currentRow, currentRow + i + residenceRowSizeShift, roadLocation, parkLocation, 49);
				if (!residenceRows.containsKey(i + residenceRowSizeShift)) {
					residenceRows.put(i + residenceRowSizeShift, new HashSet<Row>());
				}
				residenceRows.get(i + residenceRowSizeShift).add(row);

				currentRow = currentRow + i + residenceRowSizeShift + 1;
				rowNumber++;
			}
		}
	}

}
