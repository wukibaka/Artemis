/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.activities;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.discoveries.DiscoveryInfo;
import com.wynntils.models.activities.event.ActivityUpdatedEvent;
import com.wynntils.models.activities.type.ActivitySortOrder;
import com.wynntils.screens.activities.widgets.DiscoveryButton;
import com.wynntils.screens.activities.widgets.DiscoveryProgressButton;
import com.wynntils.screens.base.WynntilsListScreen;
import com.wynntils.screens.base.widgets.BackButton;
import com.wynntils.screens.base.widgets.FilterButton;
import com.wynntils.screens.base.widgets.PageSelectorButton;
import com.wynntils.screens.base.widgets.ReloadButton;
import com.wynntils.screens.base.widgets.SortOrderWidget;
import com.wynntils.screens.base.widgets.SortableActivityScreen;
import com.wynntils.screens.wynntilsmenu.WynntilsMenuScreen;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class WynntilsDiscoveriesScreen extends WynntilsListScreen<DiscoveryInfo, DiscoveryButton>
        implements SortableActivityScreen {
    private final List<FilterButton> filterButtons = new ArrayList<>();

    // Filters
    private boolean showFoundSecrets = true;
    private boolean showUndiscoveredSecrets = false;
    private boolean showFoundWorld = true;
    private boolean showUndiscoveredWorld = false;
    private boolean showFoundTerritory = true;
    private boolean showUndiscoveredTerritory = false;

    private ActivitySortOrder activitySortOrder = ActivitySortOrder.LEVEL;

    private WynntilsDiscoveriesScreen() {
        super(Component.translatable("screens.wynntils.wynntilsDiscoveries.name"));

        // Only register this once
        WynntilsMod.registerEventListener(this);
    }

    public static Screen create() {
        return new WynntilsDiscoveriesScreen();
    }

    @SubscribeEvent
    public void onDiscoveryUpdate(ActivityUpdatedEvent event) {
        if (event.getActivityType().isDiscovery() && McUtils.mc().screen == this) {
            this.reloadElements();
        }
    }

    @Override
    public void onClose() {
        WynntilsMod.unregisterEventListener(this);

        super.onClose();
    }

    @Override
    protected void doInit() {
        Models.Discovery.reloadDiscoveries();

        super.doInit();

        filterButtons.clear();

        filterButtons.add(new FilterButton(
                35,
                125,
                30,
                30,
                Texture.DISCOVERED_TERRITORY,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showFoundTerritory.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    showFoundTerritory = !showFoundTerritory;
                    reloadElements();
                },
                () -> showFoundTerritory));
        filterButtons.add(new FilterButton(
                70,
                125,
                30,
                30,
                Texture.DISCOVERED_WORLD,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showFoundWorld.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    showFoundWorld = !showFoundWorld;
                    reloadElements();
                },
                () -> showFoundWorld));
        filterButtons.add(new FilterButton(
                105,
                125,
                30,
                30,
                Texture.DISCOVERED_SECRET,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showFoundSecret.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    showFoundSecrets = !showFoundSecrets;
                    reloadElements();
                },
                () -> showFoundSecrets));
        filterButtons.add(new FilterButton(
                35,
                160,
                30,
                30,
                Texture.UNDISCOVERED_TERRITORY,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showUnfoundTerritory.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    showUndiscoveredTerritory = !showUndiscoveredTerritory;
                    reloadElements();
                },
                () -> showUndiscoveredTerritory));
        filterButtons.add(new FilterButton(
                70,
                160,
                30,
                30,
                Texture.UNDISCOVERED_WORLD,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showUnfoundWorld.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    showUndiscoveredWorld = !showUndiscoveredWorld;
                    reloadElements();
                },
                () -> showUndiscoveredWorld));
        filterButtons.add(new FilterButton(
                105,
                160,
                30,
                30,
                Texture.UNDISCOVERED_SECRET,
                true,
                List.of(Component.literal("[>] ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("screens.wynntils.wynntilsDiscoveries.showUnfoundSecret.name")
                                .withStyle(ChatFormatting.BOLD)
                                .withStyle(ChatFormatting.GREEN))),
                () -> {
                    showUndiscoveredSecrets = !showUndiscoveredSecrets;
                    reloadElements();
                },
                () -> showUndiscoveredSecrets));

        this.addRenderableWidget(new BackButton(
                (int) ((Texture.CONTENT_BOOK_BACKGROUND.width() / 2f - 16) / 2f),
                65,
                Texture.BACK_ARROW_OFFSET.width() / 2,
                Texture.BACK_ARROW_OFFSET.height(),
                WynntilsMenuScreen.create()));

        this.addRenderableWidget(new ReloadButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() - 21,
                11,
                (int) (Texture.RELOAD_ICON_OFFSET.width() / 2 / 1.7f),
                (int) (Texture.RELOAD_ICON_OFFSET.height() / 1.7f),
                "discovery",
                Models.Discovery::reloadDiscoveries));

        this.addRenderableWidget(new SortOrderWidget(
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 + 1,
                11,
                (int) (Texture.SORT_DISTANCE_OFFSET.width() / 1.7f),
                (int) (Texture.SORT_DISTANCE_OFFSET.height() / 2 / 1.7f),
                this));

        this.addRenderableWidget(new PageSelectorButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 + 50 - Texture.FORWARD_ARROW_OFFSET.width() / 2,
                Texture.CONTENT_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW_OFFSET.width() / 2,
                Texture.FORWARD_ARROW_OFFSET.height(),
                false,
                this));
        this.addRenderableWidget(new PageSelectorButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() - 50,
                Texture.CONTENT_BOOK_BACKGROUND.height() - 25,
                Texture.FORWARD_ARROW_OFFSET.width() / 2,
                Texture.FORWARD_ARROW_OFFSET.height(),
                true,
                this));

        for (FilterButton filterButton : filterButtons) {
            this.addRenderableWidget(filterButton);
        }

        this.addRenderableWidget(new DiscoveryProgressButton(50, 10, 20, 20, false));
        this.addRenderableWidget(new DiscoveryProgressButton(75, 10, 20, 20, true));
    }

    @Override
    public void doRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        renderBackgroundTexture(poseStack);

        // Make 0, 0 the top left corner of the rendered quest book background
        poseStack.pushPose();
        final float translationX = getTranslationX();
        final float translationY = getTranslationY();
        poseStack.translate(translationX, translationY, 1f);

        renderTitle(poseStack, I18n.get("screens.wynntils.wynntilsDiscoveries.name"));

        renderVersion(poseStack);

        renderWidgets(guiGraphics, mouseX, mouseY, partialTick);

        if (elements.isEmpty()) {
            renderNoDiscoveries(poseStack);
        }

        renderDescription(
                poseStack,
                I18n.get("screens.wynntils.wynntilsDiscoveries.screenDescription"),
                I18n.get("screens.wynntils.wynntilsActivities.filterHelper"));

        renderPageInfo(poseStack, currentPage + 1, maxPage + 1);

        poseStack.popPose();

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void renderNoDiscoveries(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        StyledText.fromString(I18n.get("screens.wynntils.wynntilsDiscoveries.noDiscoveries")),
                        Texture.CONTENT_BOOK_BACKGROUND.width() / 2f + 15f,
                        Texture.CONTENT_BOOK_BACKGROUND.width() - 15f,
                        0,
                        Texture.CONTENT_BOOK_BACKGROUND.height(),
                        Texture.CONTENT_BOOK_BACKGROUND.width() / 2f - 30f,
                        CommonColors.BLACK,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NONE);
    }

    @Override
    protected DiscoveryButton getButtonFromElement(int i) {
        int offset = i % getElementsPerPage();
        return new DiscoveryButton(
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 + 15,
                offset * 13 + 25,
                Texture.CONTENT_BOOK_BACKGROUND.width() / 2 - 37,
                9,
                elements.get(i));
    }

    @Override
    protected void reloadElementsList(String searchTerm) {
        // We need to filter duplicates
        elements.addAll(Stream.concat(
                        Models.Discovery.getAllDiscoveries(activitySortOrder)
                                .filter(discoveryInfo -> !discoveryInfo.isDiscovered())
                                .filter(discoveryInfo -> switch (discoveryInfo.getType()) {
                                    case TERRITORY -> showUndiscoveredTerritory;
                                    case WORLD -> showUndiscoveredWorld;
                                    case SECRET -> showUndiscoveredSecrets;
                                }),
                        Models.Discovery.getAllCompletedDiscoveries(activitySortOrder)
                                .filter(discoveryInfo -> switch (discoveryInfo.getType()) {
                                    case TERRITORY -> showFoundTerritory;
                                    case WORLD -> showFoundWorld;
                                    case SECRET -> showFoundSecrets;
                                }))
                .filter(info -> StringUtils.partialMatch(info.getName(), searchTerm))
                .toList());
    }

    @Override
    public ActivitySortOrder getActivitySortOrder() {
        return activitySortOrder;
    }

    @Override
    public void setActivitySortOrder(ActivitySortOrder newSortOrder) {
        if (newSortOrder == null) {
            throw new IllegalStateException("Tried to set null activity sort order");
        }

        // Disable DISTANCE sorting for discoveries
        if (newSortOrder == ActivitySortOrder.DISTANCE) {
            newSortOrder = ActivitySortOrder.ALPHABETIC;
        }

        this.activitySortOrder = newSortOrder;
        this.setCurrentPage(0);
    }
}
