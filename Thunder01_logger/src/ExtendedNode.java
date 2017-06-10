import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import commandcenter.CommandCenter;
import enumerate.Action;
import simulator.Simulator;
import structs.CharacterData;
import structs.FrameData;
import structs.GameData;
import structs.MotionData;

/**
 * MCTSで利用するNode
 * 
 * @author Eita Aoki
 */

public class ExtendedNode {

	/** UCTの実行時間 UCT execution time */
	public static final int UCT_TIME = 165 * 100000;

	/** UCB1の定数Cの値 The value of constant C of UCB1 */
	public static final double UCB_C = 3;

	/** 探索する木の深さ Depth of tree to search */
	// public static final int UCT_TREE_DEPTH = 2;
	public static final int UCT_TREE_DEPTH = 1;

	/** ノードを生成する閾値 */
	// public static final int UCT_CREATE_NODE_THRESHOULD = 10;
	public static final int UCT_CREATE_NODE_THRESHOULD = 5;

	/** シミュレーションを行う時間 */
	public static final int SIMULATION_TIME = 60;

	/** 乱数を利用するときに使う */
	private Random rnd;

	/** 親ノード */
	private ExtendedNode parent;

	/** 子ノード */
	private ExtendedNode[][] children;

	/** ノードの深さ */
	private int depth;

	/** ノードが探索された回数 */
	private int games;

	/** 評価値 */
	private double score;

	/** 選択できる自分の全Action */
	private LinkedList<Action> myActions;

	/** 選択できる相手の全Action */
	private LinkedList<Action> oppActions;

	/** シミュレーションするときに利用する */
	private Simulator simulator;

	/** 探索時に選んだ自分のAction */
	private LinkedList<Action> selectedMyActions;

	/** 探索時に選んだ敵のAction */
	private LinkedList<Action> selectedOppActions;

	/** シミュレーションする前の自分のHP */
	private int myOriginalHp;

	/** シミュレーションする前の相手のHP */
	private int oppOriginalHp;

	private FrameData frameData;
	private boolean playerNumber;
	private CommandCenter commandCenter;
	private GameData gameData;

	private boolean isCreateNode;

	Deque<Action> mAction;
	Deque<Action> oppAction;

	private int myEnergy;
	private int oppEnergy;
	CharacterName charName;

	public ExtendedNode(CharacterName charName, int myEnergy, int oppEnergy,
			FrameData frameData, ExtendedNode parent,
			LinkedList<Action> myActions, LinkedList<Action> oppActions,
			GameData gameData, boolean playerNumber,
			CommandCenter commandCenter, LinkedList<Action> selectedMyActions,
			LinkedList<Action> selectedOppActions) {
		this(charName, myEnergy, oppEnergy, frameData, parent, myActions,
				oppActions, gameData, playerNumber, commandCenter);

		this.selectedMyActions = selectedMyActions;
		this.selectedOppActions = selectedOppActions;

	}

	public ExtendedNode(CharacterName charName, int myEnergy, int oppEnergy,
			FrameData frameData, ExtendedNode parent,
			LinkedList<Action> myActions, LinkedList<Action> oppActions,
			GameData gameData, boolean playerNumber, CommandCenter commandCenter) {
		this.charName = charName;
		this.myEnergy = myEnergy;
		this.oppEnergy = oppEnergy;
		this.frameData = frameData;
		this.parent = parent;
		this.myActions = myActions;
		this.oppActions = oppActions;
		this.gameData = gameData;
		this.simulator = new Simulator(gameData);
		this.playerNumber = playerNumber;
		this.commandCenter = commandCenter;

		this.selectedMyActions = new LinkedList<Action>();
		this.selectedOppActions = new LinkedList<Action>();

		this.rnd = new Random();
		this.mAction = new LinkedList<Action>();
		this.oppAction = new LinkedList<Action>();

		CharacterData myCharacter = playerNumber ? frameData.getP1()
				: frameData.getP2();
		CharacterData oppCharacter = playerNumber ? frameData.getP2()
				: frameData.getP1();
		myOriginalHp = myCharacter.getHp();
		oppOriginalHp = oppCharacter.getHp();

		if (this.parent != null) {
			this.depth = this.parent.depth + 1;
		} else {
			this.depth = 0;
		}
	}

