package de.jaschastarke.minecraft.limitedcreative;

import de.jaschastarke.bukkit.lib.CoreModule;
import de.jaschastarke.modularize.IModule;
import de.jaschastarke.modularize.ModuleEntry;
import de.jaschastarke.modularize.ModuleEntry.ModuleState;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.event.Listener;

import java.util.HashMap;

public class FeatureMetrics extends CoreModule<LimitedCreative> implements Listener {
    public FeatureMetrics(LimitedCreative plugin) {
        super(plugin);
    }

    private Metrics bstats = null;

    @Override
    public void onEnable() {
        super.onEnable();
        if (bstats == null) {
            bstats = new Metrics(plugin, 10413);

            bstats.addCustomChart(new AdvancedPie("module_usage", () -> {
                HashMap<String, Integer> ret = new HashMap<>();
                for (final ModuleEntry<IModule> mod : plugin.getModules())
                    if (mod.getModule() instanceof CoreModule<?>)
                        ret.put(((CoreModule<?>) mod.getModule()).getName(), mod.getState() == ModuleState.ENABLED ? 1 : 0);
                return ret;
            }));
            bstats.addCustomChart(new AdvancedPie("dependencies", () -> {
                HashMap<String, Integer> ret = new HashMap<>();
                for (final String dep : plugin.getDescription().getSoftDepend())
                    ret.put(dep, plugin.getServer().getPluginManager().isPluginEnabled(dep) ? 1 : 0);
                return ret;
            }));
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
