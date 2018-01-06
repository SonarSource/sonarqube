/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.repository.settings;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.util.ScannerUtils;
import org.sonarqube.ws.Settings.FieldValues.Value;
import org.sonarqube.ws.Settings.Setting;
import org.sonarqube.ws.Settings.ValuesWsResponse;
import org.sonarqube.ws.client.GetRequest;

public class DefaultSettingsLoader implements SettingsLoader {

  private ScannerWsClient wsClient;
  private static final Logger LOG = Loggers.get(DefaultSettingsLoader.class);

  public DefaultSettingsLoader(ScannerWsClient wsClient) {
    this.wsClient = wsClient;
  }

  @Override
  public Map<String, String> load(@Nullable String componentKey) {
    String url = "api/settings/values.protobuf";
    Profiler profiler = Profiler.create(LOG);
    if (componentKey != null) {
      url += "?component=" + ScannerUtils.encodeForUrl(componentKey);
      profiler.startInfo("Load settings for component '" + componentKey + "'");
    } else {
      profiler.startInfo("Load global settings");
    }
    try (InputStream is = wsClient.call(new GetRequest(url)).contentStream()) {
      ValuesWsResponse values = ValuesWsResponse.parseFrom(is);
      profiler.stopInfo();
      return toMap(values.getSettingsList());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load server settings", e);
    }
  }

  @VisibleForTesting
  static Map<String, String> toMap(List<Setting> settingsList) {
    Map<String, String> result = new LinkedHashMap<>();
    for (Setting s : settingsList) {
      if (!s.getInherited()) {
        switch (s.getValueOneOfCase()) {
          case VALUE:
            result.put(s.getKey(), s.getValue());
            break;
          case VALUES:
            result.put(s.getKey(), s.getValues().getValuesList().stream().map(StringEscapeUtils::escapeCsv).collect(Collectors.joining(",")));
            break;
          case FIELDVALUES:
            convertPropertySetToProps(result, s);
            break;
          default:
            throw new IllegalStateException("Unknow property value for " + s.getKey());
        }
      }
    }
    return result;
  }

  private static void convertPropertySetToProps(Map<String, String> result, Setting s) {
    List<String> ids = new ArrayList<>();
    int id = 1;
    for (Value v : s.getFieldValues().getFieldValuesList()) {
      for (Map.Entry<String, String> entry : v.getValue().entrySet()) {
        result.put(s.getKey() + "." + id + "." + entry.getKey(), entry.getValue());
      }
      ids.add(String.valueOf(id));
      id++;
    }
    result.put(s.getKey(), ids.stream().collect(Collectors.joining(",")));
  }
}
