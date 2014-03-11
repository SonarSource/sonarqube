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
package org.sonar.api.checks;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.api.rules.ViolationFilter;

import java.util.Map;
import java.util.Set;

/**
 * @since 2.1
 * @deprecated in 3.6. Replaced by {@link org.sonar.api.issue.NoSonarFilter}
 */
@Deprecated
public class NoSonarFilter implements ViolationFilter {

  private final Map<Resource, Set<Integer>> noSonarLinesByResource = Maps.newHashMap();
  private SensorContext context;

  public NoSonarFilter(SensorContext context) {
    this.context = context;
  }

  public void addResource(Resource resource, Set<Integer> noSonarLines) {
    if (resource != null && noSonarLines != null) {
      // Reload resource to handle backward compatibility of resource keys
      resource = context.getResource(resource);
      if (resource != null) {
        noSonarLinesByResource.put(resource, noSonarLines);
      }
    }
  }

  public boolean isIgnored(Violation violation) {
    boolean ignored = false;
    if (violation.getResource() != null && violation.getLineId() != null) {
      Set<Integer> noSonarLines = noSonarLinesByResource.get(violation.getResource());
      ignored = noSonarLines != null && noSonarLines.contains(violation.getLineId());
      if (ignored && violation.getRule() != null && StringUtils.containsIgnoreCase(violation.getRule().getKey(), "nosonar")) {
        ignored = false;
      }
    }
    return ignored;
  }
}
