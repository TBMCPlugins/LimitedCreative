package de.jaschastarke.minecraft.limitedcreative.regions;

import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.FlagContext;
import com.sk89q.worldguard.protection.flags.InvalidFlagFormat;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import org.bukkit.GameMode;

/**
 * Well, that was an interesting idea, but it doesn't work.
 */
public class GameModeFlag extends Flag<GameMode> {
    public GameModeFlag(String name, RegionGroup defaultGroup) {
        super(name, defaultGroup);
    }
    
    @Override
    public GameMode parseInput(FlagContext context) throws InvalidFlagFormat {
        String input = context.getUserInput();
        input = input.trim();
        if (input.equalsIgnoreCase("creative")) {
            return GameMode.CREATIVE;
        } else if (input.equalsIgnoreCase("survival")) {
            return GameMode.SURVIVAL;
        } else if (input.equalsIgnoreCase("adventure")) {
            return GameMode.ADVENTURE;
        } else if (input.equalsIgnoreCase("none")) {
            return null;
        } else {
            throw new InvalidFlagFormat("Expected survival/creative/none but got '" + input + "'");
        }
    }
    
    @Override
    public GameMode unmarshal(Object o) {
        GameMode gm = null;
        if (o != null) {
            gm = GameMode.valueOf((String) o);
        }
        return gm;
    }

    @Override
    public Object marshal(GameMode o) {
        return o == null ? null : o.name();
    }
}
