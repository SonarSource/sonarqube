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
package org.sonar.db.measure;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Map;
import java.util.TreeMap;

public class ProjectMainBranchMeasureDto {

  private static final Gson GSON = new Gson();

  private String projectUuid;
  private Map<String, Object> metricValues = new TreeMap<>();

  public ProjectMainBranchMeasureDto() {
    // empty constructor
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public Map<String, Object> getMetricValues() {
    return metricValues;
  }

  // used by MyBatis mapper
  public void setJsonValue(String jsonValue) {
    metricValues = GSON.fromJson(jsonValue, new TypeToken<TreeMap<String, Object>>() {
    }.getType());
  }
}
