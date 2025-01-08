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

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.NestedFieldFilterScope;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@Immutable
public class NestedFieldTopAggregationDefinition<T> implements TopAggregationDefinition<NestedFieldFilterScope<T>> {
  private final NestedFieldFilterScope<T> filterScope;
  private final boolean sticky;

  public NestedFieldTopAggregationDefinition(String nestedFieldPath, T value, boolean sticky) {
    requireNonNull(nestedFieldPath, "nestedFieldPath can't be null");
    requireNonNull(value, "value can't be null");
    checkArgument(StringUtils.countMatches(nestedFieldPath, ".") == 1,
      "Field path should have only one dot: %s", nestedFieldPath);
    String[] fullPath = Arrays.stream(StringUtils.split(nestedFieldPath, '.'))
      .filter(Objects::nonNull)
      .map(String::trim)
      .filter(t -> !t.isEmpty())
      .toArray(String[]::new);
    checkArgument(fullPath.length == 2,
      "field path \"%s\" should have exactly 2 non empty field names, got: %s", nestedFieldPath, Arrays.asList(fullPath));
    this.filterScope = new NestedFieldFilterScope<>(fullPath[0], fullPath[1], value);
    this.sticky = sticky;
  }

  @Override
  public NestedFieldFilterScope<T> getFilterScope() {
    return filterScope;
  }

  @Override
  public boolean isSticky() {
    return sticky;
  }
}
