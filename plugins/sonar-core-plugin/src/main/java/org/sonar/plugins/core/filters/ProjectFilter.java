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

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.Criterion;
import org.sonar.api.web.Filter;
import org.sonar.api.web.FilterColumn;
import org.sonar.api.web.FilterTemplate;

/**
 * Default projects filter.
 *
 * @since 3.1
 */
public class ProjectFilter extends FilterTemplate {
  @Override
  public String getName() {
    return "Projects";
  }

  @Override
  public Filter createFilter() {
    Filter filter = Filter.create();
    filter.setDisplayAs(Filter.LIST);
    filter.add(Criterion.createForQualifier(Qualifiers.PROJECT));
    filter.add(FilterColumn.create("metric", CoreMetrics.ALERT_STATUS_KEY, FilterColumn.DESC, false));
    filter.add(FilterColumn.create("name", null, FilterColumn.ASC, false));
    filter.add(FilterColumn.create("version", null, FilterColumn.DESC, false));
    filter.add(FilterColumn.create("metric", CoreMetrics.NCLOC_KEY, FilterColumn.DESC, false));
    filter.add(FilterColumn.create("metric", CoreMetrics.VIOLATIONS_DENSITY_KEY, FilterColumn.DESC, false));
    filter.add(FilterColumn.create("date", null, FilterColumn.DESC, false));
    filter.add(FilterColumn.create("links", null, FilterColumn.DESC, false));
    
    return filter;
  }
}
