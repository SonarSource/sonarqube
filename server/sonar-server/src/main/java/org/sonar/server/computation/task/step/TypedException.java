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
package org.sonar.server.computation.task.step;

import static java.util.Objects.requireNonNull;

public class TypedException extends RuntimeException {

  private final String type;

  public TypedException(String type, String message) {
    super(message);
    this.type = requireNonNull(type, "Type must not be null");
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[");
    sb.append("type='").append(type).append("',");
    sb.append("message='").append(getMessage()).append("'");
    sb.append(']');
    return sb.toString();
  }
}
