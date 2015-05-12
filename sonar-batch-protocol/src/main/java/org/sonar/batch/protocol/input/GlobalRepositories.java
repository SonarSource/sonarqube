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
package org.sonar.batch.protocol.input;

import org.sonar.batch.protocol.GsonHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for all global data going from server to batch.
 * This is not an API since server and batch always share the same version.
 */
public class GlobalRepositories {

  private long timestamp;
  private Collection<Metric> metrics = new ArrayList<>();
  private Map<String, String> globalSettings = new HashMap<>();

  public Map<String, String> globalSettings() {
    return globalSettings;
  }

  public GlobalRepositories addGlobalSetting(String key, String value) {
    globalSettings.put(key, value);
    return this;
  }

  public Collection<Metric> metrics() {
    return metrics;
  }

  public GlobalRepositories addMetric(Metric metric) {
    metrics.add(metric);
    return this;
  }

  public long timestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String toJson() {
    return GsonHelper.create().toJson(this);
  }

  public static GlobalRepositories fromJson(String json) {
    return GsonHelper.create().fromJson(json, GlobalRepositories.class);
  }

}
