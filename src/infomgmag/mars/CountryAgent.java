package infomgmag.mars;

import infomgmag.Territory;
import java.util.ArrayList;

public class CountryAgent {
    private Territory territory;
    public ArrayList<CountryAgent> adjacentAgents;
    private ArrayList<Goal> goalList;
    private Mars mars;
    private double value;

    CountryAgent(Territory territory, Mars mars) {
        this.territory = territory;
        goalList = new ArrayList<>();
        this.adjacentAgents = new ArrayList<CountryAgent>();
        this.mars = mars;
    }

    public Territory getTerritory() {
        return territory;
    }

    //calculates value of owning a territory
    public void calculateOwnershipValue(Personality personality) {
        this.value =
                (this.territory.getOwner().equals(mars) ? personality.getDefensiveBonus() : personality.getOffensiveBonus()) *
                (friendlyNeighbours() * personality.getFriendlyNeighbourWeight() +
                 enemyNeighbours() * personality.getEnemyNeighbourWeight() +
                 friendlyArmies() * personality.getFriendlyArmyWeight() +
                 enemyArmies() * personality.getEnemyArmyWeight() +
                 territory.getContinentsBorderedAmount() * personality.getContinentBorderWeight() +
                 (enemyOwnsAnEntireContinent() ? 1 : 0) * personality.getEnemyOwnsWholeContinentWeight() +
                 percentageOfContinentOwned() * personality.getPercentageOfContinentWeight());
    }

    // calculates how many friendly neighbouring territories border this territory
    public long friendlyNeighbours() {
        return territory.getAdjacentTerritories().stream()
                .filter(t -> t.getOwner() == mars)
                .count();
    }

    // calculates how many enemy neighbouring territories border this territory
    public long enemyNeighbours() {
        return territory.getAdjacentTerritories().stream()
                .filter(t -> t.getOwner() != mars)
                .count();
    }

    // calculates how many friendly armies border this territory
    public long friendlyArmies() {
        return territory.getAdjacentTerritories().stream()
                .filter(t -> t.getOwner() == mars)
                .mapToInt(t -> t.getUnits())
                .sum();
    }

    // calculates how many enemy armies border this territory
    public long enemyArmies() {
        return territory.getAdjacentTerritories().stream()
                .filter(t -> t.getOwner() != mars)
                .mapToInt(t -> t.getUnits())
                .sum();
    }

    // checks if this territory borders an enemy
    public boolean bordersEnemy() { 
        return territory.getAdjacentTerritories().stream()
                .anyMatch(t -> t.getOwner() != mars);
    }

    // checks if the agent owns the whole continent except this territory
    public boolean ownWholeContinent() {
        return territory.getContinent().getTerritories().stream().allMatch(x -> x.getOwner() == this.mars);
    }

    public boolean enemyOwnsAnEntireContinent() {
        return territory.getOwner().getTerritories().stream().allMatch(x -> x.getOwner() == territory.getOwner())
                && territory.getOwner() != this.mars;
    }

    // calculates the percentage of continent owned
    public double percentageOfContinentOwned() {
        double owned = territory.getContinent().getTerritories().stream()
                .filter(t -> t.getOwner() == mars)
                .count();
        return owned / territory.getContinent().getTerritories().size();
    }

    // clears the goals
    public void clearGoals() { 
        goalList.clear();
    }

    public void addAdjacentAgent(CountryAgent ca) {
        this.adjacentAgents.add(ca);
    }

    private double getGoalSuccessOdds(Integer i, Goal goal) {
        Integer attackingUnits = this.getTerritory().getUnits() + i - goal.size() - 1;
        if (attackingUnits < 1) {
            return 0.0;
        }
        Integer defendingUnits = 0;
        for (CountryAgent ca : goal) {
            defendingUnits += ca.getTerritory().getUnits();
        }

        ProbabilityGrid grid = new ProbabilityGrid(attackingUnits, defendingUnits);
        return grid.chanceOfWin();
    }

    private Double getGoalValue(Goal goal) {
        double goalValue = 0.0;
        for (CountryAgent ca : goal) {
            goalValue += ca.value;
        }
        goalValue += (goal.completesContinentFor(mars) ? 1 : 0) * mars.getPersonality().getOwnWholeContinentWeight();
        goalValue += goal.killsPlayers(mars) * mars.getPersonality().getKillEnemyWeight();
        return goalValue;
    }

    public Double getDefenseOdds(int units) {
        int totalEnemyUnits = 0;
        for (Territory t : getTerritory().getAdjacentTerritories()) {
            if (t.getOwner() != this.getTerritory().getOwner()) {
                totalEnemyUnits += t.getUnits();
            }
        }
        ProbabilityGrid grid = new ProbabilityGrid(units, totalEnemyUnits);
        return grid.chanceOfWin();
    }

