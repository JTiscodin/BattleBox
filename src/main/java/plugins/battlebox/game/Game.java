package plugins.battlebox.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import config.ArenaConfig;
import config.Box;

import java.util.*;


public class Game {
    private final String id;
    private final String arenaId;
    private GameState state;
    private final Set<UUID> players;
    private final int MAX_PLAYERS = 8;
    
    // Team management
    private final Map<UUID, TeamColor> playerTeams;
    private final Map<TeamColor, Set<UUID>> teams;
    
    // Winner tracking
    private TeamColor winner;
    private String winReason;
    
    public enum TeamColor {
        RED(DyeColor.RED, ChatColor.RED, "Red"),
        BLUE(DyeColor.BLUE, ChatColor.BLUE, "Blue"),
        GREEN(DyeColor.GREEN, ChatColor.GREEN, "Green"),
        YELLOW(DyeColor.YELLOW, ChatColor.YELLOW, "Yellow");
        
        public final DyeColor dyeColor;
        public final ChatColor chatColor;
        public final String displayName;
        
        TeamColor(DyeColor dyeColor, ChatColor chatColor, String displayName) {
            this.dyeColor = dyeColor;
            this.chatColor = chatColor;
            this.displayName = displayName;
        }
        
        public Material getWoolMaterial() {
            return switch (this) {
                case RED -> Material.RED_WOOL;
                case BLUE -> Material.BLUE_WOOL;
                case GREEN -> Material.GREEN_WOOL;
                case YELLOW -> Material.YELLOW_WOOL;
            };
        }
    }

    public Game(String id, String arenaId){
        this.id = id;
        this.arenaId = arenaId;
        this.state = GameState.WAITING;
        this.players = new HashSet<>();
        this.playerTeams = new HashMap<>();
        this.teams = new EnumMap<>(TeamColor.class);
        
        // Initialize teams
        for (TeamColor color : TeamColor.values()) {
            teams.put(color, new HashSet<>());
        }
    }

    public String getId(){
        return id;
    }

    public String getArenaId(){
        return arenaId;
    }

    public GameState getState(){
        return state;
    }

    public void setState(GameState newState){
        this.state = newState;
    }

    public boolean addPlayer(Player player){
        if(players.contains(player.getUniqueId())){
            player.sendMessage("You are already in the game!");
            return false;
        }
        if(players.size() >= MAX_PLAYERS){
            player.sendMessage("The game is full!");
            return false;
        }
        
        // Assign player to a team
        TeamColor assignedTeam = assignPlayerToTeam();
        players.add(player.getUniqueId());
        playerTeams.put(player.getUniqueId(), assignedTeam);
        teams.get(assignedTeam).add(player.getUniqueId());
        
        player.sendMessage("§aYou joined the game: §e" + id);
        player.sendMessage("§aYou are on team: " + assignedTeam.chatColor + assignedTeam.displayName);
        
        Bukkit.getLogger().info(player.getName() + " joined game " + id);
        return true;
    }
    
    public void removePlayer(Player player){
        UUID playerId = player.getUniqueId();
        players.remove(playerId);
        TeamColor team = playerTeams.remove(playerId);
        if (team != null) {
            teams.get(team).remove(playerId);
        }
    }
    
    private TeamColor assignPlayerToTeam() {
        // Find team with fewest players
        TeamColor smallestTeam = TeamColor.RED;
        int smallestSize = teams.get(TeamColor.RED).size();
        
        for (TeamColor color : TeamColor.values()) {
            int teamSize = teams.get(color).size();
            if (teamSize < smallestSize) {
                smallestTeam = color;
                smallestSize = teamSize;
            }
        }
        
        return smallestTeam;
    }
    
