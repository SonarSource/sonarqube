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
package org.sonar.ce.task.projectanalysis.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class SubViewAttributes {
  private final String originalViewUuid;

  public SubViewAttributes(@Nullable String originalViewUuid) {
    this.originalViewUuid = originalViewUuid;
  }

  /**
   * Return the original view uuid when the sub view is a local view
   */
  @CheckForNull
  public String getOriginalViewUuid() {
    return originalViewUuid;
  }

  @Override
  public String toString() {
    return "SubViewAttributes{" +
      "originalViewUuid='" + originalViewUuid + '\'' +
      '}';
  }
}
