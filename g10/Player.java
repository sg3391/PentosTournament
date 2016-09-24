package pentos.g10;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;

import java.lang.reflect.Field;
import java.util.*;


public class Player extends pentos.g0.Player{
	/*
	 * These are the possible points for a building to start to be built. 
	 * Loop through this set to decide where exactly to put the building.
	 * 
	 * In order to pack buildings closer to the corner, 
	 * whenever a new building is settled, the set will grow to contain the 
	 * cells on this building's margin, in order to make further moves to
	 * pack new buildings closer to the existing one.
	 * */
	Set<Cell> factoryStart;
	Set<Cell> residenceStart;
	Set<Cell> borders;
	
	
	public Set<Cell> roadcells = new HashSet<Cell>();//Just changed the name so it will not overwrite the road_cells field in g0.Player
	
	public static class Action{
		Building building;
		Cell startPoint;
		int rotation;
		Set<Cell> roadCells=new HashSet<>();
		Set<Cell> pondCells=new HashSet<>();
		Set<Cell> parkCells=new HashSet<>();
		
		public Action(){
			building=null;
			startPoint=null;
			rotation=0;
		}
		public Action(Building b,Cell c){
			building=b;
			startPoint=c;
			rotation=0;
		}
		public Action(Building b,Cell c,int r){
			building=b;
			startPoint=c;
			rotation=r;
		}
		public void setRoad(Set<Cell> cells){
			roadCells=cells;
		}
		public void setPark(Set<Cell> cells){
			parkCells=cells;
		}
		public void setPond(Set<Cell> cells){
			pondCells=cells;
		}
	}
	@Override
	public void init() { // function is called once at the beginning before play is called
		System.out.println("Initiating a player with strategy to start from two corners.");
		factoryStart=new HashSet<>();
		factoryStart.add(new Cell(49,49));
		residenceStart=new HashSet<>();
		residenceStart.add(new Cell(0,0));
		
		/* Initiate border cells */
		borders=new HashSet<>();
		for(int i=0;i<50;i++){
			borders.add(new Cell(0,i));
			borders.add(new Cell(i,0));
			borders.add(new Cell(49-i,49));
			borders.add(new Cell(49,49-i));
		}
		
		/* Initiate boder roads */
		
		System.out.println(borders.size()+" border cells");
    }
	@Override
	public Move play(Building request, Land land) {
		Action willDo=packBuildingToCorner(request,land);
		if(willDo.startPoint!=null){
			if(willDo.roadCells==null)
				willDo.roadCells=new HashSet<Cell>();
			return new Move(
					true,//accept the move
					willDo.building,//building
					willDo.startPoint,//location
					willDo.rotation,//rotation
					willDo.roadCells,//road
					willDo.pondCells,//water
					willDo.parkCells//park				
					);
		}else
				return new Move(false);
		
	}
	private Action packBuildingToCorner(Building request,Land land){
		Action toTake=new Action();
		if(request.type==Building.Type.RESIDENCE){
			toTake= packResidenceToCorner(request,land);
		}else if(request.type==Building.Type.FACTORY){
			toTake= packFactoryToCorner(request,land);
		}else{
			System.out.println("Error: Type "+request.type+" building request met!");
		}
		reportAction(toTake);
		return toTake;
	}
	private void reportAction(Action action){
		if(action==null||action.building==null){
			System.out.println("Empty action.");
			return;
		}
			
		System.out.println("The action is: ");
		System.out.println("Building "+action.building.type);
		Building rotated=action.building.rotations()[action.rotation];
		System.out.println("Cells to build: "+ToolBox.shiftCells(rotated,action.startPoint));
		System.out.println("Roads in the pack: "+action.roadCells);
	}
	private Action packFactoryToCorner(Building request,Land land){
		boolean canBuild=false;
		int score=0;
		Action toTake=new Action();
		Set<Cell> toOccupy=new HashSet<>();
		Set<Cell> avail=new HashSet<>();
		Set<Cell> roads=new HashSet<>();
		
		//try all cells as start point
		for(Cell c:factoryStart){
			/* Because the factory grows from the bottom right corner,
			 * the starting point to be used to build it should be the 
			 * bottom right corner.
			 */
			Building[] rotations=request.rotations();
			int len=rotations.length;
//			System.out.println(len+" rotations available.");
			for(int i=0;i<len;i++){
				Building b=rotations[i];
				
				/* Transit the building coordination regarding the cell because it's in the bottom right corner */
				Cell[] reflected=ToolBox.offsetBottomRight(b,c);
				if(reflected==null){
//					System.out.println("Out of border. This try doesn't work");
					continue;
				}
				/* Find the top left cell of the rectangle which will be later used as starting point */
				Cell topLeft=ToolBox.findTopLeft(reflected);
				ToolBox.lookToTopLeft(reflected,topLeft);
				Building ref=new Building(reflected,Building.Type.FACTORY);
				
				/* Debug requirement: compare the top-left referenced and original building coordination */
				if(!ToolBox.compareBuildings(ref,b)){
					System.out.println("The reflected building cells are not the same anymore!");
				}
				
				/* If this move is valid, perform it. */
				canBuild=land.buildable(b, topLeft);
				if(canBuild){
//					System.out.println("One solution found:"+ref);
					
					/* Include the road cells 
					 */
					Set<Cell> shiftedCells=ToolBox.shiftCells(b,topLeft);
					roads = ToolBox.findShortestRoad(shiftedCells, land, roadcells);
//					if(roads.size()>0)
//						System.out.println("Gotta build road "+roads.toString());
					
					//Validate the building after building roads
					Set<Cell> existingRoads=ToolBox.copyLandRoads(land);
					if(roads!=null){
						for(Cell r:roads){
							existingRoads.add(new Cell(r.i+1,r.j+1));
						}
					}else{
//						System.out.println("Null roads.");
						continue;
					}
					Cell[] erc=existingRoads.toArray(new Cell[existingRoads.size()]);
					boolean roadValid=Cell.isConnected(erc, 50+2);
					if(!roadValid){
//						System.out.println("Building not valid after building roads. How could this be?");;
						continue;
						
					}

					/*
					 * This is to calculate the consequence to the neighboring if the action is made.
					 * */
					Set<Cell> occupyThen=occupyCells(shiftedCells,roads);
					Set<Cell> availThen=newSurrounding(occupyThen,land);
					
					/*
					 * The score of a solution is, for now, decided by how close it can be 
					 * packed with the existing cluster.
					 * */
					int thisScore=calculateScore(factoryStart,occupyThen,availThen);
					if(thisScore>score){
//						System.out.println("Found a better solution with score "+thisScore);
						score=thisScore;
						toTake.building=request;
						toTake.startPoint=topLeft;
						toTake.rotation=i;
						
						toTake.roadCells=roads;
						
						toOccupy=occupyThen;
						avail=availThen;
					}
				}
			}
		}
//		System.out.println("Roads before update are:"+roadcells);
//		System.out.println("Adding road cells:"+toTake.roadCells);
		
//		System.out.println("The optimal solution so far has score:"+score);
		/* If the score is not 0, perform it. */
		if(score>0){
			Building toBuild=toTake.building.rotations()[toTake.rotation];
			boolean canDo=land.buildable(toBuild, toTake.startPoint);
			if(!canDo){
//				System.out.println("The action cannot be performed?!");
				return new Action();
			}
			updateFactoryStart(toOccupy,avail);
			updateRoads(toTake.roadCells);
			updateBorders(toOccupy,avail);
		}else{
			System.out.println("No solution");
		}
		return toTake;
	}
	private Action packResidenceToCorner(Building request,Land land){
		boolean canBuild=false;
		int score=0;
		Action toTake=new Action();
		Set<Cell> toOccupy=new HashSet<>();
		Set<Cell> avail=new HashSet<>();
		Set<Cell> roads=new HashSet<>();
		
		//try all cells as start point
		for(Cell c:residenceStart){
			/* Because the factory grows from the bottom right corner,
			 * the starting point to be used to build it should be the 
			 * bottom right corner.
			 */
			Building[] rotations=request.rotations();
			int len=rotations.length;
//			System.out.println(len+" rotations available.");
			for(int i=0;i<len;i++){
				Building b=rotations[i];
				
				/* If this move is valid, perform it. */
				canBuild=land.buildable(b, c);
				if(canBuild){
//					System.out.println("One solution found:"+b);
					
					/* Include the road cells 
					 */
					Set<Cell> shiftedCells=ToolBox.shiftCells(b,c);
					roads = ToolBox.findShortestRoad(shiftedCells, land, roadcells);
					
					//Validate the building after building roads
					Set<Cell> existingRoads=ToolBox.copyLandRoads(land);
					if(roads!=null){
						for(Cell r:roads){
							existingRoads.add(new Cell(r.i+1,r.j+1));
						}
					}else{
//						System.out.println("Null roads.");
						continue;
					}
					Cell[] erc=existingRoads.toArray(new Cell[existingRoads.size()]);
					boolean roadValid=Cell.isConnected(erc, 50+2);
					if(!roadValid){
//						System.out.println("Building not valid after building roads. How could this be?");;
						continue;
						
					}
					
					
					
//					if(roads.size()>0)
//						System.out.println("Gotta build road "+roads.toString());
					
					/*
					 * Pond cells
					 * */
					
					/*
					 * Park cells
					 * */
					
					
					/*
					 * This is to calculate the consequence to the neighboring if the action is made.
					 * */
					Set<Cell> occupyThen=occupyCells(shiftedCells,roads);
					Set<Cell> availThen=newSurrounding(occupyThen,land);
					
					/*
					 * The score of a solution is, for now, decided by how close it can be 
					 * packed with the existing cluster.
					 * */
					int thisScore=calculateScore(residenceStart,occupyThen,availThen);
					if(thisScore>score){
//						System.out.println("Found a better solution with score "+thisScore);
						score=thisScore;
						toTake.building=request;
						toTake.startPoint=c;
						toTake.rotation=i;
						
						toTake.roadCells=roads;
						
						toOccupy=occupyThen;
						avail=availThen;
					}
				}
			}
		}
//		System.out.println("The optimal solution so far has score:"+score);
//		System.out.println("Roads before update are:"+roadcells);
//		System.out.println("Adding road cells:"+toTake.roadCells);
		
		/* If the score is not 0, perform it. */
		if(score>0){
			Building toBuild=toTake.building.rotations()[toTake.rotation];
			boolean canDo=land.buildable(toBuild, toTake.startPoint);
			if(!canDo){
//				System.out.println("The action cannot be performed?!");
				return new Action();
			}
			updateResidenceStart(toOccupy,avail);
			updateRoads(toTake.roadCells);
			updateBorders(toOccupy,avail);
		}else{
			System.out.println("No solution");
		}
		return toTake;
	}
	private void updateRoads(Set<Cell> roads){
		if(roads!=null)
			roadcells.addAll(roads);
//		System.out.println("Roads after update are "+roadcells);
	}
	private void updateBorders(Set<Cell> toOccupy,Set<Cell> avail){
		borders.removeAll(toOccupy);
		borders.addAll(avail);
//		System.out.println("Now the borders are:"+borders);
	}
	private Set<Cell> occupyCells(Set<Cell> buildingCells,Set<Cell> roadCells){
		Set<Cell> overall=new HashSet<>();
		overall.addAll(buildingCells);
		if(roadCells!=null)
			overall.addAll(roadCells);
		return overall;
	}
	private Set<Cell> newSurrounding(Set<Cell> toOccupy,Land land){
		Set<Cell> avail=new HashSet<>();
		for(Cell c:toOccupy){
			Cell[] neighbors=c.neighbors();
			//the cell can be a candidate for another new start if not occupied
			for(int i=0;i<neighbors.length;i++){
				Cell cc=neighbors[i];
				if(land.unoccupied(cc)){
					avail.add(cc);
				}
			}
		}
		avail.removeAll(toOccupy);
		return avail;
	}
	/*
	 * This can be the prototype of a utility function.
	 * */
	private int calculateScore(Set<Cell> start,Set<Cell> toOccupy,Set<Cell> avail){
//		System.out.println(start.size()+" candidates before score calculation.");
		//Calculate how many candidate cells this building plan occupies
		Set<Cell> intersect=new HashSet<>();
		intersect.addAll(start);
		intersect.retainAll(toOccupy);
		int score=intersect.size();
		
		//Calculate how many border cells this building plan occupies
		intersect=new HashSet<>();
		intersect.addAll(toOccupy);
		intersect.retainAll(borders);
//		System.out.println("The occupied border cells are:"+intersect);
//		System.out.println("This plan will occupy "+intersect.size()+" border cells");
		score+=intersect.size();
		
//		System.out.println("Covering "+score+" of existing start points.");
//		System.out.println(start.size()+" candidates after score calculation.");
		return score;
	}
	public Set<Cell> occupyCells(Action action){
		Set<Cell> occupied=occupyCells(action.building,action.startPoint);
		occupied.addAll(action.roadCells);
		occupied.addAll(action.parkCells);
		occupied.addAll(action.pondCells);
		return occupied;
	}
	private Set<Cell> occupyCells(Building building,Cell start){
		Set<Cell> toOccupy=new HashSet<>();
		Iterator<Cell> iter=building.iterator();
		while(iter.hasNext()){
			Cell c=iter.next();
			Cell mapped=new Cell(c.i+start.i,c.j+start.j,Cell.Type.RESIDENCE);
			toOccupy.add(mapped);
		}
		return toOccupy;
	}
	/* Obsolete */
//	public Set<Cell> occupyCells(Building building,Cell start,Set<Cell> roads){
//		Set<Cell> toOccupy=ToolBox.shiftCells(building, start);
//		toOccupy.addAll(roads);
//		
//		return toOccupy;
//	}
	private void updateResidenceStart(Set<Cell> toOccupy,Set<Cell> avail){
		residenceStart.removeAll(toOccupy);
		residenceStart.addAll(avail);
	}
	private void updateFactoryStart(Set<Cell> toOccupy,Set<Cell> avail){
		factoryStart.removeAll(toOccupy);
		factoryStart.addAll(avail);
	}
	public static class ToolBox{
			//Use reflection to get the bloody private field 
			// throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException
			public static Cell.Type getCellType(Cell c){
				try{
					Field f = c.getClass().getDeclaredField("type"); //NoSuchFieldException
					f.setAccessible(true);
					Cell.Type iWantThis = (Cell.Type) f.get(c); //IllegalAccessException
					return iWantThis;
				}catch(Exception e){
					System.out.println("Cannot get the field anyway. Return a default.");
					return Cell.Type.FACTORY;
				}
			}

