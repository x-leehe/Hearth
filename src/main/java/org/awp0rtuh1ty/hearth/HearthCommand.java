package org.awp0rtuh1ty.hearth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
                .executes(HearthCommand::showHelp)

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
                                            String itemId = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
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

                // /hearth logEnabled true|false
                .then(Commands.literal("logEnabled")
                        .then(Commands.literal("true")
                                .executes(ctx -> {
                                    HearthConfig.setLogEnabled(true);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.hearth.logEnabled.true"),
                                            true);
                                    return 1;
                                }))
                        .then(Commands.literal("false")
                                .executes(ctx -> {
                                    HearthConfig.setLogEnabled(false);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.hearth.logEnabled.false"),
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
                        .executes(HearthCommand::showHelp)
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

    // --- Help ---

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MutableComponent msg = Component.empty();

        // Header
        msg.append(Component.translatable("commands.hearth.help.header").withStyle(ChatFormatting.GOLD));

        // ========== Entity Management ==========
        msg.append(Component.literal("\n"));
        msg.append(Component.translatable("commands.hearth.help.category.entity").withStyle(ChatFormatting.GOLD));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth potionAffectEntity include <entity>"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.potionAffectEntity.include").withStyle(ChatFormatting.GRAY));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth potionAffectEntity exclude <entity>"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.potionAffectEntity.exclude").withStyle(ChatFormatting.GRAY));

        msg.append(Component.literal("\n  "));
        msg.append(Component.translatable("commands.hearth.help.entity.note").withStyle(ChatFormatting.DARK_GRAY));

        // ========== Item Settings ==========
        msg.append(Component.literal("\n"));
        msg.append(Component.translatable("commands.hearth.help.category.item").withStyle(ChatFormatting.GOLD));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth byproduct <item> <count>"));
        msg.append(Component.literal(" "));
        ResourceLocation bpItem = HearthConfig.getByproductItem();
        String bpItemStr = bpItem != null ? bpItem.toString() : "hearth:wood_ash";
        msg.append(Component.translatable("commands.hearth.help.byproduct", bpItemStr, HearthConfig.getByproductCount()).withStyle(ChatFormatting.GRAY));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth potionStack <1-64>"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.potionStack", HearthConfig.getPotionStackSize()).withStyle(ChatFormatting.GRAY));

        // ========== Toggle Settings ==========
        msg.append(Component.literal("\n"));
        msg.append(Component.translatable("commands.hearth.help.category.toggle").withStyle(ChatFormatting.GOLD));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth destroyEmptyBottles true|false"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.destroyEmptyBottles",
                Component.translatable(HearthConfig.isDestroyEmptyBottles() ? "commands.hearth.help.value.true" : "commands.hearth.help.value.false")
                        .withStyle(HearthConfig.isDestroyEmptyBottles() ? ChatFormatting.GREEN : ChatFormatting.RED))
                .withStyle(ChatFormatting.GRAY));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth logEnabled true|false"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.logEnabled",
                Component.translatable(HearthConfig.isLoggingEnabled() ? "commands.hearth.help.value.true" : "commands.hearth.help.value.false")
                        .withStyle(HearthConfig.isLoggingEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED))
                .withStyle(ChatFormatting.GRAY));

        // ========== Config Management ==========
        msg.append(Component.literal("\n"));
        msg.append(Component.translatable("commands.hearth.help.category.config").withStyle(ChatFormatting.GOLD));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth reload"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.reload").withStyle(ChatFormatting.GRAY));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth saveDefault"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.saveDefault").withStyle(ChatFormatting.GRAY));

        source.sendSuccess(() -> msg, false);
        return 1;
    }

    /** Creates a yellow clickable command component that suggests the command on click. */
    private static MutableComponent cmd(String command) {
        return Component.literal(command).withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withColor(ChatFormatting.YELLOW));
    }
}
