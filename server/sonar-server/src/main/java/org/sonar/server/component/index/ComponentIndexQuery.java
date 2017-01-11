/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.component.index;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ComponentIndexQuery {

  private final String query;
  private Set<String> qualifiers = new HashSet<>();
  private Optional<Integer> limit = Optional.empty();

  public ComponentIndexQuery(String query) {
    this.query = query;
  }

  public ComponentIndexQuery addQualifier(String qualifier) {
    this.qualifiers.add(qualifier);
    return this;
  }

  public ComponentIndexQuery addQualifiers(Collection<String> qualifiers) {
    this.qualifiers.addAll(qualifiers);
    return this;
  }

  public boolean hasQualifiers() {
    return !qualifiers.isEmpty();
  }

  public Collection<String> getQualifiers() {
    return Collections.unmodifiableSet(qualifiers);
  }

  public String getQuery() {
    return query;
  }

  public ComponentIndexQuery setLimit(int limit) {
    this.limit = Optional.of(limit);
    return this;
  }

  public Optional<Integer> getLimit() {
    return limit;
  }
}
