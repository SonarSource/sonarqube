/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.qualityprofile;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumMap;
import java.util.Map;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

public class QProfileUtils {

  private QProfileUtils() {
  }

  public static Map<SoftwareQuality, Severity> parseImpactsToMap(String impacts) {
    ObjectMapper mapper = new ObjectMapper();
    Map<SoftwareQuality, Severity> parsedMap = new EnumMap<>(SoftwareQuality.class);
    try {
      Map<String, String> stringMap = mapper.readValue(impacts, Map.class);
      for (Map.Entry<String, String> entry : stringMap.entrySet()) {
        parsedMap.put(SoftwareQuality.valueOf(entry.getKey()), Severity.valueOf(entry.getValue()));
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("The quality profile cannot be restored as it contains invalid impacts: " + impacts);
    }
    return parsedMap;
  }
}
