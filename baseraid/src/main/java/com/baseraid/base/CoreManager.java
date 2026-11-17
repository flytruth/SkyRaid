package com.baseraid.base;

import com.baseraid.BaseRaidPlugin;
import com.baseraid.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Ядро базы: парящий предмет (булава) над пьедесталом + зона взаимодействия.
 * ПКМ/ЛКМ по ядру открывает меню владельца.
 */
public class CoreManager {

    private final BaseRaidPlugin plugin;
    private final NamespacedKey key;

    public CoreManager(BaseRaidPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "base-core");
    }

    public void spawnCore(Base b) {
        removeCore(b);
        World w = Bukkit.getWorld(b.world);
        if (w == null) return;

        ItemDisplay display = w.spawn(
                new Location(w, b.x + 0.5, b.y + 2.05, b.z + 0.5),
                ItemDisplay.class);
        display.setItem(new ItemStack(plugin.settings().coreItem));
        display.setBillboard(Display.Billboard.CENTER);
        display.setTransformation(new TransformationCompat().create());
        display.setInvulnerable(true);
        display.setPersistent(true);
        display.setGlowing(true);
        display.customName(Text.c("&6Ядро базы"));
        display.setCustomNameVisible(true);
        tag(display, b);

        Interaction interaction = w.spawn(
                new Location(w, b.x + 0.5, b.y + 1.6, b.z + 0.5),
                Interaction.class);
        interaction.setInteractionWidth(1.1f);
        interaction.setInteractionHeight(2.0f);
        interaction.setResponsive(true);
        interaction.setInvulnerable(true);
        interaction.setPersistent(true);
        interaction.customName(Text.c("&6Ядро базы"));
        interaction.setCustomNameVisible(false);
        tag(interaction, b);

        b.coreDisplayId = display.getUniqueId();
        b.coreInteractionId = interaction.getUniqueId();
        plugin.bases().save();
    }

    public void removeCore(Base b) {
        World w = Bukkit.getWorld(b.world);
        if (w == null) return;
        Location center = b.center(w);
        for (Entity e : w.getNearbyEntities(center, 2.5, 3.5, 2.5)) {
            if (b.owner.toString().equals(untag(e))) e.remove();
        }
        b.coreDisplayId = null;
        b.coreInteractionId = null;
    }

    /** База, к которой привязано существо ядра. */
    public Base findByEntity(Entity entity) {
        String owner = untag(entity);
        if (owner == null) return null;
        try {
            return plugin.bases().get(UUID.fromString(owner));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void tag(Entity e, Base b) {
        e.getPersistentDataContainer().set(key, PersistentDataType.STRING, b.owner.toString());
    }

    private String untag(Entity e) {
        return e.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    /** Увеличенная модель предмета. */
    private static final class TransformationCompat {
        Display.Transformation create() {
            return new Display.Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(1.6f, 1.6f, 1.6f),
                    new Quaternionf());
        }
    }
}
