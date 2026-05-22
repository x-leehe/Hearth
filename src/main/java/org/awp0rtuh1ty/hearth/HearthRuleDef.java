package org.awp0rtuh1ty.hearth;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.*;

public final class HearthRuleDef<T> implements Comparable<HearthRuleDef<?>> {
    public final String name;
    public final String descKey;
    public final String[] categories;
    public final List<String> options;
    public final boolean strict;
    public final Class<T> type;
    public final T defaultValue;
    private final Field field;
    private final FromStringConverter<T> converter;

    private static final Map<Class<?>, FromStringConverter<?>> CONVERTER_MAP = Map.of(
            String.class, (FromStringConverter<String>) str -> str,
            Boolean.class, (FromStringConverter<Boolean>) str -> switch (str) {
                case "true" -> true;
                case "false" -> false;
                default -> throw new IllegalArgumentException("Invalid boolean: " + str);
            },
            Integer.class, (FromStringConverter<Integer>) str -> {
                try { return Integer.parseInt(str); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid integer: " + str); }
            },
            Double.class, (FromStringConverter<Double>) str -> {
                try { return Double.parseDouble(str); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid double: " + str); }
            },
            Long.class, (FromStringConverter<Long>) str -> {
                try { return Long.parseLong(str); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid long: " + str); }
            },
            Float.class, (FromStringConverter<Float>) str -> {
                try { return Float.parseFloat(str); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid float: " + str); }
            }
    );

    @FunctionalInterface
    interface FromStringConverter<T> {
        T convert(String value);
    }

    @SuppressWarnings("unchecked")
    public static <T> HearthRuleDef<T> of(Field field) {
        HearthRule ann = field.getAnnotation(HearthRule.class);
        String name = field.getName();
        String[] cats = ann.categories();
        String[] optArr = ann.options();
        boolean strict = ann.strict();

        Class<T> type = (Class<T>) field.getType();
        if (type.isPrimitive()) {
            type = (Class<T>) primitiveToWrapper(type);
        }

        List<String> options;
        if (optArr.length > 0) {
            options = List.of(optArr);
        } else if (type == Boolean.class) {
            options = List.of("true", "false");
        } else {
            options = List.of();
        }

        T defaultValue;
        try {
            defaultValue = (T) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read default value for " + name, e);
        }

        FromStringConverter<T> conv;
        if (type.isEnum()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            final Class<Enum> enumClass = (Class<Enum>) type;
            conv = str -> {
                try {
                    T val = (T) Enum.valueOf(enumClass, str.toUpperCase(Locale.ROOT));
                    return val;
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Valid values: " + List.of(enumClass.getEnumConstants()));
                }
            };
        } else {
            @SuppressWarnings("unchecked")
            FromStringConverter<T> c = (FromStringConverter<T>) CONVERTER_MAP.get(type);
            if (c == null) throw new UnsupportedOperationException("Unsupported type: " + type);
            conv = c;
        }

        return new HearthRuleDef<>(name, field, cats, options, strict, type, defaultValue, conv);
    }

    private HearthRuleDef(String name, Field field, String[] categories, List<String> options,
                          boolean strict, Class<T> type, T defaultValue, FromStringConverter<T> converter) {
        this.name = name;
        this.field = field;
        this.categories = categories;
        this.options = options;
        this.strict = strict;
        this.type = type;
        this.defaultValue = defaultValue;
        this.converter = converter;
        this.descKey = "hearth.rule." + name + ".desc";
        field.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    public T value() {
        try {
            return (T) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access field for " + name, e);
        }
    }

    public void set(CommandSourceStack source, String stringValue) {
        T newValue;
        try {
            newValue = converter.convert(stringValue);
        } catch (Exception e) {
            if (source != null) {
                source.sendFailure(net.minecraft.network.chat.Component.literal("Invalid value for " + name + ": " + stringValue));
            }
            return;
        }
        set(source, newValue, stringValue);
    }

    public void set(CommandSourceStack source, T newValue) {
        set(source, newValue, toRuleString(newValue));
    }

    private void set(CommandSourceStack source, T newValue, String userInput) {
        if (strict && !options.isEmpty() && !options.contains(userInput)) {
            if (source != null) {
                source.sendFailure(net.minecraft.network.chat.Component.literal(
                        "Value '" + userInput + "' not allowed for " + name + ". Options: " + options));
            }
            return;
        }
        try {
            T old = value();
            field.set(null, newValue);
            if (source != null) {
                HearthConfig.syncFromSettings();
                HearthConfig.save();
                HearthConfig.notifyRuleChanged(source, this, userInput);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot set field for " + name, e);
        }
    }

    public void resetToDefault(CommandSourceStack source) {
        set(source, defaultValue);
    }

    public static String toRuleString(Object value) {
        if (value instanceof Enum<?> e) return e.name().toLowerCase(Locale.ROOT);
        return String.valueOf(value);
    }

    public boolean isDefault() {
        return Objects.equals(value(), defaultValue);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HearthRuleDef<?> r && name.equals(r.name);
    }

    @Override
    public int hashCode() { return name.hashCode(); }

    @Override
    public String toString() { return name + ": " + toRuleString(value()); }

    @Override
    public int compareTo(HearthRuleDef<?> o) { return name.compareTo(o.name); }

    private static Class<?> primitiveToWrapper(Class<?> c) {
        if (c == boolean.class) return Boolean.class;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == double.class) return Double.class;
        if (c == float.class) return Float.class;
        return c;
    }

}
