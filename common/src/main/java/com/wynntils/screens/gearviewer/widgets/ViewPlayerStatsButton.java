/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.gearviewer.widgets;

import com.google.common.collect.Lists;
import com.wynntils.core.components.Managers;
import com.wynntils.core.net.UrlId;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class ViewPlayerStatsButton extends WynntilsButton {
    private static final List<Component> VIEW_STATS_TOOLTIP =
            List.of(Component.translatable("screens.wynntils.gearViewer.viewStats"));

    private final String playerName;

    public ViewPlayerStatsButton(int x, int y, int width, int height, String playerName) {
        super(x, y, width, height, Component.literal("↵"));
        this.playerName = playerName;
    }

    @Override
    public void onPress() {
        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
        Managers.Net.openLink(UrlId.LINK_WYNNCRAFT_PLAYER_STATS, Map.of("username", playerName));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        if (isHovered) {
            McUtils.mc()
                    .screen
                    .setTooltipForNextRenderPass(Lists.transform(VIEW_STATS_TOOLTIP, Component::getVisualOrderText));
        }
    }
}
