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
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class CrossProjectDuplicate extends AbstractDuplicate {
  private final String fileKey;

  public CrossProjectDuplicate(String fileKey, TextBlock textBlock) {
    super(textBlock);
    this.fileKey = requireNonNull(fileKey, "fileKey can not be null");
  }

  public String getFileKey() {
    return fileKey;
  }

  @Override
  public String toString() {
    return "CrossProjectDuplicate{" +
        "fileKey='" + fileKey + '\'' +
        ", textBlock=" + getTextBlock() +
        '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass() || !super.equals(o)) {
      return false;
    }
    CrossProjectDuplicate that = (CrossProjectDuplicate) o;
    return fileKey.equals(that.fileKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), fileKey);
  }
}
