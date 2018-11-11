package org.typemeta.funcj.codec;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

/**
 * Internal interface for {@link CodecCore} implementations.
 * @param <IN>      the encoded input type
 * @param <OUT>     the encoded output type
 * @param <CFG>     the config type
 */
public interface CodecCoreEx<IN, OUT, CFG extends CodecConfig>
        extends CodecCore<IN, OUT, CFG> {

    CodecFormat<IN, OUT, CFG> format();

    <T> Optional<NoArgsTypeCtor<T>> getNoArgsCtorOpt(Class<T> clazz);

    <T> NoArgsTypeCtor<T> getNoArgsCtor(Class<T> clazz);

    <T> Optional<ArgArrayTypeCtor<T>> getArgArrayCtorOpt(Class<T> clazz);

    default <T> boolean encodeDynamicType(Codec<T, IN, OUT, CFG> codec, T val, OUT out) {
        return format().encodeDynamicType(this, codec, val, out, this::getCodec);
    }

    default <T> T decodeDynamicType(IN in) {
        return format().decodeDynamicType(this, in);
    }

    /**
     * Lookup a {@code Codec} for a name, and, if one doesn't exist,
     * then create a new one.
     * @param clazz     the class
     * @param <T>       the raw type to be encoded/decoded
     * @return          the {@code Codec} for the specified name
     */
    <T> Codec<T, IN, OUT, CFG> getCodec(Class<T> clazz);

    /**
     * Lookup a {@code Codec} for a name, and, if one doesn't exist,
     * then create a new one using the supplier.
     * <p>
     * This method needs to be thread-safe and re-entrant in case the
     * type in question is recursive.
     * @param key       the class key
     * @param codecSupp the supplier of the new codec
     * @param <T>       the raw type to be encoded/decoded
     * @return          the {@code Codec} for the specified name
     */
    <T> Codec<T, IN, OUT, CFG> getCodec(
            ClassKey key,
            Supplier<Codec<T, IN, OUT, CFG>> codecSupp);

    <T> Codec<Collection<T>, IN, OUT, CFG> getCollCodec(
            Class<Collection<T>> collType,
            Codec<T, IN, OUT, CFG> elemCodec);

    <K, V> Codec<Map<K, V>, IN, OUT, CFG> getMapCodec(
            Class<Map<K, V>> mapType,
            Class<K> keyType,
            Class<V> valType);

    <K, V> Codec<Map<K, V>, IN, OUT, CFG> getMapCodec(
            Class<Map<K, V>> mapType,
            Codec<K, IN, OUT, CFG> keyCodec,
            Codec<V, IN, OUT, CFG> valueCodec);

    <V> Codec<Map<String, V>, IN, OUT, CFG> getMapCodec(
            Class<Map<String, V>> mapType,
            Codec<V, IN, OUT, CFG> valueCodec);

    <K, V> Codec<Map<K, V>, IN, OUT, CFG> createMapCodec(
            Class<Map<K, V>> mapType,
            Class<K> keyType,
            Class<V> valType);

    <T> Codec<T, IN, OUT, CFG> createCodec(Class<T> clazz);

    <T> Codec<T, IN, OUT, CFG> createObjectCodec(Class<T> clazz, NoArgsTypeCtor<T> ctor);

    <T> Codec<T, IN, OUT, CFG> createObjectCodec(
            Class<T> clazz,
            Map<String, FieldCodec<IN, OUT, CFG>> fieldCodecs,
            NoArgsTypeCtor<T> ctor);

    <T> Codec<T, IN, OUT, CFG> createObjectCodec(Class<T> clazz, ArgArrayTypeCtor<T> ctor);

    <T> ObjectCodecBuilder<T, IN, OUT, CFG> objectCodecDeferredRegister(Class<T> clazz);

    <T> Codec<T, IN, OUT, CFG> createObjectCodec(Class<T> clazz);

    <T> Codec<T, IN, OUT, CFG> createObjectCodec(
            Class<T> clazz,
            Map<String, ObjectCodecBuilder.FieldCodec<T, IN, OUT, CFG>> fieldCodecs,
            ArgArrayTypeCtor<T> ctor);

    <T> FieldCodec<IN, OUT, CFG> createFieldCodec(Field field);

}
