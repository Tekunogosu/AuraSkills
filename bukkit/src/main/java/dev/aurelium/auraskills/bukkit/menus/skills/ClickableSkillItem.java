package dev.aurelium.auraskills.bukkit.menus.skills;

import com.archyx.slate.menu.ActiveMenu;
import com.archyx.slate.position.PositionProvider;
import dev.aurelium.auraskills.api.skill.CustomSkill;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.common.AbstractSkillItem;
import dev.aurelium.auraskills.bukkit.menus.levelprogression.LevelProgressionOpener;
import dev.aurelium.auraskills.bukkit.util.ConfigurateItemParser;
import dev.aurelium.auraskills.common.user.User;
import fr.minuskube.inv.content.SlotPos;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ClickableSkillItem extends AbstractSkillItem {

    public ClickableSkillItem(AuraSkills plugin) {
        super(plugin);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, ItemStack item, SlotPos pos, ActiveMenu activeMenu, Skill skill) {
        User user = plugin.getUser(player);

        if (player.hasPermission("auraskills.skill." + skill.name().toLowerCase(Locale.ROOT))) {
            new LevelProgressionOpener(plugin).open(player, user, skill);
        }
    }

    @Override
    public Set<Skill> getDefinedContexts(Player player, ActiveMenu activeMenu) {
        return new HashSet<>(plugin.getSkillManager().getEnabledSkills());
    }

    @Override
    public void onInitialize(Player player, ActiveMenu activeMenu, Skill context) {
        if (!(context instanceof CustomSkill skill)) {
            return;
        }
        try {
            ConfigurateItemParser parser = new ConfigurateItemParser(plugin);
            ConfigurationNode config = parser.parseItemContext(skill.getItem());

            PositionProvider provider = parser.parsePositionProvider(config, activeMenu);
            if (provider != null) {
                activeMenu.setPositionProvider("skill", context, provider);
            }
        } catch (SerializationException e) {
            plugin.logger().warn("Error parsing ItemContext of CustomSkill " + skill.getId());
            e.printStackTrace();
        }
    }

    @Override
    public ItemStack onItemModify(ItemStack baseItem, Player player, ActiveMenu activeMenu, Skill skill) {
        if (!skill.isEnabled()) {
            return null;
        }
        if (skill instanceof CustomSkill customSkill) {
            try {
                ConfigurateItemParser parser = new ConfigurateItemParser(plugin);

                return parser.parseBaseItem(parser.parseItemContext(customSkill.getItem()));
            } catch (SerializationException | IllegalArgumentException e) {
                plugin.logger().warn("Error parsing ItemContext of CustomSkill " + customSkill.getId());
                e.printStackTrace();
            }
        }
        return baseItem;
    }
}
