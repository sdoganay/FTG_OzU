import enumerate.Action;
import enumerate.State;
import fighting.Attack;
import gameInterface.AIInterface;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import simulator.Simulator;
import structs.CharacterData;
import structs.FrameData;
import structs.GameData;
import structs.Key;
import structs.MotionData;
import commandcenter.CommandCenter;

public class Ranezi_logger implements AIInterface {

	private enum JumpState {
		RISING, PEAK, FALLING, ON_GROUND
	};

	
	//TODO: al -----------
	private CommandCenter cc;
	private FrameData fd;
	LogWriter logger;
	private static final String AGENT_NAME = "Ranezi";
	// -----------------
	
	private Simulator simulator;
	private Key key;
	private CommandCenter commandCenter;
	private boolean playerNumber;
	private GameData gameData;

	private FrameData frameData;

	private FrameData simulatorAheadFrameData;

	private LinkedList<Action> myActions;

	private LinkedList<Action> oppActions;

	private LinkedList<Integer> oppLastYPos;

	private LinkedList<Action> oppLastMoves;

	private CharacterData myCharacter;

	private CharacterData oppCharacter;

	private static final int FRAME_AHEAD = 14;

	private Vector<MotionData> myMotion;

	private Vector<MotionData> oppMotion;

	private Action[] actionAir;

	private Action[] actionGround;

	private Action spSkill;

	private Node rootNode;

	public static final boolean DEBUG_MODE = false;

	public void close() {
		//TODO:al
		logger.close();
	}

	public String getCharacter() {
		return CHARACTER_ZEN;
	}

	public void getInformation(FrameData frameData) {
		this.frameData = frameData;
		this.commandCenter.setFrameData(this.frameData, playerNumber);

		if (playerNumber) {
			myCharacter = frameData.getP1();
			oppCharacter = frameData.getP2();
		} else {
			myCharacter = frameData.getP2();
			oppCharacter = frameData.getP1();
		}
	}

	public int initialize(GameData gameData, boolean playerNumber) {
		this.playerNumber = playerNumber;
		this.gameData = gameData;

		this.key = new Key();
		this.frameData = new FrameData();
		this.commandCenter = new CommandCenter();


		this.myActions = new LinkedList<Action>();
		this.oppActions = new LinkedList<Action>();
		this.oppLastYPos = new LinkedList<Integer>();
		this.oppLastMoves = new LinkedList<Action>();

		simulator = gameData.getSimulator();

		actionAir = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DA, Action.AIR_DB,
				Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB, Action.AIR_F_D_DFA, Action.AIR_F_D_DFB,
				Action.AIR_D_DB_BA, Action.AIR_D_DB_BB, Action.AIR_D_DF_FB, Action.AIR_D_DF_FA };

