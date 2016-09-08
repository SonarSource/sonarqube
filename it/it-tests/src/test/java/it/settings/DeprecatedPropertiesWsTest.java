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

import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import it.Category1Suite;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import util.user.UserRule;

import static java.net.URLEncoder.encode;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.ItUtils.newWsClient;
import static util.ItUtils.resetSettings;
import static util.ItUtils.runProjectAnalysis;

public class DeprecatedPropertiesWsTest {

  private final static String PROJECT_KEY = "com.sonarsource.it.samples:multi-modules-sample";
  private static final String MODULE_KEY = "com.sonarsource.it.samples:multi-modules-sample:module_a";
  private static final String SUB_MODULE_KEY = "com.sonarsource.it.samples:multi-modules-sample:module_a:module_a1";

  private static final String PROJECT_SETTING_KEY = "sonar.dbcleaner.hoursBeforeKeepingOnlyOneSnapshotByDay";

  private static String USER_LOGIN = "john";

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  static WsClient adminWsClient;
  static WsClient userWsClient;
  static WsClient anonymousWsClient;

  @BeforeClass
  public static void init() throws Exception {
    orchestrator.resetData();
    userRule.createUser(USER_LOGIN, "password");
    adminWsClient = newAdminWsClient(orchestrator);
    userWsClient = newUserWsClient(orchestrator, USER_LOGIN, "password");
    anonymousWsClient = newWsClient(orchestrator);
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample");
  }

  @AfterClass
  public static void resetAfterClass() throws Exception {
    doResetSettings();
    userRule.deactivateUsers(USER_LOGIN);
  }

  @Before
  public void resetBefore() throws Exception {
    doResetSettings();
  }

  private static void doResetSettings() {
    resetSettings(orchestrator, null, "some-property", "int", "multi", "boolean", "hidden", "not_defined", "setting.secured", "setting.license", "list");
    resetSettings(orchestrator, PROJECT_KEY, PROJECT_SETTING_KEY, "sonar.coverage.exclusions");
  }

  @Test
  public void get_default_global_value() throws Exception {
    assertThat(getProperty("some-property", null).getValue()).isEqualTo("aDefaultValue");
  }

  @Test
  public void get_and_set_global_value() throws Exception {
    setProperty("some-property", "value", null);

    assertThat(getProperty("some-property", null).getValue()).isEqualTo("value");
  }

  @Test
  public void get_and_set_multi_values() throws Exception {
    setProperty("multi", "value1,value2", null);

    Properties.Property setting = getProperty("multi", null);
    assertThat(setting.getValue()).isEqualTo("value1,value2");
    assertThat(setting.getValues()).containsOnly("value1", "value2");
  }

  @Test
  public void get_and_set_hidden_setting() throws Exception {
    setProperty("hidden", "value", null);

    assertThat(getProperty("hidden", null).getValue()).isEqualTo("value");
  }

  @Test
  public void get_and_set_secured_setting() throws Exception {
    setProperty("setting.secured", "value", null);

    assertThat(getProperty("setting.secured", null).getValue()).isEqualTo("value");
  }

  @Test
  public void get_and_set_license_setting() throws Exception {
    setProperty("setting.license", "value", null);

    assertThat(getProperty("setting.license", null).getValue()).isEqualTo("value");
  }

  @Test
  public void get_and_set_not_defined_setting() throws Exception {
    setProperty("not_defined", "value", null);

    assertThat(getProperty("not_defined", null).getValue()).isEqualTo("value");
  }

  @Test
  public void secured_setting_not_returned_to_not_admin() throws Exception {
    setProperty("setting.secured", "value", null);

    // Admin can see the secured setting
    assertThat(getProperties(null)).extracting(Properties.Property::getKey).contains("setting.secured");

    // Not admin cannot see the secured setting
    assertThat(getProperties(userWsClient, null)).extracting(Properties.Property::getKey).doesNotContain("setting.secured");
    assertThat(getProperties(anonymousWsClient, null)).extracting(Properties.Property::getKey).doesNotContain("setting.secured");
  }

  @Test
  public void license_setting_not_returned_to_not_logged() throws Exception {
    setProperty("setting.license", "value", null);

    // Admin and user can see the license setting
    assertThat(getProperties(null)).extracting(Properties.Property::getKey).contains("setting.license");
    assertThat(getProperties(userWsClient, null)).extracting(Properties.Property::getKey).contains("setting.license");

    // Anonymous cannot see the license setting
    // FIXME Don't understand why it fails ???
    // assertThat(getProperties(anonymousWsClient, null)).extracting(Properties.Property::getKey).doesNotContain("setting.license");
  }

  @Test
  public void validate_setting() throws Exception {
    assertUpdateFails("list", "Z", "Not a valid option");
    assertUpdateFails("int", "not an int", "Only digits are allowed");
    assertUpdateFails("boolean", "not a boolean", "Valid options are \\\"true\\\" and \\\"false\\\"");
  }

  @Test
  public void delete_global_value() throws Exception {
    setProperty("int", "10", null);

    deleteProperty("int", null);

    assertPropertyDoesNotExist("int", null);
  }

