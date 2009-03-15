package Szymon_zerg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import Szymon_zerg.Comms;
import Szymon_zerg.Comms.CompoundMessage;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.FluxDeposit;
import battlecode.common.FluxDepositInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import static battlecode.common.GameConstants.*;

/**
 * 
 * @author Mimuw_ZERG
 *
 */

public class Archon extends GeneralRobot {

	/**
	 * Actual task of Archon. Can be viewed as state of robot.
	 */
	public enum Mission {
		NONE, 
		INIT, 
		SPAWN_SCOUT, FEED_SCOUT, 
		FIND_FLUX_DEPOSIT, FIND_RANDOM_FLUX_DEPOSIT, DRAIN_DEPOSIT,
		GO_TO_ALLIES, GO_TO_COMBAT,
		SIEGE, DEFEND_FLUX;
		
		public boolean isCombat(){
			return ((this==SIEGE)||(this==DEFEND_FLUX));				
		}
		
		public boolean isDraining(){
			return ((this==DEFEND_FLUX)
					|| (this==DRAIN_DEPOSIT));			
		}
		
		public double feedValue(RobotType rt){
			if (isCombat()){
				switch(rt){
				case ARCHON: return 0.2;
				case SOLDIER:
				case CHANNELER:
				case CANNON:
					return 0.5;
				default: return 0.3;
				}
			} else {
				switch(rt){
				case ARCHON: return 0.2;
				default: return 0.6;
				}
			}
		}
	
		public double retainValue(){
			if (isCombat())
				return 0.3;
			else
				return 0.1;
		}
	}
	
	public enum CombatMode { 
		NONE, 
		STD, SEC, FLANK;
		
		static public CombatMode supplement(CombatMode prev, Comms.MessageType mt){
			switch(mt){
			case DEFENDING: return STD;
			case SIEGE_STD:
				if ((prev == NONE)||(prev == STD))
					return SEC;
				else
					return prev;
			case SIEGE_SEC: 
				if ((prev == NONE)||(prev == SEC))
					return FLANK;
				else
					return prev;
			case SIEGE_FLANK: // we dont want trashing, so at worst all end up FLANK
				if (prev == NONE)
					return STD;
				else
					return prev;
			default:
				if (prev == NONE)
					return STD;
				else
					return prev;
			}
		}
		
		public Comms.MessageType msgType() {
			switch (this) { 
			case SEC: return Comms.MessageType.SIEGE_SEC;
			case FLANK: return Comms.MessageType.SIEGE_FLANK;
			default: return Comms.MessageType.SIEGE_STD;
			}
		}
		
		public int armySize(){
			switch(this){
			case SEC: return 4;
			default: return 2;
			}
		}
	}
	
	private FluxDepositInfo fluxInfo;
	private ArrayList<MapLocation> avoidedFlux = new ArrayList<MapLocation>();
	private int boastCounter = 0;
	private Direction previousSense = null;
	
	private int boastCountdown = 50;

	private Direction nextDirection = Direction.NONE;

	private Mission mission;
	private int myNumber = -1;
	
	private int spawned = 0;
	private boolean doneFeeding = false;
	
	private MapLocation siegeLoc;
	private CombatMode combatMode = CombatMode.NONE;
	
	private int scoutCount = 2;
	private boolean initialScouts = false;
		
	private boolean useFluxBurn = true;
	private double fluxBurnThreshold = 0.2;
	
	private class ArmyDescriptor{
		public RobotType rt;
		public int count;
		
		public ArmyDescriptor(RobotType rt, int count){
			this.rt = rt;
			this.count = count;
		}
	}
	
	private final ArmyDescriptor[] armyDescriptors = { 
			new ArmyDescriptor(RobotType.WORKER,2),
			new ArmyDescriptor(RobotType.CANNON,1),
			new ArmyDescriptor(RobotType.CHANNELER,1)
	};
	
	private MapLocation[] alliedArchons;
	
	public Archon(RobotController rc) {
		super(rc);
		fluxInfo = null;
		mission = Mission.INIT;
	}
	
	@Override
	public void processMessage(ArrayList<Comms.CompoundMessage> cmsgs){
		for (CompoundMessage cmsg : cmsgs) {
			switch(cmsg.type){
			case GOING_TO_LOC:
				avoidedFlux.add(cmsg.loc);
				break;
			case DEFENDING:
			case SIEGE_SEC:
			case SIEGE_STD:
			case SIEGE_FLANK:
				if ((!mission.isCombat() && (!mission.isDraining()))){
					myRC.setIndicatorString(1, "Going to a fight");
					mission = Mission.GO_TO_COMBAT;
					siegeLoc = cmsg.loc;
					combatMode = CombatMode.supplement(combatMode,cmsg.type);
				}
				break;
			case AIMME:
				try{
					checkCannons();
				}catch(Exception e){
					
				}
				break;
			default:
				break;
			}
		}
	}
	
