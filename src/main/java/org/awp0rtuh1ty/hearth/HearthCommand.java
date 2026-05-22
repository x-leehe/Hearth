package org.awp0rtuh1ty.hearth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.SharedSuggestionProvider.suggest;

public class HearthCommand {
    private static final String ID = "hearth";

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register(HearthCommand::register);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                 CommandBuildContext buildContext,
                                 Commands.CommandSelection environment) {
        if (dispatcher.getRoot().getChildren().stream().anyMatch(n -> n.getName().equalsIgnoreCase(ID))) return;

        LiteralArgumentBuilder<CommandSourceStack> root = literal(ID)
                .requires(s -> s.hasPermission(2));

        // --- (no args) → list all non-default settings ---
        root.executes(c -> listAllSettings(c.getSource()));

        // --- list ---
        root.then(literal("list")
                .executes(c -> listSettings(c.getSource(), HearthConfig.getRulesSorted())));

        // --- byproduct ---
        root.then(literal("byproduct")
                .executes(c -> { showByproduct(c.getSource()); return 1; })
                .then(argument("item", ItemArgument.item(buildContext))
                        .then(argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(c -> {
                                    ItemInput item = ItemArgument.getItem(c, "item");
                                    int count = IntegerArgumentType.getInteger(c, "count");
                                    String itemId = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
                                    HearthConfig.setByproduct(itemId, count);
                                    HearthMessenger.m(c.getSource(),
                                            "gi " + Component.translatable("commands.hearth.byproduct.success", itemId, count).getString());
                                    return count;
                                }))));

        // --- potionAffectEntity ---
        root.then(buildPotionAffectEntity());

        // --- reload ---
        root.then(literal("reload").executes(c -> {
            HearthConfig.initialize();
            HearthMessenger.m(c.getSource(), "gi " + Component.translatable("commands.hearth.reload.success").getString());
            return 1;
        }));

        // --- saveDefault ---
        root.then(literal("saveDefault").executes(c -> {
            HearthConfig.save();
            HearthMessenger.m(c.getSource(), "gi " + Component.translatable("commands.hearth.saveDefault.success").getString());
            return 1;
        }));

        // --- <rule> ---
        root.then(argument("rule", StringArgumentType.word())
                .suggests(RULE_SUGGEST)
                .executes(c -> displayRuleMenu(c.getSource(), contextRule(c)))
                .then(argument("value", StringArgumentType.greedyString())
                        .suggests((c, b) -> suggest(contextRule(c).options, b))
                        .executes(c -> setRule(c.getSource(), contextRule(c), StringArgumentType.getString(c, "value")))));

        dispatcher.register(root);
    }

    // ============================================================
    //  contextRule — like Carpet's contextRule
    // ============================================================

    private static HearthRuleDef<?> contextRule(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "rule");
        HearthRuleDef<?> rule = HearthConfig.getRule(name);
        if (rule == null) throw new RuntimeException("Unknown rule: " + name); // won't happen due to suggestions
        return rule;
    }

    // ============================================================
    //  setRule — like Carpet's setRule
    // ============================================================

    private static int setRule(CommandSourceStack source, HearthRuleDef<?> rule, String newValue) {
        rule.set(source, newValue);
        String displayName = rule.translatedName().getString();
        HearthMessenger.m(source,
                "w " + rule.name() + " set to " + rule.value() + ", ",
                "c [" + Component.translatable("commands.hearth.save.hint").getString() + "]",
                "^w Click to save permanently",
                "?/hearth saveDefault");
        return 1;
    }

    // ============================================================
    //  displayRuleMenu — like Carpet's displayRuleMenu
    // ============================================================

    private static int displayRuleMenu(CommandSourceStack source, HearthRuleDef<?> rule) {
        String displayName = rule.translatedName().getString();
        String desc = rule.translatedDesc().getString();

        HearthMessenger.m(source, "");
        HearthMessenger.m(source, "wb " + displayName, "?/hearth " + rule.name(), "^g Click to set");
        HearthMessenger.m(source, "w " + desc);

        // Categories
        List<Object> tags = new ArrayList<>();
        tags.add("w Categories: ");
        tags.add("c [" + rule.categories[0] + "]");
        HearthMessenger.m(source, tags.toArray());

        // Current value
        String valStr = "nb " + rule.value();
        if (rule.isDefault()) valStr = "lb " + rule.value();
        HearthMessenger.m(source, "w Current value: ", valStr + " (" + (rule.isDefault() ? "default" : "modified") + ")");

        // Options buttons
        List<Object> opts = new ArrayList<>();
        opts.add("w Options: [ ");
        for (String o : rule.options) {
            opts.add(makeSetRuleButton(rule, o));
            opts.add("w  ");
        }
        if (!opts.isEmpty()) opts.remove(opts.size() - 1);
        opts.add("w ]");
        HearthMessenger.m(source, opts.toArray());

        return 1;
    }

    // ============================================================
    //  makeSetRuleButton — like Carpet's makeSetRuleButton
    // ============================================================

    private static String makeSetRuleButton(HearthRuleDef<?> rule, String option) {
        boolean isCurrent = option.equalsIgnoreCase(String.valueOf(rule.value()));
        boolean isDefault = option.equalsIgnoreCase(String.valueOf(rule.defaultValue));
        String style = isDefault ? "e" : "y";
        if (isCurrent) style = style + "u" + (isDefault ? "b" : "");
        return style + " [" + option + "]";
    }

    // ============================================================
    //  listAllSettings — like Carpet's listAllSettings
    // ============================================================

    private static int listAllSettings(CommandSourceStack source) {
        Collection<HearthRuleDef<?>> nonDefault = HearthConfig.getRulesSorted().stream()
                .filter(r -> !r.isDefault()).toList();
        listSettings(source, nonDefault);

        // Category browser
        List<String> cats = HearthConfig.getRulesSorted().stream()
                .map(r -> r.categories[0]).distinct().toList();
        List<Object> tagList = new ArrayList<>();
        tagList.add("w Browse:\n");
        for (String t : cats) {
            tagList.add("c [" + t + "]");
            tagList.add("^g List all " + t + " rules");
            tagList.add("!/hearth list");
            tagList.add("w  ");
        }
        if (!tagList.isEmpty()) tagList.remove(tagList.size() - 1);
        HearthMessenger.m(source, tagList.toArray());
        return nonDefault.size();
    }

    private static int listSettings(CommandSourceStack source, Collection<HearthRuleDef<?>> rules) {
        for (HearthRuleDef<?> r : rules) {
            HearthMessenger.m(source, displayInteractiveSetting(r));
        }
        return rules.size();
    }

    // ============================================================
    //  displayInteractiveSetting — like Carpet's displayInteractiveSetting
    // ============================================================

    private static Component displayInteractiveSetting(HearthRuleDef<?> rule) {
        List<Object> args = new ArrayList<>();
        args.add("w - " + rule.translatedName().getString() + " ");
        args.add("?/hearth " + rule.name());
        args.add("^y " + rule.translatedDesc().getString());
        for (String opt : rule.options) {
            args.add(makeSetRuleButton(rule, opt));
            args.add("w  ");
        }
        if (!rule.options.isEmpty()) args.remove(args.size() - 1);
        return HearthMessenger.c(args.toArray());
    }

    // ============================================================
    //  showByproduct
    // ============================================================

    private static void showByproduct(CommandSourceStack source) {
        var bp = HearthConfig.getByproductItem();
        String val = (bp != null ? bp.toString() : "hearth:wood_ash") + " x" + HearthConfig.getByproductCount();
        HearthMessenger.m(source, "wb byproduct = " + val, "?/hearth byproduct ", "^g Set byproduct");
    }

    // ============================================================
    //  potionAffectEntity
    // ============================================================

    private static LiteralArgumentBuilder<CommandSourceStack> buildPotionAffectEntity() {
        var cmd = literal("potionAffectEntity");
        var include = literal("include")
                .then(argument("entity", StringArgumentType.string())
                        .suggests(ENTITY_SUGGEST)
                        .executes(c -> addEntity(c, StringArgumentType.getString(c, "entity"))));
        cmd.then(include);
        cmd.then(literal("in").redirect(include.build()));
        cmd.then(literal("+").redirect(include.build()));
        var exclude = literal("exclude")
                .then(argument("entity", StringArgumentType.string())
                        .suggests(ENTITY_SUGGEST)
                        .executes(c -> removeEntity(c, StringArgumentType.getString(c, "entity"))));
        cmd.then(exclude);
        cmd.then(literal("ex").redirect(exclude.build()));
        cmd.then(literal("-").redirect(exclude.build()));
        return cmd;
    }

    private static int addEntity(CommandContext<CommandSourceStack> ctx, String id) {
        ResourceLocation r = ResourceLocation.tryParse(id);
        if (r == null) { ctx.getSource().sendFailure(Component.translatable("commands.hearth.invalid_entity", id)); return 0; }
        HearthConfig.addAffectedEntity(r);
        HearthMessenger.m(ctx.getSource(), "gi " + Component.translatable("commands.hearth.potionAffectEntity.include", id).getString());
        return 1;
    }

    private static int removeEntity(CommandContext<CommandSourceStack> ctx, String id) {
        ResourceLocation r = ResourceLocation.tryParse(id);
        if (r == null) { ctx.getSource().sendFailure(Component.translatable("commands.hearth.invalid_entity", id)); return 0; }
        HearthConfig.removeAffectedEntity(r);
        HearthMessenger.m(ctx.getSource(), "gi " + Component.translatable("commands.hearth.potionAffectEntity.exclude", id).getString());
        return 1;
    }

    private static final SuggestionProvider<CommandSourceStack> RULE_SUGGEST =
            (c, b) -> {
                List<String> names = new ArrayList<>(HearthConfig.getRules().keySet());
                return suggest(names.stream().filter(n -> n.toLowerCase().startsWith(b.getRemainingLowerCase())), b);
            };

    private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGEST =
            (c, b) -> suggest(
                    BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                            .map(ResourceLocation::toString)
                            .filter(id -> id.startsWith(b.getRemainingLowerCase())), b);
}
