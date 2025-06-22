package plugins.battlebox.core;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameState;

/**
 * Service for managing music and sound effects during BattleBox games.
 * Handles background music, event sounds, and player-specific audio control.
 */
public class MusicService {

    private final JavaPlugin plugin;

    // Music state management
    private final Map<String, BukkitTask> gameMusicTasks = new ConcurrentHashMap<>();
    private final Map<UUID, MusicPreference> playerPreferences = new ConcurrentHashMap<>();
    private final Map<String, GameMusicState> gameMusicStates = new ConcurrentHashMap<>();
    // Music configuration
    private static final float DEFAULT_VOLUME = 0.5f;
    private static final float DEFAULT_PITCH = 1.0f;

    public MusicService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Music events that can trigger different audio
     */
    public enum MusicEvent {
        GAME_WAITING("Waiting Room", Sound.MUSIC_DISC_WAIT, 220), // 11 seconds
        GAME_KIT_SELECTION("Kit Selection", Sound.MUSIC_DISC_CAT, 185), // 9.25 seconds
        GAME_STARTING("Game Starting", Sound.MUSIC_DISC_STAL, 150), // 7.5 seconds
        GAME_IN_PROGRESS("Battle Music", Sound.MUSIC_DISC_PIGSTEP, 300), // 15 seconds
        GAME_ENDING("Victory", Sound.MUSIC_DISC_OTHERSIDE, 195), // 9.75 seconds

        // Event sounds (not looped)
        PLAYER_JOIN("Player Join", Sound.ENTITY_PLAYER_LEVELUP, 0),
        PLAYER_LEAVE("Player Leave", Sound.BLOCK_IRON_DOOR_CLOSE, 0),
        WOOL_PLACED("Wool Placed", Sound.BLOCK_WOOL_PLACE, 0),
        COUNTDOWN_TICK("Countdown", Sound.BLOCK_NOTE_BLOCK_PLING, 0),
        COUNTDOWN_FINAL("Final Countdown", Sound.BLOCK_NOTE_BLOCK_BELL, 0),
        TEAM_WIN("Team Victory", Sound.ENTITY_ENDER_DRAGON_DEATH, 0),
        GAME_DRAW("Game Draw", Sound.ENTITY_VILLAGER_NO, 0);

        public final String displayName;
        public final Sound sound;
        public final int durationTicks; // 0 means one-time sound

        MusicEvent(String displayName, Sound sound, int durationTicks) {
            this.displayName = displayName;
            this.sound = sound;
            this.durationTicks = durationTicks;
        }

        public boolean isLooped() {
            return durationTicks > 0;
        }
    }

    /**
     * Player music preferences
     */
    public static class MusicPreference {
        private boolean musicEnabled = true;
        private boolean soundEffectsEnabled = true;
        private float musicVolume = DEFAULT_VOLUME;
        private float soundVolume = DEFAULT_VOLUME;

        // Getters and setters
        public boolean isMusicEnabled() {
            return musicEnabled;
        }

        public void setMusicEnabled(boolean enabled) {
            this.musicEnabled = enabled;
        }

        public boolean isSoundEffectsEnabled() {
            return soundEffectsEnabled;
        }

        public void setSoundEffectsEnabled(boolean enabled) {
            this.soundEffectsEnabled = enabled;
        }

        public float getMusicVolume() {
            return musicVolume;
        }

        public void setMusicVolume(float volume) {
            this.musicVolume = Math.max(0.0f, Math.min(1.0f, volume));
        }

        public float getSoundVolume() {
            return soundVolume;
        }

        public void setSoundVolume(float volume) {
            this.soundVolume = Math.max(0.0f, Math.min(1.0f, volume));
        }
    }

    /**
     * Game music state tracking
     */
    private static class GameMusicState {
        private MusicEvent currentMusic;

        public GameMusicState() {
            this.currentMusic = MusicEvent.GAME_WAITING;
        }
    }

    // =================================================================
    // PUBLIC API METHODS
    // =================================================================

    /**
     * Start music for a specific game based on its state
     */
    public void startGameMusic(Game game) {
        String gameId = game.getId();
        MusicEvent musicEvent = getMusicEventForGameState(game.getState());

        gameMusicStates.put(gameId, new GameMusicState());
        playGameMusic(game, musicEvent);

        plugin.getLogger().info("Started music for game " + gameId + " with event: " + musicEvent.displayName);
    }

    /**
     * Update music when game state changes
     */
    public void updateGameMusic(Game game) {
        MusicEvent newMusicEvent = getMusicEventForGameState(game.getState());
        playGameMusic(game, newMusicEvent);
    }

    /**
     * Stop all music for a game
     */
    public void stopGameMusic(String gameId) {
        BukkitTask task = gameMusicTasks.remove(gameId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }

        gameMusicStates.remove(gameId);
        plugin.getLogger().info("Stopped music for game " + gameId);
    }

    /**
     * Play a one-time sound effect for all players in a game
     */
    public void playGameSoundEffect(Game game, MusicEvent soundEvent) {
        if (soundEvent.isLooped()) {
            plugin.getLogger().warning("Attempted to play looped music event as sound effect: " + soundEvent);
            return;
        }

        Set<Player> players = game.getPlayers();
        for (Player player : players) {
            playSoundEffect(player, soundEvent);
        }
    }

