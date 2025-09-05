package tddc17;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
import aima.core.environment.xyenv.Wall;

class Foo 
{
	public static int new_direction_after_turn(int current_direction, Action left_or_right) {
		List<Integer> arr = Arrays.asList(0, 1, 2, 3);
		int indx = arr.indexOf(current_direction);

		if (left_or_right == LIUVacuumEnvironment.ACTION_TURN_LEFT) {
			return arr.get((indx + 1) % 4);

		} else if (left_or_right == LIUVacuumEnvironment.ACTION_TURN_RIGHT) {
			return arr.get((indx - 1 + 4) % 4);
		} else {
			throw new IllegalArgumentException("Unsupported action dumbass!");
		}
	}


	public record cord(int x, int y) {}
	public static cord new_location_after_move(int x, int y, int direction) {
		return switch (direction) {
            case MyAgentState.NORTH -> new cord(x, y - 1);
            case MyAgentState.EAST  -> new cord(x + 1, y);
            case MyAgentState.SOUTH -> new cord(x, y + 1);
            case MyAgentState.WEST  -> new cord(x - 1, y);
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        };
	}

	public static int action_to_int(Action act) {
		if (act == NoOpAction.NO_OP) {
			return 0;
		}
		else if (act == LIUVacuumEnvironment.ACTION_MOVE_FORWARD) {
			return 1;
		} else if (act == LIUVacuumEnvironment.ACTION_TURN_RIGHT) {
			return 2;
		} else if (act == LIUVacuumEnvironment.ACTION_TURN_LEFT) {
			return 3;
		} else if (act == LIUVacuumEnvironment.ACTION_SUCK) {
			return 4;
		}
		throw new IllegalArgumentException("HOW?!");
	}
}

class MyAgentState
{
	public int[][] world = new int[30][30];
	public int initialized = 0;
	final int UNKNOWN 	= 0;
	final int WALL 		= 1;
	final int CLEAR 	= 2;
	final int DIRT		= 3;
	final int HOME		= 4;
	final int ACTION_NONE 			= 0;
	final int ACTION_MOVE_FORWARD 	= 1;
	final int ACTION_TURN_RIGHT 	= 2;
	final int ACTION_TURN_LEFT 		= 3;
	final int ACTION_SUCK	 		= 4;
	
	public int agent_x_position = 1;
	public int agent_y_position = 1;
	public int agent_last_action = ACTION_NONE;
	
	public static final int NORTH = 0;
	public static final int EAST = 1;
	public static final int SOUTH = 2;
	public static final int WEST = 3;
	public int agent_direction = EAST;
	
	MyAgentState()
	{
		for (int i=0; i < world.length; i++)
			for (int j=0; j < world[i].length ; j++)
				world[i][j] = UNKNOWN;
		world[1][1] = HOME;
		agent_last_action = ACTION_NONE;
	}
	// Based on the last action and the received percept updates the x & y agent position
	public void updatePosition(DynamicPercept p)
	{
		Boolean bump = (Boolean)p.getAttribute("bump");

		if (agent_last_action==ACTION_MOVE_FORWARD && !bump)
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

		if (agent_last_action == ACTION_TURN_LEFT) agent_direction = Foo.new_direction_after_turn(agent_direction, LIUVacuumEnvironment.ACTION_TURN_LEFT);
		if (agent_last_action == ACTION_TURN_RIGHT) agent_direction = Foo.new_direction_after_turn(agent_direction, LIUVacuumEnvironment.ACTION_TURN_RIGHT);
		
	}
	
	public void updateWorld(int x_position, int y_position, int info)
	{
		world[x_position][y_position] = info;
	}
	
	public void printWorldDebug()
	{
		for (int i=0; i < world.length; i++)
		{
			for (int j=0; j < world[i].length ; j++)
			{
				if (world[j][i]==UNKNOWN)
					System.out.print(" ? ");
				if (world[j][i]==WALL)
					System.out.print(" # ");
				if (world[j][i]==CLEAR)
					System.out.print(" . ");
				if (world[j][i]==DIRT)
					System.out.print(" D ");
				if (world[j][i]==HOME)
					System.out.print(" H ");
			}
			System.out.println("");
		}
	}


}

class MyAgentProgram implements AgentProgram {

	private int initnialRandomActions = 10;
	private Random random_generator = new Random();
	
	// Here you can define your variables!
	public int iterationCounter = 100;
	public MyAgentState state = new MyAgentState();

	public record Triplet(int a, int b, int c) {};
	public record Cord(int x, int y) {};

	public Set<Cord> seen = new HashSet<>();

	// Maps cordinates to x, y, dir that was used to enter the key cordinate
	public HashMap<Cord, Triplet> back_map = new HashMap<>();

