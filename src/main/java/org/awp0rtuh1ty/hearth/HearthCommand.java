package org.awp0rtuh1ty.hearth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class HearthCommand {

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(HearthCommand::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                  CommandBuildContext buildContext,
                                  Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("hearth")
                .requires(source -> source.hasPermission(2))

                // /hearth potionAffectEntity include|in|+ <entity>
                .then(Commands.literal("potionAffectEntity")
                        .then(Commands.literal("include")
                                .then(Commands.argument("entity", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String remaining = builder.getRemaining().toLowerCase();
                                            for (String e : HearthConfig.getIncludedEntities()) {
                                                if (e.toLowerCase().startsWith(remaining)) builder.suggest(e);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> addEntity(ctx, getEntityArg(ctx)))
                                ))
                        .then(Commands.literal("in")
                                .then(Commands.argument("entity", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String e : HearthConfig.getIncludedEntities()) builder.suggest(e);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> addEntity(ctx, getEntityArg(ctx)))
                                ))
                        .then(Commands.literal("+")
                                .then(Commands.argument("entity", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String e : HearthConfig.getIncludedEntities()) builder.suggest(e);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> addEntity(ctx, getEntityArg(ctx)))
                                ))
                        .then(Commands.literal("exclude")
                                .then(Commands.argument("entity", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String e : HearthConfig.getIncludedEntities()) builder.suggest(e);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> removeEntity(ctx, getEntityArg(ctx)))
                                ))
                        .then(Commands.literal("ex")
                                .then(Commands.argument("entity", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String e : HearthConfig.getIncludedEntities()) builder.suggest(e);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> removeEntity(ctx, getEntityArg(ctx)))
                                ))
                        .then(Commands.literal("-")
                                .then(Commands.argument("entity", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String e : HearthConfig.getIncludedEntities()) builder.suggest(e);
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> removeEntity(ctx, getEntityArg(ctx)))
                                ))
                )

                // /hearth potionStack true|false|<value>
                .then(Commands.literal("potionStack")
                        .then(Commands.literal("true")
                                .executes(ctx -> setPotionStack(ctx, 64)))
                        .then(Commands.literal("false")
                                .executes(ctx -> setPotionStack(ctx, 1)))
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> {
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    return setPotionStack(ctx, value);
                                }))
                )

                // /hearth byproduct <item> <count>
                .then(Commands.literal("byproduct")
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> {
                                            ItemInput item = ItemArgument.getItem(ctx, "item");
                                            int count = IntegerArgumentType.getInteger(ctx, "count");
                                            String itemId = item.getItem().builtInRegistryHolder().key().location().toString();
                                            HearthConfig.setByproduct(itemId, count);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.translatable("commands.hearth.byproduct.success", itemId, count),
                                                    true);
                                            return count;
                                        }))
                        )
                )

                // /hearth destroyEmptyBottles true|false
                .then(Commands.literal("destroyEmptyBottles")
                        .then(Commands.literal("true")
                                .executes(ctx -> {
                                    HearthConfig.setDestroyEmptyBottles(true);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.hearth.destroyEmptyBottles.true"),
                                            true);
                                    return 1;
                                }))
                        .then(Commands.literal("false")
                                .executes(ctx -> {
                                    HearthConfig.setDestroyEmptyBottles(false);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.hearth.destroyEmptyBottles.false"),
                                            true);
                                    return 1;
                                }))
                )

                // /hearth reload
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            HearthConfig.initialize();
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.hearth.reload.success"),
                                    true);
                            return 1;
                        })
                )

                // /hearth saveDefault
                .then(Commands.literal("saveDefault")
                        .executes(ctx -> {
                            HearthConfig.save();
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.hearth.saveDefault.success"),
                                    true);
                            return 1;
                        })
                )

                // /hearth help
                .then(Commands.literal("help")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.hearth.help"), false);
                            return 1;
                        })
                )
        );
    }

    private static String getEntityArg(CommandContext<CommandSourceStack> ctx) {
        return StringArgumentType.getString(ctx, "entity");
    }

    private static int addEntity(CommandContext<CommandSourceStack> ctx, String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.hearth.invalid_entity", entityId));
            return 0;
        }
        HearthConfig.addAffectedEntity(id);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.hearth.potionAffectEntity.include", entityId),
                true);
        return 1;
    }

    private static int removeEntity(CommandContext<CommandSourceStack> ctx, String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.hearth.invalid_entity", entityId));
            return 0;
        }
        HearthConfig.removeAffectedEntity(id);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.hearth.potionAffectEntity.exclude", entityId),
                true);
        return 1;
    }

    private static int setPotionStack(CommandContext<CommandSourceStack> ctx, int value) {
        HearthConfig.setPotionStackSize(value);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.hearth.potionStack.success", value),
                true);
        return value;
    }
}
