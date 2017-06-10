/*
 * ModellingAI
 * Version 0.1
 * Last update on 27 April 2017
 * Made using FightingICE version 3.10
 */

import commandcenter.CommandCenter;
import gameInterface.AIInterface;
import simulator.Simulator;
import structs.FrameData;
import structs.GameData;
import structs.Key;
import structs.CharacterData;

public class ModellingAI implements AIInterface {


	private Key inputKey;
	private boolean player;
	private FrameData frameData;
	private CommandCenter cc;
	private Simulator simulator;
	private GameData gd;
	
	private int distance;
	private int energy;
	private CharacterData opp;
	private CharacterData my;
	private boolean isGameJustStarted;
	private int xDifference;
	
	
	public void close() {
		// Nothing to do here
	}

	
	public String getCharacter() {
		// Select the player ZEN as per competition rules
		return CHARACTER_ZEN;
	}

	
	public void getInformation(FrameData frameData) {
		// Load the frame data every time getInformation gets called
		this.frameData = frameData;
	}

	
	public int initialize(GameData arg0, boolean player) {
		// Initialize the global variables at the start of the round
		inputKey = new Key();
		this.player = player;
		frameData = new FrameData();
		cc = new CommandCenter();	
		gd = arg0;
		simulator = gd.getSimulator();
		isGameJustStarted = true;
		return 0;
	}

	
	public Key input() {
		// The input is set up to the global variable inputKey
		// which is modified in the processing part
		return inputKey;
	}

	
	public void processing() {
		// First we check whether we are at the end of the round
		if(!frameData.getEmptyFlag() && frameData.getRemainingTime()>0){
			// Simulate the delay and look ahead 2 frames. The simulator class exists already in FightingICE
			if (!isGameJustStarted)
				frameData = simulator.simulate(frameData, this.player, null, null, 17); //17 is one frame time.
			else
				isGameJustStarted = false; //if the game just started, no point on simulating
			
			cc.setFrameData(frameData, player);
			
			distance = cc.getDistanceX();
			energy = frameData.getMyCharacter(player).getEnergy();
			my = cc.getMyCharacter();
			opp = cc.getEnemyCharacter();
			xDifference = my.x - opp.x;
			
		}
		else isGameJustStarted = true;
	}
}
