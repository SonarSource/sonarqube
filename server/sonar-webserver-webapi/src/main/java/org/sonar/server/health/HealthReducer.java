/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.health;


public class HealthReducer {

  private HealthReducer() {
    // no public constructor
  }

  public static Health merge(Health left, Health right) {
    Health.Builder builder = Health.builder()
      .setStatus(worseOf(left.getStatus(), right.getStatus()));
    left.getCauses().forEach(builder::addCause);
    right.getCauses().forEach(builder::addCause);
    return builder.build();
  }

  private static Health.Status worseOf(Health.Status left, Health.Status right) {
    if (left == right) {
      return left;
    }
    if (left.ordinal() > right.ordinal()) {
      return left;
    }
    return right;
  }
}
