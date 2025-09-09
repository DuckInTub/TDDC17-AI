package tddc17;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import aima.core.agent.Action;
import aima.core.agent.AgentProgram;
import aima.core.agent.Percept;
import aima.core.agent.impl.AbstractAgent;
import aima.core.agent.impl.DynamicPercept;
import aima.core.agent.impl.NoOpAction;
import aima.core.environment.liuvacuum.LIUVacuumEnvironment;

class MyAgentState
{
	public int[][] world = new int[30][30];
	final int UNKNOWN 	= 0;
	final int WALL 		= 1;
	final int CLEAR 	= 2;
	final int DIRT		= 3;
	final int HOME		= 4;
	
	private int prev_x = 1;
	private int prev_y = 1;
	public int agent_x_position = 1;
	public int agent_y_position = 1;
	
	public static final int NORTH = 0;
	public static final int EAST = 1;
	public static final int SOUTH = 2;
	public static final int WEST = 3;
	public int agent_direction = EAST;
	
	MyAgentState()
	{
		for (int r=0; r < world.length; r++)
			for (int c=0; c < world[0].length ; c++)
				world[r][c] = UNKNOWN;
		world[1][1] = HOME;
	}
	// Based on the last action and the received percept updates the x & y agent position
	public Action updatePosition(Action taken_action)
	{

		prev_x = agent_x_position;
		prev_y = agent_y_position;

		if (taken_action==LIUVacuumEnvironment.ACTION_MOVE_FORWARD)
	    {
			switch (agent_direction) {
			case MyAgentState.NORTH:
				agent_y_position--;
				break;
			case MyAgentState.EAST:
				agent_x_position++;
				break;
			case MyAgentState.SOUTH:
				agent_y_position++;
				break;
			case MyAgentState.WEST:
				agent_x_position--;
				break;
			}
	    } 
		else if (taken_action == LIUVacuumEnvironment.ACTION_TURN_LEFT) agent_direction = MyAgentProgram.left(agent_direction);
		else if (taken_action == LIUVacuumEnvironment.ACTION_TURN_RIGHT) agent_direction = MyAgentProgram.right(agent_direction);

		return taken_action;
	}

	public void restorePreviousPosition() {
		agent_x_position = prev_x;
		agent_y_position = prev_y;
	}
	
	public void updateWorld(int x, int y, int info) {
		try {
			world[y][x] = info;
		} catch (IndexOutOfBoundsException e) {
			return;
		}
	}

	public int getWorldAt(int r, int c) {
		try {
			return world[r][c];
		} catch (IndexOutOfBoundsException e) {
			return WALL;
		}
	}
	
	public void printWorldDebug() {
		for (int r=0; r < world.length; r++) {
			System.out.println("");
			for (int c=0; c < world[0].length ; c++) {
				switch (world[r][c]) {
					case UNKNOWN:
						System.out.print(" ? ");
						continue;
					case WALL:
						System.out.print(" # ");
						continue;
					case CLEAR:
						System.out.print(" . ");
						continue;
					case DIRT:
						System.out.print(" D ");
						continue;
					case HOME:
						System.out.print(" H ");
						continue;
				}
			}
		}
	}
}

class MyAgentProgram implements AgentProgram {

	private int initnialRandomActions = 10;
	private Random random_generator = new Random();
	
	// Here you can define your variables!
	public int iterationCounter = (int) 1E10;
	public MyAgentState state = new MyAgentState();

	public record Triplet(int a, int b, int c) {};
	public record Cord(int x, int y) {};

	public Set<Cord> seen = new HashSet<>();

	// Maps cordinates to x, y, dir that was used to enter the key cordinate
	public HashMap<Cord, Triplet> entered_through = new HashMap<>();

	// X, Y, Dir. Keep track of taken actions. Use to backtrack.
	// X, Y cord and the dir that was moved in.
	public Stack<Triplet> taken_actions = new Stack<>();

	// Used to quee actions for the agent
	public Queue<Action> actions_to_take = new ArrayDeque<>();

