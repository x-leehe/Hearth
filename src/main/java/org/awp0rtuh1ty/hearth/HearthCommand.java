package org.awp0rtuh1ty.hearth;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.awp0rtuh1ty.hearth.block.RepellentBlockEntity;

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

        root.executes(c -> listAllSettings(c.getSource()));

        root.then(literal("list")
                .executes(c -> listAllSettings(c.getSource()))
                .then(argument("category", StringArgumentType.word())
                        .suggests((c, b) -> suggest(
                                HearthConfig.getRulesSorted().stream()
                                        .map(r -> r.categories[0]).distinct().toList(), b))
                        .executes(c -> {
                            String cat = StringArgumentType.getString(c, "category");
                            var filtered = HearthConfig.getRulesSorted().stream()
                                    .filter(r -> Arrays.asList(r.categories).contains(cat)).toList();
                            listSettings(c.getSource(), filtered);
                            return filtered.size();
                        })));

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

        root.then(buildPotionAffectEntity());

        root.then(literal("reload").executes(c -> {
            HearthConfig.initialize();
            HearthMessenger.m(c.getSource(), "gi " + Component.translatable("commands.hearth.reload.success").getString());
            return 1;
        }));

        root.then(literal("saveDefault").executes(c -> {
            HearthConfig.save();
            HearthMessenger.m(c.getSource(), "gi " + Component.translatable("commands.hearth.saveDefault.success").getString());
            return 1;
        }));

        root.then(literal("status")
                .executes(c -> showBlockStatus(c.getSource())));

        root.then(argument("rule", StringArgumentType.word())
                .suggests(RULE_SUGGEST)
                .executes(c -> displayRuleMenu(c.getSource(), contextRule(c)))
                .then(argument("value", StringArgumentType.greedyString())
                        .suggests((c, b) -> suggest(contextRule(c).options, b))
                        .executes(c -> setRule(c.getSource(), contextRule(c), StringArgumentType.getString(c, "value")))));

        dispatcher.register(root);
    }

    // ============================================================
    //  contextRule
    // ============================================================

    private static HearthRuleDef<?> contextRule(CommandContext<CommandSourceStack> ctx) {
        String name = StringArgumentType.getString(ctx, "rule");
        HearthRuleDef<?> rule = HearthConfig.getRule(name);
        if (rule == null) throw new RuntimeException("Unknown rule: " + name);
        return rule;
    }

    // ============================================================
    //  setRule
    // ============================================================

    private static int setRule(CommandSourceStack source, HearthRuleDef<?> rule, String newValue) {
        rule.set(source, newValue);
        HearthMessenger.m(source,
                "w " + rule.translatedName().getString() + " set to " + rule.value() + ", ",
                "c [" + Component.translatable("commands.hearth.save.hint").getString() + "]",
                "^w " + Component.translatable("commands.hearth.save.hover").getString(),
                "?/hearth saveDefault");
        return 1;
    }

    // ============================================================
    //  displayRuleMenu
    // ============================================================

    private static int displayRuleMenu(CommandSourceStack source, HearthRuleDef<?> rule) {
        String displayName = rule.translatedName().getString();
        String desc = rule.translatedDesc().getString();

        HearthMessenger.m(source, "");
        HearthMessenger.m(source, "wb " + displayName, "?/hearth " + rule.name(), "^g " + displayName);
        HearthMessenger.m(source, "w " + desc);

        // Category
        HearthMessenger.m(source, "w " + Component.translatable("commands.hearth.ui.category").getString()
                + ": ", "c [" + rule.categories[0] + "]");

        // Current value
        String valStyle = rule.isDefault() ? "lb " : "nb ";
        String status = rule.isDefault()
                ? Component.translatable("commands.hearth.status.default").getString()
                : Component.translatable("commands.hearth.status.changed").getString();
        HearthMessenger.m(source, "w " + Component.translatable("commands.hearth.ui.current_value").getString()
                + ": ", valStyle + rule.value() + " (" + status + ")");

        // Options
        List<Object> opts = new ArrayList<>();
        opts.add("w " + Component.translatable("commands.hearth.ui.options").getString() + ": [ ");
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
    //  makeSetRuleButton — RETURNS COMPONENT with click/hover (反 Carpet)
    // ============================================================

    private static Component makeSetRuleButton(HearthRuleDef<?> rule, String option) {
        boolean isCurrent = option.equalsIgnoreCase(String.valueOf(rule.value()));
        boolean isDefault = option.equalsIgnoreCase(String.valueOf(rule.defaultValue));
        String style = isDefault ? "e" : "y";
        if (isCurrent) style = style + "u" + (isDefault ? "b" : "");
        String label = style + " [" + option + "]";

        if (isCurrent)
            return HearthMessenger.c(label);

        String hover = Component.translatable("commands.hearth.hover.switch_to").getString()
                + " " + option + (isDefault ? " (" + Component.translatable("commands.hearth.status.default").getString() + ")" : "");
        return HearthMessenger.c(label,
                "^g " + hover,
                "?/hearth " + rule.name() + " " + option);
    }

    // ============================================================
    //  listAllSettings
    // ============================================================

    private static int listAllSettings(CommandSourceStack source) {
        Collection<HearthRuleDef<?>> nonDefault = HearthConfig.getRulesSorted().stream()
                .filter(r -> !r.isDefault()).toList();

        if (!nonDefault.isEmpty()) {
            HearthMessenger.m(source, "wb " + Component.translatable("commands.hearth.ui.non_default").getString() + ":");
            listSettings(source, nonDefault);
        }

        // Category browser
        List<String> cats = HearthConfig.getRulesSorted().stream()
                .map(r -> r.categories[0]).distinct().toList();
        List<Object> tagList = new ArrayList<>();
        tagList.add("w " + Component.translatable("commands.hearth.ui.browse").getString() + ":\n");
        for (String t : cats) {
            tagList.add("c [" + t + "]");
            tagList.add("^g " + Component.translatable("commands.hearth.hover.list_category").getString() + " " + t);
            tagList.add("!/hearth list " + t);
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
    //  displayInteractiveSetting — one-line clickable rule summary
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

    private static void showByproduct(CommandSourceStack source) {
        var bp = HearthConfig.getByproductItem();
        String val = (bp != null ? bp.toString() : "hearth:wood_ash") + " x" + HearthConfig.getByproductCount();
        HearthMessenger.m(source, "wb byproduct = " + val, "?/hearth byproduct ", "^g Set byproduct");
    }

    // ============================================================
    //  status — show block info when crosshair is on a Hearth block
    // ============================================================

    private static int showBlockStatus(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        HitResult hit = player.pick(5.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) {
            HearthMessenger.m(source, "r No block targeted");
            return 0;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockEntity be = source.getLevel().getBlockEntity(pos);
        if (be instanceof RepellentBlockEntity repellent) {
            int ticks = repellent.getRemainingTicks();
            String variant = repellent.getPotionVariant();
            int count = repellent.getPotionCount();
            boolean powered = source.getLevel().hasNeighborSignal(pos);

            HearthMessenger.m(source, "wb -- Repellent --");
            HearthMessenger.m(source, "w Powered: ", powered ? "lb yes" : "r no");
            HearthMessenger.m(source, "w Potions: ", "c " + count);
            if (variant != null && ticks > 0) {
                int seconds = ticks / 20;
                int min = seconds / 60;
                seconds %= 60;
                String variantDisplay = switch (variant) {
                    case "long" -> "Long";
                    case "strong" -> "Strong";
                    default -> "Normal";
                };
                HearthMessenger.m(source, "w Variant: ", "c " + variantDisplay);
                HearthMessenger.m(source, "w Remaining: ", "y " + String.format("%d:%02d", min, seconds));
            } else {
                HearthMessenger.m(source, "w Status: ", "r idle");
            }
        } else if (be instanceof AbstractFurnaceBlockEntity furnace) {
            HearthMessenger.m(source, "wb -- Furnace --");
            try {
                var ashStorage = (AshStorage) furnace;
                var s3 = ashStorage.hearth$getExtraSlot3();
                var s4 = ashStorage.hearth$getExtraSlot4();
                HearthMessenger.m(source, "w Cooking: " + ashStorage.hearth$getCookingProgress() + "/" + ashStorage.hearth$getCookingTotalTime());
                HearthMessenger.m(source, "w Ash slot 3: ", s3.isEmpty() ? "g empty" : "c " + s3.getCount() + " " + s3.getHoverName().getString());
                HearthMessenger.m(source, "w Ash slot 4: ", s4.isEmpty() ? "g empty" : "c " + s4.getCount() + " " + s4.getHoverName().getString());
            } catch (Exception ignored) {}
        } else {
            HearthMessenger.m(source, "r No Hearth block targeted");
        }
        return 1;
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

    // ============================================================
    //  suggestions
    // ============================================================

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
