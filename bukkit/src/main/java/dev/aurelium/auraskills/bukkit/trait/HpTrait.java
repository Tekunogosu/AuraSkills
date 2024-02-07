package dev.aurelium.auraskills.bukkit.trait;

import dev.aurelium.auraskills.api.trait.Trait;
import dev.aurelium.auraskills.api.trait.Traits;
import dev.aurelium.auraskills.api.event.user.UserLoadEvent;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.skills.agility.AgilityAbilities;
import dev.aurelium.auraskills.bukkit.user.BukkitUser;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.data.DataUtil;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HpTrait extends TraitImpl {

    private final Map<UUID, Double> worldChangeHealth = new HashMap<>();
    private final Map<Integer, Double> hearts = new HashMap<>();
    private static final double threshold = 0.1;
    private final UUID ATTRIBUTE_ID = UUID.fromString("7d1423dd-91db-467a-8eb8-1886e30ca0b1");

    HpTrait(AuraSkills plugin) {
        super(plugin, Traits.HP);
        // Load default hearts
        this.hearts.put(10, 0.0);
        this.hearts.put(11, 24.0);
        this.hearts.put(12, 29.0);
        this.hearts.put(13, 37.0);
        this.hearts.put(14, 50.0);
        this.hearts.put(15, 71.0);
        this.hearts.put(16, 105.0);
        this.hearts.put(17, 160.0);
        this.hearts.put(18, 249.0);
        this.hearts.put(19, 393.0);
        this.hearts.put(20, 626.0);
        loadHearts();
    }

    @Override
    public double getBaseLevel(Player player, Trait trait) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) return 0.0;
        double current = attribute.getValue();
        // Subtract skills attribute value
        for (AttributeModifier am : attribute.getModifiers()) {
            if (am.getName().equals("skillsHealth")) {
                current -= am.getAmount();
            }
        }
        return current;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyScaling(event.getPlayer());
    }

    @EventHandler
    public void onLoad(UserLoadEvent event) {
        setHealth(event.getPlayer(), BukkitUser.getUser(event.getUser()));
    }

    @Override
    public void reload(Player player, Trait trait) {
        setHealth(player, plugin.getUser(player));

        plugin.getAbilityManager().getAbilityImpl(AgilityAbilities.class).removeFleeting(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void worldChange(PlayerChangedWorldEvent event) {
        if (!Traits.HP.isEnabled()) return;
        Player player = event.getPlayer();
        if (plugin.getWorldManager().isInDisabledWorld(player.getLocation()) && !plugin.getWorldManager().isDisabledWorld(event.getFrom().getName())) {
            worldChangeHealth.put(player.getUniqueId(), player.getHealth());
        }
        User user = plugin.getUser(player);
        if (Traits.HP.optionInt("update_delay") > 0) {
            plugin.getScheduler().scheduleSync(() -> setWorldChange(event, player, user),
                    Traits.HP.optionInt("update_delay") * 50L, TimeUnit.MILLISECONDS);
        } else {
            setWorldChange(event, player, user);
        }
    }

    private void setWorldChange(PlayerChangedWorldEvent event, Player player, User user) {
        setHealth(player, user);
        if (plugin.getWorldManager().isDisabledWorld(event.getFrom().getName()) && !plugin.getWorldManager().isInDisabledWorld(player.getLocation())) {
            if (worldChangeHealth.containsKey(player.getUniqueId())) {
                player.setHealth(worldChangeHealth.get(player.getUniqueId()));
                worldChangeHealth.remove(player.getUniqueId());
            }
        }
    }

    private void setHealth(Player player, User user) {
        Trait trait = Traits.HP;

        double modifier = user.getBonusTraitLevel(trait);
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) return;
        double originalMaxHealth = attribute.getValue();
        boolean hasChange = true;
        // Removes existing modifiers of the same name and check for change
        for (AttributeModifier am : attribute.getModifiers()) {
            if (am.getName().equals("skillsHealth")) {
                // Check for any changes, if not, return
                if (Math.abs(originalMaxHealth - (originalMaxHealth - am.getAmount() + modifier)) <= threshold) {
                    hasChange = false;
                }
                // Removes if it has changed
                if (hasChange) {
                    attribute.removeModifier(am);
                }
            }
        }
        // Disable health if disabled or in disable world
        if (plugin.getWorldManager().isInDisabledWorld(player.getLocation()) || !trait.isEnabled()) {
            player.setHealthScaled(false);
            for (AttributeModifier am : attribute.getModifiers()) {
                if (am.getName().equals("skillsHealth")) {
                    attribute.removeModifier(am);
                }
            }
            if (player.getHealth() >= originalMaxHealth) {
                player.setHealth(attribute.getValue());
            }
            return;
        }
        // Force base health if enabled
        if (trait.optionBoolean("force_base_health")) {
            attribute.setBaseValue(20.0);
        }
        // Return if no change
        if (hasChange) {
            // Applies modifier
            attribute.addModifier(new AttributeModifier(ATTRIBUTE_ID, "skillsHealth", modifier, AttributeModifier.Operation.ADD_NUMBER));
            // Sets health to max if over max
            if (player.getHealth() > attribute.getValue()) {
                player.setHealth(attribute.getValue());
            }
            if (trait.optionBoolean("keep_full_on_increase") && attribute.getValue() > originalMaxHealth) {
                // Heals player to full health if had full health before modifier
                if (player.getHealth() >= originalMaxHealth) {
                    player.setHealth(attribute.getValue());
                }
            }
        }
        applyScaling(player);
    }

    private void applyScaling(Player player) {
        if (!Traits.HP.isEnabled()) return;
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute == null) return;

        if (Traits.HP.optionBoolean("health_scaling")) {
            double health = attribute.getValue();
            player.setHealthScaled(true);
            int scaledHearts = 0;
            for (Integer heartNum : hearts.keySet()) {
                double healthNum = hearts.get(heartNum);
                if (health >= healthNum) {
                    if (heartNum > scaledHearts) {
                        scaledHearts = heartNum;
                    }
                }
            }
            if (scaledHearts == 0) {
                scaledHearts = 10;
            }
            player.setHealthScale(scaledHearts * 2);
        } else {
            player.setHealthScaled(false);
        }
    }

    public void loadHearts() {
        this.hearts.clear();
        // Load hearts from trait options
        Map<String, Object> map = Traits.HP.optionMap("hearts");
        try {
            for (String key : map.keySet()) {
                int heartsNum = Integer.parseInt(key);
                double healthNum = DataUtil.getDouble(map, key);
                if (healthNum != -1.0) {
                    this.hearts.put(heartsNum, healthNum);
                }
            }
        } catch (Exception e) {
            plugin.logger().warn("There was an error loading health.hearts data! Check to make sure the keys are only integers and the values are only numbers.");
        }
    }

}
