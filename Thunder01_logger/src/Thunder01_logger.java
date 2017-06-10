import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;

import commandcenter.CommandCenter;
import enumerate.Action;
import enumerate.State;
import gameInterface.AIInterface;
import simulator.Simulator;
import structs.CharacterData;
import structs.FrameData;
import structs.GameData;
import structs.Key;
import structs.MotionData;

/**
 * EMCTS: Extended MCTS(Incorporate Motion of Opponent to MCT and Consider Energy)
 * ZEN: Machete plus EMCTS
 * GARNET: EMCTS
 * LUD:EMCTS (Consider Distance)
 *
 * @author Eita Aoki
 */
public class Thunder01_logger implements AIInterface {

  private Simulator simulator;
  private Key key;
  private CommandCenter commandCenter;
  private boolean playerNumber;
  private GameData gameData;
//TODO: al -----------
	private CommandCenter cc;
	private FrameData fd;
	LogWriter logger;
	private static final String AGENT_NAME = "Thunder01";
	// -----------------
  /** 大本のFrameData */
  private FrameData frameData;

  /** 大本よりFRAME_AHEAD分遅れたFrameData */
  private FrameData simulatorAheadFrameData;

  /** 自分が行える行動全て */
  private LinkedList<Action> myActions;

  /** 相手が行える行動全て */
  private LinkedList<Action> oppActions;

  /** 自分の情報 */
  private CharacterData myCharacter;

  /** 相手の情報 */
  private CharacterData oppCharacter;

  /** フレームの調整用時間(JerryMizunoAIを参考) */
  private static final int FRAME_AHEAD = 14;

  private Vector<MotionData> myMotion;

  private Vector<MotionData> oppMotion;

  private Action[] actionAir;

  private Action[] actionGround;

  private Action spSkill;

  private ExtendedNode rootNode;

  /** デバッグモードであるかどうか。trueの場合、様々なログが出力される */
  public static final boolean DEBUG_MODE = false;

  private CharacterName charName;
  private long timeOne;

  public void close() {
		logger.close();
  }

  public String getCharacter() {
    return CHARACTER_ZEN;
  }

  public void getInformation(FrameData frameData) {
	  timeOne = (new Date()).getTime();
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

    simulator = gameData.getSimulator();
    System.out.println("thunder:"+gameData.getMyName(playerNumber));
    
    actionAir =
        new Action[] {Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DA, Action.AIR_DB,
            Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB, Action.AIR_D_DF_FA,
            Action.AIR_D_DF_FB, Action.AIR_F_D_DFA, Action.AIR_F_D_DFB, Action.AIR_D_DB_BA,
            Action.AIR_D_DB_BB};
    
    actionGround =
        new Action[] {Action.STAND_D_DB_BA, Action.BACK_STEP, Action.FORWARD_WALK, Action.DASH,
            Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD,
            Action.CROUCH_GUARD, Action.THROW_A, Action.THROW_B, Action.STAND_A, Action.STAND_B,
            Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA, Action.STAND_FB, Action.CROUCH_FA,
            Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB, Action.STAND_F_D_DFA,
            Action.STAND_F_D_DFB, Action.STAND_D_DB_BB};
    
    spSkill = Action.STAND_D_DF_FC;

    myMotion = this.playerNumber ? gameData.getPlayerOneMotion() : gameData.getPlayerTwoMotion();
    oppMotion = this.playerNumber ? gameData.getPlayerTwoMotion() : gameData.getPlayerOneMotion();

    String tmpcharname=this.gameData.getMyName(this.playerNumber);
    
    if(tmpcharname.equals(CHARACTER_ZEN))charName=CharacterName.ZEN;
    else if(tmpcharname.equals(CHARACTER_GARNET))charName=CharacterName.GARNET;
    else if(tmpcharname.equals(CHARACTER_LUD))charName=CharacterName.LUD;
    else charName=CharacterName.OTHER;

  //TODO: al -------------
  		this.fd = new FrameData();
  		this.cc = new CommandCenter();
  		logger = new LogWriter(AGENT_NAME+"_"+gameData.getMyName(playerNumber) + "_"
  				+ gameData.getOpponentName(playerNumber));
  		// --------------------
    
    return 0;
  
  }

   
  public Key input() {

	  long timeTwo = (new Date()).getTime();
	  System.out.println("time for thunder01: ");
    return key;
  }

  private void mctsProcessing(){
	  rootNode =
	            new ExtendedNode(charName,myCharacter.getEnergy(),oppCharacter.getEnergy(), simulatorAheadFrameData, null, myActions, oppActions, gameData, playerNumber,
	                commandCenter);
	        rootNode.createNode();

	        Action bestAction = rootNode.mcts(); // MCTSの実行


	        commandCenter.commandCall(bestAction.name()); // MCTSで選択された行動を実行する
  }
  
