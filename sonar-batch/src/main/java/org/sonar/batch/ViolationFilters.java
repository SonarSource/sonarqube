/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
import org.sonar.api.rules.Violation;
import org.sonar.api.rules.ViolationFilter;

public class ViolationFilters {

  private static final Logger LOG = LoggerFactory.getLogger(ViolationFilters.class);
  
  private ViolationFilter[] filters;

  public ViolationFilters(ViolationFilter[] filters) {
    this.filters = filters;
  }

  public ViolationFilters() {
    this(new ViolationFilter[0]);
  }

  public boolean isEmpty() {
    return filters.length==0;
  }

  /**
   * Return true if the violation must be saved. If false then it is ignored.
   */
  public boolean isIgnored(Violation violation) {
    boolean ignored = false;
    int index = 0;
    while (!ignored && index < filters.length) {
      ignored = filters[index].isIgnored(violation);
      if (ignored && LOG.isDebugEnabled()) {
        LOG.debug("Violation {} is excluded by the filter {}", violation, filters[index]);
      }
      index++;
    }
    return ignored;
  }
}
