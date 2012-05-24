/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.filters;

import org.sonar.api.web.Criterion;

import org.sonar.api.web.Filter;
import org.sonar.api.web.FilterColumn;
import org.sonar.api.web.FilterTemplate;

/**
 * Default treemap filter.
 *
 * @since 3.1
 */
public class TreeMapFilter extends FilterTemplate {
  @Override
  public String getName() {
    return "Treemap";
  }

  @Override
  public Filter createFilter() {
    Filter filter = Filter.create();
    filter.setDisplayAs(Filter.TREEMAP);
    filter.add(Criterion.createForQualifier("TRK"));
    filter.add(FilterColumn.create("name", null, FilterColumn.ASC, false));
    filter.add(FilterColumn.create("metric", "ncloc", FilterColumn.DESC, false));
    filter.add(FilterColumn.create("metric", "violations_density", FilterColumn.DESC, false));
    
    return filter;
  }
}
