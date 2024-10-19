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
package org.sonar.server.webhook;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public final class Branch {
  private final boolean main;
  private final String name;
  private final Type type;

  public Branch(boolean main, @Nullable String name, Type type) {
    this.main = main;
    this.name = name;
    this.type = requireNonNull(type, "type can't be null");
  }

  public boolean isMain() {
    return main;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public Type getType() {
    return type;
  }

  public enum Type {
    PULL_REQUEST, BRANCH
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Branch branch = (Branch) o;
    return main == branch.main &&
      Objects.equals(name, branch.name) &&
      type == branch.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(main, name, type);
  }

  @Override
  public String toString() {
    return "Branch{" +
      "main=" + main +
      ", name='" + name + '\'' +
      ", type=" + type +
      '}';
  }
}
