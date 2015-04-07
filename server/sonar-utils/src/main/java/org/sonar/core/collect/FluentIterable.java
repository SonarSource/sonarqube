/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.*;
import com.google.common.base.Optional;
import com.google.common.collect.*;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.*;

/**
 * FluentIterable - Fork from Guava's 18.0 FluentIterable class (introduced in 12.0) with slight cleaning modification,
 * removal of deprecated and beta methods, removal of those relying on other Guava class or methods which are not
 * available in Guava 10.1 and fix of missing generics arguments in some methods.
 *
 * @author Marcin Mikosik
 */
public abstract class FluentIterable<E> implements Iterable<E> {
  private final Iterable<E> iterable;

  protected FluentIterable() {
    this.iterable = this;
  }

  FluentIterable(Iterable<E> iterable) {
    this.iterable = Preconditions.checkNotNull(iterable);
  }

  @SuppressWarnings("unchecked")
  public static <E> FluentIterable<E> from(final Iterable<E> iterable) {
    return iterable instanceof FluentIterable ? (FluentIterable) iterable : new FluentIterable(iterable) {
      public Iterator<E> iterator() {
        return iterable.iterator();
      }
    };
  }

  public String toString() {
    return Iterables.toString(this.iterable);
  }

  public final int size() {
    return Iterables.size(this.iterable);
  }

  public final boolean contains(@Nullable Object element) {
    return Iterables.contains(this.iterable, element);
  }

  @CheckReturnValue
  public final FluentIterable<E> cycle() {
    return from(Iterables.cycle(this.iterable));
  }

  @CheckReturnValue
  @Beta
  public final FluentIterable<E> append(Iterable<? extends E> other) {
    return from(Iterables.concat(this.iterable, other));
  }

  @CheckReturnValue
  @Beta
  public final FluentIterable<E> append(E... elements) {
    return from(Iterables.concat(this.iterable, Arrays.asList(elements)));
  }

  @CheckReturnValue
  public final FluentIterable<E> filter(Predicate<? super E> predicate) {
    return from(Iterables.filter(this.iterable, predicate));
  }

  @CheckReturnValue
  @GwtIncompatible("Class.isInstance")
  public final <T> FluentIterable<T> filter(Class<T> type) {
    return from(Iterables.filter(this.iterable, type));
  }

  public final boolean anyMatch(Predicate<? super E> predicate) {
    return Iterables.any(this.iterable, predicate);
  }

  public final boolean allMatch(Predicate<? super E> predicate) {
    return Iterables.all(this.iterable, predicate);
  }

  public final Optional<E> firstMatch(Predicate<? super E> predicate) {
    Iterator<E> iterable = Iterables.filter(this.iterable, predicate).iterator();
    if (iterable.hasNext()) {
      return Optional.of(iterable.next());
    }
    return Optional.absent();
  }

  public final <T> FluentIterable<T> transform(Function<? super E, T> function) {
    return from(Iterables.transform(this.iterable, function));
  }

  public <T> FluentIterable<T> transformAndConcat(Function<? super E, ? extends Iterable<? extends T>> function) {
    return from(Iterables.concat(this.transform(function)));
  }

  public final Optional<E> first() {
    Iterator<E> iterator = this.iterable.iterator();
    return iterator.hasNext() ? Optional.<E>of(iterator.next()) : Optional.<E>absent();
  }

  public final Optional<E> last() {
    if (this.iterable instanceof List) {
      List<E> iterator1 = (List) this.iterable;
      return iterator1.isEmpty() ? Optional.<E>absent() : Optional.<E>of(iterator1.get(iterator1.size() - 1));
    } else {
      Iterator<E> iterator = this.iterable.iterator();
      if (!iterator.hasNext()) {
        return Optional.absent();
      } else if (this.iterable instanceof SortedSet) {
        SortedSet<E> current1 = (SortedSet) this.iterable;
        return Optional.<E>of(current1.last());
      } else {
        E current;
        do {
          current = iterator.next();
        } while (iterator.hasNext());

        return Optional.of(current);
      }
    }
  }

  @CheckReturnValue
  public final FluentIterable<E> skip(int numberToSkip) {
    return from(Iterables.skip(this.iterable, numberToSkip));
  }

  @CheckReturnValue
  public final FluentIterable<E> limit(int size) {
    return from(Iterables.limit(this.iterable, size));
  }

  public final boolean isEmpty() {
    return !this.iterable.iterator().hasNext();
  }

  public final ImmutableList<E> toList() {
    return ImmutableList.copyOf(this.iterable);
  }

  public final ImmutableList<E> toSortedList(Comparator<? super E> comparator) {
    return Ordering.from(comparator).immutableSortedCopy(this.iterable);
  }

  public final ImmutableSet<E> toSet() {
    return ImmutableSet.copyOf(this.iterable);
  }

  public final ImmutableSortedSet<E> toSortedSet(Comparator<? super E> comparator) {
    return ImmutableSortedSet.copyOf(comparator, this.iterable);
  }

  public final <K> ImmutableListMultimap<K, E> index(Function<? super E, K> keyFunction) {
    return Multimaps.index(this.iterable, keyFunction);
  }

  public final <K> ImmutableMap<K, E> uniqueIndex(Function<? super E, K> keyFunction) {
    return Maps.uniqueIndex(this.iterable, keyFunction);
  }

  @GwtIncompatible("Array.newArray(Class, int)")
  public final E[] toArray(Class<E> type) {
    return Iterables.toArray(this.iterable, type);
  }

  public final <C extends Collection<? super E>> C copyInto(C collection) {
    Preconditions.checkNotNull(collection);
    if (this.iterable instanceof Collection) {
      collection.addAll(castCollection(this.iterable));
    } else {
      Iterator<E> i$ = this.iterable.iterator();

      while (i$.hasNext()) {
        E item = i$.next();
        collection.add(item);
      }
    }

    return collection;
  }

  /**
   * Used to avoid http://bugs.sun.com/view_bug.do?bug_id=6558557
   */
  private static <T> Collection<T> castCollection(Iterable<T> iterable) {
    return (Collection<T>) iterable;
  }

  @Beta
  public final String join(Joiner joiner) {
    return joiner.join(this);
  }

  public final E get(int position) {
    return Iterables.get(this.iterable, position);
  }

  private static class FromIterableFunction<E> implements Function<Iterable<E>, FluentIterable<E>> {
    private FromIterableFunction() {
    }

    public FluentIterable<E> apply(Iterable<E> fromObject) {
      return FluentIterable.from(fromObject);
    }
  }
}
