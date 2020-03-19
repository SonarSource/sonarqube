/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import java.util.OptionalInt;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A top-aggregation which defines (at least) one sub-aggregation on the terms of the fields of the top-aggregation.
 */
@Immutable
public class TermTopAggregationDef implements TopAggregationDefinition {
  private final TopAggregationDef delegate;
  private final Integer maxTerms;

  public TermTopAggregationDef(String fieldName, boolean sticky, @Nullable Integer maxTerms) {
    this.delegate = new TopAggregationDef(fieldName, sticky);
    checkArgument(maxTerms == null || maxTerms >= 0, "maxTerms can't be < 0");
    this.maxTerms = maxTerms;
  }

  @Override
  public String getFieldName() {
    return delegate.getFieldName();
  }

  @Override
  public boolean isSticky() {
    return delegate.isSticky();
  }

  public OptionalInt getMaxTerms() {
    return maxTerms == null ? OptionalInt.empty() : OptionalInt.of(maxTerms);
  }
}