  private void zenProcessing(){
	  FrameData tmpFrameData = simulator.simulate(frameData, this.playerNumber, null, null, 17);
	  CommandCenter cc=this.commandCenter;
	  cc.setFrameData(tmpFrameData, playerNumber);
		int distance = cc.getDistanceX();
		int energy = frameData.getMyCharacter(playerNumber).getEnergy();
		CharacterData my = cc.getMyCharacter();
		CharacterData opp = cc.getEnemyCharacter();
		int xDifference = my.x - opp.x;

	  if ((opp.energy >= 300) && ((my.hp - opp.hp) <= 300))
			cc.commandCall("FOR_JUMP _B B B");
			// if the opp has 300 of energy, it is dangerous, so better jump!!
			// if the health difference is high we are dominating so we are fearless :)
		else if (!my.state.equals(State.AIR) && !my.state.equals(State.DOWN)) { //if not in air
			if ((distance > 150)) {
				cc.commandCall("FOR_JUMP"); //If its too far, then jump to get closer fast
			}
			else if (energy >= 300)
				cc.commandCall("STAND_D_DF_FC"); //High energy projectile
			else if ((distance > 100) && (energy >= 50))
				cc.commandCall("STAND_D_DB_BB"); //Perform a slide kick
			else if (opp.state.equals(State.AIR)) //if enemy on Air
				cc.commandCall("STAND_F_D_DFA"); //Perform a big punch
			else if (distance > 100)
				this.mctsProcessing();
				//cc.commandCall("6 6 6"); // Perform a quick dash to get closer
			else
				this.mctsProcessing();
				//cc.commandCall("B"); //Perform a kick in all other cases, introduces randomness
		}
		else if ((distance <= 150) && (my.state.equals(State.AIR) || my.state.equals(State.DOWN))
				&& (((gameData.getStageXMax() - my.x)>=200) || (xDifference > 0))
				&& ((my.x >=200) || xDifference < 0)) { //Conditions to handle game corners
			if (energy >= 5)
				this.mctsProcessing();
				//cc.commandCall("AIR_DB"); // Perform air down kick when in air
			else
				this.mctsProcessing();
				//cc.commandCall("B"); //Perform a kick in all other cases, introduces randomness
		}
		else
			this.mctsProcessing();
			//cc.commandCall("B"); //Perform a kick in all other cases, introduces randomness
  }

  private void garnetProcessing(){
	  this.mctsProcessing();
  }


  private boolean printnameflag=true;
   
  public void processing() {

	  if (canProcessing()) {
		  if (commandCenter.getskillFlag()) {
			  key = commandCenter.getSkillKey();
		  } else {
			  key.empty();
			  commandCenter.skillCancel();

			  mctsPrepare(); // MCTSの下準備を行う

			  if(charName==CharacterName.ZEN){
				  zenProcessing();
				  if(printnameflag)System.out.println("zenProcessing");
			  }else if(charName==CharacterName.GARNET){
				  garnetProcessing();
				  if(printnameflag)System.out.println("garnetProcessing");
			  }
			  else{
				  if(printnameflag)System.out.println("elseProcessing");
				  mctsProcessing();
			  }
			  printnameflag=false;

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

  /**
   * AIが行動できるかどうかを判別する
   *
   * @return AIが行動できるかどうか
   */
  public boolean canProcessing() {
    return !frameData.getEmptyFlag() && frameData.getRemainingTime() > 0;
  }

  /**
   * MCTSの下準備 <br>
   * 14フレーム進ませたFrameDataの取得などを行う
   */
  public void mctsPrepare() {
    simulatorAheadFrameData = simulator.simulate(frameData, playerNumber, null, null, FRAME_AHEAD);

    myCharacter = playerNumber ? simulatorAheadFrameData.getP1() : simulatorAheadFrameData.getP2();
    oppCharacter = playerNumber ? simulatorAheadFrameData.getP2() : simulatorAheadFrameData.getP1();

    setMyAction();
    setOppAction();
  }

  public void setMyAction() {
    myActions.clear();

    int energy = myCharacter.getEnergy();

    if (myCharacter.getState() == State.AIR) {
      for (int i = 0; i < actionAir.length; i++) {
        if (Math.abs(myMotion.elementAt(Action.valueOf(actionAir[i].name()).ordinal())
            .getAttackStartAddEnergy()) <= energy) {
          myActions.add(actionAir[i]);
        }
      }
    } else {
      if (Math.abs(myMotion.elementAt(Action.valueOf(spSkill.name()).ordinal())
          .getAttackStartAddEnergy()) <= energy) {
        myActions.add(spSkill);
      }

      for (int i = 0; i < actionGround.length; i++) {
        if (Math.abs(myMotion.elementAt(Action.valueOf(actionGround[i].name()).ordinal())
            .getAttackStartAddEnergy()) <= energy) {
          myActions.add(actionGround[i]);
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
