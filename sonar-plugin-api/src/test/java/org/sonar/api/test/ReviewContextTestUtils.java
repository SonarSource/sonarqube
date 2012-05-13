/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.test;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.reviews.ReviewContext;

import java.util.Map;

public class ReviewContextTestUtils {

  public static ReviewContext createReviewContext(String properties) {
    Map<String, Map<String, String>> reviewContextMap = Maps.newLinkedHashMap();
    for (String innerMap : StringUtils.split(properties, ';')) {
      if (StringUtils.isNotBlank(innerMap)) {
        String mapName = StringUtils.substringBefore(innerMap, "=").trim();
        Map<String, String> currentMap = Maps.newLinkedHashMap();
        for (String keyValuePairList : StringUtils.substringsBetween(innerMap, "{", "}")) {
          for (String keyValuePair : StringUtils.split(keyValuePairList, ',')) {
            currentMap.put(StringUtils.substringBefore(keyValuePair, "=").trim(), StringUtils.substringAfter(keyValuePair, "=").trim());
          }
        }
        reviewContextMap.put(mapName, currentMap);
      }
    }

    return ReviewContext.createFromMap(reviewContextMap);
  }
}
