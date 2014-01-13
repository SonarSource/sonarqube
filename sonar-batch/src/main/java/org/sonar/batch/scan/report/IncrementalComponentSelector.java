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
package org.sonar.batch.scan.report;

import com.google.common.collect.Maps;
import org.sonar.api.issue.Issue;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.api.scan.filesystem.internal.InputFile;
import org.sonar.batch.scan.filesystem.InputFileCache;

import java.util.Map;
import java.util.Set;

class IncrementalComponentSelector extends ComponentSelector {

  private final InputFileCache cache;
  private final Map<String, String> componentKeys = Maps.newHashMap();

  IncrementalComponentSelector(InputFileCache cache) {
    this.cache = cache;
  }

  @Override
  void init() {
    for (InputFile inputFile : cache.all()) {
      String status = inputFile.attribute(InputFile.ATTRIBUTE_STATUS);
      if (status != null && !InputFile.STATUS_SAME.equals(status)) {
        String componentKey = inputFile.attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_KEY);
        String componentDeprecatedKey = inputFile.attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_DEPRECATED_KEY);
        if (componentKey != null) {
          componentKeys.put(componentKey, componentDeprecatedKey);
        }
      }
    }
  }

  @Override
  boolean register(Issue issue) {
    return componentKeys.keySet().contains(issue.componentKey());
  }

  @Override
  Set<String> componentKeys() {
    return componentKeys.keySet();
  }

  @Override
  String getDeprecatedKey(String componentKey) {
    return componentKeys.get(componentKey);
  }
}
