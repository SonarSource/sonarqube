/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

public final class MoreCollectors {

  private static final String KEY_FUNCTION_CANT_RETURN_NULL_MESSAGE = "Key function can't return null";
  private static final String VALUE_FUNCTION_CANT_RETURN_NULL_MESSAGE = "Value function can't return null";

  private MoreCollectors() {
    // prevents instantiation
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
