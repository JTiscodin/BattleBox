package config.Kit;

public class Kit {
    public String name;
    public String team; // "red" or "blue"
    public Button[] buttons;
    
    public Kit() {}
    
    public Kit(String name, String team) {
        this.name = name;
        this.team = team;
        this.buttons = new Button[0];
    }
}
