package infomgmag;

import infomgmag.mars.Mars;
import infomgmag.mars.Personality;
import infomgmag.mars.PersonalityFactory;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * This class contains the main() method. This class is the bridge between the
 * visual presentation of the game (RiskVisual) and the data presentation of the
 * game (RiskPhysics).
 * 
 * @author Games&AgentsGroup8
 * @version FirstPrototype
 * @date 4/5/2018
 */
public class Risk implements CombatInterface{

    // Variables to be customized by debugger
    private boolean visible = true;
    private int randomPlayers = 0;
    private int agressivePlayers = 1;
    private int normalPlayers = 2;
    private int defensivePlayers = 2;
    private int continentPlayers = 1;

    public static Random random;

    public static ArrayList<ArrayList<Double>> DICE_ODDS_ONE, DICE_ODDS_TWO;

    private ArrayList<Player> activePlayers;
    private ArrayList<Player> defeatedPlayers;
    private Player currentPlayer;

    private Board board;
    private RiskVisual visuals;

    private int turn = 0;
    private int nrOfStartingUnits;
    private boolean StopGame;
    
    private ArrayList<CombatEvent> combatLog;

    public static void main(String[] args) {
        long seed = System.currentTimeMillis();
        random = new Random(seed);
        System.out.println(seed);
        createDiceOdds();
        Risk risk = new Risk();
        risk.run(); //this makes the game run
    }
    
    public static void createDiceOdds() {
        DICE_ODDS_ONE = new ArrayList<ArrayList<Double>>();
        ArrayList<Double> oneA = new ArrayList<Double>();
        oneA.add(15.0/36);
        oneA.add(125.0/216);
        oneA.add(855.0/1296);
        DICE_ODDS_ONE.add(oneA);
        ArrayList<Double> oneD = new ArrayList<Double>();
        oneD.add(21.0/36);
        oneD.add(91.0/216);
        oneD.add(441.0/1296);
        DICE_ODDS_ONE.add(oneD);
        DICE_ODDS_TWO = new ArrayList<ArrayList<Double>>();
        ArrayList<Double> twoA = new ArrayList<Double>();
        twoA.add(55.0/216);
        twoA.add(295.0/1296);
        twoA.add(2890.0/7776);
        DICE_ODDS_TWO.add(twoA);
        ArrayList<Double> twoD = new ArrayList<Double>();
        twoD.add(161.0/216);
        twoD.add(581.0/1296);
        twoD.add(2275.0/7776);
        DICE_ODDS_TWO.add(twoD);
        ArrayList<Double> twoL = new ArrayList<Double>();
        twoL.add(null);
        twoL.add(420.0/1296);
        twoL.add(2611.0/7776);
        DICE_ODDS_TWO.add(twoL);
    }
    
    public Risk() {
        visuals = new RiskVisual(this,visible);
        board = new Board(visuals);
        combatLog = new ArrayList<>();
        defeatedPlayers = new ArrayList<Player>();
        nrOfStartingUnits = 30;
        initializePlayers();
        int currentPlayerIndex = divideTerritories();
        initialPlaceReinforcements(currentPlayerIndex);
        visuals.setTargetFrameDuration(450);
        currentPlayer = activePlayers.get(0);
    }
    
    public void run() {
        while (!finished()) {
            visuals.update();
            currentPlayer.setHasConqueredTerritoryInTurn(false);
            int nrOfReinforcements = calculateReinforcements();
            currentPlayer.setReinforcements(nrOfReinforcements);
            currentPlayer.turnInCards(board);
            currentPlayer.placeReinforcements(board);

            int startingNrOfTerritories = currentPlayer.getTerritories().size();
            
            currentPlayer.attackPhase((CombatInterface) this);
            currentPlayer.fortifyTerritory(board);
            visuals.update();

            int endingNrOfTerritories = currentPlayer.getTerritories().size();
            if (startingNrOfTerritories < endingNrOfTerritories)
                board.drawCard(currentPlayer);
            nextCurrentPlayer();

            turn++;
        }

        visuals.log(activePlayers.get(0) + " has won!");

        if (visible)
            while(true) {
                visuals.update();
            }
    }

    public int getTurn() {
        return turn;
    }

    private void nextCurrentPlayer() {
        currentPlayer = activePlayers.get((activePlayers.indexOf(currentPlayer) + 1) % activePlayers.size());
    }

