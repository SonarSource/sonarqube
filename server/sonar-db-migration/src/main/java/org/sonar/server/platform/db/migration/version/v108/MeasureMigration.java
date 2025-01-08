/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v108;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.core.metric.SoftwareQualitiesMetrics;

public class MeasureMigration {

  static final Pattern VALUE_EXTRACTION_PATTERN = Pattern.compile("\"total\":(\\d+)");

  static final Map<String, String> MIGRATION_MAP = Map.of(
    CoreMetrics.MAINTAINABILITY_ISSUES_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY,
    CoreMetrics.NEW_MAINTAINABILITY_ISSUES_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY,
    CoreMetrics.RELIABILITY_ISSUES_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY,
    CoreMetrics.NEW_RELIABILITY_ISSUES_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY,
    CoreMetrics.SECURITY_ISSUES_KEY, SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES_KEY,
    CoreMetrics.NEW_SECURITY_ISSUES_KEY, SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY);

  private MeasureMigration() {
    //Only static methods
  }

  @CheckForNull
  public static String getMigrationMetricKey(String metricKey) {
    return MIGRATION_MAP.get(metricKey);
  }

  @CheckForNull
  public static Long migrate(Object value) {
    Matcher matcher = VALUE_EXTRACTION_PATTERN.matcher(value.toString());
    if (matcher.find()) {
      return Long.valueOf(matcher.group(1));
    }
    return null;
  }

  public static boolean isMetricPlannedForDeletion(String metricKey) {
    return DeleteSoftwareQualityRatingFromProjectMeasures.SOFTWARE_QUALITY_METRICS_TO_DELETE.contains(metricKey);
  }
}
