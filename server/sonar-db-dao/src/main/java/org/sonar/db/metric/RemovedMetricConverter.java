/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.metric;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;

/*
* This class has been introduced to contain the usage of deprecated and renamed metric 'WONT_FIX' in one place.
* It should be removed in SQ 11.0.
*/
public class RemovedMetricConverter {
  public static final String REMOVED_METRIC = CoreMetrics.WONT_FIX_ISSUES_KEY;
  public static final String REMOVED_METRIC_SHORT_NAME = CoreMetrics.WONT_FIX_ISSUES.getName();
  public static final String REMOVED_METRIC_DESCRIPTION = CoreMetrics.WONT_FIX_ISSUES.getDescription();
  public static final String DEPRECATED_METRIC_REPLACEMENT = CoreMetrics.ACCEPTED_ISSUES_KEY;

  private RemovedMetricConverter() {
    // static methods only
  }

  public static List<String> withRemovedMetricAlias(Collection<String> metrics) {
    if (metrics.contains(REMOVED_METRIC)) {
      Set<String> newMetrics = new HashSet<>(metrics);
      newMetrics.remove(REMOVED_METRIC);
      newMetrics.add(DEPRECATED_METRIC_REPLACEMENT);
      return newMetrics.stream().toList();
    } else {
      return new ArrayList<>(metrics);
    }
  }

  @CheckForNull
  public static String includeRenamedMetrics(@Nullable String metric) {
    if (REMOVED_METRIC.equals(metric)) {
      return DEPRECATED_METRIC_REPLACEMENT;
    } else {
      return metric;
    }
  }
}