	public void commonPreMission() throws GameActionException{
		if (Clock.getRoundNum() %11 == 0){
			checkChannelers();
			checkCannons();
		}
		if (mission == Mission.SIEGE)
			myRC.setIndicatorString(2, Integer.toString(myNumber) + " " + mission.toString() + " " + combatMode);
		else
			myRC.setIndicatorString(2, Integer.toString(myNumber) + " " + mission.toString());
	}
	
	public void commonPostMission() throws GameActionException{
		if (checkArchonsDied()){
//			if (mission == Mission.FIND_RANDOM_FLUX_DEPOSIT)
//				mission = Mission.GO_TO_COMBAT;
		}
		if ((mission.isCombat()) && (useFluxBurn)){
			if (myRC.getEnergonLevel() < fluxBurnThreshold * RobotType.ARCHON.maxEnergon()){
				try{
					myRC.burnFlux();
				}
				catch (Exception e){
					
				}
			}
		}
	}

	@Override
	public void idling() throws GameActionException {
		commonPreMission();
		switch (mission) { // TODO: add stuff, refactor some from action() into methods
		case DEFEND_FLUX:
			defendFlux();
			break;
		case SIEGE:
			siege();
			break;
		default:
		}
		commonPostMission();
	}

	@Override
	public void action() throws GameActionException {
		commonPreMission();
		MapLocation loc;
		switch (mission) {
		
		case INIT:
			// get archon number
			loc = myRC.getLocation();
			alliedArchons = myRC.senseAlliedArchons();
			for (int i = 0; i < alliedArchons.length; i++)
				if (loc == alliedArchons[i])
					myNumber = i;
			if (myNumber == -1) myNumber = -2; // WTF?
			// set archon initial mission - number-dependant
			if (myNumber >= 2)
				if (myNumber >= 4){
				//mission = Mission.SPAWN_SCOUT;
				mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
				spawned = 0;
				initialScouts = false;
				} else mission = Mission.FIND_FLUX_DEPOSIT;
			else {
				mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
			}
			break;			
		
		case SPAWN_SCOUT:
			if (performAttackCheck()) break;
			if (myRC.canMove(myRC.getDirection())){
				if (myRC.getEnergonLevel() - myRC.getMaxEnergonLevel()*mission.retainValue() > RobotType.SCOUT.spawnCost()){
					myRC.spawn(RobotType.SCOUT);
					mission = Mission.FEED_SCOUT;
					spawned++;
				}
			} else
				myRC.setDirection(myRC.getDirection().rotateLeft());
			break;
		
		case FEED_SCOUT:
			if (performAttackCheck()) break;
			feedRobots();
			if (doneFeeding)
				if ((spawned < scoutCount)&&initialScouts)
					mission = Mission.SPAWN_SCOUT;
				else {
					mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
				}
			break;

		case FIND_RANDOM_FLUX_DEPOSIT:
			if (performAttackCheck()) break;
			if(locateNearestFlux()){
				previousSense = null;
				mission = Mission.FIND_FLUX_DEPOSIT;
			} else {
				if (Clock.getRoundNum() % 8 == 0){
					ArrayList<MapLocation> avoidedFluxToRemove = new ArrayList<MapLocation>();
					for (MapLocation mapLocation : avoidedFlux) {
						if (mapLocation.distanceSquaredTo(myRC.getLocation()) >= 36)
							avoidedFluxToRemove.add(mapLocation);
					}
					for (MapLocation mapLocation : avoidedFluxToRemove)
						avoidedFlux.remove(mapLocation);
				}
				if (nextDirection == Direction.NONE)
					nextDirection = navigation.getRandomDirection();
				else {
					Direction nextSense = myRC.senseDirectionToUnownedFluxDeposit(); 
					if (previousSense != null) {
						if (navigation.guessFluxByTriangulation(nextDirection,
								previousSense, nextSense)) {
							mission = Mission.FIND_FLUX_DEPOSIT;
							previousSense = null;
							break;
						}
					}
					previousSense = nextSense;
				}
				if (myRC.canMove(nextDirection)) {
					try {
						if (nextDirection != myRC.getDirection())
							myRC.setDirection(nextDirection);
						else
							myRC.moveForward();
					} catch (GameActionException e) {
						nextDirection = navigation.getRandomDirection();
					}
				} else {
					loc = myRC.getLocation();
					loc = loc.add(nextDirection);
					if (myRC.senseAirRobotAtLocation(loc) == null)
						nextDirection = navigation.getRandomDirection();
					else
						navigation.goInDirection(nextDirection);
				}
			}
			break;
			
		case FIND_FLUX_DEPOSIT:
			if (performAttackCheck()) break;
			fluxInfo = null;
			if(myRC.senseNearbyFluxDeposits().length == 0){ // no deposit nearby
				nextDirection = myRC.senseDirectionToUnownedFluxDeposit();
			} else {
				// deposit nearby 
				if (locateNearestFlux()) { // deposit can be taken over 
					// locateNearestFlux() success --> fluxInfo is set
					if (boastCounter == 0) { // broadcast message
						boastCounter = boastCountdown;

						Comms.CompoundMessage cmsg = comms.new CompoundMessage();
						cmsg.type = Comms.MessageType.GOING_TO_LOC;
						cmsg.loc = fluxInfo.location;
						comms.sendMessage(cmsg);
					}
					boastCounter--;
				} else { // deposit nearby, can't be taken over
					mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
					nextDirection = Direction.NONE;
					break;
				}
			}
			goToFluxDeposit();
			break;

		case DRAIN_DEPOSIT:
			if (performDefendCheck()) break;
			/* Draining flux */
			fluxInfo = myRC.senseFluxDepositInfo(myRC.senseFluxDepositAtLocation(myRC.getLocation()));
			/* Leave if drained */
			if (fluxInfo.roundsAvailableAtCurrentHeight == 0) {
				mission = Mission.FIND_FLUX_DEPOSIT;
				avoidedFlux.add(fluxInfo.location);
				locateNearestFlux();
				break;
			}
			feedRobots();
			if (doneFeeding)
				checkArmedForces();
			break;

		case GO_TO_ALLIES:
			if (performAttackCheck()) break;
			MapLocation[] locations = myRC.senseAlliedArchons();
			nextDirection = myRC.getLocation().directionTo(locations[0]);
			navigation.goInDirection(nextDirection);
			break;
		
		case GO_TO_COMBAT:
			MapLocation tmp = siegeLoc;
			if (performAttackCheck()) break;
			siegeLoc = tmp;
			if (myRC.getLocation().distanceSquaredTo(siegeLoc) <= 2){
				mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
				break;
			}				
			nextDirection = myRC.getLocation().directionTo(siegeLoc);
			navigation.goInDirection(nextDirection);			
			break;

		case SIEGE:
			siege();
			break;
		
		case DEFEND_FLUX:
			defendFlux();
			break;
			
		default:
		}
		commonPostMission();
	}

