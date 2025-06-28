package plugins.battlebox.core;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import plugins.battlebox.game.Game;
import plugins.battlebox.game.GameState;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultra-simple music service - ALWAYS stop before playing new music
 * No overlaps, no complex state, just works
 */
public class MusicService {

    private final JavaPlugin plugin;

    // Global state: what music is currently playing for each game
    private final Map<String, GameMusicInfo> currentGameMusic = new ConcurrentHashMap<>();
    private final Map<UUID, MusicPreference> playerPreferences = new ConcurrentHashMap<>();

    private static final float DEFAULT_VOLUME = 0.5f;
    private static final float DEFAULT_PITCH = 1.0f;

    public MusicService(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("MusicService initialized - Ultra-simple mode");
    }

    /**
     * Music events
     */
    public enum MusicEvent {
        // Background music (looped)
        GAME_WAITING("Waiting Room", "battlebox.waiting", 2680),
        GAME_BATTLE("Battle Music", "battlebox.music", 3600),
        GAME_ENDING("Victory Music", "battlebox.walkoffame", 1540),

        // Sound effects (one-time)
        PLAYER_JOIN("Player Join", Sound.ENTITY_PLAYER_LEVELUP, 0),
        PLAYER_LEAVE("Player Leave", Sound.BLOCK_IRON_DOOR_CLOSE, 0),
        WOOL_PLACED("Wool Placed", Sound.BLOCK_WOOL_PLACE, 0),
        COUNTDOWN_TICK("Countdown", Sound.BLOCK_NOTE_BLOCK_PLING, 0),
        COUNTDOWN_FINAL("Final Countdown", Sound.BLOCK_NOTE_BLOCK_BELL, 0),
        TEAM_WIN("Team Victory", Sound.ENTITY_ENDER_DRAGON_DEATH, 0),
        GAME_DRAW("Game Draw", Sound.ENTITY_VILLAGER_NO, 0);

        public final String displayName;
        public final Sound sound;
        public final String customSound;
        public final int durationTicks;

        // Constructor for Bukkit sounds
        MusicEvent(String displayName, Sound sound, int durationTicks) {
            this.displayName = displayName;
            this.sound = sound;
            this.customSound = null;
            this.durationTicks = durationTicks;
        }

        // Constructor for custom sounds
        MusicEvent(String displayName, String customSound, int durationTicks) {
            this.displayName = displayName;
            this.sound = null;
            this.customSound = customSound;
            this.durationTicks = durationTicks;
        }

