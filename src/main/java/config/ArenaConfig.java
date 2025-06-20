package config;

import config.Kit.Kit;

public class ArenaConfig {
    public String id;
    public String world;
    public Box centerBox; // 3x3 center area for wool placement
    public TeamSpawns teamSpawns;
    public Kit[] kits;
    
    public static class TeamSpawns {
        public Location redSpawn;
        public Location blueSpawn;
        public Location redTeleport;
        public Location blueTeleport;
    }
    
    public static class Location {
        public double x, y, z;
        public float yaw, pitch;
        
        public Location() {}
        
        public Location(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
        
        public org.bukkit.Location toBukkitLocation(org.bukkit.World world) {
            return new org.bukkit.Location(world, x, y, z, yaw, pitch);
        }
        
        public static Location fromBukkitLocation(org.bukkit.Location loc) {
            return new Location(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        }
    }
}
