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

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

abstract class AbstractDuplicate implements Duplicate {
  private final TextBlock textBlock;

  protected AbstractDuplicate(TextBlock textBlock) {
    this.textBlock = requireNonNull(textBlock, "textBlock of duplicate can not be null");
  }

  @Override
  public TextBlock getTextBlock() {
    return textBlock;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractDuplicate that = (AbstractDuplicate) o;
    return textBlock.equals(that.textBlock);
  }

  @Override
  public int hashCode() {
    return textBlock.hashCode();
  }

}
