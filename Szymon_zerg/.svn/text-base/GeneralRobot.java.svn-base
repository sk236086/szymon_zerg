package Szymon_zerg;

import static battlecode.common.GameConstants.ENERGON_RESERVE_SIZE;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import Szymon_zerg.Comms;
import Szymon_zerg.Comms.CompoundMessage;
import Szymon_zerg.map.GameMap;
import Szymon_zerg.map.Vicinity;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

/**
 * 
 * @author Mimuw_ZERG
 *
 */

public abstract class GeneralRobot {
	protected final RobotController myRC;
	protected Navigation navigation;
	protected Comms comms;
	protected int spawnTime;
	protected GameMap gameMap;
	protected Vicinity vicinity;

	public GeneralRobot(RobotController rc) {
		myRC = rc;
		navigation = new Navigation(myRC);
		comms = new Comms(myRC);
		gameMap = new GameMap(myRC);
		vicinity = new Vicinity(myRC);
	}

	public abstract void idling() throws GameActionException;
	public abstract void action() throws GameActionException;
	public void processMessage(ArrayList<Comms.CompoundMessage> cmsgs) throws GameActionException{
		myRC.setIndicatorString(1, "Got (" + cmsgs.size() + ") message(s)");
	}
	
	public void checkChannelers() throws GameActionException{
		ArrayList<RobotInfo> robots = vicinity.getInfo(true);
		Map<RobotInfo,CompoundMessage> channelers = new Hashtable<RobotInfo, CompoundMessage>();
		for (RobotInfo ri : robots) {
			// is it one of our channelers?
			if (ri.type == RobotType.CHANNELER){
				// can I see enough to notify it?
				if (ri.location.distanceSquaredTo(myRC.getLocation()) <=  
					myRC.getRobotType().sensorRadius()*myRC.getRobotType().sensorRadius() 
					+ RobotType.CHANNELER.attackRadiusMaxSquared()){
						// remember it
						channelers.put(ri,comms.new CompoundMessage());
						channelers.get(ri).type = Comms.MessageType.YOU_STOP_DRAIN;
						channelers.get(ri).address = ri.location;
				}
			}
		}
		if (channelers.size() == 0)
			return;
		robots = vicinity.getInfo(false);
		for (RobotInfo ri : robots) {
			// check every remembered channeler
			for (RobotInfo chan : channelers.keySet()) {
				// is the enemy in range?
				if (ri.location.distanceSquaredTo(chan.location) <= RobotType.CHANNELER
						.attackRadiusMaxSquared()) {
					channelers.get(chan).type = Comms.MessageType.YOU_DRAIN;
				} else {
					if (channelers.get(chan).type == Comms.MessageType.YOU_STOP_DRAIN) {
						MapLocation loc = chan.location;
						loc = loc.add(loc.directionTo(ri.location));
						if (myRC.getLocation().distanceSquaredTo(loc) <= 2) {
							channelers.get(chan).type = Comms.MessageType.YOU_GOTO;
							channelers.get(chan).loc = loc;
						}
					}
				}
			}
		}		
		for (CompoundMessage cmsg : channelers.values()){
			comms.sendMessage(cmsg);				
		}
	}

	public void checkCannons() throws GameActionException{
		ArrayList<RobotInfo> robots = vicinity.getInfo(true); 
		Map<RobotInfo,CompoundMessage> cannons = new Hashtable<RobotInfo, CompoundMessage>();
		for (RobotInfo ri : robots) {
			// is it one of our cannons?
			if (ri.type == RobotType.CANNON) {
				if (ri.roundsUntilAttackIdle <= 3)
					// remember it
					cannons.put(ri, comms.new CompoundMessage());
			}
		}
		if (cannons.size()==0) return;
		robots = vicinity.getInfo(false);
		for (RobotInfo ri : robots) {
			// check every remembered cannon
			for (RobotInfo cann : cannons.keySet()) {
				// is the enemy in range?
				if (ri.location.distanceSquaredTo(cann.location) <= RobotType.CANNON
						.attackRadiusMaxSquared()) {
					if ((cannons.get(cann).type == Comms.MessageType.NONE)
							|| (cannons.get(cann).type == Comms.MessageType.GOTO)) {
						Comms.CompoundMessage cmsg = comms.new CompoundMessage();
						cmsg.type = Comms.MessageType.YOU_ARTY;
						cmsg.address = cann.location;
						cmsg.loc = ri.location; 
						// airborne target?
						if ((ri.type == RobotType.ARCHON)||(ri.type == RobotType.SCOUT))
							cmsg.param = 1;
						else
							cmsg.param = 0;
						comms.sendMessage(cmsg);
						cannons.put(cann, cmsg);
					}
				} else {
					if (cannons.get(cann).type != Comms.MessageType.NONE)
						break;
					Comms.CompoundMessage cmsg = comms.new CompoundMessage();
					cmsg.type = Comms.MessageType.YOU_GOTO;
					cmsg.address = cann.location;
					cmsg.loc = ri.location;
					cannons.put(cann, cmsg);
				}
			}
		}
		for(CompoundMessage cmsg : cannons.values()){
			if (cmsg!=null)
				comms.sendMessage(cmsg);
		}
	}

	public void feedRobots() throws GameActionException{
		List<RobotInfo> robots = vicinity.getInfo(true);

		double myEnergonLevel = myRC.getEnergonLevel() - myRC.getMaxEnergonLevel()*0.5;
		if (myEnergonLevel < 0)
			return;
		for (RobotInfo info : robots) {
			if (info.location.isAdjacentTo(myRC.getLocation())
					|| info.location.equals(myRC.getLocation())) {
				if ((info.energonLevel < myEnergonLevel)
						&& (info.energonLevel <= 0.4 * info.maxEnergon)) {
					double transferAmount = Math.min(ENERGON_RESERVE_SIZE
							- info.energonReserve, myEnergonLevel);
					if ((info.type == RobotType.ARCHON)||(info.type == RobotType.SCOUT))
						myRC.transferEnergon(transferAmount, info.location, RobotLevel.IN_AIR);
					else
						myRC.transferEnergon(transferAmount, info.location, RobotLevel.ON_GROUND);
					return;
				}
			}
		}		
	}
}
