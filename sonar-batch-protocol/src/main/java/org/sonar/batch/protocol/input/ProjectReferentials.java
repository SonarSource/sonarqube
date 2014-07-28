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

import com.google.gson.Gson;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for all project data going from server to batch.
 * This is not an API since server and batch always share the same version.
 */
public class ProjectReferentials {

  private long timestamp;
  private Map<String, QProfile> qprofilesByLanguage = new HashMap<String, QProfile>();
  private Collection<ActiveRule> activeRules = new ArrayList<ActiveRule>();
  private Map<String, Map<String, String>> settingsByModule = new HashMap<String, Map<String, String>>();

  public Map<String, String> settings(String projectKey) {
    return settingsByModule.containsKey(projectKey) ? settingsByModule.get(projectKey) : Collections.<String, String>emptyMap();
  }

  public ProjectReferentials addSettings(String projectKey, Map<String, String> settings) {
    Map<String, String> existingSettings = settingsByModule.get(projectKey);
    if (existingSettings == null) {
      existingSettings = new HashMap<String, String>();
    }
    existingSettings.putAll(settings);
    settingsByModule.put(projectKey, existingSettings);
    return this;
  }

  public Collection<QProfile> qProfiles() {
    return qprofilesByLanguage.values();
  }

  public ProjectReferentials addQProfile(QProfile qProfile) {
    qprofilesByLanguage.put(qProfile.language(), qProfile);
    return this;
  }

  public Collection<ActiveRule> activeRules() {
    return activeRules;
  }

  public ProjectReferentials addActiveRule(ActiveRule activeRule) {
    activeRules.add(activeRule);
    return this;
  }

  public long timestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String toJson() {
    return new Gson().toJson(this);
  }

  public static ProjectReferentials fromJson(Reader input) {
    return new Gson().fromJson(input, ProjectReferentials.class);
  }

}