			public static Set<Cell> getRoads(Land land){
				try{
					Field f = land.getClass().getDeclaredField("road_network"); //NoSuchFieldException
					f.setAccessible(true);
					@SuppressWarnings("unchecked")
					Set<Cell> iWantThis = (Set<Cell>) f.get(land); //IllegalAccessException
					return iWantThis;
				}catch(Exception e){
					System.out.println("Cannot get the roads anyway. Return a default.");
					return new HashSet<Cell>();
				}

			}

			/*
			 * Shift cells to make it start from the offset.
			 * */
			public static Set<Cell> shiftCells(Building building,Cell start){
				int len=building.size();
				Cell[] shifted=new Cell[len];
				Iterator<Cell> iter=building.iterator();
				int i=0;
				Cell.Type t=null;
				while(iter.hasNext()){
					Cell c=iter.next();
					if(t==null){
						t=getCellType(c);
						//				System.out.println("Detected the cell is of type "+t);
					}
					shifted[i]=new Cell(c.i+start.i,c.j+start.j,t);
					i++;
				}

				return new HashSet<Cell>(Arrays.asList(shifted));
			}
			/* The starting point is the bottom right corner.
			 * The reaction is to just multiple the coord of each cell by -1 to make it a reflection.
			 * */
			public static Cell[] offsetBottomRight(Building building,Cell start){
				Iterator<Cell> iter=building.iterator();
				int len=building.size();
				int i=0;
				Cell[] reflected=new Cell[len];
				Cell.Type t=null;
				while(iter.hasNext()){
					Cell c=iter.next();
					if(t==null){
						t=getCellType(c);
						//				System.out.println("Detected the cell is of type "+t);
					}
					int row=start.i-c.i;
					int col=start.j-c.j;
					if(row<0||col<0)
						return null;
					reflected[i]=new Cell(row,col,t);
					i++;
				}
				return reflected;
			}
			// build shortest sequence of road cells to connect to a set of cells b
			public static Set<Cell> findShortestRoad(Set<Cell> b, Land land, Set<Cell> road_cells) {
				Set<Cell> output = new HashSet<Cell>();
				boolean[][] checked = new boolean[land.side][land.side];
				Queue<Cell> queue = new LinkedList<Cell>();
				// add border cells that don't have a road currently
				Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
				for (int z=0; z<land.side; z++) {
					if (b.contains(new Cell(0,z)) || b.contains(new Cell(z,0)) || b.contains(new Cell(land.side-1,z)) || b.contains(new Cell(z,land.side-1))) //if already on border don't build any roads
						return output;
					if (land.unoccupied(0,z))
						queue.add(new Cell(0,z,source));
					if (land.unoccupied(z,0))
						queue.add(new Cell(z,0,source));
					if (land.unoccupied(z,land.side-1))
						queue.add(new Cell(z,land.side-1,source));
					if (land.unoccupied(land.side-1,z))
						queue.add(new Cell(land.side-1,z,source));
				}
				// add cells adjacent to current road cells
				for (Cell p : road_cells) {
					for (Cell q : p.neighbors()) {
						if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q)) 
							queue.add(new Cell(q.i,q.j,p)); // use tail field of cell to keep track of previous road cell during the search
					}
				}	
				while (!queue.isEmpty()) {
					Cell p = queue.remove();
					checked[p.i][p.j] = true;
					for (Cell x : p.neighbors()) {		
						if (b.contains(x)) { // trace back through search tree to find path
							Cell tail = p;
							while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
								output.add(new Cell(tail.i,tail.j));
								tail = tail.previous;
							}
							if (!output.isEmpty())
								return output;
						}
						else if (!checked[x.i][x.j] && land.unoccupied(x.i,x.j)) {
							x.previous = p;
							queue.add(x);	      
						} 

					}
				}
				if (output.isEmpty() && queue.isEmpty())
					return null;
				else
					return output;
			}
			public static Cell findTopLeft(Cell[] cells){
				int minRow=100;
				int minCol=100;
				Cell top=null;
				Cell left=null;
				for(int i=0;i<cells.length;i++){
					Cell c=cells[i];
					if(c.i<=minRow){
						minRow=c.i;
						top=c;
					}
					if(c.j<=minCol){
						minCol=c.j;
						left=c;
					}
				}
				//		System.out.println("Found the top left cell "+minRow+","+minCol);
				if(top.equals(left))
					return top;
				else{
					System.out.println("Top cell is not left cell in a rectangle! Top:"+top+" Left:"+left);
					return null;
				}
			}
			public static void lookToTopLeft(Cell[] cells,Cell topLeft){
				if(cells.length==0)
					return;
				Cell.Type t=getCellType(cells[0]);
				for(int k=0;k<cells.length;k++){
					Cell c=cells[k];
					Cell n=new Cell(c.i-topLeft.i,c.j-topLeft.j,t);
					cells[k]=n;
				}
			}
			public static boolean compareBuildings(Building a,Building b){
				Set<Cell> aCells=getBuildingCells(a);
				Set<Cell> bCells=getBuildingCells(b);
				return aCells.containsAll(bCells)&&bCells.containsAll(aCells);
			}
			public static Set<Cell> getBuildingCells(Building b){
				Iterator<Cell> iter=b.iterator();
				Set<Cell> cells=new HashSet<>();
				while(iter.hasNext()){
					cells.add(iter.next());
				}
				return cells;
			}
			public static Set<Cell> copyLandRoads(Land land){
				try{
					//Copy the set of road cells
					Set<Cell> landRoads=getRoads(land);
					Set<Cell> copyRoads=new HashSet<>();
					copyRoads.addAll(landRoads);
					return copyRoads;
				}catch(Exception e){
					System.out.println("Cannot copy road network");
					return new HashSet<>();
				}
			}
	}
}