    public TeamColor getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }
    
    public Set<UUID> getTeamPlayers(TeamColor team) {
        return new HashSet<>(teams.get(team));
    }
    
    public Map<TeamColor, Integer> getTeamCounts() {
        Map<TeamColor, Integer> counts = new EnumMap<>(TeamColor.class);
        for (TeamColor color : TeamColor.values()) {
            counts.put(color, teams.get(color).size());
        }
        return counts;
    }
    
    /**
     * Calculate the winner based on wool blocks in the arena box
     */
    public void calculateWinner(ArenaConfig arena) {
        if (arena == null || arena.box == null) {
            winner = null;
            winReason = "Arena configuration invalid";
            return;
        }
        
        World world = Bukkit.getWorld(arena.world);
        if (world == null) {
            winner = null;
            winReason = "World not found: " + arena.world;
            return;
        }
        
        Map<TeamColor, Integer> woolCounts = countWoolInBox(world, arena.box);
        
        // Find team with most wool
        TeamColor winningTeam = null;
        int maxWool = 0;
        boolean isTie = false;
        
        for (Map.Entry<TeamColor, Integer> entry : woolCounts.entrySet()) {
            if (entry.getValue() > maxWool) {
                winningTeam = entry.getKey();
                maxWool = entry.getValue();
                isTie = false;
            } else if (entry.getValue() == maxWool && maxWool > 0) {
                isTie = true;
            }
        }
          if (maxWool == 0) {
            winner = null;
            winReason = "Draw - No wool placed in the box";
        } else if (isTie) {
            winner = null;
            winReason = "Draw - Teams tied with " + maxWool + " wool blocks each";
        } else if (winningTeam != null) {
            winner = winningTeam;
            winReason = winningTeam.chatColor + winningTeam.displayName + " team wins with " + maxWool + " wool blocks!";
        } else {
            winner = null;
            winReason = "Draw - Unable to determine winner";
        }
    }
    
    private Map<TeamColor, Integer> countWoolInBox(World world, Box box) {
        Map<TeamColor, Integer> counts = new EnumMap<>(TeamColor.class);
        
        // Initialize counts
        for (TeamColor color : TeamColor.values()) {
            counts.put(color, 0);
        }
        
        // Get box boundaries
        int minX = Math.min(box.x1, box.x2);
        int maxX = Math.max(box.x1, box.x2);
        int minY = Math.min(box.y1, box.y2);
        int maxY = Math.max(box.y1, box.y2);
        int minZ = Math.min(box.z1, box.z2);
        int maxZ = Math.max(box.z1, box.z2);
        
        // Count wool blocks in the box
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location loc = new Location(world, x, y, z);
                    Material blockType = world.getBlockAt(loc).getType();
                    
                    for (TeamColor color : TeamColor.values()) {
                        if (blockType == color.getWoolMaterial()) {
                            counts.put(color, counts.get(color) + 1);
                            break;
                        }
                    }
                }
            }
        }
        
        return counts;
    }
    
    public TeamColor getWinner() {
        return winner;
    }
    
    public String getWinReason() {
        return winReason;
    }
    
    public boolean hasWinner() {
        return winner != null;
    }
    
    public boolean isDraw() {
        return winner == null && winReason != null && !winReason.contains("invalid");
    }
    
    /**
     * Get detailed wool count information
     */
    public String getWoolCountInfo(ArenaConfig arena) {
        if (arena == null || arena.box == null) {
            return ChatColor.RED + "Arena configuration invalid";
        }
        
        World world = Bukkit.getWorld(arena.world);
        if (world == null) {
            return ChatColor.RED + "World not found: " + arena.world;
        }
        
        Map<TeamColor, Integer> woolCounts = countWoolInBox(world, arena.box);
        
        StringBuilder info = new StringBuilder();
        info.append(ChatColor.GOLD).append("=== Wool Count in Box ===\n");
        
        for (TeamColor color : TeamColor.values()) {
            int count = woolCounts.get(color);
            info.append(color.chatColor).append(color.displayName).append(" Team: ")
                .append(ChatColor.WHITE).append(count).append(" blocks\n");
        }
        
        return info.toString();
    }
      public int getPlayerCount() {
        return players.size();
    }
    
    /**
     * Get all online players in this game
     */
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

    public void startGame(){

        // Teleport the players to the arena spawn points (below)
        // Start a countdown, the GameState will be set to STARTING
        // After the countdown, set the GameState to IN_PROGRESS and the barrier will be removed, Game Starts
        // Handle the game logic here
        for(UUID playerId: players){
            Player player = Bukkit.getPlayer(playerId);
            if(player != null){
                //TODO: tp the player to the arena spawn point (get it from the config)
            }
        }
    }
}
