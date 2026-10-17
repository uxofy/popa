package com.example.popa;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class PopaClient implements ClientModInitializer {
    private static boolean enabled = true;
    private static int interval = 3; // тиков между действиями
    private static int cooldown = 0;

    @Override
    public void onInitializeClient() {
        // /popa - вкл/выкл, /popa delay <тики> - интервал
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("popa")
                .executes(ctx -> {
                    enabled = !enabled;
                    ctx.getSource().sendFeedback(Component.literal("Popa: " + (enabled ? "ВКЛ" : "ВЫКЛ")));
                    return 1;
                })
                .then(ClientCommandManager.literal("delay")
                    .then(ClientCommandManager.argument("ticks", IntegerArgumentType.integer(1, 20))
                        .executes(ctx -> {
                            interval = IntegerArgumentType.getInteger(ctx, "ticks");
                            ctx.getSource().sendFeedback(Component.literal("Popa: интервал " + interval + " тик."));
                            return 1;
                        })))));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> tick(mc));
    }

    private static void tick(Minecraft mc) {
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null || mc.screen != null) {
            cooldown = 0;
            return;
        }
        // ПКМ зажата, в руке кристалл
        if (!mc.options.keyUse.isDown() || !mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
            cooldown = 0;
            return;
        }
        // Прицел на обсидиан или бедрок
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        var block = mc.level.getBlockState(pos);
        if (!block.is(Blocks.OBSIDIAN) && !block.is(Blocks.BEDROCK)) {
            return;
        }

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // Если над блоком уже стоит кристалл - взрываем, иначе ставим
        List<EndCrystal> crystals = mc.level.getEntitiesOfClass(EndCrystal.class, new AABB(pos.above()));
        if (!crystals.isEmpty()) {
            mc.gameMode.attack(mc.player, crystals.get(0));
        } else {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
        cooldown = interval - 1;
    }
}
