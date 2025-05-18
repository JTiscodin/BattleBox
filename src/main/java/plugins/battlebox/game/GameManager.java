package plugins.battlebox.game;

import java.util.HashMap;

public class GameManager {
    private final HashMap<String, Game> games;

    public GameManager() {
        this.games = new HashMap<>();
    }
}
