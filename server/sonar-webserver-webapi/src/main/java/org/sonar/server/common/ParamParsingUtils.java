/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonarsource.compliancereports.reports.ReportKey;

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

  public static Map<ReportKey, Set<String>> parseComplianceStandardsFilter(@Nullable String param) {
    if (param == null) {
      return Map.of();
    }

    String decodedParam;
    try {
      decodedParam = URLDecoder.decode(param, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Can't URI decode: " + param, e);
    }

    Map<ReportKey, Set<String>> categoriesByStandard = new HashMap<>();

    String[] parts = decodedParam.split("&");
    for (String part : parts) {
      String[] keyValue = part.split("=");
      if (keyValue.length != 2) {
        throw new IllegalArgumentException("Invalid format: " + decodedParam);
      }
      Set<String> values = Arrays.stream(keyValue[1].split(",")).filter(s -> !s.isBlank()).collect(Collectors.toSet());
      categoriesByStandard.put(ReportKey.parse(keyValue[0]), values);
    }

    return categoriesByStandard;
  }
}