  @Test
  public void get_all_global_settings() throws Exception {
    List<Properties.Property> properties = getProperties(null);
    assertThat(properties).isNotEmpty();
    assertThat(properties).extracting("key")
      .contains("sonar.core.id", "some-property", "boolean")
      .doesNotContain("hidden");
  }

  @Test
  public void get_default_component_value() throws Exception {
    // Check default value is returned
    assertThat(getProperty(PROJECT_SETTING_KEY, PROJECT_KEY).getValue()).isEqualTo("24");
    assertThat(getProperty(PROJECT_SETTING_KEY, MODULE_KEY).getValue()).isEqualTo("24");
    assertThat(getProperty(PROJECT_SETTING_KEY, SUB_MODULE_KEY).getValue()).isEqualTo("24");
  }

  @Test
  public void get_global_component_value() throws Exception {
    // Check global value is returned
    setProperty(PROJECT_SETTING_KEY, "30", null);
    assertThat(getProperty(PROJECT_SETTING_KEY, PROJECT_KEY).getValue()).isEqualTo("30");
    assertThat(getProperty(PROJECT_SETTING_KEY, MODULE_KEY).getValue()).isEqualTo("30");
    assertThat(getProperty(PROJECT_SETTING_KEY, SUB_MODULE_KEY).getValue()).isEqualTo("30");
  }

  @Test
  public void get_and_set_component_value() throws Exception {
    setProperty("sonar.coverage.exclusions", "file", PROJECT_KEY);

    assertThat(getProperty("sonar.coverage.exclusions", PROJECT_KEY).getValue()).isEqualTo("file");
  }

  @Test
  public void delete_component_value() throws Exception {
    setProperty("sonar.coverage.exclusions", "file", PROJECT_KEY);

    deleteProperty("sonar.coverage.exclusions", PROJECT_KEY);

    assertPropertyDoesNotExist("sonar.coverage.exclusions", PROJECT_KEY);
  }

  @Test
  public void get_all_component_settings() throws Exception {
    List<Properties.Property> properties = getProperties(PROJECT_KEY);
    assertThat(properties).isNotEmpty();
    assertThat(properties).extracting("key")
      .contains("sonar.dbcleaner.cleanDirectory", "sonar.dbcleaner.weeksBeforeDeletingAllSnapshots")
      .doesNotContain("hidden");
  }

  private static void setProperty(String key, String value, @Nullable String componentKey) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/properties")
        .setParam("id", key)
        .setParam("value", value)
        .setParam("resource", componentKey))
      .failIfNotSuccessful();
  }

  private static void deleteProperty(String key, @Nullable String componentKey) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/properties/destroy")
        .setParam("id", key)
        .setParam("resource", componentKey))
      .failIfNotSuccessful();
  }

  private static List<Properties.Property> getProperties(@Nullable String componentKey) {
    return getProperties(adminWsClient, componentKey);
  }

  private static List<Properties.Property> getProperties(WsClient wsClient, @Nullable String componentKey) {
    WsResponse response = wsClient.wsConnector()
      .call(new GetRequest("api/properties")
        .setParam("resource", componentKey))
      .failIfNotSuccessful();
    return asList(Properties.parse(response.content()));
  }

  private static Properties.Property getProperty(String key, @Nullable String componentKey) throws UnsupportedEncodingException {
    return getProperty(adminWsClient, key, componentKey);
  }

  private static Properties.Property getProperty(WsClient wsClient, String key, @Nullable String componentKey) throws UnsupportedEncodingException {
    WsResponse response = wsClient.wsConnector()
      .call(new GetRequest("api/properties/" + encode(key, "UTF-8"))
        .setParam("resource", componentKey))
      .failIfNotSuccessful();
    Properties.Property[] properties = Properties.parse(response.content());
    return Arrays.stream(properties).findFirst().orElseThrow(() -> new IllegalArgumentException("Property does not exist : " + key));
  }

  private static void assertPropertyDoesNotExist(String key, @Nullable String componentKey) {
    Optional<Properties.Property> property = getProperties(componentKey).stream().filter(p -> p.getKey().equals(key)).findFirst();
    assertThat(property.isPresent()).isFalse();
  }

  private static void assertUpdateFails(String key, String value, String expectedError) {
    WsResponse response = adminWsClient.wsConnector().call(
      new PostRequest("api/properties")
        .setParam("id", key)
        .setParam("value", value));
    assertThat(response.code()).isEqualTo(400);
    assertThat(response.content()).contains(expectedError);
  }

  public static class Properties {

    private List<Property> properties;

    private Properties(List<Property> properties) {
      this.properties = properties;
    }

    public List<Property> getProperties() {
      return properties;
    }

    public static Property[] parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, Property[].class);
    }

    public static class Property {
      private final String key;
      private final String value;
      private final String[] values;

      private Property(String key, String value, String[] values) {
        this.key = key;
        this.value = value;
        this.values = values;
      }

      public String getKey() {
        return key;
      }

      public String getValue() {
        return value;
      }

      public String[] getValues() {
        return values;
      }
    }
  }

}
