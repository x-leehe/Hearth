package org.awp0rtuh1ty.hearth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
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
                .executes(ctx -> { showAll(ctx); return 1; })

                // --- potionAffectEntity ---
                .then(buildPotionAffectEntity())

                // --- potionStack ---
                .then(Commands.literal("potionStack")
                        .executes(ctx -> { showRule(ctx, "potionStack"); return 1; })
                        .then(Commands.literal("true")
                                .executes(ctx -> setPotionAndShow(ctx, 64)))
                        .then(Commands.literal("false")
                                .executes(ctx -> setPotionAndShow(ctx, 1)))
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> setPotionAndShow(ctx,
                                        IntegerArgumentType.getInteger(ctx, "value"))))
                )

                // --- byproduct ---
                .then(Commands.literal("byproduct")
                        .executes(ctx -> { showRule(ctx, "byproduct"); return 1; })
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> {
                                            ItemInput item = ItemArgument.getItem(ctx, "item");
                                            int count = IntegerArgumentType.getInteger(ctx, "count");
                                            String itemId = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
                                            HearthConfig.setByproduct(itemId, count);
                                            ctx.getSource().sendSuccess(
                                                    () -> formatSetResult("byproduct",
                                                            itemId + " x" + count,
                                                            "/hearth saveDefault"),
                                                    true);
                                            return count;
                                        }))
                        )
                )

                // --- destroyEmptyBottles ---
                .then(Commands.literal("destroyEmptyBottles")
                        .executes(ctx -> { showRule(ctx, "destroyEmptyBottles"); return 1; })
                        .then(Commands.literal("true")
                                .executes(ctx -> toggleAndShow(ctx, "destroyEmptyBottles", true)))
                        .then(Commands.literal("false")
                                .executes(ctx -> toggleAndShow(ctx, "destroyEmptyBottles", false)))
                )

                // --- logEnabled ---
                .then(Commands.literal("logEnabled")
                        .executes(ctx -> { showRule(ctx, "logEnabled"); return 1; })
                        .then(Commands.literal("true")
                                .executes(ctx -> toggleAndShow(ctx, "logEnabled", true)))
                        .then(Commands.literal("false")
                                .executes(ctx -> toggleAndShow(ctx, "logEnabled", false)))
                )

                // --- reload ---
                .then(Commands.literal("reload")
                        .executes(ctx -> {
                            HearthConfig.initialize();
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.hearth.reload.success"), true);
                            return 1;
                        }))

                // --- saveDefault ---
                .then(Commands.literal("saveDefault")
                        .executes(ctx -> {
                            HearthConfig.save();
                            ctx.getSource().sendSuccess(
                                    () -> Component.translatable("commands.hearth.saveDefault.success"), true);
                            return 1;
                        }))

                // --- help ---
                .then(Commands.literal("help").executes(HearthCommand::showHelp))
        );
    }

    // ============================================================
    //  showAll — /hearth list all settings with values (Carpet style)
    // ============================================================

    private static void showAll(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();

        src.sendSuccess(() -> Component.translatable("commands.hearth.list.header"), false);

        // potionStack
        int ps = HearthConfig.getPotionStackSize();
        String psLabel = ps == 64 ? "true" : ps == 1 ? "false" : String.valueOf(ps);
        src.sendSuccess(() -> ruleLine("potionStack", psLabel,
                Component.translatable("commands.hearth.list.desc.potionStack")), false);

        // byproduct
        ResourceLocation bp = HearthConfig.getByproductItem();
        String bpStr = bp != null ? bp.toString() : "hearth:wood_ash";
        src.sendSuccess(() -> ruleLine("byproduct", bpStr + " x" + HearthConfig.getByproductCount(),
                Component.translatable("commands.hearth.list.desc.byproduct")), false);

        // destroyEmptyBottles
        src.sendSuccess(() -> ruleLine("destroyEmptyBottles",
                HearthConfig.isDestroyEmptyBottles() ? "true" : "false",
                Component.translatable("commands.hearth.list.desc.destroyEmptyBottles")), false);

        // logEnabled
        src.sendSuccess(() -> ruleLine("logEnabled",
                HearthConfig.isLoggingEnabled() ? "true" : "false",
                Component.translatable("commands.hearth.list.desc.logEnabled")), false);

        src.sendSuccess(() -> Component.translatable("commands.hearth.list.footer"), false);
    }

    private static MutableComponent ruleLine(String name, String value, Component desc) {
        MutableComponent line = Component.literal("  ");
        line.append(Component.literal(name).withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/hearth " + name))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("commands.hearth.hover.rule", name)))));
        line.append(Component.literal(" = ").withStyle(ChatFormatting.GRAY));
        line.append(Component.literal(value).withStyle(ChatFormatting.WHITE));
        line.append("  ");
        line.append(desc.copy().withStyle(ChatFormatting.GRAY));
        return line;
    }

    // ============================================================
    //  showRule — /hearth <rule> detail (Carpet style)
    // ============================================================

    private static void showRule(CommandContext<CommandSourceStack> ctx, String name) {
        var src = ctx.getSource();
        switch (name) {
            case "potionStack" -> showPotionStackDetail(ctx);
            case "byproduct" -> showByproductDetail(ctx);
            case "destroyEmptyBottles" -> showBoolDetail(ctx, "destroyEmptyBottles",
                    HearthConfig.isDestroyEmptyBottles());
            case "logEnabled" -> showBoolDetail(ctx, "logEnabled",
                    HearthConfig.isLoggingEnabled());
            default -> src.sendSuccess(
                    () -> ruleInfoHeader(name, String.valueOf(name), Component.empty()), false);
        }
    }

    private static void showPotionStackDetail(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        int val = HearthConfig.getPotionStackSize();
        String label = val == 64 ? "true" : val == 1 ? "false" : String.valueOf(val);
        MutableComponent line = ruleInfoHeader("potionStack", label,
                Component.translatable("commands.hearth.list.desc.potionStack"));
        src.sendSuccess(() -> line, false);

        MutableComponent opts = Component.literal("  ")
                .append(Component.translatable("commands.hearth.show.options").withStyle(ChatFormatting.GRAY))
                .append(" ");
        opts.append(optButton("potionStack", "true"));
        opts.append(" ");
        opts.append(optButton("potionStack", "false"));
        opts.append(" ");
        opts.append(optButton("potionStack", "64"));
        opts.append(" ");
        opts.append(optButton("potionStack", "16"));
        opts.append(" ");
        opts.append(optButton("potionStack", "1"));
        src.sendSuccess(() -> opts, false);
    }

    private static void showByproductDetail(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        ResourceLocation bp = HearthConfig.getByproductItem();
        String bpStr = bp != null ? bp.toString() : "hearth:wood_ash";
        String val = bpStr + " x" + HearthConfig.getByproductCount();

        MutableComponent line = ruleInfoHeader("byproduct", val,
                Component.translatable("commands.hearth.list.desc.byproduct"));
        src.sendSuccess(() -> line, false);

        MutableComponent hint = Component.literal("  ")
                .append(Component.literal("/hearth byproduct <item> <count>")
                        .withStyle(ChatFormatting.GRAY));
        src.sendSuccess(() -> hint, false);
    }

    private static void showBoolDetail(CommandContext<CommandSourceStack> ctx, String name, boolean val) {
        var src = ctx.getSource();
        MutableComponent line = ruleInfoHeader(name, val ? "true" : "false",
                Component.translatable("commands.hearth.list.desc." + name));
        src.sendSuccess(() -> line, false);

        MutableComponent opts = Component.literal("  ")
                .append(Component.translatable("commands.hearth.show.options").withStyle(ChatFormatting.GRAY))
                .append(" ");
        opts.append(optButton(name, "true"));
        opts.append(" ");
        opts.append(optButton(name, "false"));
        src.sendSuccess(() -> opts, false);
    }

    private static MutableComponent ruleInfoHeader(String name, String value, Component desc) {
        MutableComponent line = Component.literal(name).withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/hearth " + name)));
        line.append(Component.literal(" = ").withStyle(ChatFormatting.GRAY));
        line.append(Component.literal(value).withStyle(ChatFormatting.WHITE));
        line.append("  ");
        line.append(desc.copy().withStyle(ChatFormatting.GRAY));
        return line;
    }

    private static MutableComponent optButton(String name, String value) {
        return Component.literal("[" + value + "]").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/hearth " + name + " " + value))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("/hearth " + name + " " + value))));
    }

    // ============================================================
    //  set helpers — success + save hint
    // ============================================================

    private static int toggleAndShow(CommandContext<CommandSourceStack> ctx, String name, boolean value) {
        if (name.equals("destroyEmptyBottles")) HearthConfig.setDestroyEmptyBottles(value);
        else if (name.equals("logEnabled")) HearthConfig.setLogEnabled(value);
        ctx.getSource().sendSuccess(
                () -> formatSetResult(name, String.valueOf(value), "/hearth saveDefault"), true);
        return 1;
    }

    private static int setPotionAndShow(CommandContext<CommandSourceStack> ctx, int value) {
        HearthConfig.setPotionStackSize(value);
        ctx.getSource().sendSuccess(
                () -> formatSetResult("potionStack", String.valueOf(value), "/hearth saveDefault"), true);
        return value;
    }

    private static Component formatSetResult(String name, String value, String saveCmd) {
        MutableComponent msg = Component.translatable("commands.hearth.rule.set", name, value).copy();
        msg.append(Component.literal("  "));
        msg.append(Component.literal("[")
                .append(Component.translatable("commands.hearth.save.hint"))
                .append(Component.literal("]"))
                .withStyle(style -> style
                        .withColor(ChatFormatting.GRAY)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, saveCmd))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal(saveCmd)))));
        return msg;
    }

    // ============================================================
    //  potionAffectEntity
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> buildPotionAffectEntity() {
        var cmd = Commands.literal("potionAffectEntity");
        var include = Commands.literal("include")
                .then(Commands.argument("entity", StringArgumentType.string())
                        .suggests(ENTITY_SUGGESTION)
                        .executes(ctx -> addEntity(ctx, StringArgumentType.getString(ctx, "entity"))));
        cmd.then(include);
        cmd.then(Commands.literal("in").redirect(include.build()));
        cmd.then(Commands.literal("+").redirect(include.build()));
        var exclude = Commands.literal("exclude")
                .then(Commands.argument("entity", StringArgumentType.string())
                        .suggests(ENTITY_SUGGESTION)
                        .executes(ctx -> removeEntity(ctx, StringArgumentType.getString(ctx, "entity"))));
        cmd.then(exclude);
        cmd.then(Commands.literal("ex").redirect(exclude.build()));
        cmd.then(Commands.literal("-").redirect(exclude.build()));
        return cmd;
    }

    private static int addEntity(CommandContext<CommandSourceStack> ctx, String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.hearth.invalid_entity", entityId));
            return 0;
        }
        HearthConfig.addAffectedEntity(id);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("commands.hearth.potionAffectEntity.include", entityId), true);
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
                () -> Component.translatable("commands.hearth.potionAffectEntity.exclude", entityId), true);
        return 1;
    }

    // ============================================================
    //  help
    // ============================================================

    private static int showHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MutableComponent msg = Component.empty();

        msg.append(Component.translatable("commands.hearth.help.header").withStyle(ChatFormatting.GOLD));
        msg.append(Component.literal("\n"));
        msg.append(Component.translatable("commands.hearth.help.category.rules").withStyle(ChatFormatting.GOLD));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.list").withStyle(ChatFormatting.GRAY));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth potionStack"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.potionStack", HearthConfig.getPotionStackSize()).withStyle(ChatFormatting.GRAY));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth byproduct <item> <count>"));
        msg.append(Component.literal(" "));
        ResourceLocation bpItem = HearthConfig.getByproductItem();
        msg.append(Component.translatable("commands.hearth.help.byproduct",
                bpItem != null ? bpItem.toString() : "hearth:wood_ash",
                HearthConfig.getByproductCount()).withStyle(ChatFormatting.GRAY));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth destroyEmptyBottles true|false"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.destroyEmptyBottles",
                Component.translatable(HearthConfig.isDestroyEmptyBottles() ? "commands.hearth.value.true" : "commands.hearth.value.false")
                        .withStyle(HearthConfig.isDestroyEmptyBottles() ? ChatFormatting.GREEN : ChatFormatting.RED))
                .withStyle(ChatFormatting.GRAY));

        msg.append(Component.literal("\n  "));
        msg.append(cmd("/hearth logEnabled true|false"));
        msg.append(Component.literal(" "));
        msg.append(Component.translatable("commands.hearth.help.logEnabled",
                Component.translatable(HearthConfig.isLoggingEnabled() ? "commands.hearth.value.true" : "commands.hearth.value.false")
                        .withStyle(HearthConfig.isLoggingEnabled() ? ChatFormatting.GREEN : ChatFormatting.RED))
                .withStyle(ChatFormatting.GRAY));

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

    private static MutableComponent cmd(String command) {
        return Component.literal(command).withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
    }

    private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTION =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                            .map(ResourceLocation::toString)
                            .filter(id -> id.startsWith(builder.getRemainingLowerCase())),
                    builder);
}
