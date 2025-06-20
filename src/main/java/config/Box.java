package config;

public class Box {
    public int x1, y1, z1;
    public int x2, y2, z2;
    
    public Box() {}
    
    public Box(int x1, int y1, int z1, int x2, int y2, int z2) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
    }
    
    public boolean contains(int x, int y, int z) {
        return x >= Math.min(x1, x2) && x <= Math.max(x1, x2) &&
               y >= Math.min(y1, y2) && y <= Math.max(y1, y2) &&
               z >= Math.min(z1, z2) && z <= Math.max(z1, z2);
    }
}