        public boolean isCustomSound() {
            return customSound != null;
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
     * Game music info
     */
    private static class GameMusicInfo {
        private final MusicEvent musicEvent;
        private final BukkitTask loopTask;

        public GameMusicInfo(MusicEvent musicEvent, BukkitTask loopTask) {
            this.musicEvent = musicEvent;
            this.loopTask = loopTask;
        }

        public void stop() {
            if (loopTask != null && !loopTask.isCancelled()) {
                loopTask.cancel();
            }
        }
    }

    // =================================================================
    // PUBLIC API METHODS - SIMPLE AND RELIABLE
    // =================================================================

    /**
     * Start music for a game - ALWAYS stops previous music first
     */
    public void startGameMusic(Game game) {
        MusicEvent musicEvent = getMusicEventForGameState(game.getState());
        playMusicForGame(game, musicEvent);
    }

    /**
     * Update music when game state changes - ALWAYS stops previous music first
     */
    public void updateGameMusic(Game game) {
        MusicEvent musicEvent = getMusicEventForGameState(game.getState());
        playMusicForGame(game, musicEvent);
    }

    /**
     * Stop all music for a game - GUARANTEED to stop everything
     */
    public void stopGameMusic(String gameId) {
        GameMusicInfo info = currentGameMusic.remove(gameId);
        if (info != null) {
            info.stop();
            plugin.getLogger().info("Stopped all music for game " + gameId);
        }

        // EXTRA SAFETY: Stop all sounds for all players in the game (if we can find
        // them)
        stopAllSoundsForGame(gameId);
    }

    /**
     * Play a one-time sound effect
     */
    public void playGameSoundEffect(Game game, MusicEvent soundEvent) {
        if (soundEvent.isLooped()) {
            plugin.getLogger().warning("Cannot play looped music as sound effect: " + soundEvent.displayName);
            return;
        }

        Set<Player> players = game.getRealPlayers();
        for (Player player : players) {
            if (VirtualPlayerUtil.canPerformNetworkOperations(player)) {
                playSoundEffect(player, soundEvent);
            }
        }
    }

    /**
     * Play sound effect for specific player
     */
    public void playSoundEffect(Player player, MusicEvent soundEvent) {
        if (!VirtualPlayerUtil.canPerformNetworkOperations(player))
            return;

        MusicPreference pref = getPlayerPreference(player);
        if (!pref.isSoundEffectsEnabled())
            return;

        try {
            player.playSound(player.getLocation(), soundEvent.sound,
                    SoundCategory.NEUTRAL, pref.getSoundVolume(), DEFAULT_PITCH);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to play sound effect for " + player.getName() + ": " + e.getMessage());
        }
    }

    // Event handlers
    public void onPlayerJoinGame(Player player, Game game) {
        playSoundEffect(player, MusicEvent.PLAYER_JOIN);
    }

    public void onPlayerLeaveGame(Player player, Game game) {
        playSoundEffect(player, MusicEvent.PLAYER_LEAVE);
    }

    public void onCountdownTick(Game game, int secondsLeft) {
        MusicEvent soundEvent = (secondsLeft <= 3) ? MusicEvent.COUNTDOWN_FINAL : MusicEvent.COUNTDOWN_TICK;
        playGameSoundEffect(game, soundEvent);
    }

    public void onWoolPlaced(Player player, Game game) {
        playSoundEffect(player, MusicEvent.WOOL_PLACED);
    }

    public void onGameEnd(Game game, Game.TeamColor winner) {
        if (winner != null) {
            playGameSoundEffect(game, MusicEvent.TEAM_WIN);
        } else {
            playGameSoundEffect(game, MusicEvent.GAME_DRAW);
        }
    }

    // Player preferences
    public MusicPreference getPlayerPreference(Player player) {
        return playerPreferences.computeIfAbsent(player.getUniqueId(), k -> new MusicPreference());
    }

    public void updatePlayerPreference(Player player, MusicPreference preference) {
        playerPreferences.put(player.getUniqueId(), preference);
    }

    public boolean togglePlayerMusic(Player player) {
        MusicPreference pref = getPlayerPreference(player);
        pref.setMusicEnabled(!pref.isMusicEnabled());
        return pref.isMusicEnabled();
    }

    public boolean togglePlayerSoundEffects(Player player) {
        MusicPreference pref = getPlayerPreference(player);
        pref.setSoundEffectsEnabled(!pref.isSoundEffectsEnabled());
        return pref.isSoundEffectsEnabled();
    }

    // =================================================================
    // PRIVATE METHODS - THE CORE LOGIC
    // =================================================================

    private MusicEvent getMusicEventForGameState(GameState state) {
        return switch (state) {
            case WAITING -> MusicEvent.GAME_WAITING;
            case KIT_SELECTION, IN_PROGRESS -> MusicEvent.GAME_BATTLE;
            case ENDING -> MusicEvent.GAME_ENDING;
        };
    }

    /**
     * THE CORE METHOD: Always stop, then play new music
     * FIXED: No more overlapping - music only loops AFTER it finishes
     */
    private void playMusicForGame(Game game, MusicEvent musicEvent) {
        String gameId = game.getId();

        plugin.getLogger().info("MUSIC CHANGE for game " + gameId + ": " + musicEvent.displayName);

        // STEP 1: ALWAYS STOP EVERYTHING FIRST
        stopGameMusic(gameId);

        // STEP 2: Aggressive stopping - stop all sounds for all players immediately
        Set<Player> players = game.getRealPlayers();
        for (Player player : players) {
            if (VirtualPlayerUtil.canPerformNetworkOperations(player)) {
                try {
                    player.stopAllSounds();
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to stop sounds for " + player.getName() + ": " + e.getMessage());
                }
            }
        }

        // STEP 3: Wait for sounds to actually stop
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            // STEP 4: Play new music for all players
            Set<Player> currentPlayers = game.getRealPlayers();
            plugin.getLogger().info("Playing " + musicEvent.displayName + " for " + currentPlayers.size() + " players");

            for (Player player : currentPlayers) {
                if (VirtualPlayerUtil.canPerformNetworkOperations(player)) {
                    playMusicForPlayer(player, musicEvent);
                }
            }

            // STEP 5: Set up looping ONLY if needed and AFTER track finishes
            BukkitTask loopTask = null;
            if (musicEvent.isLooped()) {
                // FIXED: Wait for track to FINISH, then start looping
                loopTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    Set<Player> activePlayers = game.getRealPlayers();
                    plugin.getLogger()
                            .fine("Looping " + musicEvent.displayName + " for " + activePlayers.size() + " players");

                    // Stop any residual sounds before playing again
                    for (Player player : activePlayers) {
                        if (VirtualPlayerUtil.canPerformNetworkOperations(player)) {
                            try {
                                player.stopAllSounds(); // Ensure clean slate
                                playMusicForPlayer(player, musicEvent);
                            } catch (Exception e) {
                                plugin.getLogger().warning(
                                        "Failed to loop music for " + player.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                }, musicEvent.durationTicks + 10L, musicEvent.durationTicks + 10L); // +10 ticks buffer to ensure track
                                                                                    // finishes
            }

            // STEP 6: Store the current music info
            currentGameMusic.put(gameId, new GameMusicInfo(musicEvent, loopTask));

        }, 10L); // Wait 10 ticks (0.5 seconds) for previous sounds to stop
    }

    private void playMusicForPlayer(Player player, MusicEvent musicEvent) {
        if (!VirtualPlayerUtil.canPerformNetworkOperations(player))
            return;

        MusicPreference pref = getPlayerPreference(player);
        if (!pref.isMusicEnabled())
            return;

        try {
            if (musicEvent.isCustomSound()) {
                // Use MUSIC category for better control over custom streaming music
                player.playSound(player, musicEvent.customSound,
                        SoundCategory.NEUTRAL, pref.getMusicVolume(), DEFAULT_PITCH);
            } else {
                player.playSound(player, musicEvent.sound,
                        SoundCategory.NEUTRAL, pref.getMusicVolume(), DEFAULT_PITCH);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to play music for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Extra safety method - try to stop all sounds for players
     */
    private void stopAllSoundsForGame(String gameId) {
        // Find the game and stop all sounds for its players
        try {
            // We need to access the game manager to get the game
            if (plugin instanceof plugins.battlebox.BattleBox) {
                plugins.battlebox.BattleBox battleBoxPlugin = (plugins.battlebox.BattleBox) plugin;
                plugins.battlebox.game.Game game = battleBoxPlugin.getGameManager().getGame(gameId);

                if (game != null) {
                    Set<Player> players = game.getRealPlayers();
                    plugin.getLogger()
                            .info("Force-stopping all sounds for " + players.size() + " players in game " + gameId);

                    for (Player player : players) {
                        if (VirtualPlayerUtil.canPerformNetworkOperations(player)) {
                            try {
                                // Stop ALL sounds for this player
                                player.stopAllSounds();
                                plugin.getLogger().fine("Stopped all sounds for player: " + player.getName());
                            } catch (Exception e) {
                                plugin.getLogger().warning(
                                        "Failed to stop sounds for " + player.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                } else {
                    plugin.getLogger().fine("Could not find game " + gameId + " to stop sounds");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error stopping sounds for game " + gameId + ": " + e.getMessage());
        }
    }

    /**
     * Cleanup when plugin disables
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down MusicService...");

        // Stop all music
        for (GameMusicInfo info : currentGameMusic.values()) {
            info.stop();
        }

        currentGameMusic.clear();
        playerPreferences.clear();

        plugin.getLogger().info("MusicService shutdown complete");
    }
}
