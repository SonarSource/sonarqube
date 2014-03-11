/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.wsclient.qualitygate;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.3
 */
public class UpdateCondition {

  private final Map<String, Object> params;

  private UpdateCondition() {
    params = new HashMap<String, Object>();
  }

  public static UpdateCondition create(long id) {
    UpdateCondition newCondition = new UpdateCondition();
    newCondition.params.put("id", id);
    return newCondition;
  }

  public Map<String, Object> urlParams() {
    return params;
  }

  public UpdateCondition metricKey(String metricKey) {
    params.put("metric", metricKey);
    return this;
  }

  public UpdateCondition operator(String operator) {
    params.put("op", operator);
    return this;
  }

  public UpdateCondition warningThreshold(@Nullable String warning) {
    params.put("warning", warning);
    return this;
  }

  public UpdateCondition errorThreshold(@Nullable String error) {
    params.put("error", error);
    return this;
  }

  public UpdateCondition period(@Nullable Integer period) {
    params.put("period", period);
    return this;
  }
}

