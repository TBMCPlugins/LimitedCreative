/*
 * Limited Creative - (Bukkit Plugin)
 * Copyright (C) 2012 jascha@ja-s.de
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.jaschastarke.minecraft.limitedcreative.inventories.store;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.YamlConfiguration;

import de.jaschastarke.bukkit.lib.CoreModule;
import de.jaschastarke.bukkit.lib.ModuleLogger;
import de.jaschastarke.minecraft.limitedcreative.inventories.Inventory;
import de.jaschastarke.minecraft.limitedcreative.inventories.Inventory.Target;

public class InvYamlStorage extends InvConfStorage {
    private static final String SUFFIX = ".yml";
    
    private CoreModule<?> mod;
    private File dir;
    public InvYamlStorage(CoreModule<?> mod, File file) {
        this.mod = mod;
        dir = file;
    }
    
    public ModuleLogger getLog() {
        return mod.getLog();
    }
    
    @Override
    public void load(Inventory pinv, Target target) {
        load(pinv, YamlConfiguration.loadConfiguration(getFile(pinv, target, false)));
    }
    
    @Override
    public void store(Inventory pinv, Target target) {
        YamlConfiguration yml = new YamlConfiguration();
        yml.options().header("DO NOT MODIFY THIS FILE");
        store(pinv, yml);
        try {
            File nameFile=getFile(pinv, target, false);
            File uuidFile=getFile(pinv, target, true);
            if(!nameFile.equals(uuidFile)) //It'd be recreated right after, still, don't remove if the same
                nameFile.delete(); //Delete file with name so it doesn't get loaded again
            yml.save(uuidFile);
        } catch (IOException e) {
            mod.getLog().warn("Failed to save Inventory for Player " + pinv.getPlayer().getName());
            e.printStackTrace();
        }
    }

    @Override
    public void remove(Inventory pinv, Target target) {
        getFile(pinv, target, true).delete();
    }

    @Override
    public boolean contains(Inventory pinv, Target target) {
        return getFile(pinv, target, false).exists();
    }
    
    protected File getFile(Inventory pinv, Target target, boolean uuidonly) {
        File file;
        String player;
        do {
            player = uuidonly ? pinv.getPlayer().getUniqueId().toString() : pinv.getPlayer().getName();
            if (target != default_target) {
                file = new File(dir, player + "_" + target.toString().toLowerCase() + SUFFIX);
            } else {
                file = new File(dir, player + SUFFIX);
            }
            if(uuidonly)
                return file; //Use file with UUID, even if doesn't exist
            uuidonly = true; //Run again with UUID, then return...
        } while(!file.exists()); //...if the file with name is not found
        return file; //Found file with player name
    }
}
