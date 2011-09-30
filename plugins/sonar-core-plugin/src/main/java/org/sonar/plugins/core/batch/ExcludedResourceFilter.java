/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.core.batch;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ResourceFilter;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

/**
 * @since 1.12
 */
public class ExcludedResourceFilter implements ResourceFilter {

  private Configuration conf;

  public ExcludedResourceFilter(Configuration conf) {
    this.conf = conf;
  }

  public boolean isIgnored(Resource resource) {
    if (ResourceUtils.isUnitTestClass(resource)) {
      // See SONAR-1115 Exclusion patterns do not apply to unit tests.
      return false;
    }

    String[] patterns = getExclusionPatterns();
    if (patterns != null) {
      for (String pattern : patterns) {
        if (resource.matchFilePattern(pattern)) {
          return true;
        }
      }
    }
    return false;
  }

  String[] getExclusionPatterns() {
    return conf.getStringArray(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY);
  }
}