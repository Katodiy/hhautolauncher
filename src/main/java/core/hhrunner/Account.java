package core.hhrunner;

import java.util.ArrayList;
import java.util.List;

public class Account {
    public String username;
    public String password;
    public List<String> characters;
    
    public Account() {
        this.characters = new ArrayList<>();
    }
    
    public Account(String username, String password) {
        this.username = username;
        this.password = password;
        this.characters = new ArrayList<>();
    }
    
    public void addCharacter(String characterName) {
        if (!characters.contains(characterName)) {
            characters.add(characterName);
        }
    }
    
    public void removeCharacter(String characterName) {
        characters.remove(characterName);
    }
    
    @Override
    public String toString() {
        return username + " (" + characters.size() + " characters)";
    }
}