	private void performArmySpawn() throws GameActionException {
		Direction dir = findSpawnLocation(myRC.getDirection());
		if (dir != myRC.getDirection())
			myRC.setDirection(dir);
		RobotType rt = null;
		switch(combatMode){
		case SEC:
			rt = RobotType.SOLDIER;
			break;
		default:
			rt = RobotType.CANNON;
			break;
		}
		if (myRC.getEnergonLevel() - myRC.getMaxEnergonLevel()*mission.retainValue() > rt.spawnCost()) {
			myRC.spawn(rt);
			spawned++;
		}
	}

	private void performArmySend() throws GameActionException {
		if (spawned == combatMode.armySize()) {
			spawned = 0;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = Comms.MessageType.ATTACK;
			cmsg.loc = siegeLoc;
			comms.sendMessage(cmsg);
		}
	}
	
	private boolean performAttackCheck() throws GameActionException {
		if (checkEnemiesToAttack()) {
			mission = Mission.SIEGE;
			spawned = 0;

			boastCounter = boastCountdown;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = combatMode.msgType();
			cmsg.loc = siegeLoc;
			comms.sendMessage(cmsg);

			myRC.setIndicatorString(1, "We will crush you like an ant!");
			return true;
		} else
			return false;
	}
	
	private boolean performDefendCheck() throws GameActionException {
		if (checkEnemiesToAttack()) {
			mission = Mission.DEFEND_FLUX;
			spawned = 0;

			boastCounter = boastCountdown;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = Comms.MessageType.DEFENDING;
			cmsg.loc = myRC.getLocation();
			comms.sendMessage(cmsg);

			myRC.setIndicatorString(1, "We will hold our ground!");
			return true;
		} else
			return false; 
	}
	
