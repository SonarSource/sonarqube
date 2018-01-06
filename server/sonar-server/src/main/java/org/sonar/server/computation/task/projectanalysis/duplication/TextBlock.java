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

import java.util.Objects;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A block of text in some file represented by its first line index (1-based) and its last line index (included).
 * <p>
 * This class defines a natural ordering which sorts {@link TextBlock} by lowest start line first and then, in case of
 * same start line, by smallest size (ie. lowest end line).
 * </p>
 */
public class TextBlock implements Comparable<TextBlock> {
  private final int start;
  private final int end;

  /**
   * @throws IllegalArgumentException if {@code start} is 0 or less
   * @throws IllegalStateException if {@code end} is less than {@code start}
   */
  public TextBlock(int start, int end) {
    checkArgument(start > 0, "First line index must be >= 1");
    checkArgument(end >= start, "Last line index must be >= first line index");
    this.start = start;
    this.end = end;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }

  @Override
  public int compareTo(TextBlock other) {
    int res = start - other.start;
    if (res == 0) {
      return end - other.end;
    }
    return res;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TextBlock textBlock = (TextBlock) o;
    return start == textBlock.start && end == textBlock.end;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return "TextBlock{" +
      "start=" + start +
      ", end=" + end +
      '}';
  }
}
