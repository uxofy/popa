package com.example.popa;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class PopaClient implements ClientModInitializer {
    private static int cooldown = 0;

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("popa", "main"));
    private static KeyMapping toggleKey;
    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        PopaConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.popa.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.popa.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, CATEGORY));

        // /popa - вкл/выкл, /popa delay <1-20> - интервал
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("popa")
                .executes(ctx -> {
                    PopaConfig.enabled = !PopaConfig.enabled;
                    PopaConfig.save();
                    ctx.getSource().sendFeedback(Component.literal("Popa: " + (PopaConfig.enabled ? "ВКЛ" : "ВЫКЛ")));
                    return 1;
                })
                .then(ClientCommandManager.literal("delay")
                    .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(1, 20))
                        .executes(ctx -> {
                            PopaConfig.interval = IntegerArgumentType.getInteger(ctx, "ticks");
                            PopaConfig.save();
                            ctx.getSource().sendFeedback(Component.literal("Popa: интервал " + PopaConfig.interval + " тик."));
                            return 1;
                        })))));

        ClientTickEvents.END_CLIENT_TICK.register(PopaClient::tick);
    }

    private static void tick(Minecraft mc) {
        while (toggleKey.consumeClick()) {
            PopaConfig.enabled = !PopaConfig.enabled;
            PopaConfig.save();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        Component.literal("Popa: " + (PopaConfig.enabled ? "ВКЛ" : "ВЫКЛ")), true);
            }
        }
        while (menuKey.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new PopaScreen(null));
        }

        if (!PopaConfig.enabled || mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) {
            cooldown = 0;
            return;
        }
        // ПКМ зажата, в руке кристалл
        if (!mc.options.keyUse.isDown() || !mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
            cooldown = 0;
            return;
        }

        HitResult hr = mc.hitResult;
        EndCrystal target = null;     // кристалл, который нужно взорвать
        BlockHitResult placeHit = null; // блок, на который нужно поставить

        if (hr instanceof EntityHitResult eh && eh.getEntity() instanceof EndCrystal c) {
            // Прицел на уже поставленном кристалле (так и бывает после постановки)
            target = c;
        } else if (hr instanceof BlockHitResult bh && bh.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bh.getBlockPos();
            var state = mc.level.getBlockState(pos);
            if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) {
                return;
            }
            List<EndCrystal> crystals = mc.level.getEntitiesOfClass(EndCrystal.class, new AABB(pos.above()));
            if (!crystals.isEmpty()) target = crystals.get(0);
            else placeHit = bh;
        } else {
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        boolean acted = false;
        if (target != null && PopaConfig.explode) {
            mc.gameMode.attack(mc.player, target);
            acted = true;
        } else if (placeHit != null && PopaConfig.place) {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, placeHit);
            acted = true;
        }
        if (acted) {
            mc.player.swing(InteractionHand.MAIN_HAND);
            cooldown = PopaConfig.interval - 1;
        }
    }
}
