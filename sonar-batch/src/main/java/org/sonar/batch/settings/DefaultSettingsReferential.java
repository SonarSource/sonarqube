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
package org.sonar.batch.settings;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.bootstrap.ServerClient;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link SettingsReferential} that fetch settings from remote SQ server using WS.
 * @since 4.4
 */
public class DefaultSettingsReferential implements SettingsReferential {

  private static final String BATCH_BOOTSTRAP_PROPERTIES_URL = "/batch_bootstrap/properties";

  private final ServerClient serverClient;
  private final AnalysisMode analysisMode;

  public DefaultSettingsReferential(ServerClient serverClient, AnalysisMode analysisMode) {
    this.serverClient = serverClient;
    this.analysisMode = analysisMode;
  }

  @Override
  public Map<String, String> globalSettings() {
    return downloadSettings(null);
  }

  @Override
  public Map<String, String> projectSettings(String moduleKey) {
    return downloadSettings(moduleKey);
  }

  private Map<String, String> downloadSettings(@Nullable String moduleKey) {
    Map<String, String> result = Maps.newHashMap();
    String url = BATCH_BOOTSTRAP_PROPERTIES_URL + "?dryRun=" + analysisMode.isPreview();
    if (moduleKey != null) {
      url += "&project=" + moduleKey;
    }
    String jsonText = serverClient.request(url);

    List<Map<String, String>> json = new Gson().fromJson(jsonText, new TypeToken<List<Map<String, String>>>() {
    }.getType());

    for (Map<String, String> jsonProperty : json) {
      String key = jsonProperty.get("k");
      String value = jsonProperty.get("v");
      result.put(key, value);
    }

    return result;
  }

}
