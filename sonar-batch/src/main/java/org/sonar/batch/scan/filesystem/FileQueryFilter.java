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
package org.sonar.batch.scan.filesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.InputFileFilter;
import org.sonar.batch.bootstrap.AnalysisMode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

class FileQueryFilter {

  private final List<InputFileFilter> filters;

  FileQueryFilter(AnalysisMode analysisMode, FileQuery query) {
    filters = Lists.newArrayList();
    for (String pattern : query.inclusions()) {
      filters.add(new InclusionFilter(pattern));
    }
    for (String pattern : query.exclusions()) {
      filters.add(new ExclusionFilter(pattern));
    }
    for (Map.Entry<String, Collection<String>> entry : query.attributes().entrySet()) {
      filters.add(new AttributeFilter(entry.getKey(), entry.getValue()));
    }

    // TODO speed-up the following algorithm. Cache ?
    if (analysisMode.isIncremental()) {
      Collection<String> status = query.attributes().get(InputFile.ATTRIBUTE_STATUS);
      if (status == null || status.isEmpty()) {
        // TODO should be not(SAME) instead of is(ADDED, CHANGED)
        filters.add(new AttributeFilter(InputFile.ATTRIBUTE_STATUS, Lists.newArrayList(InputFile.STATUS_ADDED, InputFile.STATUS_CHANGED)));
      }
    }
  }

  @VisibleForTesting
  List<InputFileFilter> filters() {
    return filters;
  }

  boolean accept(InputFile inputFile) {
    for (InputFileFilter filter : filters) {
      if (!filter.accept(inputFile)) {
        return false;
      }
    }
    return true;
  }
}
