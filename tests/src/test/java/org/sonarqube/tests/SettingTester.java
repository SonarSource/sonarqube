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
package org.sonarqube.tests;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.setting.ListDefinitionsRequest;
import org.sonarqube.ws.client.setting.ResetRequest;
import org.sonarqube.ws.client.setting.SetRequest;
import org.sonarqube.ws.client.setting.SettingsService;

import static org.sonarqube.ws.Settings.Type.LICENSE;

public class SettingTester {

  private static final Set<String> EMAIL_SETTINGS = ImmutableSet.of("email.smtp_host.secured", "email.smtp_port.secured", "email.smtp_secure_connection.secured",
    "email.smtp_username.secured", "email.smtp_password.secured", "email.from", "email.prefix");

  private final Session session;

  SettingTester(Session session) {
    this.session = session;
  }

  public SettingsService service() {
    return session.wsClient().settings();
  }

  void deleteAll() {
    List<String> settingKeys = Stream.concat(
      session.wsClient().settings().listDefinitions(ListDefinitionsRequest.builder().build()).getDefinitionsList()
        .stream()
        .filter(def -> def.getType() != LICENSE)
        .map(Settings.Definition::getKey),
      EMAIL_SETTINGS.stream())
      .collect(Collectors.toList());
    session.wsClient().settings().reset(ResetRequest.builder().setKeys(settingKeys).build());
  }

  public void resetSettings(String... keys){
    session.wsClient().settings().reset(ResetRequest.builder().setKeys(keys).build());
  }

  public void setGlobalSetting(String key, @Nullable String value) {
    setSetting(null, key, value);
  }

  public void setGlobalSettings(String... properties) {
    for (int i = 0; i < properties.length; i += 2) {
      setSetting(null, properties[i], properties[i + 1]);
    }
  }

  public void setProjectSetting(String componentKey, String key, @Nullable String value) {
    setSetting(componentKey, key, value);
  }

  public void setProjectSettings(String componentKey, String... properties) {
    for (int i = 0; i < properties.length; i += 2) {
      setSetting(componentKey, properties[i], properties[i + 1]);
    }
  }

  private void setSetting(@Nullable String componentKey, String key, @Nullable String value) {
    if (value == null) {
      session.wsClient().settings().reset(ResetRequest.builder().setKeys(key).setComponent(componentKey).build());
    } else {
      session.wsClient().settings().set(SetRequest.builder().setKey(key).setValue(value).setComponent(componentKey).build());
    }
  }

}
