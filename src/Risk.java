import com.sun.javafx.image.IntPixelGetter;
import sun.rmi.transport.ObjectTable;

import java.lang.reflect.Array;
import java.util.*;
import java.sql.Time;
import java.util.ArrayList;

/**
 * This class contains the main() method. This class is the bridge between the visual presentation of 
 * the game (RiskVisual) and the data presentation of the game (RiskPhysics).
 * @author Games&AgentsGroup8
 * @version FirstPrototype
 * @date 4/5/2018
 */
public class Risk {

	public static Random random;

	private ArrayList<Player> players;
	private Board board;
	private Player currentPlayer;
	private RiskVisual visuals;
	private Integer nrOfStartingUnits;

	public static void main(String[] args) {
		random = new Random(111);
		Risk risk = new Risk();
		risk.run();
		System.out.println("Game is won by a player!");
	}

	public Risk(){
		initializeGame();
	}

	public void run(){
		long targetFrameDuration = (long) (1000/1.0);
		long frameDuration = 1000;
		long lastFrameTime = System.currentTimeMillis();
		while (!finished()) {
			frameDuration = -(lastFrameTime - (lastFrameTime = System.currentTimeMillis() / 1000000000));
			if (frameDuration > targetFrameDuration) {
				try {
					Thread.sleep(targetFrameDuration - frameDuration);
				} catch (InterruptedException e) {
					System.exit(0);
				}
			}

			System.out.println("Current Player: " + currentPlayer.toString());
			Integer nrOfReinforcements = calculateReinforcements();
			currentPlayer.setReinforcements(currentPlayer.getReinforcements() + nrOfReinforcements);
			visuals.update();
			currentPlayer.turnInCards(board);
			visuals.update();
			while(currentPlayer.getReinforcements() != 0){
				currentPlayer.placeSingleReinforcement(board);
				visuals.update();
			}
			CombatMove combatMove;
			while((combatMove = currentPlayer.getCombatMove()) != null){
				performCombatMove(combatMove);
			}

			nextCurrentPlayer();// Possibly even more updates throughout the turn...
		}
	}

	private void nextCurrentPlayer(){
		currentPlayer = players.get((players.indexOf(currentPlayer) + 1) % players.size());
	}

	private void performCombatMove(CombatMove combatMove){
		System.out.println("Performing combatMove: " + combatMove.toString());
		ArrayList<Integer> attackThrows = new ArrayList<Integer>();
		ArrayList<Integer> defenseThrows = new ArrayList<Integer>();

		//Attacker throws dices
		for(int i = 0; i < combatMove.getAttackingUnits(); i++){
			int value = Risk.random.nextInt(6) + 1;
			attackThrows.add(value);
		}

		//Defender throws dices
		for(int i = 0; i < combatMove.getDefendingUnits(); i++){
			int value = Risk.random.nextInt(6) + 1;
			defenseThrows.add(value);
		}

		//Determining the losses of both sides and changes those values
		int attackLoss = 0, defenseLoss = 0;

		if(Collections.max(attackThrows) > Collections.max(defenseThrows)){
			defenseLoss++;
		}else{
			attackLoss++;
		}

		attackThrows.remove(Collections.max(attackThrows));
		if(combatMove.getDefendingUnits() > 1 && combatMove.getAttackingUnits() > 1 && Collections.max(attackThrows) > Collections.min(defenseThrows)){
			defenseLoss++;
		}else if(combatMove.getDefendingUnits() > 1 && combatMove.getAttackingUnits() > 1){
			attackLoss++;
		}

		combatMove.getAttackingTerritory().setUnits(combatMove.getAttackingTerritory().getNUnits() - attackLoss);
		combatMove.getDefendingTerritory().setUnits(combatMove.getDefendingTerritory().getNUnits() - defenseLoss);

		//Update number of units on both territories and new owner
		if(combatMove.getDefendingTerritory().getNUnits() == 0){
			System.out.println(currentPlayer + " conquered " + combatMove.getDefendingTerritory().getName());
			combatMove.getDefendingTerritory().setOwner(currentPlayer);
			int transferredUnits = combatMove.getAttackingTerritory().getNUnits() - 1;
			combatMove.getDefendingTerritory().setUnits(transferredUnits);
			combatMove.getAttackingTerritory().setUnits(combatMove.getAttackingTerritory().getNUnits() - transferredUnits);
		}
	}

	private Integer calculateReinforcements(){
		System.out.println("Calculate reinforcements");
		Integer reinforcements = 0;
		reinforcements += Integer.max(3, currentPlayer.territories.size() / 3);
		reinforcements += calculateContinentBonus();
		return reinforcements;
	}

	private Integer calculateContinentBonus(){
		System.out.println("Calculating continent Bonus");
		int bonus = 0;
		for(Continent continent : board.getContinents()){
			boolean controlsContinent = true;
			for(Territory territory : continent.getMembers()){
				if(territory.getOwner() != currentPlayer){
					controlsContinent = false;
					break;
				}
			}
			if(controlsContinent){
				bonus += continent.getNReinforcements();
			}
		}
		return bonus;
	}

	private void initializeGame() {
		System.out.println("Initializing game");
        visuals = new RiskVisual(this);
		board = new Board();
		nrOfStartingUnits = 30;
        initializePlayers();
        Integer currentPlayerIndex = divideTerritories();
		initialPlaceReinforcements(currentPlayerIndex);
		currentPlayer = players.get(0);

		for(int i = 0; i < players.size(); i++){
			System.out.println(players.get(i).toString());
		}
	}

	private void initializePlayers() {
		System.out.println("Initializing players");
		players = new ArrayList<Player>();
		//TODO deciding number of startingUnits using number of players and evt. number territorries
		for (int i = 0; i < 4; i++) {
			Objective objective = new Objective(Objective.type.TOTAL_DOMINATION);
			Bot player = new Bot(objective, nrOfStartingUnits, "player" + i);
			players.add(player);
		}
	}

	//Divide players randomly over territories
	private Integer divideTerritories(){
		System.out.println("Dividing territories");
		Collections.shuffle(board.getTerritories());
		int player = 0;
		for(Territory territory : board.getTerritories()){
			territory.setOwner(players.get(player % players.size()));
			territory.setUnits(1);
			territory.getOwner().setReinforcements(territory.getOwner().getReinforcements() - 1);
			player++;
		}
		return player % players.size();
	}

	private void initialPlaceReinforcements(Integer currentPlayerIndex){
		System.out.println("Placing initial reinforcements");
		int player = currentPlayerIndex;
		for(int i = 0; i < (players.size() * nrOfStartingUnits) - board.getTerritories().size() ; i++){
			players.get(player % players.size()).placeSingleReinforcement(board);
			player++;
		}
	}

	public Board getBoard() {
		return this.board;
	}
	
	/**
	 * Returns true if there is a winner.
	 */
	private boolean finished() {
		if (board.GetWinner() == null) {
			return false;
		}
		return true;
	}
}
