/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public final class MoreCollectors {

  private static final int DEFAULT_HASHMAP_CAPACITY = 0;

  private MoreCollectors() {
    // prevents instantiation
  }

  /**
   * A Collector into an {@link ImmutableList}.
   */
  public static <T> Collector<T, List<T>, List<T>> toList() {
    return Collector.of(
      ArrayList::new,
      List::add,
      (left, right) -> {
        left.addAll(right);
        return left;
      },
      ImmutableList::copyOf);
  }

  /**
   * A Collector into an {@link ImmutableList} of the specified expected size.
   *
   * <p>Note: using this method with a parallel stream will likely not have the expected memory usage benefit as all
   * processing threads will use a List with a capacity large enough for the final size.</p>
   */
  public static <T> Collector<T, List<T>, List<T>> toList(int expectedSize) {
    // use ArrayList rather than ImmutableList.Builder because initial capacity of builder can not be specified
    return Collector.of(
      () -> new ArrayList<>(expectedSize),
      List::add,
      (left, right) -> {
        left.addAll(right);
        return left;
      },
      ImmutableList::copyOf);
  }

  /**
   * A Collector into an {@link ImmutableSet}.
   */
  public static <T> Collector<T, Set<T>, Set<T>> toSet() {
    return Collector.of(
      HashSet::new,
      Set::add,
      (left, right) -> {
        left.addAll(right);
        return left;
      },
      ImmutableSet::copyOf);
  }

  /**
   * A Collector into an {@link ImmutableSet} of the specified expected size.
   *
   * <p>Note: using this method with a parallel stream will likely not have the expected memory usage benefit as all
   * processing threads will use a Set with a capacity large enough for the final size.</p>
   */
  public static <T> Collector<T, Set<T>, Set<T>> toSet(int expectedSize) {
    // use HashSet rather than ImmutableSet.Builder because initial capacity of builder can not be specified
    return Collector.of(
      () -> new HashSet<>(expectedSize),
      Set::add,
      (left, right) -> {
        left.addAll(right);
        return left;
      },
      ImmutableSet::copyOf);
  }

  /**
   * A Collector into an {@link EnumSet} of specified enumeration.
   */
  public static <E extends Enum<E>> Collector<E, ?, EnumSet<E>> toEnumSet(Class<E> enumClass) {
    return Collectors.toCollection(() -> EnumSet.noneOf(enumClass));
  }

  /**
   * Delegates to {@link java.util.stream.Collectors#toCollection(Supplier)}.
   */
  public static <T> Collector<T, ?, ArrayList<T>> toArrayList() {
    return java.util.stream.Collectors.toCollection(ArrayList::new);
  }

  /**
   * Does {@code java.util.stream.MoreCollectors.toCollection(() -> new ArrayList<>(size));} which is equivalent to
   * {@link #toArrayList()} but avoiding array copies when the size of the resulting list is already known.
   *
   * <p>Note: using this method with a parallel stream will likely not have the expected memory usage benefit as all
   * processing threads will use a ArrayList with a capacity large enough for the final size.</p>
   *
   * @see java.util.stream.Collectors#toList()
   * @see java.util.stream.Collectors#toCollection(Supplier)
   */
  public static <T> Collector<T, ?, ArrayList<T>> toArrayList(int size) {
    return java.util.stream.Collectors.toCollection(() -> new ArrayList<>(size));
  }

  /**
   * Delegates to {@link java.util.stream.Collectors#toCollection(Supplier)}.
   */
  public static <T> Collector<T, ?, HashSet<T>> toHashSet() {
    return java.util.stream.Collectors.toCollection(HashSet::new);
  }

  /**
   * Does {@code java.util.stream.MoreCollectors.toCollection(() -> new HashSet<>(size));} which is equivalent to
   * {@link #toHashSet()} but avoiding array copies when the size of the resulting set is already known.
   *
   * <p>Note: using this method with a parallel stream will likely not have the expected memory usage benefit as all
   * processing threads will use a HashSet with a capacity large enough for the final size.</p>
   *
   * @see java.util.stream.Collectors#toSet()
   * @see java.util.stream.Collectors#toCollection(Supplier)
   */
  public static <T> Collector<T, ?, HashSet<T>> toHashSet(int size) {
    return java.util.stream.Collectors.toCollection(() -> new HashSet<>(size));
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
    return uniqueIndex(keyFunction, Function.<E>identity());
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
    return uniqueIndex(keyFunction, Function.<E>identity(), expectedSize);
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
    requireNonNull(keyFunction, "Key function can't be null");
    requireNonNull(valueFunction, "Value function can't be null");
    BiConsumer<Map<K, V>, E> accumulator = (map, element) -> {
      K key = requireNonNull(keyFunction.apply(element), "Key function can't return null");
      V value = requireNonNull(valueFunction.apply(element), "Value function can't return null");

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

  /**
   * For stream of one expected element, return the element
   *
   * @throws IllegalArgumentException if stream has no element or more than 1 element
   */
  public static <T> Collector<T, ?, T> toOneElement() {
    return java.util.stream.Collectors.collectingAndThen(
      java.util.stream.Collectors.toList(),
      list -> {
        if (list.size() != 1) {
          throw new IllegalStateException("Stream should have only one element");
        }
        return list.get(0);
      });
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
    return index(keyFunction, Function.<E>identity());
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
    requireNonNull(keyFunction, "Key function can't be null");
    requireNonNull(valueFunction, "Value function can't be null");
    BiConsumer<ImmutableListMultimap.Builder<K, V>, E> accumulator = (map, element) -> {
      K key = requireNonNull(keyFunction.apply(element), "Key function can't return null");
      V value = requireNonNull(valueFunction.apply(element), "Value function can't return null");

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
   * Applies the specified {@link Joiner} to the current stream.
   *
   * @throws NullPointerException of {@code joiner} is {@code null}
   * @throws IllegalStateException if a merge operation happens because parallel processing has been enabled on the current stream
   */
  public static <E> Collector<E, List<E>, String> join(Joiner joiner) {
    requireNonNull(joiner, "Joiner can't be null");

    return Collector.of(
      ArrayList::new,
      List::add,
      mergeNotSupportedMerger(),
      joiner::join);
  }

  public static <R> BinaryOperator<R> mergeNotSupportedMerger() {
    return (m1, m2) -> {
      throw new IllegalStateException("Parallel processing is not supported");
    };
  }
}
