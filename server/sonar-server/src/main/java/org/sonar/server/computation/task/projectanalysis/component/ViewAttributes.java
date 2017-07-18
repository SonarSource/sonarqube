/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.component;

import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class ViewAttributes {
  private final String qualifier;

  public ViewAttributes(String qualifier) {
    this.qualifier = requireNonNull(qualifier, "Qualifier cannot be null");
  }

  /**
   * Return the qualifier of the view : either {@link org.sonar.api.resources.Qualifiers.VIEW} or {@link org.sonar.api.resources.Qualifiers.APP}
   */
  public String getQualifier() {
    return qualifier;
  }

  @Override
  public String toString() {
    return "viewAttributes{" +
      "qualifier='" + qualifier + '\'' +
      '}';
  }
}