		actionGround = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP, Action.FORWARD_WALK, Action.DASH,
				Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD, Action.CROUCH_GUARD, Action.THROW_A,
				Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA,
				Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB,
				Action.STAND_F_D_DFA, Action.STAND_F_D_DFB, Action.STAND_D_DB_BB };
		spSkill = Action.STAND_D_DF_FC;

		myMotion = this.playerNumber ? gameData.getPlayerOneMotion() : gameData.getPlayerTwoMotion();
		oppMotion = this.playerNumber ? gameData.getPlayerTwoMotion() : gameData.getPlayerOneMotion();

		//TODO: al -------------
		this.fd = new FrameData();
		this.cc = new CommandCenter();
		logger = new LogWriter(AGENT_NAME+"_"+gameData.getMyName(playerNumber) + "_"
				+ gameData.getOpponentName(playerNumber));
		// --------------------
		
		return 0;
	}

	public Key input() {
		return key;
	}

	public void processing() {

		if (canProcessing()) {
			if (commandCenter.getskillFlag()) {
				key = commandCenter.getSkillKey();
			} else {
				key.empty();
				commandCenter.skillCancel();

				mctsPrepare();
				rootNode = new Node(simulatorAheadFrameData, null, myActions, oppActions, gameData, playerNumber,
						commandCenter);
				rootNode.createNode();

				Action bestAction = rootNode.mcts();
				if (Ranezi_logger.DEBUG_MODE) {
					rootNode.printNode(rootNode);
				}
				commandCenter.commandCall(bestAction.name());
			}
			
			//TODO: al ---------------------------
			// Simulate the delay and look ahead 2 frames. The simulator class exists already in FightingICE
			fd = simulator.simulate(frameData, this.playerNumber, null, null, 17); //17 is one frame time
			cc.setFrameData(fd, this.playerNumber);
			
			CharacterData my = cc.getMyCharacter();
			CharacterData opp = cc.getEnemyCharacter();
						
			logger.writeLine(""+my.x+","+my.y+","+my.energy+","+my.hp+","+my.action+","+
					opp.x+","+opp.y+","+opp.energy+","+opp.hp+","+opp.action+","+ frameData.getFrameNumber());
			//-------------------
		}

	}

	public boolean canProcessing() {
		return !frameData.getEmptyFlag() && frameData.getRemainingTime() > 0;
	}

	public void mctsPrepare() {
		simulatorAheadFrameData = simulator.simulate(frameData, playerNumber, null, null, FRAME_AHEAD);

		myCharacter = playerNumber ? simulatorAheadFrameData.getP1() : simulatorAheadFrameData.getP2();
		oppCharacter = playerNumber ? simulatorAheadFrameData.getP2() : simulatorAheadFrameData.getP1();
		oppLastYPos.add(oppCharacter.getY());
		if (oppLastYPos.size() > 3) {
			oppLastYPos.removeFirst();
		}

		oppLastMoves.add(oppCharacter.getAction());
		if (oppLastMoves.size() > 10)
			oppLastMoves.removeFirst();

		setMyAction();
		setOppAction();
	}

	/*
	 * Check whether the enemy has launched at least one projectile
	 */
	private boolean checkForProjectile() {
		boolean opponentProjectile = false;
		int ownNumber = (this.playerNumber) ? 0 : 1;
		Iterator<Attack> itr = frameData.getAttack().iterator();
		while (itr.hasNext() && !opponentProjectile) {
			Attack current = itr.next();
			if (current.getPlayerNumber() != ownNumber)
				opponentProjectile = true;
		}
		return opponentProjectile;
	}
	
	/*
	 * Check whether the enemy is at the start or the middle or the end of his
	 * jump process
	 */
	private JumpState oppFindJumpState() {
		if (oppLastYPos.size() < 3)
			return JumpState.ON_GROUND;
		int currY = oppLastYPos.get(2);
		int lastY = oppLastYPos.get(1);
		int last2Y = oppLastYPos.get(0);

		if (oppCharacter.getState() != State.AIR)
			return JumpState.ON_GROUND;

		if (lastY <= last2Y) {
			if (currY < lastY)
				return JumpState.RISING;
			else if (currY > lastY)
				return JumpState.PEAK;
		} else {
			return JumpState.FALLING;
		}

		return JumpState.ON_GROUND;
	}

	public void setMyAction() {
		myActions.clear();

		int distanceX = commandCenter.getDistanceX();
		boolean opponentProjectile = checkForProjectile();
		JumpState oppJumpState = oppFindJumpState();

		int energy = myCharacter.getEnergy();

		if (myCharacter.getState() == State.AIR) {
			for (int i = 0; i < actionAir.length; i++) {
				if (Math.abs(myMotion.elementAt(Action.valueOf(actionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					myActions.add(actionAir[i]);
				}
			}
		} else {

			if (Math.abs(
					myMotion.elementAt(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
				myActions.add(spSkill);
			}
			// Check whether there is any projectile and they are not too close
			// to each other
			if (opponentProjectile && distanceX > gameData.getStageXMax() / 4) {
				myActions.add(Action.BACK_JUMP);
			}
			// Check whether there is any projectile and they are not too close
			// to each other
			else if (opponentProjectile && distanceX > gameData.getStageXMax() / 6) {

				myActions.add(Action.FOR_JUMP);
				// Check whether the players are far away to each other
			} else if (distanceX > gameData.getStageXMax() / 2) {
				myActions.add(Action.FORWARD_WALK);

			}
			// Check whether the enemy jumps up
			else if (oppJumpState == JumpState.RISING) {
				myActions.add(Action.STAND_FB);
				myActions.add(Action.STAND_F_D_DFA);

			}

			else {
				for (int i = 0; i < actionGround.length; i++) {
					if (Math.abs(myMotion.elementAt(Action.valueOf(actionGround[i].name()).ordinal())
							.getAttackStartAddEnergy()) <= energy) {
						myActions.add(actionGround[i]);
					}
				}
			}
		}

	}

	public void setOppAction() {
		oppActions.clear();

		int energy = oppCharacter.getEnergy();

		if (oppCharacter.getState() == State.AIR) {
			for (int i = 0; i < actionAir.length; i++) {
				if (Math.abs(oppMotion.elementAt(Action.valueOf(actionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(actionAir[i]);
				}
			}
		} else {
			if (Math.abs(oppMotion.elementAt(Action.valueOf(spSkill.name()).ordinal())
					.getAttackStartAddEnergy()) <= energy) {
				oppActions.add(spSkill);
			}

			for (int i = 0; i < actionGround.length; i++) {
				if (Math.abs(oppMotion.elementAt(Action.valueOf(actionGround[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					oppActions.add(actionGround[i]);
				}
			}
		}
	}
}
