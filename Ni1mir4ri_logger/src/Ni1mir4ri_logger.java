import java.util.Random;

import commandcenter.CommandCenter;
import enumerate.Action;
import gameInterface.AIInterface;
import simulator.Simulator;
import structs.CharacterData;
import structs.FrameData;
import structs.GameData;
import structs.Key;
import structs.MotionData;

public class Ni1mir4ri_logger implements AIInterface {

	Key inputKey;
	FrameData  fd;
	CommandCenter cc;
	boolean playerNumber;
	boolean p;
	GameData gd;
	
	Random random = new Random();

	//TODO: al -----------
	private CommandCenter cc_log;
	private FrameData fd_log;
	LogWriter logger;
	private static final String AGENT_NAME = "Ni1mir4ri";
	// -----------------
	
	boolean use = true;
	int AreaXL=0, AreaXR=0, AreaYT=0, AreaYB=0;
	int MyX=0, MyY=0, OppX=0, OppY=0, EnemyL=0,EnemyR=0, EnemyT=0, MyL=0;;
	int jumpFrameCounter = 0;
	int edgeEscape = 0;
	double jumpFlag = 0, otherFlag=0, jumpRate=0;
	int distance = 0, myenergy = 0;
	String mymotion;
	String oppmotion;
	boolean stopflag = true;
	int round = -1;
	double hprate = 0;
	String command;
	
	public void close() {
		// TODO Auto-generated method stub

		logger.close();
	}

	public String getCharacter() {
		// TODO Auto-generated method stub
		return CHARACTER_ZEN;
	}

	public void getInformation(FrameData frameData) {
		// TODO Auto-generated method stub
		fd = frameData;
		cc.setFrameData(frameData, p);

	}

	public int initialize(GameData gameData, boolean playerNumber) {
		// TODO Auto-generated method stub
		gd = gameData;
		p = playerNumber;
		inputKey = new Key();
		fd = new FrameData();
		cc = new CommandCenter();
		
		//TODO: al -------------
				this.fd_log = new FrameData();
				this.cc_log = new CommandCenter();
				logger = new LogWriter(AGENT_NAME+"_"+gameData.getMyName(playerNumber) + "_"
						+ gameData.getOpponentName(playerNumber));
				// --------------------
				
		return 0;
	}

	public Key input() {
		// TODO Auto-generated method stub
		return inputKey;
	}

