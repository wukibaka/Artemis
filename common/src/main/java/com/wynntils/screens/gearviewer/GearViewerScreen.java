/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.gearviewer;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.gear.type.GearInfo;
import com.wynntils.models.gear.type.GearInstance;
import com.wynntils.models.items.FakeItemStack;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.screens.base.WynntilsContainerScreen;
import com.wynntils.screens.gearviewer.widgets.ViewPlayerStatsButton;
import com.wynntils.utils.mc.LoreUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.wynn.ItemUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team.Visibility;

public final class GearViewerScreen extends WynntilsContainerScreen<GearViewerMenu> {
    private static final String TEAM_NAME = "GearViewerTeam";

    private final Player player;
    private final Scoreboard scoreboard;
    private final PlayerTeam gearViewerTeam;
    private final PlayerTeam oldTeam;
    private ViewPlayerStatsButton viewPlayerStatsButton;

    private GearViewerScreen(Player player, GearViewerMenu menu) {
        super(menu, player.getInventory(), Component.empty());

        this.player = player;
        this.scoreboard = player.level().getScoreboard();

        if (scoreboard.getTeamNames().contains(TEAM_NAME)) {
            gearViewerTeam = scoreboard.getPlayerTeam(TEAM_NAME);
        } else {
            gearViewerTeam = scoreboard.addPlayerTeam(TEAM_NAME);
            gearViewerTeam.setNameTagVisibility(Visibility.NEVER);
        }

        // this is done to prevent the player's nametag from rendering in the GUI
        oldTeam = scoreboard.getPlayersTeam(player.getScoreboardName());
        scoreboard.addPlayerToTeam(player.getScoreboardName(), gearViewerTeam);
    }

    public static Screen create(Player player) {
        ItemStack heldItem = createDecoratedItemStack(player.getMainHandItem(), player.getName());

        List<ItemStack> armorItems = new ArrayList<>();
        for (ItemStack armorSlot : player.getArmorSlots()) {
            armorItems.add(createDecoratedItemStack(armorSlot, player.getName()));
        }
        Collections.reverse(armorItems);

        return new GearViewerScreen(player, GearViewerMenu.create(heldItem, armorItems));
    }

    private static ItemStack createDecoratedItemStack(ItemStack itemStack, Component playerName) {
        if (itemStack.getItem() == Items.AIR) {
            return itemStack;
        }

        // This must specifically NOT be normalized; the ֎ is significant
        String gearName = StyledText.fromComponent(itemStack.getHoverName()).getStringWithoutFormatting();
        MutableComponent description = ItemUtils.getNonGearDescription(itemStack, gearName);
        if (description != null) {
            itemStack.setHoverName(description);
            return itemStack;
        }

        GearInfo gearInfo = Models.Gear.getGearInfoFromApiName(gearName);
        if (gearInfo == null) {
            return itemStack;
        }

        JsonObject itemData = LoreUtils.getJsonFromIngameLore(itemStack);
        GearInstance gearInstance = Models.Gear.parseInstance(gearInfo, itemData);
        return new FakeItemStack(new GearItem(gearInfo, gearInstance), "From " + playerName.getString());
    }

    @Override
    protected void doInit() {
        this.leftPos = (this.width - Texture.GEAR_VIEWER_BACKGROUND.width()) / 2;
        this.topPos = (this.height - Texture.GEAR_VIEWER_BACKGROUND.height()) / 2;

        viewPlayerStatsButton = new ViewPlayerStatsButton(
                leftPos - 20,
                topPos + (Texture.GEAR_VIEWER_BACKGROUND.height() / 4),
                18,
                20,
                StyledText.fromComponent(player.getName()).getStringWithoutFormatting());
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.doRender(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        renderPlayerModel(guiGraphics, mouseX, mouseY);

        viewPlayerStatsButton.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderPlayerModel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int posX = (int) (this.width / 2f);
        int posY = (int) (this.height / 2f) + 32;

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics, posX + 26, posY + 8, posX + 75, posY + 78, 30, 1f, 0, 0, player);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawTexturedRect(poseStack, Texture.GEAR_VIEWER_BACKGROUND, this.leftPos, this.topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // we don't want to draw any labels
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return viewPlayerStatsButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        // do nothing here, because we don't want the user interacting with slots at all
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == McUtils.options().keyInventory.key.getValue()) {
            this.onClose();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        // restore previous scoreboard team setup
        scoreboard.removePlayerFromTeam(player.getScoreboardName(), gearViewerTeam);
        if (oldTeam != null) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), oldTeam);
        }

        super.onClose();
    }

    public Player getPlayer() {
        return player;
    }
}
