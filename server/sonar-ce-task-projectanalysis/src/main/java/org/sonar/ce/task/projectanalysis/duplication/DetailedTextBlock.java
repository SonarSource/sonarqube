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

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Given that:
 * <ul>
 *   <li>some language plugins are able to multiple duplication for the same textblock but with a
 * different starting and/or ending offset</li>
 *   <li>there is no way to distinguish these block from each other as the offsets (or any other
 *   information) is not sent in the analysis report</li>
 * </ul>,
 * we are uniquely (and blindly) identifying each original block reported in the analysis report.
 */
public class DetailedTextBlock extends TextBlock {
  private final int id;
  /**
   * @throws IllegalArgumentException if {@code start} is 0 or less
   * @throws IllegalStateException    if {@code end} is less than {@code start}
   */
  public DetailedTextBlock(int id, int start, int end) {
    super(start, end);
    this.id = id;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    DetailedTextBlock that = (DetailedTextBlock) o;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), id);
  }
}
