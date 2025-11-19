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
package org.sonar.server.common;

import io.sonarcloud.compliancereports.reports.ReportKey;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

public class ParamParsingUtils {
  private ParamParsingUtils() {
    // utility class
  }

  public static Pair<SoftwareQuality, Severity> parseImpact(String impact) {
    String[] parts = impact.split("=");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid impact format: " + impact);
    }
    return Pair.of(SoftwareQuality.valueOf(parts[0]),
      Severity.valueOf(parts[1]));
  }

  public static Map<ReportKey, String> parseComplianceStandardsFilter(@Nullable Collection<String> paramAsStrings) {
    if (paramAsStrings == null) {
      return Map.of();
    }

    Map<ReportKey, String> categoriesByStandard = new HashMap<>();

    for (String complianceStandardsFilter : paramAsStrings) {
      String[] parts = complianceStandardsFilter.split("=");
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid format. Expected key=value: " + complianceStandardsFilter);
      }

      int i = parts[0].indexOf(':');
      if (i < 0) {
        throw new IllegalArgumentException("Invalid format. Expected standard:version in " + complianceStandardsFilter);
      }
      categoriesByStandard.put(new ReportKey(parts[0].substring(0, i), parts[0].substring(i + 1)), parts[1]);
    }
    return categoriesByStandard;
  }
}
