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
package org.sonarqube.tests.settings;

import com.google.common.collect.ImmutableMap;
import com.sonar.orchestrator.Orchestrator;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.tests.Category1Suite;
import org.sonarqube.ws.Settings;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permissions.AddGroupRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.permissions.RemoveGroupRequest;
import org.sonarqube.ws.client.settings.ResetRequest;
import org.sonarqube.ws.client.settings.SetRequest;
import org.sonarqube.ws.client.settings.SettingsService;
import org.sonarqube.ws.client.settings.ValuesRequest;
import util.user.UserRule;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonarqube.ws.Settings.Setting;
import static org.sonarqube.ws.Settings.ValuesWsResponse;
import static util.ItUtils.expectBadRequestError;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.newWsClient;
import static util.ItUtils.resetSettings;

public class SettingsTest {

  /**
   * This setting is defined by server-plugin
   */
  private final static String PLUGIN_SETTING_KEY = "some-property";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  private static UserRule userRule = UserRule.from(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(userRule);

  private static WsClient adminWsClient;
  private static SettingsService anonymousSettingsService;
  private static SettingsService userSettingsService;
  private static SettingsService scanSettingsService;
  private static SettingsService adminSettingsService;

  @BeforeClass
  public static void initSettingsService() {
    userRule.createUser("setting-user", "setting-user");
    userRule.createUser("scanner-user", "scanner-user");
    adminWsClient = newAdminWsClient(orchestrator);
    // Remove 'Execute Analysis' permission from anyone
    adminWsClient.permissions().removeGroup(new RemoveGroupRequest().setGroupName("anyone").setPermission("scan"));

    // Anonymous user, without 'Execute Analysis' permission
    anonymousSettingsService = newWsClient(orchestrator).settings();

    // Authenticated user, without 'Execute Analysis' permission
    userSettingsService = newUserWsClient(orchestrator, "setting-user", "setting-user").settings();

    // User with 'Execute Analysis' permission
    adminWsClient.permissions().addUser(new AddUserRequest().setLogin("scanner-user").setPermission("scan"));
    scanSettingsService = newUserWsClient(orchestrator, "scanner-user", "scanner-user").settings();

    // User with 'Administer System' permission but without 'Execute Analysis' permission
    adminSettingsService = adminWsClient.settings();
  }

  @AfterClass
  public static void tearDown() {
    userRule.deactivateUsers("setting-user", "scanner-user");
    // Restore 'Execute Analysis' permission to anyone
    adminWsClient.permissions().addGroup(new AddGroupRequest().setGroupName("anyone").setPermission("scan"));
  }

  @After
  public void reset_settings() {
    resetSettings(orchestrator, null, PLUGIN_SETTING_KEY, "globalPropertyChange.received", "hidden", "setting.secured", "setting.license.secured");
  }

  /**
   * SONAR-3320
   */
  @Test
  public void global_property_change_extension_point() throws IOException {
    adminSettingsService.set(new SetRequest().setKey("globalPropertyChange.received").setValue("NEWVALUE"));
    assertThat(FileUtils.readFileToString(orchestrator.getServer().getWebLogs()))
      .contains("Received change: [key=globalPropertyChange.received, newValue=NEWVALUE]");
  }

  @Test
  public void get_default_value() {
    Setting setting = getSetting(PLUGIN_SETTING_KEY, anonymousSettingsService);
    assertThat(setting.getValue()).isEqualTo("aDefaultValue");
    assertThat(setting.getInherited()).isTrue();
  }

  @Test
  public void set_setting() {
    adminSettingsService.set(new SetRequest().setKey(PLUGIN_SETTING_KEY).setValue("some value"));

    String value = getSetting(PLUGIN_SETTING_KEY, anonymousSettingsService).getValue();
    assertThat(value).isEqualTo("some value");
  }

  @Test
  public void remove_setting() {
    adminSettingsService.set(new SetRequest().setKey(PLUGIN_SETTING_KEY).setValue("some value"));
    adminSettingsService.set(new SetRequest().setKey("sonar.links.ci").setValue("http://localhost"));

    adminSettingsService.reset(new ResetRequest().setKeys(Arrays.asList(PLUGIN_SETTING_KEY, "sonar.links.ci")));
    assertThat(getSetting(PLUGIN_SETTING_KEY, anonymousSettingsService).getValue()).isEqualTo("aDefaultValue");
    assertThat(getSetting("sonar.links.ci", anonymousSettingsService)).isNull();
  }

  @Test
  public void hidden_setting() {
    adminSettingsService.set(new SetRequest().setKey("hidden").setValue("test"));
    assertThat(getSetting("hidden", anonymousSettingsService).getValue()).isEqualTo("test");
  }

  @Test
  public void secured_setting() {
    adminSettingsService.set(new SetRequest().setKey("setting.secured").setValue("test"));
    assertThat(getSetting("setting.secured", anonymousSettingsService)).isNull();
    // assertThat(getSetting("setting.secured", userSettingsService)).isNull();
    assertThat(getSetting("setting.secured", scanSettingsService).getValue()).isEqualTo("test");
    assertThat(getSetting("setting.secured", adminSettingsService).getValue()).isEqualTo("test");
  }

  @Test
  public void license_setting() {
    adminSettingsService.set(new SetRequest().setKey("setting.license.secured").setValue("test"));
    assertThat(getSetting("setting.license.secured", anonymousSettingsService)).isNull();
    assertThat(getSetting("setting.license.secured", userSettingsService).getValue()).isEqualTo("test");
    assertThat(getSetting("setting.license.secured", scanSettingsService).getValue()).isEqualTo("test");
    assertThat(getSetting("setting.license.secured", adminSettingsService).getValue()).isEqualTo("test");
  }

  @Test
  public void multi_values_setting() {
    adminSettingsService.set(new SetRequest().setKey("multi").setValues(asList("value1", "value2", "value3")));
    assertThat(getSetting("multi", anonymousSettingsService).getValues().getValuesList()).containsOnly("value1", "value2", "value3");
  }

  @Test
  public void property_set_setting() {
    adminSettingsService.set(new SetRequest().setKey("sonar.jira").setFieldValues(asList(
      "{\"key\":\"jira1\", \"url\":\"http://jira1\", \"port\":\"12345\", \"type\":\"A\"}",
      "{\"key\":\"jira2\", \"url\":\"http://jira2\", \"port\":\"54321\"}")));

    assertThat(getSetting("sonar.jira", anonymousSettingsService).getFieldValues().getFieldValuesList()).extracting(Settings.FieldValues.Value::getValue).containsOnly(
      ImmutableMap.of("key", "jira1", "url", "http://jira1", "port", "12345", "type", "A"),
      ImmutableMap.of("key", "jira2", "url", "http://jira2", "port", "54321"));
  }

  @Test
  public void return_defined_settings_when_no_key_provided() {
    adminSettingsService.set(new SetRequest().setKey(PLUGIN_SETTING_KEY).setValue("some value"));
    adminSettingsService.set(new SetRequest().setKey("hidden").setValue("test"));

    assertThat(adminSettingsService.values(new ValuesRequest()).getSettingsList())
      .extracting(Setting::getKey)
      .contains(PLUGIN_SETTING_KEY, "hidden", "sonar.forceAuthentication",
        // Settings for scanner
        "sonar.core.startTime");

    assertThat(adminSettingsService.values(new ValuesRequest()).getSettingsList())
      .extracting(Setting::getKey, Setting::getValue)
      .contains(tuple(PLUGIN_SETTING_KEY, "some value"), tuple("hidden", "test"));
  }

  /**
   * SONAR-10300 Do not allow to use settings defined in sonar.properties in WS api/settings/values
   */
  @Test
  public void infra_properties_are_excluded_from_values_response() {
    ValuesWsResponse values = adminSettingsService.values(new ValuesRequest());

    assertThat(values.getSettingsList())
      .extracting(Setting::getKey)
      .doesNotContain("sonar.jdbc.url", "sonar.jdbc.password", "sonar.web.javaOpts" /* an others */);
  }

  /**
   * SONAR-10300 Do not allow to use settings defined in sonar.properties in WS api/settings/values
   */
  @Test
  public void requesting_an_infra_property_is_not_allowed() {
    ValuesRequest request = new ValuesRequest().setKeys(asList("sonar.jdbc.url"));

    expectBadRequestError(() -> adminSettingsService.values(request));
  }

  /**
   * SONAR-10300 Do not allow to use settings defined in sonar.properties in WS api/settings/set
   */
  @Test
  public void values_of_infra_properties_cant_be_changed() {
    SetRequest request = new SetRequest().setKey("sonar.jdbc.url").setValue("jdbc:h2:foo");

    expectBadRequestError(() -> adminSettingsService.set(request));
  }

  @CheckForNull
  private static Setting getSetting(String key, SettingsService settingsService) {
    ValuesWsResponse response = settingsService.values(new ValuesRequest().setKeys(asList(key)));
    List<Settings.Setting> settings = response.getSettingsList();
    return settings.isEmpty() ? null : settings.get(0);
  }

}
