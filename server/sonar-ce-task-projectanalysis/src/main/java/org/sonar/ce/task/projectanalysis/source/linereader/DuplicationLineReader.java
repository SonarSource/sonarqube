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
package org.sonar.ce.task.projectanalysis.source.linereader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.duplication.Duplicate;
import org.sonar.ce.task.projectanalysis.duplication.Duplication;
import org.sonar.ce.task.projectanalysis.duplication.InnerDuplicate;
import org.sonar.ce.task.projectanalysis.duplication.TextBlock;
import org.sonar.db.protobuf.DbFileSources;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Iterables.size;

public class DuplicationLineReader implements LineReader {

  private final Map<TextBlock, Integer> duplicatedTextBlockIndexByTextBlock;

  public DuplicationLineReader(Iterable<Duplication> duplications) {
    this.duplicatedTextBlockIndexByTextBlock = createIndexOfDuplicatedTextBlocks(duplications);
  }

  @Override
  public Optional<ReadError> read(DbFileSources.Line.Builder lineBuilder) {
    Predicate<Map.Entry<TextBlock, Integer>> containsLine = new TextBlockContainsLine(lineBuilder.getLine());
    // list is sorted to cope with the non-guaranteed order of Map entries which would trigger false detection of changes
    // in {@link DbFileSources.Line#getDuplicationList()}
    duplicatedTextBlockIndexByTextBlock.entrySet().stream()
      .filter(containsLine)
      .map(Map.Entry::getValue)
      .sorted(Comparator.naturalOrder())
      .forEach(lineBuilder::addDuplication);

    return Optional.empty();
  }

  /**
   * <p>
   * This method uses the natural order of TextBlocks to ensure that given the same set of TextBlocks, they get the same
   * index. It avoids false detections of changes in {@link DbFileSources.Line#getDuplicationList()}.
   * </p>
   */
  private static Map<TextBlock, Integer> createIndexOfDuplicatedTextBlocks(Iterable<Duplication> duplications) {
    return extractAllDuplicatedTextBlocks(duplications)
      .stream().sorted()
      .collect(Collectors.toMap(e -> e, new TextBlockIndexGenerator(), (a, b) -> a, LinkedHashMap::new));
  }

  /**
   * Duplicated blocks in the current file are either {@link Duplication#getOriginal()} or {@link Duplication#getDuplicates()}
   * when the {@link Duplicate} is a {@link InnerDuplicate}.
   * <p>
   * The returned list is mutable on purpose because it will be sorted.
   * </p>
   *
   * @see {@link #createIndexOfDuplicatedTextBlocks(Iterable)}
   */
  private static List<TextBlock> extractAllDuplicatedTextBlocks(Iterable<Duplication> duplications) {
    List<TextBlock> duplicatedBlock = new ArrayList<>(size(duplications));
    for (Duplication duplication : duplications) {
      duplicatedBlock.add(duplication.getOriginal());
      for (InnerDuplicate duplicate : from(duplication.getDuplicates()).filter(InnerDuplicate.class)) {
        duplicatedBlock.add(duplicate.getTextBlock());
      }
    }
    return duplicatedBlock;
  }

  private static class TextBlockContainsLine implements Predicate<Map.Entry<TextBlock, Integer>> {
    private final int line;

    public TextBlockContainsLine(int line) {
      this.line = line;
    }

    @Override
    public boolean test(@Nonnull Map.Entry<TextBlock, Integer> input) {
      return isLineInBlock(input.getKey(), line);
    }

    private static boolean isLineInBlock(TextBlock range, int line) {
      return line >= range.getStart() && line <= range.getEnd();
    }
  }

  private static class TextBlockIndexGenerator implements Function<TextBlock, Integer> {
    int i = 1;

    @Nullable
    @Override
    public Integer apply(TextBlock input) {
      return i++;
    }
  }
}