	public void processing() {
		// TODO Auto-generated method stub
		if(!fd.getEmptyFlag() && fd.getRemainingTime() > 0)	//NullPointerException�� ���� ����. �׻� Ȯ���ؾ� �Ѵ�.
		{
			
			
			if(cc.getskillFlag())	//��ų �Է������� Ȯ��.
			{
				inputKey = cc.getSkillKey();	//���� Ű �����͸� ����.
			}
			else
			{
				Action oppAct = cc.getEnemyCharacter().getAction();	//�� ĳ������ ���� ��ü�� ����
				Action myAct = cc.getMyCharacter().getAction();
				
				MotionData oppMotion = new MotionData();	//��� ���� ��ü ����
				MotionData myMotion = new MotionData();	//��� ���� ��ü ����
				
				if(p)
				{
					//�÷��̾�1�� ���
					oppMotion = (MotionData) gd.getPlayerTwoMotion().elementAt(oppAct.ordinal());	
					myMotion = (MotionData) gd.getPlayerOneMotion().elementAt(myAct.ordinal());
				}
				else
				{
					//�÷��̾�2�� ���
					oppMotion = (MotionData) gd.getPlayerOneMotion().elementAt(oppAct.ordinal());	//p1�� ��� �����͸� ��´�
					myMotion = (MotionData) gd.getPlayerTwoMotion().elementAt(myAct.ordinal());	//p2�� ��� �����͸� ��´�
				}
				
				//�ڽ��� ���� ��ġ ����
				MyX = cc.getMyX();
				MyY = cc.getMyY();
				
				
				//���� ��ġ�� ����
				OppX = cc.getEnemyX();
 				OppY = cc.getEnemyY();
 				
 				
 				EnemyL = cc.getEnemyCharacter().getLeft();	//���� ��Ʈ �ڽ� ���� ���� ��ġ X�� ����
 				EnemyR = cc.getEnemyCharacter().getRight();	//���� ��Ʈ �ڽ� ���� ������ ��ġ X�� ����
 				EnemyT = cc.getEnemyCharacter().getTop();
 				
 				MyL = cc.getMyCharacter().getLeft();
 				
 				distance = cc.getDistanceX();
 				
 				mymotion = myMotion.getMotionName();
 				oppmotion = oppMotion.getMotionName();
 				
 				myenergy = cc.getMyEnergy();
 				
 				hprate = (cc.getMyHP()-1.0) / (cc.getEnemyHP()-1.0);
 				
				inputKey.empty();	//Ű�� �ȴ��� ���·� ����
				//��ų �����Ϳ� ��ų �÷��׸� ����.
				cc.skillCancel();
				
		
				
				System.out.println("distance : " + distance + " action : " + mymotion);
				
				
				
				System.out.println("round : " + round);
				System.out.println("myL : " + MyL + " oppL : " + EnemyL);
				
				
				System.out.println(hprate);
				if((fd.getRemainingTime() < 30000) && (hprate > 3.0))
				{
					
					if (distance >= 280 && myenergy < 30)
					{
						command = "FOR_JUMP";
						
						if (mymotion.equals("AIR"))
						{
							inputKey.B = true;
						}
						
					}
					else if ((distance >= 280) && (myenergy >= 30) && (distance < 535))
					{
					
						if (oppmotion.equals("AIR"))
						{
							cc.commandCall("STAND_D_DF_FB");
						}
						else
						{
							cc.commandCall("STAND_D_DF_FA");
						}
						
					}
					else
					{
						command = "CROUCH_B";
						if (cc.getMyCharacter().isFront() == true)
							inputKey.R = true;
						else
							inputKey.L = true;

						if (distance >= 50 && distance < 53)
						{
							cc.commandCall("CROUCH_FA");
						}
						
						if (distance >= 53 && distance < 280)
						{
							command = "FOR_JUMP";
							inputKey.B = true;
						}

						if (myenergy >= 300 && distance < 535 && !oppmotion.equals("AIR"))
						{
							command = "STAND_D_DF_FC";
						}

					}
					
					cc.commandCall(command);
				}
				else
				{
					//withdraw as soon as the game is started
					if(stopflag || round != fd.getRound())
					{
						if(round != fd.getRound())
						{
							cc.commandCall("STAND_D_DF_FA");
							stopflag = true;
							round++;
						}
						else if(distance==535)	//distance when the game was started
						{
							cc.commandCall("7");	//jump to back
						}
						else
						{
							stopflag = false;
						}
					}		
					//If a opponent moves
					else
					{
						//��ǳ ���ϱ�
						if(oppmotion.equalsIgnoreCase("STAND_D_DF_FC") || oppmotion.equalsIgnoreCase("STAND_D_DF_FA")
								|| oppmotion.equalsIgnoreCase("STAND_D_DF_FB") && distance<540)
						{
							
							if(MyL > EnemyL && EnemyL > 300)
							{
								System.out.println("case 1");
								cc.commandCall("9");
							}
							else if(MyL < EnemyL && EnemyL < 600)
							{
								System.out.println("case 2");
								cc.commandCall("9");
							}
							else if(distance <= 357)
							{
								System.out.println("case 3");
								cc.commandCall("9");
							}
							else if(distance > 357 && distance <= 540)
							{	
								System.out.println("case 4");
								cc.commandCall("7");
							}
						}
					
					
						//��ǳ ������
						if(distance >=150 && distance < 535 && myenergy>=300)
						{
							cc.commandCall("STAND_D_DF_FC");
						}
						
						if(distance >=150 && distance < 535 && myenergy>=30 && !mymotion.contains("AIR"))
						{
							cc.commandCall("STAND_D_DF_FB");
						}
						//��ǳ���
						if(distance >=300 && !oppmotion.equalsIgnoreCase("AIR"))
						{
							cc.commandCall("STAND_D_DF_FA");
						}
				
				
				
						//���� ������
						if(distance <300)
						{	
							if(myenergy >=30 && MyX<680 && MyX>-91 && (random.nextInt()%4 != 0))
							{
									cc.commandCall("7");
							}
							
							if(!mymotion.contains("AIR"))
							{
									cc.commandCall("9");
							}
							else
							{
								cc.commandCall("B");
							}
						}
					}
					
					
				}
			}
			
			// Simulate the delay and look ahead 2 frames. The simulator class exists already in FightingICE
			Simulator simulator = gd.getSimulator();
			fd_log = simulator.simulate(fd, this.playerNumber, null, null, 17); //17 is one frame time
			cc_log.setFrameData(fd_log, this.playerNumber);
			
			CharacterData my = cc_log.getMyCharacter();
			CharacterData opp = cc_log.getEnemyCharacter();
						
			logger.writeLine(""+my.x+","+my.y+","+my.energy+","+my.hp+","+my.action+","+
					opp.x+","+opp.y+","+opp.energy+","+opp.hp+","+opp.action+","+ fd_log.getFrameNumber());
			//-------------------
		}
	}

}
