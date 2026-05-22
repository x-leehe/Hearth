package org.awp0rtuh1ty.hearth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class HearthCommand {

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(HearthCommand::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext buildContext,
                                 Commands.CommandSelection environment) {
        var root = Commands.literal("hearth")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> { listAllSettings(ctx); return 1; });

        // Auto-register every @HearthRule as a subcommand
        for (var rule : HearthConfig.getRules().values()) {
            root.then(buildRuleCommand(rule));
        }

        // byproduct — dual-arg, manual
        root.then(Commands.literal("byproduct")
                .executes(ctx -> { showRule(ctx, "byproduct"); return 1; })
                .then(Commands.argument("item", net.minecraft.commands.arguments.item.ItemArgument.item(buildContext))
                        .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> {
                                    var item = net.minecraft.commands.arguments.item.ItemArgument.getItem(ctx, "item");
                                    int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count");
                                    String itemId = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
                                    HearthConfig.setByproduct(itemId, count);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.translatable("commands.hearth.byproduct.success", itemId, count), true);
                                    return count;
                                }))
                )
        );

        // potionAffectEntity
        root.then(buildPotionAffectEntity());

        // list
        root.then(Commands.literal("list").executes(ctx -> { listAllSettings(ctx); return 1; }));

        // reload
        root.then(Commands.literal("reload").executes(ctx -> {
            HearthConfig.initialize();
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.hearth.reload.success"), true);
            return 1;
        }));

        // saveDefault
        root.then(Commands.literal("saveDefault").executes(ctx -> {
            HearthConfig.save();
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.hearth.saveDefault.success"), true);
            return 1;
        }));

        dispatcher.register(root);
    }

    // ============================================================
    //  buildRuleCommand — like Carpet's setRule + displayRuleMenu
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> buildRuleCommand(HearthRuleDef<?> rule) {
        var node = Commands.literal(rule.name)
                .executes(ctx -> { displayRuleMenu(ctx, rule); return 1; });

        List<String> opts = rule.options;

        if (rule.type == Boolean.class) {
            node.then(Commands.literal("true").executes(ctx -> setRule(ctx, rule, "true")));
            node.then(Commands.literal("false").executes(ctx -> setRule(ctx, rule, "false")));
        } else if (rule.type == Integer.class) {
            if (!opts.isEmpty()) {
                for (String opt : opts) {
                    node.then(Commands.literal(opt).executes(ctx -> setRule(ctx, rule, opt)));
                }
            }
            int max = rule.name.contains("Stack") || rule.name.contains("Count") ? 64 : 128;
            node.then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, max))
                    .executes(ctx -> setRule(ctx, rule,
                            String.valueOf(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "value")))));
        } else if (rule.type == String.class) {
            for (String opt : opts) {
                node.then(Commands.literal(opt).executes(ctx -> setRule(ctx, rule, opt)));
            }
            node.then(Commands.argument("value", com.mojang.brigadier.arguments.StringArgumentType.string())
                    .executes(ctx -> setRule(ctx, rule,
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "value"))));
        }

        return node;
    }

    // ============================================================
    //  displayRuleMenu — like Carpet's displayInteractiveSetting
    // ============================================================

    private static void displayRuleMenu(CommandContext<CommandSourceStack> ctx, HearthRuleDef<?> rule) {
        var src = ctx.getSource();
        Object val = rule.value();

        // Name = value  (description)
        src.sendSuccess(() -> {
            MutableComponent m = Component.literal(rule.name).withStyle(ChatFormatting.YELLOW);
            m.append(Component.literal(" = ").withStyle(ChatFormatting.GRAY));
            m.append(Component.literal(String.valueOf(val)).withStyle(ChatFormatting.WHITE));
            m.append("  ");
            m.append(Component.translatable(rule.descKey).withStyle(ChatFormatting.GRAY));
            return m;
        }, false);

        // Categories
        src.sendSuccess(() -> {
            MutableComponent m = Component.literal("  ").append("Categories: ").withStyle(ChatFormatting.GRAY);
            for (int i = 0; i < rule.categories.length; i++) {
                if (i > 0) m.append(", ");
                m.append(Component.literal(rule.categories[i]).withStyle(ChatFormatting.GOLD));
            }
            return m;
        }, false);

        // Options as clickable buttons
        src.sendSuccess(() -> {
            MutableComponent m = Component.literal("  ").append("Options: ").withStyle(ChatFormatting.GRAY);
            List<String> opts = rule.options;
            if (!opts.isEmpty()) {
                for (int i = 0; i < opts.size(); i++) {
                    if (i > 0) m.append(" ");
                    String opt = opts.get(i);
                    m.append(makeSetRuleButton(rule, opt));
                }
            } else {
                m.append(Component.literal("(any)").withStyle(ChatFormatting.WHITE));
            }
            m.append("  ");
            m.append(makeResetButton(rule));
            return m;
        }, false);
    }

    // ============================================================
    //  setRule — like Carpet's setRule with save hint
    // ============================================================

    private static int setRule(CommandContext<CommandSourceStack> ctx, HearthRuleDef<?> rule, String value) {
        rule.set(ctx.getSource(), value);
        var src = ctx.getSource();
        Object newVal = rule.value();
        src.sendSuccess(() -> {
            MutableComponent m = Component.literal(rule.name).withStyle(ChatFormatting.YELLOW);
            m.append(Component.literal(" set to ").withStyle(ChatFormatting.GRAY));
            m.append(Component.literal(String.valueOf(newVal)).withStyle(ChatFormatting.WHITE));
            m.append("  ");
            m.append(makeSetDefaultLink());
            return m;
        }, true);
        return 1;
    }

    // ============================================================
    //  listAllSettings — like Carpet's listSettings
    // ============================================================

    private static void listAllSettings(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        String lastCat = "";

        for (var rule : HearthConfig.getRules().values()) {
            String cat = rule.categories[0];
            if (!cat.equals(lastCat)) {
                lastCat = cat;
                src.sendSuccess(() -> {
                    String key = "hearth.category." + cat;
                    return Component.translatable(key);
                }, false);
            }
            src.sendSuccess(() -> formatRuleLine(rule), false);
        }

        // Subcommands
        src.sendSuccess(() -> Component.literal("§7--- §8Subcommands §7---"), false);
        src.sendSuccess(() -> makeCmdLink("/hearth potionAffectEntity include|exclude <entity>").withStyle(ChatFormatting.GRAY), false);
        src.sendSuccess(() -> makeCmdLink("/hearth reload").withStyle(ChatFormatting.GRAY), false);
        src.sendSuccess(() -> makeCmdLink("/hearth saveDefault").withStyle(ChatFormatting.GRAY), false);
    }

    private static MutableComponent formatRuleLine(HearthRuleDef<?> rule) {
        MutableComponent m = Component.literal("  ");
        // Clickable name
        m.append(Component.literal(rule.name).withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/hearth " + rule.name))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Click to configure " + rule.name)))));
        m.append(Component.literal(" = ").withStyle(ChatFormatting.GRAY));
        m.append(Component.literal(String.valueOf(rule.value())).withStyle(ChatFormatting.WHITE));
        if (!rule.isDefault()) {
            m.append("  ").append(Component.literal("(changed)").withStyle(ChatFormatting.GREEN));
        }
        return m;
    }

    // ============================================================
    //  clickable button helpers (like Carpet's makeSetRuleButton)
    // ============================================================

    private static MutableComponent makeSetRuleButton(HearthRuleDef<?> rule, String value) {
        return Component.literal("[" + value + "]").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/hearth " + rule.name + " " + value))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("/hearth " + rule.name + " " + value))));
    }

    private static MutableComponent makeResetButton(HearthRuleDef<?> rule) {
        return Component.literal("[reset]").withStyle(style -> style
                .withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                        "/hearth " + rule.name + " " + rule.defaultValue))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Reset " + rule.name + " to " + rule.defaultValue))));
    }

    private static MutableComponent makeSetDefaultLink() {
        return Component.literal("[save permanently?]").withStyle(style -> style
                .withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/hearth saveDefault"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Save all settings permanently"))));
    }

    private static MutableComponent makeCmdLink(String cmd) {
        return Component.literal(cmd).withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd.split(" ")[0] + " " + cmd.split(" ")[1])));
    }

    // ============================================================
    //  showRule (non-@HearthRule settings like byproduct)
    // ============================================================

    private static void showRule(CommandContext<CommandSourceStack> ctx, String name) {
        var src = ctx.getSource();
        if ("byproduct".equals(name)) {
            var bp = HearthConfig.getByproductItem();
            src.sendSuccess(() -> {
                MutableComponent m = Component.literal("byproduct").withStyle(ChatFormatting.YELLOW);
                m.append(Component.literal(" = ").withStyle(ChatFormatting.GRAY));
                m.append(Component.literal((bp != null ? bp.toString() : "hearth:wood_ash")
                        + " x" + HearthConfig.getByproductCount()).withStyle(ChatFormatting.WHITE));
                m.append("  ");
                m.append(Component.translatable("commands.hearth.help.byproduct",
                        bp != null ? bp.toString() : "hearth:wood_ash",
                        HearthConfig.getByproductCount()).withStyle(ChatFormatting.GRAY));
                return m;
            }, false);
        }
    }

    // ============================================================
    //  potionAffectEntity
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> buildPotionAffectEntity() {
        var cmd = Commands.literal("potionAffectEntity");
        var include = Commands.literal("include")
                .then(Commands.argument("entity", com.mojang.brigadier.arguments.StringArgumentType.string())
                        .suggests(ENTITY_SUGGESTION)
                        .executes(ctx -> addEntity(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "entity"))));
        cmd.then(include);
        cmd.then(Commands.literal("in").redirect(include.build()));
        cmd.then(Commands.literal("+").redirect(include.build()));
        var exclude = Commands.literal("exclude")
                .then(Commands.argument("entity", com.mojang.brigadier.arguments.StringArgumentType.string())
                        .suggests(ENTITY_SUGGESTION)
                        .executes(ctx -> removeEntity(ctx, com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "entity"))));
        cmd.then(exclude);
        cmd.then(Commands.literal("ex").redirect(exclude.build()));
        cmd.then(Commands.literal("-").redirect(exclude.build()));
        return cmd;
    }

    private static int addEntity(CommandContext<CommandSourceStack> ctx, String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null) { ctx.getSource().sendFailure(Component.translatable("commands.hearth.invalid_entity", entityId)); return 0; }
        HearthConfig.addAffectedEntity(id);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.hearth.potionAffectEntity.include", entityId), true);
        return 1;
    }

    private static int removeEntity(CommandContext<CommandSourceStack> ctx, String entityId) {
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        if (id == null) { ctx.getSource().sendFailure(Component.translatable("commands.hearth.invalid_entity", entityId)); return 0; }
        HearthConfig.removeAffectedEntity(id);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.hearth.potionAffectEntity.exclude", entityId), true);
        return 1;
    }

    private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTION =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                            .map(ResourceLocation::toString)
                            .filter(id -> id.startsWith(builder.getRemainingLowerCase())),
                    builder);
}
