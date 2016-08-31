/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.settings;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.setting.ResetRequest;
import org.sonarqube.ws.client.setting.SetRequest;
import org.sonarqube.ws.client.setting.SettingsService;
import org.sonarqube.ws.client.setting.ValuesRequest;
import util.selenium.SeleneseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.setServerProperty;

public class SettingsTest {

  /**
   * This setting is defined by server-plugin
   */
  private final static String PLUGIN_SETTING_KEY = "some-property";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  static SettingsService SETTINGS;

  @BeforeClass
  public static void initSettingsService() throws Exception {
    SETTINGS = newAdminWsClient(orchestrator).settingsService();
  }

  @After
  public void reset_settings() throws Exception {
    setServerProperty(orchestrator, PLUGIN_SETTING_KEY, null);
  }

  // SONAR-4404
  @Test
  public void should_get_settings_default_value() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("settings-default-value",
      "/settings/SettingsTest/settings-default-value.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  /**
   * SONAR-3320
   */
  @Test
  public void global_property_change_extension_point() throws IOException {
    orchestrator.getServer().adminWsClient().post("api/properties/create?id=globalPropertyChange.received&value=NEWVALUE");
    assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs())).contains("Received change: [key=globalPropertyChange.received, newValue=NEWVALUE]");
  }

  @Test
  public void get_default_value() throws Exception {
    Settings.Setting setting = getSetting(PLUGIN_SETTING_KEY);
    assertThat(setting.getValue()).isEqualTo("aDefaultValue");
    assertThat(setting.getInherited()).isTrue();
  }

  @Test
  public void set_value() throws Exception {
    SETTINGS.set(SetRequest.builder().setKey(PLUGIN_SETTING_KEY).setValue("some value").build());

    String value = getSetting(PLUGIN_SETTING_KEY).getValue();
    assertThat(value).isEqualTo("some value");
  }

  @Test
  public void remove_value() throws Exception {
    SETTINGS.set(SetRequest.builder().setKey(PLUGIN_SETTING_KEY).setValue("some value").build());
    SETTINGS.reset(ResetRequest.builder().setKey(PLUGIN_SETTING_KEY).build());
    assertThat(getSetting(PLUGIN_SETTING_KEY).getValue()).isEqualTo("aDefaultValue");
  }

  private Settings.Setting getSetting(String key) {
    Settings.ValuesWsResponse response = SETTINGS.values(ValuesRequest.builder().setKeys(key).build());
    List<Settings.Setting> settings = response.getSettingsList();
    assertThat(settings).hasSize(1);
    return settings.get(0);
  }
}