	// X, Y, Dir. Keep track of taken actions. Use to backtrack.
	public Stack<Triplet> action_stack = new Stack<>();

	// Used to quee actions for the agent
	public Queue<Action> action_quee = new ArrayDeque<>();

	int left(int dir) {return (dir - 1 + 4) % 4;};
	int right(int dir) {return (dir + 1) % 4;};
	int around(int dir) {return (dir + 2) % 4;};

	Cord neighbor(int x, int y, int dir) {
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
		state.updatePosition(percept);
		if(action==0) {
		    state.agent_direction = ((state.agent_direction-1) % 4);
		    if (state.agent_direction<0) 
		    	state.agent_direction +=4;
		    state.agent_last_action = state.ACTION_TURN_LEFT;
			return LIUVacuumEnvironment.ACTION_TURN_LEFT;
		} else if (action==1) {
			state.agent_direction = ((state.agent_direction+1) % 4);
		    state.agent_last_action = state.ACTION_TURN_RIGHT;
		    return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
		} 
		state.agent_last_action=state.ACTION_MOVE_FORWARD;
		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	}
	
	
	@Override
	public Action execute(Percept percept) {
		
		// DO NOT REMOVE this if condition!!!
    	if (initnialRandomActions>0) {
    		return moveToRandomStartPosition((DynamicPercept) percept);
    	} else if (initnialRandomActions==0) {
    		// process percept for the last step of the initial random actions
    		initnialRandomActions--;
    		state.updatePosition((DynamicPercept) percept);
			System.out.println("Processing percepts after the last execution of moveToRandomStartPosition()");
			state.agent_last_action=state.ACTION_SUCK;

	    	return LIUVacuumEnvironment.ACTION_SUCK;
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

	    DynamicPercept p = (DynamicPercept) percept;
	    Boolean bump = (Boolean)p.getAttribute("bump");
	    Boolean dirt = (Boolean)p.getAttribute("dirt");
	    Boolean home = (Boolean)p.getAttribute("home");
	    System.out.println("percept: " + p);

	    // State update based on the percept value and the last action
	    state.updatePosition((DynamicPercept)percept);
	    if (bump) {
			switch (state.agent_direction) {
			case MyAgentState.NORTH:
				state.updateWorld(state.agent_x_position,state.agent_y_position-1,state.WALL);
				break;
			case MyAgentState.EAST:
				state.updateWorld(state.agent_x_position+1,state.agent_y_position,state.WALL);
				break;
			case MyAgentState.SOUTH:
				state.updateWorld(state.agent_x_position,state.agent_y_position+1,state.WALL);
				break;
			case MyAgentState.WEST:
				state.updateWorld(state.agent_x_position-1,state.agent_y_position,state.WALL);
				break;
			}
	    }
	    if (dirt)
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.DIRT);
	    else
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.CLEAR);
	    
	    state.printWorldDebug();
	    
	    
	    // Next action selection based on the percept value
		// --- NOTE: Work from here ---
		
	    if (dirt)
	    {
	    	System.out.println("DIRT -> choosing SUCK action!");
	    	state.agent_last_action=state.ACTION_SUCK;
	    	return LIUVacuumEnvironment.ACTION_SUCK;
	    } 

		// Initialization
		seen.add(new Cord(state.agent_x_position, state.agent_y_position));
		// back_map.put(new Cord(state.agent_x_position, state.agent_y_position), 0);
		// action_stack.add(new Triplet(state.agent_x_position, state.agent_y_position, state.agent_direction));

		if (!action_quee.isEmpty()) return action_quee.poll();

		if (!action_stack.isEmpty()) {
			Triplet t = action_stack.pop();
			int x = t.a();
			int y = t.a();
			int dir = t.a();
			boolean moved = false;

			for (int new_dir : new int[]{0, 1, 2, 3}) {
				Cord neigh = neighbor(x, y, new_dir);
				if (seen.contains(neigh)) continue;
				if (state.world[neigh.x][neigh.y] == state.WALL) continue;
				List<Action> move_steps = actions_to_move_in_dir(dir, new_dir);
				move_steps.add(LIUVacuumEnvironment.ACTION_MOVE_FORWARD);
				action_quee.addAll(move_steps);
				back_map.put(neigh, new Triplet(x, y, new_dir));
				seen.add(neigh);
				action_stack.add(t)
				moved = true;
				break;

			}

			if (!moved) {
				// Backtrack
				
			}

		}


	}
}

public class MyVacuumAgent extends AbstractAgent {
    public MyVacuumAgent() {
    	super(new MyAgentProgram());
	}
}
