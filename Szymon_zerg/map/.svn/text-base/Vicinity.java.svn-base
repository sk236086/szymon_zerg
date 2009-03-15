package Szymon_zerg.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Vicinity {
	private Map<RobotType, Integer> friendlyNearby = new Hashtable<RobotType, Integer>();
	private int friendlyCount = 0;
	private Map<RobotType, Integer> hostileNearby = new Hashtable<RobotType, Integer>();
	private int hostileCount = 0;
	private ArrayList<RobotInfo> friendlyInfo = new ArrayList<RobotInfo>();
	private ArrayList<RobotInfo> hostileInfo = new ArrayList<RobotInfo>();
	
	private RobotController myRC;
	
	private int lastRefresh = 0;
	
	public Vicinity(RobotController rc){
		myRC = rc;
	}
	
	private void performRefresh() throws GameActionException{
		friendlyCount = 0;
		hostileCount = 0;
		friendlyNearby.clear();
		hostileNearby.clear();
		hostileInfo.clear();
		friendlyInfo.clear();
		
		List<Robot> robots = new ArrayList<Robot>();
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		
		for (Robot robot : robots) {
			RobotInfo info = myRC.senseRobotInfo(robot);
			RobotType rt = info.type;
			if (info.team == myRC.getTeam()) { // friendly
				friendlyCount++;
				Integer res = friendlyNearby.get(rt);
				if (res == null)
					friendlyNearby.put(rt, 1);
				else
					friendlyNearby.put(rt,res+1);
				friendlyInfo.add(info);
			} else { // hostile
				hostileCount++;
				Integer res = hostileNearby.get(rt);
				if (res == null)
					hostileNearby.put(rt, 1);
				else
					hostileNearby.put(rt,res+1);
				hostileInfo.add(info);
			}
		}
	}
	
	public ArrayList<RobotInfo> getInfo(boolean friendly) throws GameActionException{
		int time = Clock.getRoundNum();
		if (time > lastRefresh){
			lastRefresh = time;
			performRefresh();
		}
		if (friendly)
			return friendlyInfo;
		else
			return hostileInfo;
	}
	
	public int getCount(boolean friendly) throws GameActionException{
		return getCount(friendly,null);
	}
	
	public int getCount(boolean friendly, RobotType rt) throws GameActionException{
		int time = Clock.getRoundNum();
		if (time > lastRefresh){
			lastRefresh = time;
			performRefresh();
		}
		if (rt==null){
			if (friendly)
				return friendlyCount;
			else
				return hostileCount;
		} else {
			if (friendly){
				Integer res = friendlyNearby.get(rt);
				if (res!=null)
					return res;
				else
					return 0;
			} else {
				Integer res = hostileNearby.get(rt);
				if (res!=null)
					return res;
				else
					return 0;				
			}
		}
	}
}