    public void performCombatMove(CombatMove combatMove) {
        int defendingAmount = combatMove.getDefendingTerritory().getOwner().getDefensiveDice(combatMove);
        if (defendingAmount > combatMove.getDefendingTerritory().getUnits() || defendingAmount > 2 || defendingAmount < 1)
            throw new RuntimeException("Rule breach: Defending amount not allowed: " + combatMove);
        combatMove.setDefendingUnits(defendingAmount);

        visuals.update(combatMove);
        ArrayList<Integer> attackThrows = new ArrayList<>();
        ArrayList<Integer> defenseThrows = new ArrayList<>();

        // Attacker throws dices
        for (int i = 0; i < combatMove.getAttackingUnits(); i++) {
            int value = Risk.random.nextInt(6) + 1;
            attackThrows.add(value);
        }

        // Defender throws dices
        for (int i = 0; i < combatMove.getDefendingUnits(); i++) {
            int value = Risk.random.nextInt(6) + 1;
            defenseThrows.add(value);
        }

        // Determining the losses of both sides and changes those values
        int attackLoss = 0, defenseLoss = 0;

        if (Collections.max(attackThrows) > Collections.max(defenseThrows))
            defenseLoss++;
        else
            attackLoss++;

        attackThrows.remove(Collections.max(attackThrows));
        if (combatMove.getDefendingUnits() > 1 && combatMove.getAttackingUnits() > 1
                && Collections.max(attackThrows) > Collections.min(defenseThrows))
            defenseLoss++;
        else if (combatMove.getDefendingUnits() > 1 && combatMove.getAttackingUnits() > 1)
            attackLoss++;

        combatMove.getAttackingTerritory().setUnits(combatMove.getAttackingTerritory().getUnits() - attackLoss);
        combatMove.getDefendingTerritory().setUnits(combatMove.getDefendingTerritory().getUnits() - defenseLoss);

        // Update number of units on both territories and new owner
        boolean captured = combatMove.getDefendingTerritory().getUnits() == 0;
        if (captured) {
            Player defender = combatMove.getDefendingTerritory().getOwner();
            combatMove.getDefendingTerritory().setOwner(currentPlayer);
            currentPlayer.movingInAfterInvasion(board, combatMove);

            if (isPlayerDead(defender)) {
                // Attacker receives all the territory cards of the defender.
                currentPlayer.hand.setWildCards(currentPlayer.hand.getWildcards() + defender.hand.getWildcards());
                currentPlayer.hand.setArtillery(currentPlayer.hand.getArtillery() + defender.hand.getArtillery());
                currentPlayer.hand.setCavalry(currentPlayer.hand.getCavalry() + defender.hand.getCavalry());
                currentPlayer.hand.setInfantry(currentPlayer.hand.getInfantry() + defender.hand.getInfantry());
                while (currentPlayer.hand.getNumberOfCards() > 4)
                    currentPlayer.turnInCards(board);
                activePlayers.remove(defender);
                if (activePlayers.size() > 1)
                    currentPlayer.placeReinforcements(board);
                defeatedPlayers.add(defender);
            }
        }

        combatLog.add(new CombatEvent(
                combatMove.getAttackingTerritory().getOwner(), 
                combatMove.getDefendingTerritory().getOwner(), 
                combatMove.getAttackingTerritory(), 
                combatMove.getDefendingTerritory(), 
                combatMove.getAttackingUnits(), 
                combatMove.getDefendingUnits(), 
                attackLoss == 0 ? CombatEvent.ATTACKER_WINS :
                    (defenseLoss == 0 ? CombatEvent.DEFENDER_WINS :
                        CombatEvent.ONE_EACH), 
                captured));
    }

    private int calculateReinforcements() {
        int reinforcements = 0;
        reinforcements += Integer.max(3, currentPlayer.territories.size() / 3);
        reinforcements += calculateContinentBonus();
        return reinforcements;
    }

    private int calculateContinentBonus() {
        int bonus = 0;
        for (Continent continent : board.getContinents()) {
            boolean controlsContinent = true;
            for (Territory territory : continent.getTerritories())
                if (territory.getOwner() != currentPlayer) {
                    controlsContinent = false;
                    break;
                }
            if (controlsContinent)
                bonus += continent.getReinforcements();
        }
        return bonus;
    }

