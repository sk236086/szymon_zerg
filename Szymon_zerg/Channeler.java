package Szymon_zerg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Szymon_zerg.Comms;
import Szymon_zerg.Comms.CompoundMessage;
import Szymon_zerg.Navigation.NaviResult;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Channeler extends GeneralRobot {

	/**
	 * Actual task of Channeler. Can be viewed as state of robot.
	 */
	public enum Mission {
		NONE, PING, CONSTANT_DRAIN
	}

	private Mission mission;
	
	private double lastEnergonLevel;

	public Channeler(RobotController rc) {
		super(rc);
		mission = Mission.PING;
		lastEnergonLevel = rc.getEnergonLevel();
	}

	@Override
	public void processMessage(ArrayList<Comms.CompoundMessage> cmsgs){
		for (CompoundMessage cmsg : cmsgs) {
			switch(cmsg.type){
			case STOP_DRAIN:
				mission = Mission.PING;
				myRC.setIndicatorString(1, "told to cease");
				break;
			case DRAIN:
				mission = Mission.CONSTANT_DRAIN;
				myRC.setIndicatorString(1, "told to drain");
				break;
			case GOTO:
				// Go to given location 
				if (myRC.getRoundsUntilMovementIdle() != 0){
					break; // Unfortunately we can't move now. Ignore message 
				}
				List<MapLocation> tempPath = Collections.singletonList(cmsg.loc);
				while (true){
					NaviResult naviResult = navigation.goUsingPath(tempPath);
					if (naviResult == NaviResult.MOVED){
						myRC.yield();
						break; // Moved. Back to other tasks.
					}else if (naviResult == NaviResult.CHANGED_DIRECTION){
						myRC.yield();
					}else{
						break; // Some error occured. Move not possible. Ignore command.
					}
				}
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
		case PING:
			checkEnergonLoss();
			break;
		
		case CONSTANT_DRAIN:
			if (!myRC.isAttackActive())
				myRC.drain();
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

	/**
	 * Checks if energon loss is greater than expected.
	 */
	public void checkEnergonLoss(){
		double expectedEnergonLevel = lastEnergonLevel - myRC.getRobotType().energonUpkeep();
		lastEnergonLevel = myRC.getEnergonLevel();
		if (expectedEnergonLevel > lastEnergonLevel){
			/* most likely we're being attacked */
			mission = Mission.CONSTANT_DRAIN;
		}
	}
}
