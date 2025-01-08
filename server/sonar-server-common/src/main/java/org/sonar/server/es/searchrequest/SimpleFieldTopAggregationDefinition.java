/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.es.searchrequest;

import javax.annotation.concurrent.Immutable;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.FilterScope;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link TopAggregationDefinition} with a filter scope for a simple field.
 */
@Immutable
public final class SimpleFieldTopAggregationDefinition implements TopAggregationDefinition<FilterScope> {
  private final FilterScope filterScope;
  private final boolean sticky;

  public SimpleFieldTopAggregationDefinition(String fieldName, boolean sticky) {
    requireNonNull(fieldName, "fieldName can't be null");
    this.filterScope = new SimpleFieldFilterScope(fieldName);
    this.sticky = sticky;
  }

  @Override
  public FilterScope getFilterScope() {
    return filterScope;
  }

  @Override
  public boolean isSticky() {
    return sticky;
  }

}
