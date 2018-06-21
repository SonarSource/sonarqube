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
package org.sonar.server.computation.task.projectanalysis.duplication;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Ordering;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Objects.requireNonNull;

@Immutable
public final class Duplication {
  private static final Ordering<Duplicate> DUPLICATE_ORDERING = Ordering.from(DuplicateComparatorByType.INSTANCE)
    .compound(Ordering.natural().onResultOf(DuplicateToFileKey.INSTANCE))
    .compound(Ordering.natural().onResultOf(DuplicateToTextBlock.INSTANCE));

  private final TextBlock original;
  private final SortedSet<Duplicate> duplicates;

  /**
   * @throws NullPointerException if {@code original} is {@code null} or {@code duplicates} is {@code null} or {@code duplicates} contains {@code null}
   * @throws IllegalArgumentException if {@code duplicates} is empty
   * @throws IllegalArgumentException if {@code duplicates} contains a {@link InnerDuplicate} with {@code original}
   */
  public Duplication(final TextBlock original, final Iterable<Duplicate> duplicates) {
    this.original = requireNonNull(original, "original TextBlock can not be null");
    this.duplicates = from(requireNonNull(duplicates, "duplicates can not be null"))
      .filter(FailOnNullDuplicate.INSTANCE)
      .filter(new EnsureInnerDuplicateIsNotOriginalTextBlock(original))
      .toSortedSet(DUPLICATE_ORDERING);
    checkArgument(!this.duplicates.isEmpty(), "duplicates can not be empty");
  }

  /**
   * The duplicated block.
   */
  public TextBlock getOriginal() {
    return this.original;
  }

  /**
   * The duplicates of the original, sorted by inner duplicates, then project duplicates, then cross-project duplicates.
   * For each category of duplicate, they are sorted by:
   * <ul>
   *   <li>file key (unless it's an InnerDuplicate)</li>
   *   <li>then by TextBlocks by start line and in case of same line, by shortest first</li>
   * </ul
   * <p>The returned set can not be empty and no inner duplicate can contain the original {@link TextBlock}.</p>
   */
  public SortedSet<Duplicate> getDuplicates() {
    return this.duplicates;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Duplication that = (Duplication) o;
    return original.equals(that.original) && duplicates.equals(that.duplicates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(original, duplicates);
  }

  @Override
  public String toString() {
    return "Duplication{" +
      "original=" + original +
      ", duplicates=" + duplicates +
      '}';
  }

  private enum FailOnNullDuplicate implements Predicate<Duplicate> {
    INSTANCE;

    @Override
    public boolean apply(@Nullable Duplicate input) {
      requireNonNull(input, "duplicates can not contain null");
      return true;
    }
  }

  private enum DuplicateComparatorByType implements Comparator<Duplicate> {
    INSTANCE;

    @Override
    public int compare(Duplicate o1, Duplicate o2) {
      return toIndexType(o1) - toIndexType(o2);
    }

    private static int toIndexType(Duplicate duplicate) {
      if (duplicate instanceof InnerDuplicate) {
        return 0;
      }
      if (duplicate instanceof InProjectDuplicate) {
        return 1;
      }
      if (duplicate instanceof CrossProjectDuplicate) {
        return 2;
      }
      throw new IllegalArgumentException("Unsupported type of Duplicate " + duplicate.getClass().getName());
    }
  }

  private enum DuplicateToTextBlock implements Function<Duplicate, TextBlock> {
    INSTANCE;

    @Override
    @Nonnull
    public TextBlock apply(@Nonnull Duplicate input) {
      return input.getTextBlock();
    }
  }

  private static class EnsureInnerDuplicateIsNotOriginalTextBlock implements Predicate<Duplicate> {
    private final TextBlock original;

    public EnsureInnerDuplicateIsNotOriginalTextBlock(TextBlock original) {
      this.original = original;
    }

    @Override
    public boolean apply(@Nullable Duplicate input) {
      if (input instanceof InnerDuplicate) {
        checkArgument(!original.equals(input.getTextBlock()), "TextBlock of an InnerDuplicate can not be the original TextBlock");
      }
      return true;
    }
  }

  private enum DuplicateToFileKey implements Function<Duplicate, String> {
    INSTANCE;

    @Override
    @Nonnull
    public String apply(@Nonnull Duplicate duplicate) {
      if (duplicate instanceof InnerDuplicate) {
        return "";
      }
      if (duplicate instanceof InProjectDuplicate) {
        return ((InProjectDuplicate) duplicate).getFile().getKey();
      }
      if (duplicate instanceof CrossProjectDuplicate) {
        return ((CrossProjectDuplicate) duplicate).getFileKey();
      }
      throw new IllegalArgumentException("Unsupported type of Duplicate " + duplicate.getClass().getName());
    }
  }
}
