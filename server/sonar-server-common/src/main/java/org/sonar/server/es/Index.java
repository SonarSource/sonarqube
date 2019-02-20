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
package org.sonar.server.es;

import java.util.Objects;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public final class Index {
  public static final Index ALL_INDICES = Index.simple("_all");

  private final String name;
  private final boolean relations;

  private Index(String name, boolean acceptsRelations) {
    checkArgument(name != null && !name.isEmpty(), "Index name can't be null nor empty");
    checkArgument("_all".equals(name) || StringUtils.isAllLowerCase(name), "Index name must be lower-case letters or '_all': %s", name);
    this.name = name;
    this.relations = acceptsRelations;
  }

  public static Index simple(String name) {
    return new Index(name, false);
  }

  public static Index withRelations(String name) {
    return new Index(name, true);
  }

  public String getName() {
    return name;
  }

  public boolean acceptsRelations() {
    return relations;
  }

  /**
   * @return the name of the join field for this index if it accepts relations
   * @throws IllegalStateException if index does not accept relations
   * @see #acceptsRelations()
   */
  public String getJoinField() {
    checkState(relations, "Only index accepting relations has a join field");
    return "join_" + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Index index = (Index) o;
    return relations == index.relations && name.equals(index.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, relations);
  }

  @Override
  public String toString() {
    return "[" + name + (relations ? "|*" : "|") + ']';
  }
}
