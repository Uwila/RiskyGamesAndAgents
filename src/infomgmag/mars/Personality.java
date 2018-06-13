package infomgmag.mars;

public class Personality {
    private double friendlyNeighbourWeight;
    private double enemyNeighbourWeight;
    private double friendlyArmyWeight;
    private double enemyArmyWeight;
    private double continentBorderWeight;
    private double ownWholeContinentWeight;
    private double enemyOwnsWholeContinentWeight;
    private double percentageOfContinentWeight;
    private int goalLength;
    private double offensiveMultiplier;
    private double defensiveMultiplier;
    private double killEnemyWeight;
    private String name;
    private double clusteringWeight;
    private double attackFirstCountryWeight;

    public Personality(String name,
                       double defensiveMultiplier,
                       double offensiveMultiplier,
                       double friendlyNeighbourWeight,
                       double enemyNeighbourWeight,
                       double friendlyArmyWeight,
                       double enemyArmyWeight,
                       double continentBorderWeight,
                       double ownWholeContinentWeight,
                       double enemyOwnsWholeContinentWeight,
                       double percentageOfContinentWeight,
                       double killEnemyWeight,
                       double clusteringWeight,
                       int goalLength,
                       double attackFirstTerritoryWeight){
        this.name = name;
        this.defensiveMultiplier = defensiveMultiplier;
        this.offensiveMultiplier = offensiveMultiplier;
        this.friendlyNeighbourWeight = friendlyNeighbourWeight;
        this.enemyNeighbourWeight = enemyNeighbourWeight;
        this.friendlyArmyWeight = friendlyArmyWeight;
        this.enemyArmyWeight = enemyArmyWeight;
        this.continentBorderWeight = continentBorderWeight;
        this.ownWholeContinentWeight = ownWholeContinentWeight;
        this.enemyOwnsWholeContinentWeight = enemyOwnsWholeContinentWeight;
        this.percentageOfContinentWeight = percentageOfContinentWeight;
        this.killEnemyWeight = killEnemyWeight;
        this.goalLength = goalLength;
        this.clusteringWeight = clusteringWeight;
        this.attackFirstCountryWeight = attackFirstTerritoryWeight;

    }

    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }

    public double getFriendlyNeighbourWeight() {
        return friendlyNeighbourWeight;
    }

    public double getEnemyNeighbourWeight() {
        return enemyNeighbourWeight;
    }

    public double getFriendlyArmyWeight() {
        return friendlyArmyWeight;
    }

    public double getEnemyArmyWeight() {
        return enemyArmyWeight;
    }

    public double getContinentBorderWeight() {
        return continentBorderWeight;
    }

    public double getOwnWholeContinentWeight() {
        return ownWholeContinentWeight;
    }

    public double getEnemyOwnsWholeContinentWeight() {
        return enemyOwnsWholeContinentWeight;
    }

    public double getPercentageOfContinentWeight() {
        return percentageOfContinentWeight;
    }

    public int getGoalLength() {
        return goalLength;
    }

    public double getDefensiveBonus() {
        return defensiveMultiplier;
    }

    public double getOffensiveBonus() {
        return offensiveMultiplier;
    }

    public double getKillEnemyWeight() {
        return killEnemyWeight;
    }

    public double getClusteringWeight() {
        return clusteringWeight;
    }

    public double getAttackFirstCountryWeight() { return attackFirstCountryWeight; }
}
