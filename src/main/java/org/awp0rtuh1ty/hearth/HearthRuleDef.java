package org.awp0rtuh1ty.hearth;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.*;

public final class HearthRuleDef<T> {
    private final String name;
    public final String[] categories;
    public final List<String> options;
    public final boolean strict;
    public final Class<T> type;
    public final T defaultValue;
    private final Field field;
    private final FromString<T> converter;

    @FunctionalInterface
    public interface FromString<T> {
        T convert(String value);
    }

    private static final Map<Class<?>, FromString<?>> CONVERTERS = Map.ofEntries(
            Map.entry(String.class, (FromString<String>) s -> s),
            Map.entry(Boolean.class, (FromString<Boolean>) s -> switch (s) {
                case "true" -> true; case "false" -> false;
                default -> throw new IllegalArgumentException("Invalid boolean: " + s);
            }),
            Map.entry(Integer.class, (FromString<Integer>) s -> {
                try { return Integer.parseInt(s); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid integer: " + s); }
            }),
            Map.entry(Double.class, (FromString<Double>) s -> {
                try { return Double.parseDouble(s); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid double: " + s); }
            }),
            Map.entry(Long.class, (FromString<Long>) s -> {
                try { return Long.parseLong(s); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid long: " + s); }
            }),
            Map.entry(Float.class, (FromString<Float>) s -> {
                try { return Float.parseFloat(s); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid float: " + s); }
            })
    );

    @SuppressWarnings("unchecked")
    static <T> HearthRuleDef<T> of(Field field) {
        HearthRule ann = field.getAnnotation(HearthRule.class);
        String name = field.getName();
        String[] cats = ann.categories();
        String[] optArr = ann.options();
        boolean strict = ann.strict();

        Class<T> type = (Class<T>) field.getType();
        if (type.isPrimitive()) type = (Class<T>) wrap(type);

        List<String> options;
        if (optArr.length > 0) options = List.of(optArr);
        else if (type == Boolean.class) options = List.of("true", "false");
        else options = List.of();

        T defaultValue;
        try { defaultValue = (T) field.get(null); }
        catch (IllegalAccessException e) { throw new RuntimeException(e); }

        FromString<T> conv;
        if (type.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            final Class<Enum> enumClass = (Class<Enum>) type;
            conv = s -> {
                try { return (T) Enum.valueOf(enumClass, s.toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Valid: " + List.of(enumClass.getEnumConstants()));
                }
            };
        } else {
            @SuppressWarnings("unchecked")
            FromString<T> c = (FromString<T>) CONVERTERS.get(type);
            if (c == null) throw new UnsupportedOperationException("Unsupported type: " + type);
            conv = c;
        }

        return new HearthRuleDef<>(name, field, cats, options, strict, type, defaultValue, conv);
    }

    private HearthRuleDef(String name, Field field, String[] categories, List<String> options,
                          boolean strict, Class<T> type, T defaultValue, FromString<T> converter) {
        this.name = name; this.field = field; this.categories = categories;
        this.options = options; this.strict = strict; this.type = type;
        this.defaultValue = defaultValue; this.converter = converter;
        field.setAccessible(true);
    }

    public String name() { return name; }

    @SuppressWarnings("unchecked")
    public T value() {
        try { return (T) field.get(null); }
        catch (IllegalAccessException e) { throw new RuntimeException(e); }
    }

    public boolean isDefault() { return Objects.equals(value(), defaultValue); }

    public void set(CommandSourceStack source, String stringValue) {
        T newValue;
        try { newValue = converter.convert(stringValue); }
        catch (Exception e) {
            if (source != null) source.sendFailure(Component.literal("Invalid value: " + stringValue));
            return;
        }
        if (strict && !options.isEmpty() && !options.contains(stringValue)) {
            if (source != null) source.sendFailure(Component.literal("Not allowed. Options: " + options));
            return;
        }
        try {
            field.set(null, newValue);
            HearthConfig.syncFromSettings();
            HearthConfig.save();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetToDefault(CommandSourceStack source) { set(source, toRuleString(defaultValue)); }

    public static String toRuleString(Object v) {
        if (v instanceof Enum<?> e) return e.name().toLowerCase(Locale.ROOT);
        return String.valueOf(v);
    }

    public Component translatedName() {
        return Component.translatable("hearth.rule." + name + ".name");
    }
    public Component translatedDesc() {
        return Component.translatable("hearth.rule." + name + ".desc");
    }

    @Override public String toString() { return name + ": " + toRuleString(value()); }
    @Override public boolean equals(Object o) { return o instanceof HearthRuleDef<?> r && name.equals(r.name); }
    @Override public int hashCode() { return name.hashCode(); }

    private static Class<?> wrap(Class<?> c) {
        if (c == boolean.class) return Boolean.class;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        return c;
    }
}