	public int sumGames(ExtendedNode[] nodes) {
		int sum = 0;
		for (ExtendedNode node : nodes)
			sum += node.games;
		return sum;
	}

	public int sumScore(ExtendedNode[] nodes) {
		int sum = 0;
		for (ExtendedNode node : nodes)
			sum += node.score;
		return sum;
	}

	/**
	 * MCTSを行う
	 * 
	 * @return 最終的なノードの探索回数が多いAction -Action with the highest number of node
	 *         findings finally
	 */
	public Action mcts() {
		// 時間の限り、UCTを繰り返す
		long start = System.nanoTime();
		for (; System.nanoTime() - start <= UCT_TIME;) {
			uct();
		}

		return getBestVisitAction();
	}

	static final int playout_turn = 3;// 3が強そう？元は5

	/**
	 * プレイアウト(シミュレーション)を行う Perform playout (simulation)
	 * 
	 * @return プレイアウト結果の評価値 - Evaluation value of playout result
	 */
	public double playout() {
		// playout_turnターン先まで考えてプレイアウト Think and play until the turn

		mAction.clear();
		oppAction.clear();

		int tmp_playout_turn = (selectedMyActions.size() > playout_turn ? selectedMyActions
				.size() : playout_turn);
		for (int i = 0; i < selectedMyActions.size(); i++) {
			mAction.add(selectedMyActions.get(i));
		}

		for (int i = 0; i < tmp_playout_turn - selectedMyActions.size(); i++) {
			mAction.add(myActions.get(rnd.nextInt(myActions.size())));
		}

		// ///////////////////////////
		// Consider Opponent Action//
		// ///////////////////////////
		for (int i = 0; i < selectedOppActions.size(); i++) {
			oppAction.add(selectedOppActions.get(i));
		}

		for (int i = 0; i < tmp_playout_turn - selectedOppActions.size(); i++) {
			oppAction.add(oppActions.get(rnd.nextInt(oppActions.size())));
		}

		// ///////////////////////////

		FrameData nFrameData = simulator.simulate(frameData, playerNumber,
				mAction, oppAction, SIMULATION_TIME); // シミュレーションを実行 Run
														// simulation

		return getScore(nFrameData);
	}

	/**
	 * UCTを行う <br>
	 * 
	 * @return 評価値
	 */
	public double uct() {

		ExtendedNode[] selectedNodes = null;
		double bestUcb;

		bestUcb = -99999;

		for (ExtendedNode[] child : this.children) {
			int sumgames = sumGames(child);
			double ucb;
			if (sumgames == 0) {
				ucb = 9999 + rnd.nextInt(50);
			} else {
				ucb = getUcb(sumScore(child) / sumgames, games, sumgames);
			}

			if (bestUcb < ucb) {
				selectedNodes = child;
				bestUcb = ucb;
			}

		}
		ExtendedNode selectedNode = null;
		bestUcb = -99999;
		for (ExtendedNode child : selectedNodes) {
			double ucb;
			if (child.games == 0) {
				ucb = 9999 + rnd.nextInt(50);
			} else {
				ucb = getUcb(-child.score / child.games,
						sumGames(selectedNodes), child.games);
			}

			if (bestUcb < ucb) {
				selectedNode = child;
				bestUcb = ucb;
			}

		}

		double score = 0;
		if (selectedNode.games == 0) {
			score = selectedNode.playout();
		} else {
			if (selectedNode.children == null) {
				if (selectedNode.depth < UCT_TREE_DEPTH) {
					if (UCT_CREATE_NODE_THRESHOULD <= selectedNode.games) {
						selectedNode.createNode();
						selectedNode.isCreateNode = true;
						score = selectedNode.uct();
					} else {
						score = selectedNode.playout();
					}
				} else {
					score = selectedNode.playout();
				}
			} else {
				if (selectedNode.depth < UCT_TREE_DEPTH) {
					score = selectedNode.uct();
				} else {
					selectedNode.playout();
				}
			}

		}

		selectedNode.games++;
		selectedNode.score += score;

		if (depth == 0) {
			games++;
		}

		return score;
	}

