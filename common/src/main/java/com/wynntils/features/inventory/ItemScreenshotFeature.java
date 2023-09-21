/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.RegisterKeyBind;
import com.wynntils.core.keybinds.KeyBind;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.SystemUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.wynn.ItemUtils;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import javax.imageio.ImageIO;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.INVENTORY)
public class ItemScreenshotFeature extends Feature {
    // The 4, 4 offset is intentional, otherwise the tooltip will be rendered outside of the screen
    private static final ClientTooltipPositioner NO_POSITIONER =
            (int screenWidth, int screenHeight, int mouseX, int mouseY, int tooltipWidth, int tooltipHeight) ->
                    new Vector2i(4, 4);

    @RegisterKeyBind
    private final KeyBind itemScreenshotKeyBind =
            new KeyBind("Screenshot Item", GLFW.GLFW_KEY_F4, true, null, this::onInventoryPress);

    @Persisted
    public final Config<Boolean> saveToDisk = new Config<>(false);

    private Slot screenshotSlot = null;

    private void onInventoryPress(Slot hoveredSlot) {
        screenshotSlot = hoveredSlot;
    }

    // All other features (besides scaling) must be able to update the tooltip first
    @SubscribeEvent(priority = EventPriority.LOW)
    public void render(ItemTooltipRenderEvent.Pre e) {
        if (!Models.WorldState.onWorld()) return;
        if (screenshotSlot == null || !screenshotSlot.hasItem()) return;

        Screen screen = McUtils.mc().screen;
        if (!(screen instanceof AbstractContainerScreen<?>)) return;

        // has to be called during a render period
        takeScreenshot(screen, screenshotSlot, e.getTooltips());
        makeChatPrompt(screenshotSlot);
        screenshotSlot = null;
    }

    private void takeScreenshot(Screen screen, Slot hoveredSlot, List<Component> itemTooltip) {
        ItemStack itemStack = hoveredSlot.getItem();
        List<Component> tooltip = new ArrayList<>(itemTooltip);
        removeLoreTooltipLines(tooltip);

        Font font = FontRenderer.getInstance().getFont();

        // width calculation
        int width = 0;
        for (Component c : tooltip) {
            int w = font.width(c.getString());
            if (w > width) {
                width = w;
            }
        }
        width += 8;

        // height calculation

        int height = 16;
        if (tooltip.size() > 1) {
            height += 2 + (tooltip.size() - 1) * 10;
        }

        // calculate tooltip size to fit to framebuffer
        float scaleh = (float) screen.height / height;
        float scalew = (float) screen.width / width;

        // Create tooltip renderer
        Screen.DeferredTooltipRendering deferredTooltipRendering = new Screen.DeferredTooltipRendering(
                Lists.transform(tooltip, Component::getVisualOrderText), NO_POSITIONER);

        // draw tooltip to framebuffer, create image
        McUtils.mc().getMainRenderTarget().unbindWrite();

        GuiGraphics guiGraphics = new GuiGraphics(McUtils.mc(), MultiBufferSource.immediate(new BufferBuilder(256)));
        RenderTarget fb = new MainTarget(width * 2, height * 2);
        fb.setClearColor(1f, 1f, 1f, 0f);
        fb.createBuffers(width * 2, height * 2, false);
        fb.bindWrite(false);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scalew, scaleh, 1);
        guiGraphics.renderTooltip(
                FontRenderer.getInstance().getFont(),
                deferredTooltipRendering.tooltip(),
                deferredTooltipRendering.positioner(),
                0,
                0);
        guiGraphics.pose().popPose();
        guiGraphics.flush();
        fb.unbindWrite();
        McUtils.mc().getMainRenderTarget().bindWrite(true);

        BufferedImage bi = SystemUtils.createScreenshot(fb);