    /**
     * Play a sound effect for a specific player
     */
    public void playSoundEffect(Player player, MusicEvent soundEvent) {
        MusicPreference pref = getPlayerPreference(player);

        if (!pref.isSoundEffectsEnabled()) {
            return;
        }

        player.playSound(player.getLocation(), soundEvent.sound,
                SoundCategory.MASTER, pref.getSoundVolume(), DEFAULT_PITCH);
    }

    /**
     * Handle player joining a game
     */
    public void onPlayerJoinGame(Player player, Game game) {
        // Play join sound for other players
        playSoundEffect(player, MusicEvent.PLAYER_JOIN);

        // Start appropriate music for the new player
        GameMusicState musicState = gameMusicStates.get(game.getId());
        if (musicState != null && musicState.currentMusic != null) {
            playMusicForPlayer(player, musicState.currentMusic);
        }
    }

    /**
     * Handle player leaving a game
     */
    public void onPlayerLeaveGame(Player player, Game game) {
        playSoundEffect(player, MusicEvent.PLAYER_LEAVE);
        stopMusicForPlayer(player);
    }

    /**
     * Handle countdown events
     */
    public void onCountdownTick(Game game, int secondsLeft) {
        MusicEvent soundEvent = (secondsLeft <= 3) ? MusicEvent.COUNTDOWN_FINAL : MusicEvent.COUNTDOWN_TICK;
        playGameSoundEffect(game, soundEvent);
    }

    /**
     * Handle wool placement
     */
    public void onWoolPlaced(Player player, Game game) {
        playSoundEffect(player, MusicEvent.WOOL_PLACED);
    }

    /**
     * Handle game end with winner
     */
    public void onGameEnd(Game game, Game.TeamColor winner) {
        if (winner != null) {
            playGameSoundEffect(game, MusicEvent.TEAM_WIN);
        } else {
            playGameSoundEffect(game, MusicEvent.GAME_DRAW);
        }

        // Stop the current music after a delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playGameMusic(game, MusicEvent.GAME_ENDING);
        }, 40L); // 2 seconds delay
    }

    // =================================================================
    // PLAYER PREFERENCE MANAGEMENT
    // =================================================================

    /**
     * Get player's music preferences
     */
    public MusicPreference getPlayerPreference(Player player) {
        return playerPreferences.computeIfAbsent(player.getUniqueId(), k -> new MusicPreference());
    }

    /**
     * Update player's music preferences
     */
    public void updatePlayerPreference(Player player, MusicPreference preference) {
        playerPreferences.put(player.getUniqueId(), preference);
    }

    /**
     * Toggle player's music on/off
     */
    public boolean togglePlayerMusic(Player player) {
        MusicPreference pref = getPlayerPreference(player);
        pref.setMusicEnabled(!pref.isMusicEnabled());

        if (!pref.isMusicEnabled()) {
            stopMusicForPlayer(player);
        }

        return pref.isMusicEnabled();
    }

    /**
     * Toggle player's sound effects on/off
     */
    public boolean togglePlayerSoundEffects(Player player) {
        MusicPreference pref = getPlayerPreference(player);
        pref.setSoundEffectsEnabled(!pref.isSoundEffectsEnabled());
        return pref.isSoundEffectsEnabled();
    } // =================================================================
    // PRIVATE HELPER METHODS
    // =================================================================

    private MusicEvent getMusicEventForGameState(GameState state) {
        return switch (state) {
            case WAITING -> MusicEvent.GAME_WAITING;
            case KIT_SELECTION -> MusicEvent.GAME_STARTING; // Use GAME_STARTING music for kit selection
            case IN_PROGRESS -> MusicEvent.GAME_IN_PROGRESS;
            case ENDING -> MusicEvent.GAME_ENDING;
        };
    }

    private void playGameMusic(Game game, MusicEvent musicEvent) {
        String gameId = game.getId();
        GameMusicState musicState = gameMusicStates.get(gameId);

        if (musicState == null) {
            return;
        }

        // Stop current music if playing
        BukkitTask currentTask = gameMusicTasks.get(gameId);
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel();
        }

        musicState.currentMusic = musicEvent;

        // Start new music for all players in the game
        Set<Player> players = game.getPlayers();
        for (Player player : players) {
            playMusicForPlayer(player, musicEvent);
        } // If it's looped music, set up the repeating task
        if (musicEvent.isLooped()) {
            BukkitTask musicTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                Set<Player> currentPlayers = game.getPlayers();
                for (Player player : currentPlayers) {
                    playMusicForPlayer(player, musicEvent);
                }
            }, 0L, musicEvent.durationTicks);

            gameMusicTasks.put(gameId, musicTask);
        }
    }

    private void playMusicForPlayer(Player player, MusicEvent musicEvent) {
        MusicPreference pref = getPlayerPreference(player);

        if (!pref.isMusicEnabled()) {
            return;
        }

        player.playSound(player.getLocation(), musicEvent.sound,
                SoundCategory.MUSIC, pref.getMusicVolume(), DEFAULT_PITCH);
    }

    private void stopMusicForPlayer(Player player) {
        // Note: Minecraft doesn't have a direct way to stop sounds for a specific
        // player
        // This is a limitation of the Bukkit API
        // You could implement a workaround using resource packs or client-side mods
        plugin.getLogger().fine("Stopped music for player " + player.getName());
    }

    /**
     * Cleanup when plugin disables
     */
    public void shutdown() {
        // Cancel all music tasks
        for (BukkitTask task : gameMusicTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }

        gameMusicTasks.clear();
        gameMusicStates.clear();
        playerPreferences.clear();

        plugin.getLogger().info("MusicService shutdown complete");
    }
}