	private boolean checkEnemiesToAttack() throws GameActionException{
		List<Robot> robots = new ArrayList<Robot>();
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		int seen = 0;
		for (Robot robot : robots) {
			RobotInfo info = myRC.senseRobotInfo(robot);			
			if ((info.team != myRC.getTeam())) {
				if ((info.type == RobotType.ARCHON) && // hard-coded, so what
						(info.location.distanceSquaredTo(myRC.getLocation()) < 40)) {
					siegeLoc = info.location;
					return true;
				}
				if (info.type == RobotType.SOLDIER)
					seen+=2;
				else if ((info.type == RobotType.CANNON) || (info.type == RobotType.CHANNELER))
					seen+=3;
				else
					seen++;
				if (seen >= 4){
					siegeLoc = info.location;
					return true;
				}
			}
		}
		siegeLoc = null;
		return false;
	}
	
	@Override
	public void feedRobots() throws GameActionException {
		List<Robot> robots = new ArrayList<Robot>();
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		doneFeeding = true;
		double threshold, chosenRobotLifetime = 100.0f, lifetime;
		Robot chosenRobot = null;
		RobotInfo chosenInfo = null;

		double myEnergonLevel = myRC.getEnergonLevel() - myRC.getMaxEnergonLevel() * mission.retainValue();
		for (Robot robot : robots) {
			RobotInfo info = myRC.senseRobotInfo(robot);
			if (info.team == myRC.getTeam()) {
				threshold = mission.feedValue(info.type);
				if ((info.type != RobotType.ARCHON) && (info.energonLevel <= threshold * info.maxEnergon))
					if (info.location.isAdjacentTo(myRC.getLocation()))
						doneFeeding = false;
				if (info.location.isAdjacentTo(myRC.getLocation()) || info.location.equals(myRC.getLocation()))
					if ((info.energonLevel < myEnergonLevel)
							&& (info.energonLevel <= threshold * info.maxEnergon)) {
						lifetime = info.energonLevel / info.type.energonUpkeep();
						if (lifetime < chosenRobotLifetime){
							chosenRobot = robot;
							chosenInfo = info;
							chosenRobotLifetime = lifetime;
						}
					}
			}
		}

		if (chosenRobot != null){
		double transferAmount = Math.min(ENERGON_RESERVE_SIZE
				- chosenInfo.energonReserve, myEnergonLevel);
		myRC.transferEnergon(transferAmount, chosenInfo.location,
				chosenRobot.getRobotLevel());
		}
	}

	private void goToFluxDeposit() throws GameActionException {
		if (nextDirection == Direction.NONE) {
			mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
			return;
		}
		if (nextDirection == Direction.OMNI) {
			mission = Mission.DRAIN_DEPOSIT;
			boastCounter = 0;
			return;
		}

		navigation.goInDirection(nextDirection);
	};
	
	private boolean checkFluxOkToTakeOver(FluxDepositInfo info) throws GameActionException {
		if (avoidedFlux.contains(info.location)){
			myRC.setIndicatorString(1, "Flux blacklisted, leave");
			return false;			
		}
		Robot r = myRC.senseAirRobotAtLocation(info.location);
		if (r == null){
			myRC.setIndicatorString(1, "No one over flux, get it!");
			return true;
		}
		if (r == myRC.getRobot()){
			myRC.setIndicatorString(1, "Directly over flux, happy!");
			return true;
		}
		RobotInfo ri = myRC.senseRobotInfo(r);
		if ((ri.team == myRC.getTeam()) && (ri.type == RobotType.ARCHON)){
			myRC.setIndicatorString(1, "Flux controlled by one of ours, leave");
			return false;
		}
		myRC.setIndicatorString(1, "Flux controlled by them! Get it!");
		return true;
	}

	private boolean locateNearestFlux() throws GameActionException {
		FluxDeposit[] fluxDeposits = myRC.senseNearbyFluxDeposits();
		for (FluxDeposit deposit : fluxDeposits) {
			FluxDepositInfo info = myRC.senseFluxDepositInfo(deposit);
			if (checkFluxOkToTakeOver(info)){
				nextDirection = myRC.getLocation().directionTo(info.location);
				fluxInfo = info;
				return true;
			}
		}
		return false;
	}

	public Direction findSpawnLocation(Direction start) throws GameActionException {
		MapLocation archonLoc = myRC.getLocation();
		Direction[] allDirections = navigation.getAllDirections(myRC.getDirection());
		for (Direction direction : allDirections) {
			MapLocation spawnLoc = archonLoc.add(direction);
			if (myRC.senseTerrainTile(spawnLoc).isTraversableAtHeight(RobotLevel.ON_GROUND))
				if (myRC.senseGroundRobotAtLocation(spawnLoc) == null)
					return direction;
		}
		return Direction.NONE;
	}