    public Double getDefenseOddsOfCapture(Goal goal, int units) {
        int totalEnemyUnits = goal.getFinalGoal().getTerritory().getAdjacentTerritories().stream()
                .filter(t -> t.getOwner() != this.getTerritory().getOwner()
                        && !goal.stream().map(x -> x.getTerritory()).anyMatch(x -> x == t))
                .mapToInt(t -> t.getUnits()).sum();
        ProbabilityGrid grid = new ProbabilityGrid(units + this.getTerritory().getUnits(), totalEnemyUnits);
        return grid.chanceOfWin();
    }

    private double getGoalUtility(Goal goal, Integer i) {
        double p = getGoalSuccessOdds(i, goal);
        double w = getGoalValue(goal);
        double d = getDefenseOddsOfCapture(goal, i);
        return p * w * d;
    }
    
    private double getGoalUtilityPerUnit(Goal goal, Integer i) {
        double total = getGoalUtility(goal, i);
        if (i == 0) {
            return total;
        }
        return total / i;
    }

    private double getDefenseUtility(Integer i) {
        double v = value;
        double d = getDefenseOdds(this.getTerritory().getUnits() + i);
        return v * d;
    }
    
    private double getDefenseUtilityPerUnit(Integer i) {
        double total = getDefenseUtility(i);
        if (i == 0) {
            return total;
        }
        return total / i;
    }

    public ArrayList<ReinforcementBid> getBids(Integer unitsLeft) {
        ArrayList<ReinforcementBid> result = new ArrayList<>();
        for (Goal goal : goalList) {
            result.addAll(getOffensiveBids(unitsLeft, goal));
        }

        DefensiveBid defBid = getDefensiveBid(unitsLeft);
        result.add(defBid);
        return result;
    }

    public ReinforcementBid getBestBid(Integer unitsLeft) {
        return getBids(unitsLeft).stream()
            .max((x, y) -> (x.getUtility() < y.getUtility() ? -1 : (x.getUtility() == y.getUtility() ? 0 : 1)))
            .get();
    }

    public DefensiveBid getDefensiveBid(Integer unitsLeft) {
        DefensiveBid bestBid = null;
        for (int i = 0; i <= unitsLeft; i++) {
            double bidUtil = getDefenseUtilityPerUnit(i);
            if (bestBid == null || bidUtil > bestBid.getUtility()) {
                bestBid = new DefensiveBid(this, i, bidUtil);
            }
        }
        return bestBid;
    }

    public ArrayList<FortifierBid> getFortifierBids () {
        ArrayList<FortifierBid> result = new ArrayList<>();
        
        if (!goalList.isEmpty()) {
            for (Goal goal : goalList) {
                for (int i = 1; i < territory.getUnits(); i++) {
                    double util = getGoalUtility(goal, -i) * i - getGoalUtilityPerUnit(goal,0);
                    result.add(new FortifierBid(this, i, util));
                }
            }
        }
        for (int i = 1; i < territory.getUnits(); i++) {
            double util = getDefenseUtility(-i) * i - getDefenseUtilityPerUnit(0);
            result.add(new FortifierBid(this,i,util));
        }
        return result;
    }
    
    public ReinforcementBid getBestMaxBid(Integer unitsLeft) {
        return this.getBids(unitsLeft).stream()
                .filter(x -> x.getUnits() == unitsLeft)
                .max((x, y) -> (x.getUtility() < y.getUtility() ? -1 : (x.getUtility() == y.getUtility() ? 0 : 1)))
                .get();
    }

    private ArrayList<OffensiveBid> getOffensiveBids(Integer unitsLeft, Goal goal) {
        ArrayList<OffensiveBid> result = new ArrayList<>();
        OffensiveBid bestBid = null;
        for (int i = 0; i <= unitsLeft; i++) {
            double bidUtil = getGoalUtilityPerUnit(goal, i);
            OffensiveBid bid = new OffensiveBid(this, goal, i, bidUtil);
            result.add(bid);
            if (bestBid == null || bidUtil > bestBid.getUtility()) {
                bestBid = bid;
            }
        }
        return result;
    }

    public String toString() {
        return territory.toString();
    }

    public void createGoals() {
        for (CountryAgent ca : adjacentAgents) {
            Goal goal = new Goal();
            goal.addEarlierGoal(this);
            ca.createGoals(goal);
        }
    }

    public void createGoals(Goal goal) {
        if(goal.size() >= this.mars.getPersonality().getGoalLength())
            return;

        if (territory.getOwner() == mars) {
            goalList.add(goal);
        } else {
            for (CountryAgent ca : adjacentAgents) {
                if (!goal.contains(ca)) {
                    Goal newGoal = (Goal) goal.clone();
                    newGoal.addEarlierGoal(this);
                    createGoals(newGoal);
                }
            }
        }
    }
}
