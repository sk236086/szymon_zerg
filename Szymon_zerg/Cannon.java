package Szymon_zerg;

import java.util.*;

import Szymon_zerg.Comms;
import Szymon_zerg.Comms.CompoundMessage;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

/**
 * 
 * @author Mimuw_ZERG
 *
 */

public class Cannon extends GeneralRobot {
	/** If true then print debug strings */
	private final boolean DEBUG = false;

	/**
	 * Actual task of Cannon. Can be viewed as state of robot.
	 */
	public enum Mission {
		AUTO, MANUAL
	}

	private Mission mission;
	
	private MapLocation nextTarget = null;
	private boolean targetInAir = false;

	public Cannon(RobotController rc) {
		super(rc);
		mission = Mission.AUTO;
	}

	@Override
	public void processMessage(ArrayList<Comms.CompoundMessage> cmsgs) throws GameActionException{
		for (CompoundMessage cmsg : cmsgs) {
			switch(cmsg.type){
			case ARTY:
				mission = Mission.MANUAL;
				nextTarget = cmsg.loc;
				targetInAir = (cmsg.param > 0); 
				String s;
				if (targetInAir)
					s = " in AIR";
				else
					s = " on GROUND";
				myRC.setIndicatorString(1, "aimed at " + cmsg.loc + s);
				break;
			case GOTO:
				if (DEBUG) System.out.println("GOTO: Destination: "+myRC.getLocation().directionTo(cmsg.loc));
				// Go to given location 
				if (myRC.getRoundsUntilMovementIdle() != 0){
					if (DEBUG) System.out.println("GOTO: We can't move now");
					break; // Unfortunately we can't move now. Ignore message 
				}
				Direction dir = myRC.getLocation().directionTo(cmsg.loc);
				navigation.goInDirection(dir);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void idling() throws GameActionException {
		myRC.setIndicatorString(2, mission.toString());
		switch (mission) {
		case MANUAL:
			// facing proper?
			if (nextTarget != null) {
				Direction dir = myRC.getLocation().directionTo(nextTarget);
				if (myRC.getDirection() != dir) {
					myRC.setDirection(dir);
					break;
				}
			}
			if ((nextTarget==null) 
					&& ((myRC.getRoundsUntilAttackIdle() <= 1))){
				Comms.CompoundMessage cmsg = comms.new CompoundMessage();
				cmsg.type = Comms.MessageType.AIMME;
				cmsg.loc = myRC.getLocation();
				comms.sendMessage(cmsg);
			}
			// can attack at all?
			if (myRC.getRoundsUntilAttackIdle() > 0) break;
			if (nextTarget == null){
				mission = Mission.AUTO;
				break;
			}
			// is in range?
			if (myRC.getLocation().distanceSquaredTo(nextTarget) <= RobotType.CANNON.attackRadiusMaxSquared()){
				try{
				if (targetInAir)
					myRC.attackAir(nextTarget);
				else
					myRC.attackGround(nextTarget);
				} catch(Exception e){
					
				}
			}
			nextTarget = null;
			break;
		case AUTO:
			if ((myRC.getRoundsUntilAttackIdle() == 1) 
					|| ((myRC.getRoundsUntilAttackIdle()==0) 
							&& (Clock.getRoundNum()%8 == myRC.getRobot().getID()%3))){
				Comms.CompoundMessage cmsg = comms.new CompoundMessage();
				cmsg.type = Comms.MessageType.AIMME;
				cmsg.loc = myRC.getLocation();
				comms.sendMessage(cmsg);
			}
			if (myRC.getRoundsUntilAttackIdle() > 0) break;
			String s;
			if (targetInAir)
				s = " in AIR";
			else
				s = " on GROUND";
			myRC.setIndicatorString(1, "aimed at " + nextTarget + s);
			ArrayList<Robot> robots = new ArrayList<Robot>(); 
			Collections.addAll(robots, myRC.senseNearbyGroundRobots());
			Collections.addAll(robots, myRC.senseNearbyAirRobots());

			for (Robot robot : robots) {
				RobotInfo ri = myRC.senseRobotInfo(robot);
				if (ri.team != myRC.getTeam()) {
					nextTarget = myRC.senseRobotInfo(robot).location;
					try {
						if (robot.getRobotLevel() == RobotLevel.IN_AIR)
							myRC.attackAir(nextTarget);
						else
							myRC.attackGround(nextTarget);
					} catch (Exception e) {

					}
				}
			}
			nextTarget = null;
			break;
		default:
		}
	}
	
	@Override
	public void action() throws GameActionException {
		idling();
		if (myRC.getEnergonLevel() < myRC.getMaxEnergonLevel() * 0.5){
			// find fooood
			MapLocation archons[] = myRC.senseAlliedArchons();
			MapLocation chosenArchon = null;
			MapLocation loc = myRC.getLocation();
			for (MapLocation archon : archons) {
				if (chosenArchon == null)
					chosenArchon = archon;
				else
					if (loc.distanceSquaredTo(archon) < loc.distanceSquaredTo(chosenArchon))
						chosenArchon = archon;				
			}
			if (!loc.isAdjacentTo(chosenArchon)){
				Direction dir = myRC.getLocation().directionTo(chosenArchon);
				navigation.goInDirection(dir);
			}
		}
	}
}
