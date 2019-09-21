package de.jaschastarke.minecraft.limitedcreative.inventories.store;

import de.jaschastarke.bukkit.lib.CoreModule;
import de.jaschastarke.bukkit.lib.ModuleLogger;
import de.jaschastarke.minecraft.limitedcreative.inventories.Inventory;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ReflectionStorage extends PlayerInventoryStorage {
    private CoreModule<?> mod;
    private File dir;
    private String nms;
    private InvYamlStorage yamlStorage;

    public ReflectionStorage(CoreModule<?> mod, File file) {
        this.mod = mod;
        dir = file;
        yamlStorage = new InvYamlStorage(mod, file);
    }

    @Override
    public ModuleLogger getLog() {
        return mod.getLog();
    }

    private File getFile(UUID uuid) {
        return new File(dir, uuid.toString() + "_ref.yml");
    }

    private Object getInventory(Player player) throws Exception {
        org.bukkit.inventory.Inventory inv = player.getInventory();
        if (getInventory == null)
            getInventory = inv.getClass().getMethod("getInventory");
        Object handle = getInventory.invoke(inv);
        if (nms == null)
            nms = handle.getClass().getPackage().getName();
        return handle;
    }

    @Override
    public void store(Inventory pinv, Inventory.Target target) {
        try {
            File f = getFile(pinv.getPlayer().getUniqueId());
            YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
            config.set(target.name(), serialize(getInventory(pinv.getPlayer())));
            config.save(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Method getInventory;

    @Override
    public void load(Inventory pinv, Inventory.Target target) {
        Player player = pinv.getPlayer();
        try {
            File f = getFile(player.getUniqueId());
            if (!f.exists()) { //If not found use the older file(s)
                yamlStorage.load(pinv, target);
                return;
            }
            //String content = new String(Files.readAllBytes(f.toPath()));
            Configuration config = YamlConfiguration.loadConfiguration(f);
            String content = config.getString(target.name());
            if (content == null) {
                yamlStorage.load(pinv, target);
                return;
            }
            setFromSerialized(getInventory(player), content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(Inventory pinv, Inventory.Target target) {
        File f = getFile(pinv.getPlayer().getUniqueId());
        if (!f.exists()) return;
        Configuration config = YamlConfiguration.loadConfiguration(f);
        config.set(target.name(), null);
    }

    @Override
    public boolean contains(Inventory pinv, Inventory.Target target) {
        File f = getFile(pinv.getPlayer().getUniqueId());
        if (!f.exists()) return yamlStorage.contains(pinv, target);
        Configuration config = YamlConfiguration.loadConfiguration(f);
        return config.contains(target.name()) || yamlStorage.contains(pinv, target);
    }

    //Based on iie's per-world inventory
    //https://github.com/TBMCPlugins/iiePerWorldInventory/blob/master/src/buttondevteam/perworld/serializers/inventory.java

    private Method save;
    private Class<?> nbtcl;
    private Method nbtcsta;
    private Class<?> nbtcstcl;

    //SERIALIZE ITEMSTACK
    private String serializeItemStack(Object itemStack) throws Exception {
        if (nbtcl == null)
            nbtcl = Class.forName(nms + ".NBTTagCompound");
        if (save == null)
            save = itemStack.getClass().getMethod("save", nbtcl);
        if (nbtcstcl == null)
            nbtcstcl = Class.forName(nms + ".NBTCompressedStreamTools");
        if (nbtcsta == null)
            nbtcsta = nbtcstcl.getMethod("a", nbtcl, OutputStream.class);
        Object tag = save.invoke(itemStack, nbtcl.newInstance());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        nbtcsta.invoke(null, tag, outputStream);

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }


    private Method nbtcstaa;
    private Function<Object, Object> createStack;

    //DESERIALIZE ITEMSTACK
    private Object deserializeItemStack(String itemStackString) throws Exception {
        if (nbtcstcl == null)
            nbtcstcl = Class.forName(nms + ".NBTCompressedStreamTools");
        if (nbtcstaa == null)
            nbtcstaa = nbtcstcl.getMethod("a", InputStream.class);
        if (nbtcl == null)
            nbtcl = Class.forName(nms + ".NBTTagCompound");
        try {
            if (createStack == null) {
                final Method a = iscl.getMethod("a", nbtcl);
                createStack = nbt -> {
                    try {
                        return a.invoke(null, nbt);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
        } catch (NoSuchMethodException ex) { //It can only get here inside the if
            final Constructor<?> constructor = iscl.getConstructor(nbtcl);
            createStack = nbt -> {
                try {
                    return constructor.newInstance(nbt);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(itemStackString));

        Object nbtTagCompound = nbtcstaa.invoke(null, inputStream);
        return createStack.apply(nbtTagCompound);
    }

    private Method getSize;
    private Method getItem;

    //SERIALIZE INVENTORY
    private String serialize(Object invInventory) throws Exception {
        if (getSize == null)
            getSize = invInventory.getClass().getMethod("getSize");
        if (getItem == null)
            getItem = invInventory.getClass().getMethod("getItem", int.class);
        return IntStream.range(0, (int) getSize.invoke(invInventory))
                .mapToObj(s -> {
                    try {
                        //nms ItemStack
                        Object i = getItem.invoke(invInventory, s);
                        return Objects.isNull(i) ? null : s + "#" + serializeItemStack(i);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining(";"));
    }

    private Method clear;
    private Method setItem;
    private Class<?> iscl;

    //SET INVENTORY FROM SERIALIZED
    private void setFromSerialized(Object invInventory, String invString) throws Exception {
        if (clear == null)
            clear = invInventory.getClass().getMethod("clear");
        if (iscl == null)
            iscl = Class.forName(nms + ".ItemStack");
        if (setItem == null)
            setItem = invInventory.getClass().getMethod("setItem", int.class, iscl);
        clear.invoke(invInventory); //clear inventory
        if (invString != null && !invString.isEmpty())
            Arrays.asList(invString.split(";"))
                    .parallelStream()
                    .forEach(s -> {
                        String[] e = s.split("#");
                        try {
                            setItem.invoke(invInventory, Integer.parseInt(e[0]), deserializeItemStack(e[1]));
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    });
    }
}
