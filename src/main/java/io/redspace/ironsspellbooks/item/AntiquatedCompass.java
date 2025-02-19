package io.redspace.ironsspellbooks.item;

import io.redspace.ironsspellbooks.util.ModTags;
import io.redspace.ironsspellbooks.util.SpellbookModCreativeTabs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AntiquatedCompass extends Item {
    private static final Component description = Component.translatable("item.irons_spellbooks.antiquated_compass_desc").withStyle(Style.EMPTY.withColor(0x873200));
    public AntiquatedCompass() {
        super(new Properties().tab(SpellbookModCreativeTabs.SPELL_EQUIPMENT_TAB));
    }

    public static GlobalPos getCitadelLocation(Entity entity, CompoundTag compoundTag) {
        if (!(entity.level.dimension() == Level.NETHER && compoundTag.contains("CitadelPos")))
            return null;

        return GlobalPos.of(entity.level.dimension(), NbtUtils.readBlockPos(compoundTag.getCompound("CitadelPos")));
    }

    @Override
    public void onCraftedBy(ItemStack pStack, Level pLevel, Player pPlayer) {
        if (pLevel instanceof ServerLevel serverlevel) {
            BlockPos blockpos = serverlevel.findNearestMapStructure(ModTags.ANTIQUATED_COMPASS_LOCATOR, pPlayer.blockPosition(), 100, false);
            if (blockpos != null) {
                var tag = pStack.getOrCreateTag();
                tag.put("CitadelPos", NbtUtils.writeBlockPos(blockpos));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        pTooltipComponents.add(description);
    }
}
