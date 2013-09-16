/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.measure;

import com.google.common.collect.ImmutableList;
import org.sonar.api.BatchExtension;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @since 4.0
 *
 */
public class MeasurementFilters implements BatchExtension {

  private List<MeasurementFilter> filters;

  public MeasurementFilters() {
    this.filters = ImmutableList.of();
  }

  public MeasurementFilters(MeasurementFilter[] filters) {
    this.filters = ImmutableList.copyOf(filters);
  }

  public boolean accept(Resource resource, Measure measure) {
    boolean accept = true;
    Iterator<MeasurementFilter> iteratorFilter = filters.iterator();
    while(accept && iteratorFilter.hasNext()) {
      accept &= iteratorFilter.next().accept(resource, measure);
    }
    return accept;
  }
}
