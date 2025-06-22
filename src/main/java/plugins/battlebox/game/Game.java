package plugins.battlebox.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import config.ArenaConfig;

public class Game {
    private final String id;
    private final String arenaId;
    private GameState state;
    private final Set<UUID> players;
    private final int MAX_PLAYERS = 8;
    private Location arenaLocation; // Arena instance location

    // Team management
    private final Map<UUID, TeamColor> playerTeams = new HashMap<>();
    private boolean hasWinner = false;
    private String winReason = "";
    private boolean isDraw = false;

    public Game(String id, String arenaId) {
        this.id = id;
        this.arenaId = arenaId;
        this.state = GameState.WAITING;
        this.players = new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public String getArenaId() {
        return arenaId;
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState newState) {
        this.state = newState;
    }

    public String getTimerId() {
        return "game_" + this.id;
    }

    public boolean addPlayer(Player player) {
        if (players.contains(player.getUniqueId())) {
            player.sendMessage("You are already in the game!");
            return false;
        }
        if (players.size() >= MAX_PLAYERS) {
            player.sendMessage("The game is full!");
            return false;
        }

        // Only allow players to join during WAITING and KIT_SELECTION states
        if (state != GameState.WAITING && state != GameState.KIT_SELECTION) {
            player.sendMessage("Cannot join game - game is in progress or ending!");
            return false;
        }

        players.add(player.getUniqueId());

        // Auto-assign team based on current team sizes
        assignPlayerToTeam(player);

        // Handle teleportation here.
        player.sendMessage("§aYou joined the game: §e" + id);
        Bukkit.getLogger().info(player.getName() + " joined game " + id);
        return true;
    }

    public void removePlayer(Player player) {
        players.remove(player.getUniqueId());
    }

    public void startGame() { // Teleport the players to the arena spawn points (below)
        // Start a countdown, then set the GameState to IN_PROGRESS and the barrier will
        // be
        // removed, Game Starts
        // Handle the game logic here
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // TODO: tp the player to the arena spawn point (get it from the config)
            }
        }
    }

    public Location getArenaLocation() {
        return arenaLocation;
    }

    public void setArenaLocation(Location arenaLocation) {
        this.arenaLocation = arenaLocation;
    }

    public Set<Player> getPlayers() {
        Set<Player> onlinePlayers = new HashSet<>();
        for (UUID playerId : players) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                onlinePlayers.add(player);
            }
        }
        return onlinePlayers;
    }

    public int getPlayerCount() {
        return players.size();
    }

    // Team management methods
    public TeamColor getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public void setPlayerTeam(Player player, TeamColor team) {
        playerTeams.put(player.getUniqueId(), team);
    }

    public Map<TeamColor, Integer> getTeamCounts() {
        Map<TeamColor, Integer> counts = new HashMap<>();
        for (TeamColor color : TeamColor.values()) {
            counts.put(color, 0);
        }

        for (TeamColor team : playerTeams.values()) {
            if (team != null) {
                counts.put(team, counts.get(team) + 1);
            }
        }

        return counts;
    }

    public Set<UUID> getTeamPlayers(TeamColor team) {
        Set<UUID> teamPlayers = new HashSet<>();
        for (Map.Entry<UUID, TeamColor> entry : playerTeams.entrySet()) {
            if (entry.getValue() == team) {
                teamPlayers.add(entry.getKey());
            }
        }
        return teamPlayers;
    }

    // Game logic methods
    public void calculateWinner(ArenaConfig arena) {
        // Reset previous state
        hasWinner = false;
        isDraw = false;
        winReason = "";

        // Get the center box coordinates from arena config
        if (arena == null || arena.centerBox == null) {
            winReason = "Arena center box not configured!";
            return;
        }
        // Assume centerBox has x1, y1, z1, x2, y2, z2
        int minX = Math.min(arena.centerBox.x1, arena.centerBox.x2);
        int maxX = Math.max(arena.centerBox.x1, arena.centerBox.x2);
        int minZ = Math.min(arena.centerBox.z1, arena.centerBox.z2);
        int maxZ = Math.max(arena.centerBox.z1, arena.centerBox.z2);
        int y = arena.centerBox.y1;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(arena.world);
        if (world == null) {
            winReason = "Arena world not found!";
            return;
        }

        int redCount = 0, blueCount = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                if (block.getType() == org.bukkit.Material.RED_WOOL) {
                    redCount++;
                } else if (block.getType() == org.bukkit.Material.BLUE_WOOL) {
                    blueCount++;
                }
            }
        }
        int boxSize = (maxX - minX + 1) * (maxZ - minZ + 1);
        if (redCount == boxSize) {
            hasWinner = true;
            winReason = "Red team filled the center!";
        } else if (blueCount == boxSize) {
            hasWinner = true;
            winReason = "Blue team filled the center!";
        } else if (state == GameState.IN_PROGRESS) {
            return; // Game still in progress, no winner yet
        } else if (redCount > blueCount) {
            hasWinner = true;
            winReason = "Red team has majority: " + redCount + " vs " + blueCount;
        } else if (blueCount > redCount) {
            hasWinner = true;
            winReason = "Blue team has majority: " + blueCount + " vs " + redCount;
        } else {
            isDraw = true;
            winReason = "Draw! Equal wool count or no wool placed.";
        }
    }

    public boolean hasWinner() {
        return hasWinner;
    }

    public boolean isDraw() {
        return isDraw;
    }

    public String getWinReason() {
        return winReason;
    }

    public TeamColor getWinner() {
        if (!hasWinner)
            return null;

        // Parse the win reason to determine which team won
        if (winReason.toLowerCase().contains("red")) {
            return TeamColor.RED;
        } else if (winReason.toLowerCase().contains("blue")) {
            return TeamColor.BLUE;
        }
        return null;
    }

    public String getWoolCountInfo(ArenaConfig arena) {
        if (arena == null || arena.centerBox == null) {
            return "Arena center box not configured!";
        }
        int minX = Math.min(arena.centerBox.x1, arena.centerBox.x2);
        int maxX = Math.max(arena.centerBox.x1, arena.centerBox.x2);
        int minZ = Math.min(arena.centerBox.z1, arena.centerBox.z2);
        int maxZ = Math.max(arena.centerBox.z1, arena.centerBox.z2);
        int y = arena.centerBox.y1;
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(arena.world);

        if (world == null) {
            return "Arena world not found!";
        }

        int redCount = 0, blueCount = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                if (block.getType() == org.bukkit.Material.RED_WOOL) {
                    redCount++;
                } else if (block.getType() == org.bukkit.Material.BLUE_WOOL) {
                    blueCount++;
                }
            }
        }

        return ChatColor.RED + "Red wool: " + redCount + ChatColor.RESET + " | " +
                ChatColor.BLUE + "Blue wool: " + blueCount;
    }

    /**
     * Check if a block placement location is within the center area
     */
    public boolean isValidPlacementLocation(org.bukkit.Location location, ArenaConfig arena) {
        if (arena == null || arena.centerBox == null) {
            return false;
        }
        int minX = Math.min(arena.centerBox.x1, arena.centerBox.x2);
        int maxX = Math.max(arena.centerBox.x1, arena.centerBox.x2);
        int minZ = Math.min(arena.centerBox.z1, arena.centerBox.z2);
        int maxZ = Math.max(arena.centerBox.z1, arena.centerBox.z2);
        int y = arena.centerBox.y1;

        return location.getBlockX() >= minX && location.getBlockX() <= maxX &&
                location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ &&
                location.getBlockY() == y;
    }

    /**
     * Assign a player to the team with fewer players
     */
    private void assignPlayerToTeam(Player player) {
        Map<TeamColor, Integer> teamCounts = getTeamCounts();

        // For demo, we only use RED and BLUE teams
        int redCount = teamCounts.get(TeamColor.RED);
        int blueCount = teamCounts.get(TeamColor.BLUE);

        TeamColor assignedTeam;
        if (redCount <= blueCount) {
            assignedTeam = TeamColor.RED;
        } else {
            assignedTeam = TeamColor.BLUE;
        }

        setPlayerTeam(player, assignedTeam);
        player.sendMessage(
                assignedTeam.chatColor + "You have been assigned to the " + assignedTeam.displayName + " team!");
    }

    // Team color enum
    public enum TeamColor {
        RED("Red", "§c"),
        BLUE("Blue", "§9");

        public final String displayName;
        public final String chatColor;

        TeamColor(String displayName, String chatColor) {
            this.displayName = displayName;
            this.chatColor = chatColor;
        }
    }
}
