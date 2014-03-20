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
package org.sonar.batch.qualitygate;

import javax.annotation.CheckForNull;

import com.google.gson.JsonObject;
import org.sonar.api.measures.Metric;

public class ResolvedCondition {

  private static final String ATTRIBUTE_PERIOD = "period";

  private static final String ATTRIBUTE_ERROR = "error";

  private static final String ATTRIBUTE_WARNING = "warning";

  private JsonObject json;

  private Metric metric;

  public ResolvedCondition(JsonObject jsonObject, Metric metric) {
    this.json = jsonObject;
    this.metric = metric;
  }

  public Long id() {
    return json.get("id").getAsLong();
  }

  public String metricKey() {
    return json.get("metric").getAsString();
  }

  public Metric metric() {
    return metric;
  }

  public String operator() {
    return json.get("op").getAsString();
  }

  @CheckForNull
  public String warningThreshold() {
    return json.has(ATTRIBUTE_WARNING) ? json.get(ATTRIBUTE_WARNING).getAsString() : null;
  }

  @CheckForNull
  public String errorThreshold() {
    return json.has(ATTRIBUTE_ERROR) ? json.get(ATTRIBUTE_ERROR).getAsString() : null;
  }

  @CheckForNull
  public Integer period() {
    return json.has(ATTRIBUTE_PERIOD) ? json.get(ATTRIBUTE_PERIOD).getAsInt() : null;
  }
}