    public ArrayList<Player> getActivePlayers() {
        return activePlayers;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    private Color[] playerColors = {
            Color.RED,
            Color.BLUE,
            Color.BLACK,
            Color.GRAY,
            Color.ORANGE,
            Color.MAGENTA
    };

    private void initializePlayers() {
        activePlayers = new ArrayList<>();
        // TODO deciding number of startingUnits using number of players and evt. number
        // territorries
        int i;
        for (i = 0; i < randomPlayers; i++) {
            Objective objective = new Objective(Objective.type.TOTAL_DOMINATION);
            Color color;
            if (i < playerColors.length) {
                color = playerColors[i];
            } else {
                color = new Color(Risk.random.nextFloat() * 0.8f + 0.2f, Risk.random.nextFloat() * 0.8f + 0.2f,
                        Risk.random.nextFloat() * 0.8f + 0.2f);
            }
            RandomBot player = new RandomBot(objective, 0, "Player " + i + " (Random Bot)",color);
            activePlayers.add(player);
        }

        ArrayList<Personality> personalities = new ArrayList<>();
        for (int j = 0; j < defensivePlayers; j++)
            personalities.add(PersonalityFactory.defensivePersonality());
        for (int j = 0; j < agressivePlayers; j++)
            personalities.add(PersonalityFactory.agressivePersonality());
        for (int j = 0; j < normalPlayers; j++)
            personalities.add(PersonalityFactory.normalPersonality());
        for (int j = 0; j < continentPlayers; j++)
            personalities.add(PersonalityFactory.continentPersonality());

        for (; i < randomPlayers + agressivePlayers + defensivePlayers + normalPlayers + continentPlayers; i++) {
            Color color;
            if (i < playerColors.length) {
                color = playerColors[i];
            } else {
                color = new Color(Risk.random.nextFloat() * 0.8f + 0.2f, Risk.random.nextFloat() * 0.8f + 0.2f,
                        Risk.random.nextFloat() * 0.8f + 0.2f);
            }
            Objective objective = new Objective(Objective.type.TOTAL_DOMINATION);
            Personality personality = personalities.get(i - randomPlayers);
            Mars player = new Mars(this, objective, 0, "Player " + i + " (" + personality + " MARS)",color, personality);
            activePlayers.add(player);
        }
        shufflePlayers();
    }
    
    private void shufflePlayers() {
    	int index;
    	Player temp;
    	for(int i = activePlayers.size() - 1; i > 0; i--) {
    		index = random.nextInt(i + 1);
    		temp = activePlayers.get(index);
    		activePlayers.set(index, activePlayers.get(i));
    		activePlayers.set(i, temp);
    	}
    }

    // Divide players randomly over territories
    private int divideTerritories() {
        Collections.shuffle(board.getTerritories(), Risk.random);
        int player = 0;
        for (Territory territory : board.getTerritories()) {
            territory.setOwner(activePlayers.get(player % activePlayers.size()));
            territory.setUnits(1);
            player++;
        }
        return player % activePlayers.size();
    }

    private void initialPlaceReinforcements(int currentPlayerIndex) {
        int player = currentPlayerIndex;
        for (int i = 0; i < (activePlayers.size() * nrOfStartingUnits) - board.getTerritories().size(); i++) {
            activePlayers.get(player % activePlayers.size()).setReinforcements(1);
            activePlayers.get(player % activePlayers.size()).placeReinforcements(board);
            player++;
        }
    }

    public Board getBoard() {
        return this.board;
    }

    public Boolean isPlayerDead(Player player) {
        return player.getTerritories().size() == 0;
    }

    /**
     * Returns true if there is a winner.
     */
    private boolean finished() {
        return activePlayers.size() == 1 || StopGame;
    }
    
    public static ArrayList<Territory> getConnectedTerritories(Territory origin) {
        ArrayList<Territory> visited = new ArrayList<>();
        ArrayList<Territory> result = new ArrayList<>();
        result.add(origin);
        boolean foundTerritory = true;
        while (foundTerritory) {
            foundTerritory = false;
            ArrayList<Territory> add = new ArrayList<>();
            for (Territory t : result)
                if (!visited.contains(t)) {
                    for (Territory c : t.getAdjacentTerritories())
                        if (!result.contains(c) && origin.getOwner().getTerritories().contains(c)) {
                            add.add(c);
                            foundTerritory = true;
                        }
                    visited.add(t);
                }
            for (Territory t : add)
                result.add(t);
        }
        return result;
    }

    public static void printError(String str) {
        System.err.println("Error:"+str);
        System.exit(1);
    }

    public ArrayList<Player> getDefeatedPlayers(){
        return this.defeatedPlayers;
    }

    public int getActivePlayerAmount() {
        return activePlayers.size();
    }
}
