package dev.aurelium.auraskills.common.source.parser.util;

import dev.aurelium.auraskills.api.item.ItemFilterMeta;
import dev.aurelium.auraskills.api.item.PotionData;
import dev.aurelium.auraskills.api.source.BaseContext;
import dev.aurelium.auraskills.api.source.UtilityParser;
import dev.aurelium.auraskills.common.item.SourceItemMeta;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;

public class ItemFilterMetaParser implements UtilityParser<ItemFilterMeta> {

    @Override
    public ItemFilterMeta parse(ConfigurationNode source, BaseContext context) throws SerializationException {
        String displayName = source.node("display_name").getString();
        List<String> lore = source.node("lore").getList(String.class);
        // Deserialize PotionData
        PotionData potionData;
        if (!source.node("potion_data").virtual()) {
            potionData = new PotionDataParser().parse(source.node("potion_data"), context);
        } else {
            potionData = null;
        }
        int customModelData = source.node("custom_model_data").getInt(Integer.MIN_VALUE);

        return new SourceItemMeta(displayName, lore, potionData, customModelData);
    }

}
