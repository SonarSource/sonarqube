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
package org.sonar.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.ResourceFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.Scopes;

/**
 * @since 1.12
 */
public class ResourceFilters {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceFilters.class);

  private ResourceFilter[] filters;

  public ResourceFilters(ResourceFilter[] filters) {
    this.filters = filters == null ? new ResourceFilter[0] : filters;
  }

  public ResourceFilters() {
    this(null);
  }

  public ResourceFilter[] getFilters() {
    return filters;
  }

  /**
   * Return true if the violation must be saved. If false then it is ignored.
   */
  public boolean isExcluded(Resource resource) {
    boolean ignored = false;
    if (Scopes.isHigherThanOrEquals(resource, Scopes.FILE)) {
      int index = 0;
      while (!ignored && index < filters.length) {
        ResourceFilter filter = filters[index];
        ignored = filter.isIgnored(resource);
        if (ignored && LOG.isDebugEnabled()) {
          LOG.debug("Resource {} is excluded by the filter {}", resource, filter);
        }
        index++;
      }
    }
    return ignored;
  }

}