	/**
	 * ノードを生成する
	 */
	public void createNode() {

		Vector<MotionData> myMotion = this.playerNumber ? gameData
				.getPlayerOneMotion() : gameData.getPlayerTwoMotion();
		Vector<MotionData> oppMotion = this.playerNumber ? gameData
				.getPlayerTwoMotion() : gameData.getPlayerOneMotion();
		this.children = new ExtendedNode[myActions.size()][oppActions.size()];
		for (int i = 0; i < myActions.size(); i++) {
			int myEnergy = this.myEnergy;
			LinkedList<Action> my = new LinkedList<Action>();
			for (Action act : selectedMyActions) {
				my.add(act);
			}

			Action mai = myActions.get(i);
			int maiEnergy = Math.abs(myMotion.elementAt(mai.ordinal())
					.getAttackStartAddEnergy());

			// Consider Energy
			if (maiEnergy > myEnergy)
				continue;
			my.add(mai);

			for (int j = 0; j < oppActions.size(); j++) {
				LinkedList<Action> opp = new LinkedList<Action>();
				for (Action act : selectedOppActions) {
					opp.add(act);
				}

				Action oaj = oppActions.get(j);
				int oajEnergy = Math.abs(oppMotion.elementAt(oaj.ordinal())
						.getAttackStartAddEnergy());
				if (oajEnergy > oppEnergy)
					continue;
				opp.add(oaj);

				children[i][j] = new ExtendedNode(charName, myEnergy
						- maiEnergy, oppEnergy - oajEnergy, frameData, this,
						myActions, oppActions, gameData, playerNumber,
						commandCenter, my, opp);
			}
		}
	}

	/**
	 * 最多訪問回数のノードのActionを返す
	 * 
	 * @return 最多訪問回数のノードのAction
	 */
	public Action getBestVisitAction() {

		int selected = -1;
		double bestGames = -9999;

		for (int i = 0; i < children.length; i++) {

			int tmpgames = sumGames(children[i]);

			if (bestGames < tmpgames) {
				bestGames = tmpgames;
				selected = i;
			}

		}

		return this.myActions.get(selected);
	}

	/**
	 * 最多スコアのノードのActionを返す
	 * 
	 * @return 最多スコアのノードのAction
	 */
	public Action getBestScoreAction() {

		int selected = -1;
		double bestScore = -9999;
		for (int i = 0; i < children.length; i++) {

			int sumgames = sumGames(children[i]);
			if (sumgames == 0)
				continue;
			double meanScore = sumScore(children[i]) / sumgames;
			if (bestScore < meanScore) {
				bestScore = meanScore;
				selected = i;
			}

		}

		return this.myActions.get(selected);
	}

	/**
	 * 評価値を返す
	 * 
	 * @param fd
	 *            フレームデータ(これにhpとかの情報が入っている)
	 * @return 評価値
	 */
	public int getScore(FrameData fd) {

		CharacterData myP = playerNumber ? fd.getP1() : fd.getP2();
		CharacterData oppP = playerNumber ? fd.getP2() : fd.getP1();
		int baseScore = (myP.hp - myOriginalHp) - (oppP.hp - oppOriginalHp);

		int distanceX = Math.abs(myP.x - oppP.x);
		if (distanceX < 50)
			distanceX = 50;
		// ////////////////////////////////////////////////////////////////
		// LUD Consider Distance!! Because if Both of LUD repeat run away ,both
		// of LUD can't win. I confirmed ZEN and GARNET don't repeat run away.
		// But LUD is unknown.
		if (this.charName == CharacterName.LUD) {
			if (myOriginalHp - oppOriginalHp <= 0)
				return baseScore * 100 - distanceX;
			else
				return baseScore + (myP.hp - myOriginalHp);
		} else {
			return baseScore;
		}

	}

	/**
	 * 評価値と全プレイアウト試行回数とそのActionのプレイアウト試行回数からUCB1値を返す Returns the UCB 1 value
	 * from the evaluation value, the total number of playout attempts, and the
	 * number of playout attempts of that Action
	 * 
	 * @param score
	 *            評価値
	 * @param n
	 *            全プレイアウト試行回数
	 * @param ni
	 *            そのActionのプレイアウト試行回数
	 * @return UCB1値
	 * 
	 *         Param score evaluation value Param n Total number of playout
	 *         attempts Param ni Number of playout attempts for that Action
	 *         return UCB1 value
	 */
	public double getUcb(double score, int n, int ni) {
		return score + UCB_C * Math.sqrt((2 * Math.log(n)) / ni);
	}

}
