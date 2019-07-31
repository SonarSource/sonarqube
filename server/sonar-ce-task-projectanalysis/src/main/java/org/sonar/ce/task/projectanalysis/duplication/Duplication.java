/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.duplication;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@Immutable
public final class Duplication {
  private static final Comparator<Duplicate> DUPLICATE_COMPARATOR = DuplicateComparatorByType.INSTANCE
    .thenComparing(DuplicateToFileKey.INSTANCE).thenComparing(DuplicateToTextBlock.INSTANCE);

  private final TextBlock original;
  private final Duplicate[] duplicates;

  /**
   * @throws NullPointerException     if {@code original} is {@code null} or {@code duplicates} is {@code null} or {@code duplicates} contains {@code null}
   * @throws IllegalArgumentException if {@code duplicates} is empty
   * @throws IllegalArgumentException if {@code duplicates} contains a {@link InnerDuplicate} with {@code original}
   */
  public Duplication(TextBlock original, List<Duplicate> duplicates) {
    this.original = requireNonNull(original, "original TextBlock can not be null");
    validateDuplicates(original, duplicates);
    this.duplicates = duplicates.stream().sorted(DUPLICATE_COMPARATOR).distinct().toArray(Duplicate[]::new);
  }

  private static void validateDuplicates(TextBlock original, List<Duplicate> duplicates) {
    requireNonNull(duplicates, "duplicates can not be null");
    checkArgument(!duplicates.isEmpty(), "duplicates can not be empty");

    for (Duplicate dup : duplicates) {
      requireNonNull(dup, "duplicates can not contain null");
      if (dup instanceof InnerDuplicate) {
        checkArgument(!original.equals(dup.getTextBlock()), "TextBlock of an InnerDuplicate can not be the original TextBlock");
      }
    }
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
   * <li>file key (unless it's an InnerDuplicate)</li>
   * <li>then by TextBlocks by start line and in case of same line, by shortest first</li>
   * </ul
   * <p>The returned set can not be empty and no inner duplicate can contain the original {@link TextBlock}.</p>
   */
  public Duplicate[] getDuplicates() {
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
    return original.equals(that.original) && Arrays.equals(duplicates, that.duplicates);
  }

  @Override
  public int hashCode() {
    return Arrays.deepHashCode(new Object[] {original, duplicates});
  }

  @Override
  public String toString() {
    return "Duplication{" +
      "original=" + original +
      ", duplicates=" + Arrays.toString(duplicates) +
      '}';
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

  private enum DuplicateToFileKey implements Function<Duplicate, String> {
    INSTANCE;

    @Override
    @Nonnull
    public String apply(@Nonnull Duplicate duplicate) {
      if (duplicate instanceof InnerDuplicate) {
        return "";
      }
      if (duplicate instanceof InProjectDuplicate) {
        return ((InProjectDuplicate) duplicate).getFile().getDbKey();
      }
      if (duplicate instanceof CrossProjectDuplicate) {
        return ((CrossProjectDuplicate) duplicate).getFileKey();
      }
      throw new IllegalArgumentException("Unsupported type of Duplicate " + duplicate.getClass().getName());
    }
  }
}