	private void checkArmedForces() throws GameActionException{
		for (int i = 0; i < armyDescriptors.length; i++) {
			ArmyDescriptor ad = armyDescriptors[i];
			if (vicinity.getCount(true, ad.rt) < ad.count) {
				Direction dir = findSpawnLocation(myRC.getDirection());
				if (dir == Direction.NONE)
					return;
				if (dir != myRC.getDirection()) {
					myRC.setDirection(dir);
					return;
				}
				if (myRC.getEnergonLevel() - myRC.getMaxEnergonLevel()
						* mission.retainValue() > ad.rt.spawnCost()) {
					myRC.spawn(ad.rt);
					return;
				}
			}
		}
	}
	
	private void defendFlux() throws GameActionException{
		if (!checkEnemiesToAttack()) {
			mission = Mission.FIND_FLUX_DEPOSIT;
			myRC.setIndicatorString(1, "");
			return;
		}
		combatMode = CombatMode.NONE;
		if (Clock.getRoundNum() % 2 == 0) {
			feedRobots();
		} else
			doneFeeding = true;
		if (doneFeeding) {
			performArmySpawn();
			performArmySend();
		}
		if (boastCounter <= 0) {
			boastCounter = boastCountdown;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = Comms.MessageType.DEFENDING;
			cmsg.loc = siegeLoc;
			comms.sendMessage(cmsg);
		}
		boastCounter--;
		checkDangerLevel();
	}
	
	private void siege() throws GameActionException{

		if (!checkEnemiesToAttack()){
			mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
			return;
		}
		if (combatMode == CombatMode.NONE)
			combatMode = CombatMode.STD;			
		if (Clock.getRoundNum() % 2 == 0){
			feedRobots();
		} else 
			doneFeeding = true;
		if (doneFeeding){
			performArmySpawn();
			performArmySend();
		}
		if (boastCounter <= 0) {
			boastCounter = boastCountdown;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = combatMode.msgType();				
			cmsg.loc = siegeLoc;
			comms.sendMessage(cmsg);
		}
		boastCounter--;
		checkDangerLevel();
	}
	
	private boolean checkArchonsDied() throws GameActionException{
		MapLocation[] newData = myRC.senseAlliedArchons();
		if (newData.length == alliedArchons.length){
			alliedArchons = newData;
			return false;
		}
		alliedArchons = newData;
		System.out.println("One of ours died!");
		return true;
	}
	
	private void checkDangerLevel() throws GameActionException{
		Map<Direction,Double> danger = new Hashtable<Direction, Double>();
		ArrayList<RobotInfo> enemies = vicinity.getInfo(false);
		MapLocation loc = myRC.getLocation();
		// calculate danger from enemies
		for (RobotInfo robotInfo : enemies) {
			double dist = loc.distanceSquaredTo(robotInfo.location);
			double range = robotInfo.type.attackRadiusMaxSquared();
			double input;
			switch(robotInfo.type){
			case CHANNELER:
				input = 4.0;
				break;
			case CANNON:
				input = 3.0;
				break;
			case ARCHON:
				input = 2.0;
				break;
			case SOLDIER:
				input = 4.0;
				break;
			case SCOUT:
				input = 0.5;
				break;
			default:
				input = 0.1;
			}
			Direction dir = loc.directionTo(robotInfo.location);
			Double was;
			if (!danger.containsKey(dir)){
				danger.put(dir, 0.0);
				was = 0.0;
			} else was = danger.get(dir);
			if (dist < range)
				danger.put(dir,was+input);
			else
				danger.put(dir,was + input * range / dist);
		}
		// sum up danger levels
		Direction worstDir = null;
		Double worstDanger = null;
		double totalDanger = 0.0; 
		for (Direction dir : danger.keySet()){
			totalDanger += danger.get(dir);
			if (worstDir == null) {
				worstDir = dir;
				worstDanger = danger.get(dir);
			}
			else
				if (worstDanger < danger.get(dir)){
					worstDir = dir;
					worstDanger = danger.get(dir);
				}
		}
		// calculate threshold
		double threshold;
		if (mission.isDraining())
			threshold = 5.0;
		else
			threshold = 4.0;
		if (myRC.getEnergonLevel() < myRC.getMaxEnergonLevel() * 0.2)
			threshold -= 1.0;
		if (myRC.getEnergonLevel() < myRC.getMaxEnergonLevel() * 0.1)
			threshold -= 1.5;
		// decision?
		if (worstDir!=null){
			if (totalDanger >= threshold){ // run away!
				navigation.goInDirection(worstDir.opposite());
			} 
		}
	}
}
