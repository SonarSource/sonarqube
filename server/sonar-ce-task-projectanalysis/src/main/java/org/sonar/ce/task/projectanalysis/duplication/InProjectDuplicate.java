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
import javax.annotation.concurrent.Immutable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.Component;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@Immutable
public class InProjectDuplicate extends AbstractDuplicate {
  protected final Component file;

  public InProjectDuplicate(Component file, TextBlock textBlock) {
    super(textBlock);
    requireNonNull(file, "file can not be null");
    checkArgument(file.getType() == Component.Type.FILE, "file must be of type FILE");
    this.file = file;
  }

  public Component getFile() {
    return file;
  }

  @Override
  public String toString() {
    return "InProjectDuplicate{" +
      "file=" + file +
      ", textBlock=" + getTextBlock() +
      '}';
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
    InProjectDuplicate that = (InProjectDuplicate) o;
    return file.equals(that.file);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), file);
  }
}
