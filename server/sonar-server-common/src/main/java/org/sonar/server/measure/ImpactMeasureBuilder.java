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
package org.sonar.server.measure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.issue.impact.Severity;

import static org.sonar.api.utils.Preconditions.checkArgument;

/**
 * Builder class to help build measures based on impacts with payload such as @{link {@link org.sonar.api.measures.CoreMetrics#RELIABILITY_ISSUES}}.
 */
public class ImpactMeasureBuilder {

  private static final Gson GSON = new GsonBuilder().create();
  private static final Type GSON_MAP_TYPE = new TypeToken<Map<String, Long>>() {
  }.getType();
  public static final String TOTAL_KEY = "total";

  private final Map<String, Long> map;

  private ImpactMeasureBuilder(Map<String, Long> map) {
    this.map = new LinkedHashMap<>(map);
  }

  public static ImpactMeasureBuilder createEmpty() {
    Map<String, Long> severityMap = new LinkedHashMap<>();
    for (Severity severity : Severity.values()) {
      severityMap.put(severity.name(), 0L);
    }
    severityMap.put(TOTAL_KEY, 0L);
    return new ImpactMeasureBuilder(severityMap);
  }

  public static ImpactMeasureBuilder newInstance() {
    return new ImpactMeasureBuilder(new LinkedHashMap<>());
  }

  public static ImpactMeasureBuilder fromMap(Map<String, Long> map) {
    checkImpactMap(map);
    return new ImpactMeasureBuilder(map);
  }

  private static void checkImpactMap(Map<String, Long> map) {
    checkArgument(map.containsKey(TOTAL_KEY), "Map must contain a total key");
    Arrays.stream(Severity.values()).forEach(severity -> checkArgument(map.containsKey(severity.name()), "Map must contain a key for severity " + severity.name()));
  }

  public static ImpactMeasureBuilder fromString(String value) {
    Map<String, Long> impactMap = GSON.fromJson(value, GSON_MAP_TYPE);
    checkImpactMap(impactMap);
    return new ImpactMeasureBuilder(impactMap);
  }

  public ImpactMeasureBuilder setSeverity(Severity severity, long value) {
    map.put(severity.name(), value);
    return this;
  }

  public ImpactMeasureBuilder setTotal(long value) {
    map.put(TOTAL_KEY, value);
    return this;
  }

  @CheckForNull
  public Long getTotal() {
    return map.get(TOTAL_KEY);
  }

  public ImpactMeasureBuilder add(ImpactMeasureBuilder other) {
    other.buildAsMap().forEach((key, val) -> map.merge(key, val, Long::sum));
    return this;
  }

  public String buildAsString() {
    checkImpactMap(map);
    return GSON.toJson(map);
  }

  public Map<String, Long> buildAsMap() {
    checkImpactMap(map);
    return map;
  }
}
