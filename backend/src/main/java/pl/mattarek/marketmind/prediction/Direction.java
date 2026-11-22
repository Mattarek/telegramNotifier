package pl.mattarek.marketmind.prediction;

public enum Direction {
    UP,
    DOWN,
    FLAT;

    public static Direction parse(final String value) {
        if (value == null) {
            return FLAT;
        }
        return switch (value.strip().toUpperCase()) {
            case "UP", "LONG", "BULLISH" -> UP;
            case "DOWN", "SHORT", "BEARISH" -> DOWN;
            default -> FLAT;
        };
    }
}
