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
package org.sonar.server.qualitygate;

import java.util.Objects;
import java.util.Set;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.stream.MoreCollectors.toSet;

@Immutable
public class QualityGate {
  private final String id;
  private final String name;
  private final Set<Condition> conditions;

  public QualityGate(String id, String name, Set<Condition> conditions) {
    this.id = requireNonNull(id, "id can't be null");
    this.name = requireNonNull(name, "name can't be null");
    this.conditions = requireNonNull(conditions, "conditions can't be null")
      .stream()
      .map(c -> requireNonNull(c, "condition can't be null"))
      .collect(toSet(conditions.size()));
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Set<Condition> getConditions() {
    return conditions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualityGate that = (QualityGate) o;
    return Objects.equals(id, that.id) &&
      Objects.equals(name, that.name) &&
      Objects.equals(conditions, that.conditions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, conditions);
  }

  @Override
  public String toString() {
    return "QualityGate{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", conditions=" + conditions +
      '}';
  }
}
