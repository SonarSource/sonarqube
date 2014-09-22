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
public class NewCondition {

  private final Map<String, Object> params;

  private NewCondition() {
    params = new HashMap<String, Object>();
  }

  public static NewCondition create(long qGateId) {
    NewCondition newCondition = new NewCondition();
    newCondition.params.put("gateId", qGateId);
    return newCondition;
  }

  public Map<String, Object> urlParams() {
    return params;
  }

  public NewCondition metricKey(String metricKey) {
    params.put("metric", metricKey);
    return this;
  }

  public NewCondition operator(String operator) {
    params.put("op", operator);
    return this;
  }

  public NewCondition warningThreshold(@Nullable String warning) {
    params.put("warning", warning);
    return this;
  }

  public NewCondition errorThreshold(@Nullable String error) {
    params.put("error", error);
    return this;
  }

  public NewCondition period(@Nullable Integer period) {
    params.put("period", period);
    return this;
  }
}

