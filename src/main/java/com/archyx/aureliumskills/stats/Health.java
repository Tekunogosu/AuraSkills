package com.archyx.aureliumskills.stats;

import com.archyx.aureliumskills.AureliumSkills;
import com.archyx.aureliumskills.abilities.AgilityAbilities;
import com.archyx.aureliumskills.configuration.Option;
import com.archyx.aureliumskills.configuration.OptionL;
import com.archyx.aureliumskills.skills.SkillLoader;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Health implements Listener {

	private final AureliumSkills plugin;
	private final AgilityAbilities agilityAbilities;
	private final Map<UUID, Double> worldChangeHealth = new HashMap<>();
	private static final double threshold = 0.1;

	public Health(AureliumSkills plugin) {
		this.plugin = plugin;
		this.agilityAbilities = new AgilityAbilities(plugin);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onJoin(PlayerJoinEvent event) {
		setHealth(event.getPlayer());
	}

	public void reload(Player player) {
		if (player != null) {
			setHealth(player);
			agilityAbilities.removeFleeting(player);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void worldChange(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		if (plugin.getWorldManager().isInDisabledWorld(player.getLocation()) && !plugin.getWorldManager().isDisabledWorld(event.getFrom())) {
			worldChangeHealth.put(player.getUniqueId(), player.getHealth());
		}
		if (OptionL.getInt(Option.HEALTH_UPDATE_DELAY) > 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					setHealth(player);
					if (plugin.getWorldManager().isDisabledWorld(event.getFrom()) && !plugin.getWorldManager().isInDisabledWorld(player.getLocation())) {
						if (worldChangeHealth.containsKey(player.getUniqueId())) {
							player.setHealth(worldChangeHealth.get(player.getUniqueId()));
							worldChangeHealth.remove(player.getUniqueId());
						}
					}
				}
			}.runTaskLater(plugin, OptionL.getInt(Option.HEALTH_UPDATE_DELAY));
		}
		else {
			setHealth(player);
			if (plugin.getWorldManager().isDisabledWorld(event.getFrom()) && !plugin.getWorldManager().isInDisabledWorld(player.getLocation())) {
				if (worldChangeHealth.containsKey(player.getUniqueId())) {
					player.setHealth(worldChangeHealth.get(player.getUniqueId()));
					worldChangeHealth.remove(player.getUniqueId());
				}
			}
		}
	}

	private void setHealth(Player player) {
		//Calculates the amount of health to add
		PlayerStat playerStat = SkillLoader.playerStats.get(player.getUniqueId());
		if (playerStat != null) {
			double modifier = (playerStat.getStatLevel(Stat.HEALTH)) * OptionL.getDouble(Option.HEALTH_MODIFIER);
			AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
			if (attribute != null) {
				double originalMaxHealth = attribute.getValue();
				boolean hasChange = true;
				//Removes existing modifiers of the same name and check for change
				for (AttributeModifier am : attribute.getModifiers()) {
					if (am.getName().equals("skillsHealth")) {
						//Check for any changes, if not, return
						if (Math.abs(originalMaxHealth - (originalMaxHealth - am.getAmount() + modifier)) <= threshold) {
							hasChange = false;
						}
						//Removes if has change
						if (hasChange) {
							attribute.removeModifier(am);
						}
					}
				}
				//Disable health if in disable world
				if (plugin.getWorldManager().isInDisabledWorld(player.getLocation())) {
					player.setHealthScaled(false);
					for (AttributeModifier am : attribute.getModifiers()) {
						if (am.getName().equals("skillsHealth")) {
							attribute.removeModifier(am);
						}
					}
					return;
				}
				// Force base health if enabled
				if (OptionL.getBoolean(Option.HEALTH_FORCE_BASE_HEALTH)) {
					attribute.setBaseValue(20.0);
				}
				// Return if no change
				if (hasChange) {
					//Applies modifier
					attribute.addModifier(new AttributeModifier("skillsHealth", modifier, Operation.ADD_NUMBER));
					//Sets health to max if over max
					if (player.getHealth() > attribute.getValue()) {
						player.setHealth(attribute.getValue());
					}
				}
				//Applies health scaling
				if (OptionL.getBoolean(Option.HEALTH_HEALTH_SCALING)) {
					double health = attribute.getValue();
					player.setHealthScaled(true);
					if (health < 23) {
						player.setHealthScale(20.0);
					} else if (health < 28) {
						player.setHealthScale(22.0);
					} else if (health < 36) {
						player.setHealthScale(24.0);
					} else if (health < 49) {
						player.setHealthScale(26.0);
					} else if (health < 70) {
						player.setHealthScale(28.0);
					} else if (health < 104) {
						player.setHealthScale(30.0);
					} else if (health < 159) {
						player.setHealthScale(32.0);
					} else if (health < 248) {
						player.setHealthScale(34.0);
					} else if (health < 392) {
						player.setHealthScale(36.0);
					} else if (health < 625) {
						player.setHealthScale(38.0);
					} else {
						player.setHealthScale(40.0);
					}
				} else {
					player.setHealthScaled(false);
				}
			}
		}
	}
}
