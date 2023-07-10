/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.util.stream;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class MoreCollectors {

  private static final int DEFAULT_HASHMAP_CAPACITY = 0;
  private static final String KEY_FUNCTION_CANT_RETURN_NULL_MESSAGE = "Key function can't return null";
  private static final String VALUE_FUNCTION_CANT_RETURN_NULL_MESSAGE = "Value function can't return null";

  private MoreCollectors() {
    // prevents instantiation
  }


  /**
   * Creates an {@link ImmutableMap} from the stream where the values are the values in the stream and the keys are the
   * result of the provided {@link Function keyFunction} applied to each value in the stream.
   *
   * <p>
   * The {@link Function keyFunction} must return a unique (according to the key's type {@link Object#equals(Object)}
   * and/or {@link Comparable#compareTo(Object)} implementations) value for each of them, otherwise a
   * {@link IllegalArgumentException} will be thrown.
   * </p>
   *
   * <p>
   * {@link Function keyFunction} can't return {@code null}, otherwise a {@link NullPointerException} will be thrown.
   * </p>
   *
   * @throws NullPointerException if {@code keyFunction} is {@code null}.
   * @throws NullPointerException if result of {@code keyFunction} is {@code null}.
   * @throws IllegalArgumentException if {@code keyFunction} returns the same value for multiple entries in the stream.
   */
  public static <K, E> Collector<E, Map<K, E>, ImmutableMap<K, E>> uniqueIndex(Function<? super E, K> keyFunction) {
    return uniqueIndex(keyFunction, Function.identity());
  }

  /**
   * Same as {@link #uniqueIndex(Function)} but using an underlying {@link Map} initialized with a capacity for the
   * specified expected size.
   *
   * <p>Note: using this method with a parallel stream will likely not have the expected memory usage benefit as all
   * processing threads will use a Map with a capacity large enough for the final size.</p>
   *
   * <p>
   * {@link Function keyFunction} can't return {@code null}, otherwise a {@link NullPointerException} will be thrown.
   * </p>
   *
   * @throws NullPointerException if {@code keyFunction} is {@code null}.
   * @throws NullPointerException if result of {@code keyFunction} is {@code null}.
   * @throws IllegalArgumentException if {@code keyFunction} returns the same value for multiple entries in the stream.
   * @see #uniqueIndex(Function)
   */
  public static <K, E> Collector<E, Map<K, E>, ImmutableMap<K, E>> uniqueIndex(Function<? super E, K> keyFunction, int expectedSize) {
    return uniqueIndex(keyFunction, Function.identity(), expectedSize);
  }

  /**
   * Creates an {@link ImmutableMap} from the stream where the values are the result of {@link Function valueFunction}
   * applied to the values in the stream and the keys are the result of the provided {@link Function keyFunction}
   * applied to each value in the stream.
   *
   * <p>
   * The {@link Function keyFunction} must return a unique (according to the key's type {@link Object#equals(Object)}
   * and/or {@link Comparable#compareTo(Object)} implementations) value for each of them, otherwise a
   * {@link IllegalArgumentException} will be thrown.
   * </p>
   *
   * <p>
   * Neither {@link Function keyFunction} nor {@link Function valueFunction} can return {@code null}, otherwise a
   * {@link NullPointerException} will be thrown.
   * </p>
   *
   * @throws NullPointerException if {@code keyFunction} or {@code valueFunction} is {@code null}.
   * @throws NullPointerException if result of {@code keyFunction} or {@code valueFunction} is {@code null}.
   * @throws IllegalArgumentException if {@code keyFunction} returns the same value for multiple entries in the stream.
   */
  public static <K, E, V> Collector<E, Map<K, V>, ImmutableMap<K, V>> uniqueIndex(Function<? super E, K> keyFunction,
    Function<? super E, V> valueFunction) {
    return uniqueIndex(keyFunction, valueFunction, DEFAULT_HASHMAP_CAPACITY);
  }

  /**
   * Same as {@link #uniqueIndex(Function, Function)} but using an underlying {@link Map} initialized with a capacity
   * for the specified expected size.
   *
   * <p>Note: using this method with a parallel stream will likely not have the expected memory usage benefit as all
   * processing threads will use a Map with a capacity large enough for the final size.</p>
   *
   * <p>
   * Neither {@link Function keyFunction} nor {@link Function valueFunction} can return {@code null}, otherwise a
   * {@link NullPointerException} will be thrown.
   * </p>
   *
   * @throws NullPointerException if {@code keyFunction} or {@code valueFunction} is {@code null}.
   * @throws NullPointerException if result of {@code keyFunction} or {@code valueFunction} is {@code null}.
   * @throws IllegalArgumentException if {@code keyFunction} returns the same value for multiple entries in the stream.
   * @see #uniqueIndex(Function, Function)
   */
  public static <K, E, V> Collector<E, Map<K, V>, ImmutableMap<K, V>> uniqueIndex(Function<? super E, K> keyFunction,
    Function<? super E, V> valueFunction, int expectedSize) {
    verifyKeyAndValueFunctions(keyFunction, valueFunction);

    BiConsumer<Map<K, V>, E> accumulator = (map, element) -> {
      K key = requireNonNull(keyFunction.apply(element), KEY_FUNCTION_CANT_RETURN_NULL_MESSAGE);
      V value = requireNonNull(valueFunction.apply(element), VALUE_FUNCTION_CANT_RETURN_NULL_MESSAGE);

      putAndFailOnDuplicateKey(map, key, value);
    };
    BinaryOperator<Map<K, V>> merger = (m1, m2) -> {
      for (Map.Entry<K, V> entry : m2.entrySet()) {
        putAndFailOnDuplicateKey(m1, entry.getKey(), entry.getValue());
      }
      return m1;
    };
    return Collector.of(
      newHashMapSupplier(expectedSize),
      accumulator,
      merger,
      ImmutableMap::copyOf,
      Collector.Characteristics.UNORDERED);
  }

  private static <K, V> Supplier<Map<K, V>> newHashMapSupplier(int expectedSize) {
    return () -> expectedSize == DEFAULT_HASHMAP_CAPACITY ? new HashMap<>() : new HashMap<>(expectedSize);
  }

  private static <K, V> void putAndFailOnDuplicateKey(Map<K, V> map, K key, V value) {
    V existingValue = map.put(key, value);
    if (existingValue != null) {
      throw new IllegalArgumentException(String.format("Duplicate key %s", key));
    }
  }

  /**
   * Creates an {@link com.google.common.collect.ImmutableListMultimap} from the stream where the values are the values
   * in the stream and the keys are the result of the provided {@link Function keyFunction} applied to each value in the
   * stream.
   *
   * <p>
   * Neither {@link Function keyFunction} nor {@link Function valueFunction} can return {@code null}, otherwise a
   * {@link NullPointerException} will be thrown.
   * </p>
   *
   * @throws NullPointerException if {@code keyFunction} or {@code valueFunction} is {@code null}.
   * @throws NullPointerException if result of {@code keyFunction} or {@code valueFunction} is {@code null}.
   */
  public static <K, E> Collector<E, ImmutableListMultimap.Builder<K, E>, ImmutableListMultimap<K, E>> index(Function<? super E, K> keyFunction) {
    return index(keyFunction, Function.identity());
  }

  /**
   * Creates an {@link com.google.common.collect.ImmutableListMultimap} from the stream where the values are the result
   * of {@link Function valueFunction} applied to the values in the stream and the keys are the result of the provided
   * {@link Function keyFunction} applied to each value in the stream.
   *
   * <p>
   * Neither {@link Function keyFunction} nor {@link Function valueFunction} can return {@code null}, otherwise a
   * {@link NullPointerException} will be thrown.
   * </p>
   *
   * @throws NullPointerException if {@code keyFunction} or {@code valueFunction} is {@code null}.
   * @throws NullPointerException if result of {@code keyFunction} or {@code valueFunction} is {@code null}.
   */
  public static <K, E, V> Collector<E, ImmutableListMultimap.Builder<K, V>, ImmutableListMultimap<K, V>> index(Function<? super E, K> keyFunction,
    Function<? super E, V> valueFunction) {
    verifyKeyAndValueFunctions(keyFunction, valueFunction);

    BiConsumer<ImmutableListMultimap.Builder<K, V>, E> accumulator = (map, element) -> {
      K key = requireNonNull(keyFunction.apply(element), KEY_FUNCTION_CANT_RETURN_NULL_MESSAGE);
      V value = requireNonNull(valueFunction.apply(element), VALUE_FUNCTION_CANT_RETURN_NULL_MESSAGE);

      map.put(key, value);
    };
    BinaryOperator<ImmutableListMultimap.Builder<K, V>> merger = (m1, m2) -> {
      for (Map.Entry<K, V> entry : m2.build().entries()) {
        m1.put(entry.getKey(), entry.getValue());
      }
      return m1;
    };
    return Collector.of(
      ImmutableListMultimap::builder,
      accumulator,
      merger,
      ImmutableListMultimap.Builder::build);
  }

  /**
   * Creates an {@link com.google.common.collect.ImmutableSetMultimap} from the stream where the values are the values
   * in the stream and the keys are the result of the provided {@link Function keyFunction} applied to each value in the
   * stream.
   *
   * <p>
   * Neither {@link Function keyFunction} nor {@link Function valueFunction} can return {@code null}, otherwise a
   * {@link NullPointerException} will be thrown.
   * </p>
   *
   * @throws NullPointerException if {@code keyFunction} or {@code valueFunction} is {@code null}.
   * @throws NullPointerException if result of {@code keyFunction} or {@code valueFunction} is {@code null}.
   */
  public static <K, E> Collector<E, ImmutableSetMultimap.Builder<K, E>, ImmutableSetMultimap<K, E>> unorderedIndex(Function<? super E, K> keyFunction) {
    return unorderedIndex(keyFunction, Function.identity());
  }

  /**
   * Creates an {@link com.google.common.collect.ImmutableSetMultimap} from the stream where the values are the result
   * of {@link Function valueFunction} applied to the values in the stream and the keys are the result of the provided
   * {@link Function keyFunction} applied to each value in the stream.
   *
   * <p>
   * Neither {@link Function keyFunction} nor {@link Function valueFunction} can return {@code null}, otherwise a
   * {@link NullPointerException} will be thrown.
   * </p>
   *
   * @throws NullPointerException if {@code keyFunction} or {@code valueFunction} is {@code null}.
   * @throws NullPointerException if result of {@code keyFunction} or {@code valueFunction} is {@code null}.
   */
  public static <K, E, V> Collector<E, ImmutableSetMultimap.Builder<K, V>, ImmutableSetMultimap<K, V>> unorderedIndex(Function<? super E, K> keyFunction,
    Function<? super E, V> valueFunction) {
    verifyKeyAndValueFunctions(keyFunction, valueFunction);

    BiConsumer<ImmutableSetMultimap.Builder<K, V>, E> accumulator = (map, element) -> {
      K key = requireNonNull(keyFunction.apply(element), KEY_FUNCTION_CANT_RETURN_NULL_MESSAGE);
      V value = requireNonNull(valueFunction.apply(element), VALUE_FUNCTION_CANT_RETURN_NULL_MESSAGE);

      map.put(key, value);
    };
    BinaryOperator<ImmutableSetMultimap.Builder<K, V>> merger = (m1, m2) -> {
      for (Map.Entry<K, V> entry : m2.build().entries()) {
        m1.put(entry.getKey(), entry.getValue());
      }
      return m1;
    };
    return Collector.of(
      ImmutableSetMultimap::builder,
      accumulator,
      merger,
      ImmutableSetMultimap.Builder::build);
  }

  /**
   * A Collector similar to {@link #unorderedIndex(Function, Function)} except that it expects the {@code valueFunction}
   * to return a {@link Stream} which content will be flatten into the returned {@link ImmutableSetMultimap}.
   *
   * @see #unorderedIndex(Function, Function)
   */
  public static <K, E, V> Collector<E, ImmutableSetMultimap.Builder<K, V>, ImmutableSetMultimap<K, V>> unorderedFlattenIndex(
    Function<? super E, K> keyFunction, Function<? super E, Stream<V>> valueFunction) {
    verifyKeyAndValueFunctions(keyFunction, valueFunction);

    BiConsumer<ImmutableSetMultimap.Builder<K, V>, E> accumulator = (map, element) -> {
      K key = requireNonNull(keyFunction.apply(element), KEY_FUNCTION_CANT_RETURN_NULL_MESSAGE);
      Stream<V> valueStream = requireNonNull(valueFunction.apply(element), VALUE_FUNCTION_CANT_RETURN_NULL_MESSAGE);

      valueStream.forEach(value -> map.put(key, value));
    };
    BinaryOperator<ImmutableSetMultimap.Builder<K, V>> merger = (m1, m2) -> {
      for (Map.Entry<K, V> entry : m2.build().entries()) {
        m1.put(entry.getKey(), entry.getValue());
      }
      return m1;
    };
    return Collector.of(
      ImmutableSetMultimap::builder,
      accumulator,
      merger,
      ImmutableSetMultimap.Builder::build);
  }

  private static void verifyKeyAndValueFunctions(Function<?, ?> keyFunction, Function<?, ?> valueFunction) {
    requireNonNull(keyFunction, "Key function can't be null");
    requireNonNull(valueFunction, "Value function can't be null");
  }

}