        if (saveToDisk.get()) {
            // First try to save it to disk
            String itemNameForFile = StyledText.fromComponent(itemStack.getHoverName())
                    .trim()
                    .replaceAll("[/ ]", "_")
                    .getNormalized()
                    .getString(PartStyle.StyleType.NONE);
            File screenshotDir = new File(McUtils.mc().gameDirectory, "screenshots");
            String filename = Util.getFilenameFormattedDateTime() + "-" + itemNameForFile + ".png";
            try {
                File outputfile = new File(screenshotDir, filename);
                ImageIO.write(bi, "png", outputfile);

                McUtils.sendMessageToClient(Component.translatable(
                                "feature.wynntils.itemScreenshot.save.message",
                                itemStack.getHoverName(),
                                Component.literal(outputfile.getName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(style -> style.withClickEvent(new ClickEvent(
                                                ClickEvent.Action.OPEN_FILE, outputfile.getAbsolutePath()))))
                        .withStyle(ChatFormatting.GREEN));
            } catch (IOException e) {
                WynntilsMod.error("Failed to save image to disk", e);
                McUtils.sendErrorToClient(
                        I18n.get("feature.wynntils.itemScreenshot.save.error", itemStack.getHoverName(), filename));
            }

            if (SystemUtils.isMac()) {
                McUtils.sendMessageToClient(Component.translatable("feature.wynntils.itemScreenshot.copy.mac")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }
        } else if (SystemUtils.isMac()) {
            McUtils.sendMessageToClient(Component.translatable("feature.wynntils.itemScreenshot.copy.mac2")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable("feature.wynntils.itemScreenshot.copy.mac.clickHere")
                            .withStyle(ChatFormatting.GRAY)
                            .withStyle(ChatFormatting.UNDERLINE)
                            .withStyle(style -> style.withClickEvent(new ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/wynntils config set ItemScreenshot saveToDisk true")))));
            return;
        }

        try {
            SystemUtils.copyImageToClipboard(bi);
            McUtils.sendMessageToClient(Component.translatable("feature.wynntils.itemScreenshot.copy.message")
                    .withStyle(ChatFormatting.GREEN));
        } catch (HeadlessException ex) {
            WynntilsMod.error("Failed to copy image to clipboard", ex);
            McUtils.sendErrorToClient(I18n.get("feature.wynntils.itemScreenshot.copy.error"));
        }
    }

    private static void makeChatPrompt(Slot hoveredSlot) {
        // chat item prompt
        Optional<GearItem> gearItemOpt = Models.Item.asWynnItem(hoveredSlot.getItem(), GearItem.class);
        if (gearItemOpt.isEmpty()) return;

        GearItem gearItem = gearItemOpt.get();
        if (gearItem.isUnidentified()) {
            // We can only send chat encoded gear of identified gear
            WynntilsMod.warn("Cannot make chat link of unidentified gear");
            McUtils.sendErrorToClient(I18n.get("feature.wynntils.itemScreenshot.chatItemError"));
            return;
        }
        String encoded = Models.Gear.toEncodedString(gearItem);

        McUtils.sendMessageToClient(Component.translatable("feature.wynntils.itemScreenshot.chatItemMessage")
                .withStyle(ChatFormatting.DARK_GREEN)
                .withStyle(ChatFormatting.UNDERLINE)
                .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, encoded)))
                .withStyle(s -> s.withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("feature.wynntils.itemScreenshot.chatItemTooltip")
                                .withStyle(ChatFormatting.DARK_AQUA)))));
    }

    /**
     * Create a list of ItemIdentificationContainer corresponding to the given GearProfile, formatted for item guide items
     *
     * @param item the profile of the item
     * @return a list of appropriately formatted ItemIdentificationContainer
     */
    private static void removeLoreTooltipLines(List<Component> tooltip) {
        int loreStart = -1;
        for (int i = 0; i < tooltip.size(); i++) {
            // only remove text after the item type indicator
            String tooltipLine = tooltip.get(i).getString();
            Matcher matcher = ItemUtils.ITEM_RARITY_PATTERN.matcher(tooltipLine);
            if (matcher.find()) {
                loreStart = i + 1;
                break;
            }
        }

        // type indicator was found
        if (loreStart != -1) {
            tooltip.subList(loreStart, tooltip.size()).clear();
        }
    }
}
