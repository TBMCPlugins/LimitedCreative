package de.jaschastarke.minecraft.limitedcreative.hooks;

import de.jaschastarke.minecraft.limitedcreative.Hooks;
import de.jaschastarke.minecraft.limitedcreative.LimitedCreative;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.entity.Player;

//@Deprecated // AuthMe 3.0 released. Compatibility for older versions will be removed sometime
public class AuthMeHooks {
    public AuthMeHooks(final LimitedCreative plugin) {
        Hooks.IsLoggedIn.register(new PlayerCheckHooker.Check() {
            @Override
            public boolean test(Player player) {
                boolean li = AuthMeApi.getInstance().isAuthenticated(player);
                if (plugin.isDebug()) // not nessesary, but so no string concation without debug needed
                    plugin.getLog().debug("AuthMe: "+player.getName()+": logged in: "+li);
                return li;
            }
        });
    }
}
