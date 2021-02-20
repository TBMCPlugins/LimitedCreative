package de.jaschastarke.minecraft.limitedcreative.blockstate.worldedit;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import de.jaschastarke.minecraft.limitedcreative.ModBlockStates;
import de.jaschastarke.minecraft.limitedcreative.blockstate.BlockState;
import de.jaschastarke.minecraft.limitedcreative.blockstate.BlockState.Source;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Date;

public class EditSessionExtent extends AbstractDelegateExtent {
    private ModBlockStates mod;
    private Player player = null;
    private World world;

    public EditSessionExtent(Extent extent, ModBlockStates mod, Player player, World world) {
        super(extent);
        this.mod = mod;
        this.player = player;
        this.world = world;
    }

    /**
     * Called when a block is being changed.
     *
     * @param pt       the position
     * @param newBlock the new block to replace the old one
     */
    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 pt, T newBlock) throws WorldEditException {
        if (mod.isDebug())
            mod.getLog().debug("WorldEdit-Integration: BlockChange: " + pt.toString() + " BB: " + newBlock.toString());
        Location loc = new Location(world, pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
        if (newBlock.getBlockType().getMaterial().isAir()) {
            mod.getModel().removeState(loc.getBlock());
        } else {
            BlockState s = mod.getModel().getState(loc.getBlock());
            if (s == null) {
                s = new BlockState();
                s.setLocation(loc);
            }
            s.setGameMode(null);
            s.setPlayerNameOrUUID(player.getUniqueId().toString());
            s.setDate(new Date());
            s.setSource(Source.EDIT);
            if (mod.isDebug())
                mod.getLog().debug("WorldEdit-Integration: Saving BlockState: " + s.toString());

            mod.getModel().setState(s);
        }
        super.setBlock(pt, newBlock);
        return true;
    }
}
