package infomgmag.mars;

public class PersonalityFactory {
    public static Personality agressivePersonality() {
        return new Personality(
                "Aggressive",
                3.5,
                170.0,
                1.2,
                -0.3,
                0.05,
                -0.03,
                0.5,
                20.0,
                4.0,
                5.0,
                4);
    }

    public static Personality normalPersonality() {
        return new Personality(
                "Normal",
                120.0,
                170.0,
                1.2,
                -0.3,
                0.05,
                -0.03,
                0.5,
                20.0,
                4.0,
                5.0,
                4);
    }

    public static Personality defensivePersonality() {
        return new Personality(
                "Defensive",
                250.0,
                170.0,
                1.2,
                -0.3,
                0.05,
                -0.03,
                0.5,
                20.0,
                4.0,
                5.0,
                4);
     }
}
