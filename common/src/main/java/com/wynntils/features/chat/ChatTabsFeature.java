/*
 * Copyright © Wynntils 2022-2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.chat;

import com.google.common.collect.Sets;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Services;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.core.persisted.config.HiddenConfig;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.mc.event.ChatPacketReceivedEvent;
import com.wynntils.mc.event.ChatScreenKeyTypedEvent;
import com.wynntils.mc.event.ChatSentEvent;
import com.wynntils.mc.event.ClientsideMessageEvent;
import com.wynntils.mc.event.ScreenFocusEvent;
import com.wynntils.mc.event.ScreenInitEvent;
import com.wynntils.mc.event.ScreenRenderEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import com.wynntils.screens.chattabs.widgets.ChatTabAddButton;
import com.wynntils.screens.chattabs.widgets.ChatTabButton;
import com.wynntils.services.chat.ChatTab;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@ConfigCategory(Category.CHAT)
public class ChatTabsFeature extends Feature {
    // These should move to ChatTabManager, as Storage
    @Persisted
    public final HiddenConfig<List<ChatTab>> chatTabs = new HiddenConfig<>(new ArrayList<>(List.of(
            new ChatTab("All", false, null, null, null),
            new ChatTab("Global", false, null, Sets.newHashSet(RecipientType.GLOBAL), null),
            new ChatTab("Local", false, null, Sets.newHashSet(RecipientType.LOCAL), null),
            new ChatTab("Guild", false, "/g  ", Sets.newHashSet(RecipientType.GUILD), null),
            new ChatTab("Party", false, "/p  ", Sets.newHashSet(RecipientType.PARTY), null),
            new ChatTab("Private", false, "/r  ", Sets.newHashSet(RecipientType.PRIVATE), null),
            new ChatTab("Shout", false, null, Sets.newHashSet(RecipientType.SHOUT), null))));

    // We do this here, and not in Services.ChatTab to not introduce a feature-model dependency.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChatReceived(ChatMessageReceivedEvent event) {
        // We are already sending this message to every matching tab, so we can cancel it.
        event.setCanceled(true);

        Services.ChatTab.matchMessage(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientsideChat(ClientsideMessageEvent event) {
        // We've already sent this message to every matching tab, so we can cancel it.
        event.setCanceled(true);

        boolean isRenderThread = McUtils.mc().isSameThread();
        if (isRenderThread) {
            Services.ChatTab.matchMessage(event);
        } else {
            // It can happen that client-side messages are sent from some other thread
            // That will cause race conditions with vanilla ChatComponent code, so
            // schedule this update by the renderer thread instead
            Managers.TickScheduler.scheduleNextTick(() -> Services.ChatTab.matchMessage(event));
        }
    }

    @SubscribeEvent
    public void onScreenInit(ScreenInitEvent event) {
        if (event.getScreen() instanceof ChatScreen chatScreen) {
            int xOffset = 0;

            chatScreen.addRenderableWidget(new ChatTabAddButton(xOffset + 2, chatScreen.height - 35, 12, 13));
            xOffset += 15;

            for (ChatTab chatTab : Services.ChatTab.getTabs().toList()) {
                chatScreen.addRenderableWidget(new ChatTabButton(xOffset + 2, chatScreen.height - 35, 40, 13, chatTab));
                xOffset += 43;
            }
        }
    }

    @SubscribeEvent
    public void onScreenFocusChange(ScreenFocusEvent event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;

        GuiEventListener guiEventListener = event.getGuiEventListener();

        // These should not be focused
        if (guiEventListener instanceof ChatTabButton || guiEventListener instanceof ChatTabAddButton) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldState.NOT_CONNECTED) {
            Services.ChatTab.resetFocusedTab();
            return;
        }

        if (event.getNewState() == WorldState.WORLD && Services.ChatTab.getFocusedTab() == null) {
            Services.ChatTab.refocusFirstTab();
        }
    }

    @SubscribeEvent
    public void onScreenRender(ScreenRenderEvent event) {
        if (!(event.getScreen() instanceof ChatScreen chatScreen)) return;

        // We render this twice for chat screen, but it is not heavy and this is a simple and least conflicting way of
        // rendering command suggestions on top of chat tab buttons.
        chatScreen.commandSuggestions.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public void onChatScreenKeyTyped(ChatScreenKeyTypedEvent event) {
        // We can't use keybinds here to not conflict with TAB key's other behaviours.
        if (event.getKeyCode() != GLFW.GLFW_KEY_TAB) return;
        if (!(McUtils.mc().screen instanceof ChatScreen)) return;
        if (!KeyboardUtils.isShiftDown()) return;

        event.setCanceled(true);
        Services.ChatTab.setFocusedTab(Services.ChatTab.getNextFocusedTab());
    }

    @SubscribeEvent
    public void onChatSend(ChatSentEvent event) {
        if (Services.ChatTab.getFocusedTab() == null) return;
        if (event.getMessage().isBlank()) return;

        ChatTab focusedTab = Services.ChatTab.getFocusedTab();
        if (focusedTab.getAutoCommand() != null && !focusedTab.getAutoCommand().isBlank()) {
            event.setCanceled(true);

            String autoCommand = focusedTab.getAutoCommand();
            autoCommand = autoCommand.startsWith("/") ? autoCommand.substring(1) : autoCommand;
            McUtils.sendCommand(autoCommand + event.getMessage());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChatPacket(ChatPacketReceivedEvent.Player event) {
        if (Services.ChatTab.getFocusedTab() == null) return;

        // Cancel all remaining messages, if we have a focused tab, we will handle it.
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChatPacket(ChatPacketReceivedEvent.System event) {
        if (Services.ChatTab.getFocusedTab() == null) return;

        // Cancel all remaining messages, if we have a focused tab, we will handle it.
        event.setCanceled(true);
    }

    @Override
    public void onEnable() {
        Services.ChatTab.refocusFirstTab();
    }

    @Override
    public void onDisable() {
        Services.ChatTab.resetFocusedTab();
    }

    @Override
    protected void onConfigUpdate(Config<?> config) {
        Services.ChatTab.refocusFirstTab();

        if ((McUtils.mc().screen instanceof ChatScreen chatScreen)) {
            // Reload chat tab buttons
            chatScreen.init(
                    McUtils.mc(),
                    McUtils.window().getGuiScaledWidth(),
                    McUtils.window().getGuiScaledHeight());
        }
    }
}