	// Cords on the path back home
	Stack<Cord> back_to_home_path = new Stack<>(); 

	boolean ready_to_go_home = false;

	public static int left(int dir) {return (dir - 1 + 4) % 4;};
	public static int right(int dir) {return (dir + 1) % 4;};
	public static int around(int dir) {return (dir + 2) % 4;};

	Cord neighbor(int x, int y, int dir) {
		// North, East, South, West.
		return switch (dir) {
			case 0 -> new Cord(x, y-1);
			case 1 -> new Cord(x+1, y);
			case 2 -> new Cord(x, y+1);
			case 3 -> new Cord(x-1, y);
			default -> throw new IllegalArgumentException("What?");
		};
	}

	List<Action> actions_to_move_in_dir(int cur_dir, int target_dir) {
		int dir_diff = (target_dir - cur_dir + 4) % 4;
		return switch (dir_diff) {
			case 0 -> new ArrayList<>();
			case 1 -> List.of(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
			case 2 -> List.of(LIUVacuumEnvironment.ACTION_TURN_LEFT, LIUVacuumEnvironment.ACTION_TURN_LEFT);
			case 3 -> List.of(LIUVacuumEnvironment.ACTION_TURN_LEFT);
			default -> throw new IllegalAccessError();
		};
	}

	// moves the Agent to a random start position
	// uses percepts to update the Agent position - only the position, other percepts are ignored
	// returns a random action
	private Action moveToRandomStartPosition(DynamicPercept percept) {
		int action = random_generator.nextInt(6);
		initnialRandomActions--;
		if(action==0) {
			return state.updatePosition(LIUVacuumEnvironment.ACTION_TURN_LEFT);
		} else if (action==1) {
			return state.updatePosition(LIUVacuumEnvironment.ACTION_TURN_RIGHT);
		}

    	System.out.println("x=" + state.agent_x_position);
    	System.out.println("y=" + state.agent_y_position);
    	System.out.println("dir=" + state.agent_direction);
		return state.updatePosition(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
	}
	
	
	@Override
	public Action execute(Percept percept) {

	    DynamicPercept p = (DynamicPercept) percept;
	    Boolean bump = (Boolean)p.getAttribute("bump");
	    Boolean dirt = (Boolean)p.getAttribute("dirt");
	    Boolean home = (Boolean)p.getAttribute("home");
	    System.out.println("percept: " + p);
		
		// DO NOT REMOVE this if condition!!!
    	if (initnialRandomActions>0) {
			if (bump) state.restorePreviousPosition();
    		return moveToRandomStartPosition((DynamicPercept) percept);
    	} else if (initnialRandomActions==0) {
    		// process percept for the last step of the initial random actions
    		initnialRandomActions--;
			System.out.println("Processing percepts after the last execution of moveToRandomStartPosition()");
	    	return state.updatePosition(LIUVacuumEnvironment.ACTION_SUCK);
    	}
		
    	// This example agent program will update the internal agent state while only moving forward.
    	// START HERE - code below should be modified!
    	    	
    	System.out.println("x=" + state.agent_x_position);
    	System.out.println("y=" + state.agent_y_position);
    	System.out.println("dir=" + state.agent_direction);
    	
		
	    iterationCounter--;
	    
	    if (iterationCounter==0) {
			System.out.println("DONE FOR FUCK SAKE!");
	    	return NoOpAction.NO_OP;
		}


		// NOTE: If we have bump set, we are currently in an illegal state on top of a wall.
		// We remedy this instantly.
	    if (bump) {
			switch (state.agent_direction) {
			case MyAgentState.NORTH:
				state.updateWorld(state.agent_x_position,state.agent_y_position,state.WALL);
				break;
			case MyAgentState.EAST:
				state.updateWorld(state.agent_x_position,state.agent_y_position,state.WALL);
				break;
			case MyAgentState.SOUTH:
				state.updateWorld(state.agent_x_position,state.agent_y_position,state.WALL);
				break;
			case MyAgentState.WEST:
				state.updateWorld(state.agent_x_position,state.agent_y_position,state.WALL);
				break;
			}
			// Restores to previous x, y state
			state.restorePreviousPosition();
			taken_actions.pop();
	    } 

		if (dirt)
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.DIRT);
	    else
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.CLEAR);
	    
	    state.printWorldDebug();
		System.out.println("Backtrack stack is " + taken_actions);
		System.out.println("Action to take are" + actions_to_take);
		System.out.println("Seen is" + seen);
	    
	    // Next action selection based on the percept value
		// --- NOTE: Work from here ---
		
	    if (dirt)
	    {
	    	System.out.println("DIRT -> choosing SUCK action!");
	    	return state.updatePosition(LIUVacuumEnvironment.ACTION_SUCK);
	    } 

		// Initialization
		seen.add(new Cord(state.agent_x_position, state.agent_y_position));

		if (!actions_to_take.isEmpty()) 
			return state.updatePosition(actions_to_take.poll());

		int x = state.agent_x_position;
		int y = state.agent_y_position;
		int dir = state.agent_direction;
		boolean moved = false;

		if (ready_to_go_home) {
			if (x == 1 && y == 1) {
				return NoOpAction.NO_OP;
			}

			moved = false;
			Cord neigh = back_to_home_path.pop();
			int dx = neigh.x - x;
			int dy = neigh.y - y;
			assert Math.abs(dx) <= 1;
			assert Math.abs(dy) <= 1;
			int new_dir = 0;

			if (dx == 1) new_dir = MyAgentState.EAST;
			if (dx == -1) new_dir = MyAgentState.WEST;
			if (dy == -1) new_dir = MyAgentState.NORTH;
			if (dy == 1) new_dir = MyAgentState.SOUTH;

			actions_to_take.addAll(actions_to_move_in_dir(dir, new_dir));
			actions_to_take.add(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);

			return state.updatePosition(actions_to_take.poll());
		}

		for (int dir_dx : new int[]{-1, 0, 1, 2}) {
			int new_dir = (dir + dir_dx + 4) % 4;
			Cord neigh = neighbor(x, y, new_dir);
			if (seen.contains(neigh)) continue;
			if (state.getWorldAt(neigh.y, neigh.x) == state.WALL || state.getWorldAt(neigh.y, neigh.x) == state.CLEAR) {
				continue;
			}
			actions_to_take.addAll(actions_to_move_in_dir(dir, new_dir));
			actions_to_take.add(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
			entered_through.put(neigh, new Triplet(x, y, new_dir));
			moved = true;
			taken_actions.add(new Triplet(x, y, new_dir));
			break;
		}
		
		// Backtrack
		if (!moved) {
			System.out.println("Starting backtrack");

			if (taken_actions.empty()) {
				
				List<Cord> arround_me = List.of(new Cord(x + 1, y + 0),
				new Cord(x - 1, y + 0), new Cord(x + 0, y + 1), new Cord(x + 0, y - 1));

				Cord the_cord = new Cord(1, 1);
				back_to_home_path.add(the_cord);
				while (!arround_me.contains(the_cord)) {
					Triplet trip = entered_through.get(the_cord);
					the_cord = new Cord(trip.a(), trip.b());
					back_to_home_path.add(the_cord);
				}

				ready_to_go_home = true;
				moved = true;
				System.out.println("AAAAAAAAAAAAAa");
				return state.updatePosition(LIUVacuumEnvironment.ACTION_SUCK);
			}

			Triplet last_action = taken_actions.pop();
			int back_dir = around(last_action.c());
			// entered_through.put(new Cord(last_action.a(), last_action.b()), new Triplet(x, y, back_dir));
			// actions_to_take.addAll(actions_to_move_in_dir(dir, back_dir));
			actions_to_take.add(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
		}

		return state.updatePosition(actions_to_take.poll());
	}
}

public class MyVacuumAgent extends AbstractAgent {
    public MyVacuumAgent() {
    	super(new MyAgentProgram());
	}
}
