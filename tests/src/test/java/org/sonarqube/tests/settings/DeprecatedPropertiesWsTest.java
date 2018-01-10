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

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import org.junit.rules.RuleChain;
import org.sonarqube.tests.Category1Suite;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.settings.SetRequest;
import org.sonarqube.ws.client.settings.SettingsService;
import util.user.UserRule;

import static java.net.URLEncoder.encode;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
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

  private static UserRule userRule = UserRule.from(orchestrator);

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(userRule);

  static WsClient adminWsClient;
  static WsClient userWsClient;
  static WsClient anonymousWsClient;

  static SettingsService adminSettingsService;

  @BeforeClass
  public static void init() {
    orchestrator.resetData();
    userRule.createUser(USER_LOGIN, "password");
    adminWsClient = newAdminWsClient(orchestrator);
    userWsClient = newUserWsClient(orchestrator, USER_LOGIN, "password");
    anonymousWsClient = newWsClient(orchestrator);
    adminSettingsService = newAdminWsClient(orchestrator).settings();
    runProjectAnalysis(orchestrator, "shared/xoo-multi-modules-sample");
  }

  @AfterClass
  public static void resetAfterClass() {
    doResetSettings();
    userRule.deactivateUsers(USER_LOGIN);
  }

  @Before
  public void resetBefore() {
    doResetSettings();
  }

  private static void doResetSettings() {
    resetSettings(orchestrator, null, "some-property", "custom-property", "int", "multi", "boolean", "hidden", "not_defined", "setting.secured", "setting.license.secured", "list",
      "undefined");
    resetSettings(orchestrator, PROJECT_KEY, PROJECT_SETTING_KEY, "sonar.coverage.exclusions", "project.setting");
  }

  @Test
  public void get_default_global_value() throws Exception {
    assertThat(getProperty("some-property", null).getValue()).isEqualTo("aDefaultValue");
  }

  @Test
  public void get_global_value() throws Exception {
    setProperty("some-property", "value", null);

    assertThat(getProperty("some-property", null).getValue()).isEqualTo("value");
  }

  @Test
  public void get_multi_values() throws Exception {
    setProperty("multi", asList("value1", "value2", "value,3"), null);

    Properties.Property setting = getProperty("multi", null);
    assertThat(setting.getValue()).isEqualTo("value1,value2,value%2C3");
    assertThat(setting.getValues()).containsOnly("value1", "value2", "value,3");
  }

  @Test
  public void get_hidden_setting() throws Exception {
    setProperty("hidden", "value", null);

    assertThat(getProperty("hidden", null).getValue()).isEqualTo("value");
  }

  @Test
  public void get_secured_setting() throws Exception {
    setProperty("setting.secured", "value", null);

    assertThat(getProperty("setting.secured", null).getValue()).isEqualTo("value");
  }

  @Test
  public void get_license_setting() throws Exception {
    setProperty("setting.license.secured", "value", null);

    assertThat(getProperty("setting.license.secured", null).getValue()).isEqualTo("value");
  }

  @Test
  public void get_not_defined_setting() throws Exception {
    setProperty("not_defined", "value", null);

    assertThat(getProperty("not_defined", null).getValue()).isEqualTo("value");
  }

  @Test
  public void secured_setting_not_returned_to_not_admin() {
    setProperty("setting.secured", "value", null);

    // Admin can see the secured setting
    assertThat(getProperties(null)).extracting(Properties.Property::getKey).contains("setting.secured");

    // Not admin cannot see the secured setting
    assertThat(getProperties(userWsClient, null)).extracting(Properties.Property::getKey).doesNotContain("setting.secured");
    assertThat(getProperties(anonymousWsClient, null)).extracting(Properties.Property::getKey).doesNotContain("setting.secured");
  }

  @Test
  public void license_setting_not_returned_to_not_logged() {
    setProperty("setting.license.secured", "value", null);

    // Admin and user can see the license setting
    assertThat(getProperties(null)).extracting(Properties.Property::getKey).contains("setting.license.secured");
    assertThat(getProperties(userWsClient, null)).extracting(Properties.Property::getKey).contains("setting.license.secured");

    // Anonymous cannot see the license setting
    assertThat(getProperties(anonymousWsClient, null)).extracting(Properties.Property::getKey).doesNotContain("setting.license.secured");
  }

  @Test
  public void get_all_global_settings() {
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
  public void get_component_value() throws Exception {
    setProperty("sonar.coverage.exclusions", asList("file"), PROJECT_KEY);

    assertThat(getProperty("sonar.coverage.exclusions", PROJECT_KEY).getValue()).isEqualTo("file");
  }

  @Test
  public void get_global_value_when_component_is_unknown() throws Exception {
    setProperty("some-property", "value", null);

    assertThat(getProperty("some-property", PROJECT_KEY).getValue()).isEqualTo("value");
  }

  @Test
  public void get_all_component_settings() {
    List<Properties.Property> properties = getProperties(PROJECT_KEY);
    assertThat(properties).isNotEmpty();
    assertThat(properties).extracting("key")
      .contains("sonar.dbcleaner.cleanDirectory", "sonar.dbcleaner.weeksBeforeDeletingAllSnapshots")
      .doesNotContain("hidden");
  }

  @Test
  public void get_global_value_using_id_parameter() throws Exception {
    setProperty("some-property", "value", null);

    assertThat(getProperty(adminWsClient, "some-property", null, true).getValue()).isEqualTo("value");
  }

  @Test
  public void put_property() throws Exception {
    putProperty("some-property", "some-value", null, false);

    assertThat(getProperty("some-property", null).getValue()).isEqualTo("some-value");
  }

  @Test
  public void put_property_using_id_parameter() throws Exception {
    putProperty("some-property", "some-value", null, true);

    assertThat(getProperty("some-property", null).getValue()).isEqualTo("some-value");
  }

  @Test
  public void put_property_on_project() throws Exception {
    putProperty("project.setting", "some-value", PROJECT_KEY, false);

    assertThat(getProperty("project.setting", PROJECT_KEY).getValue()).isEqualTo("some-value");
  }

  @Test
  public void put_property_for_undefined_setting() throws Exception {
    putProperty("undefined", "some-value", null, false);

    assertThat(getProperty("undefined", null).getValue()).isEqualTo("some-value");
  }

  @Test
  public void put_property_multi_values() throws Exception {
    putProperty("multi", "value1,value2,value3", null, false);

    Properties.Property setting = getProperty("multi", null);
    assertThat(setting.getValue()).isEqualTo("value1,value2,value3");
    assertThat(setting.getValues()).containsOnly("value1", "value2", "value3");
  }

  @Test
  public void fail_with_error_400_when_put_property_without_id() throws Exception {
    Response response = putProperty("", "some-value", null, false);
    assertThat(response.code()).isEqualTo(400);
  }

  @Test
  public void delete_property() throws Exception {
    setProperty("custom-property", "value", null);

    deleteProperty("custom-property", null, false);

    assertThat(getProperty("custom-property", null)).isNull();
  }

  @Test
  public void delete_property_using_id_parameter() throws Exception {
    setProperty("custom-property", "value", null);

    deleteProperty("custom-property", null, true);

    assertThat(getProperty("custom-property", null)).isNull();
  }

  @Test
  public void delete_property_on_project() throws Exception {
    setProperty("project.setting", "value", PROJECT_KEY);

    deleteProperty("project.setting", PROJECT_KEY, false);

    assertThat(getProperty("project.setting", PROJECT_KEY)).isNull();
  }

  private static void setProperty(String key, String value, @Nullable String componentKey) {
    adminSettingsService.set(new SetRequest().setKey(key).setValue(value).setComponent(componentKey));
  }

  private static void setProperty(String key, List<String> values, @Nullable String componentKey) {
    adminSettingsService.set(new SetRequest().setKey(key).setValues(values).setComponent(componentKey));
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
    return getProperty(adminWsClient, key, componentKey, false);
  }

  @CheckForNull
  private static Properties.Property getProperty(WsClient wsClient, String key, @Nullable String componentKey, boolean useIdParameter) throws UnsupportedEncodingException {
    GetRequest getRequest = useIdParameter ? new GetRequest("api/properties").setParam("id", encode(key, "UTF-8")).setParam("resource", componentKey)
      : new GetRequest("api/properties/" + encode(key, "UTF-8")).setParam("resource", componentKey);
    WsResponse response = wsClient.wsConnector()
      .call(getRequest)
      .failIfNotSuccessful();
    Properties.Property[] properties = Properties.parse(response.content());
    return Arrays.stream(properties).findFirst().orElseGet(() -> null);
  }

  private static Response putProperty(String key, String value, @Nullable String componentKey, boolean useIdParameter) throws UnsupportedEncodingException {
    String url = useIdParameter ? orchestrator.getServer().getUrl() + "/api/properties?id=" + encode(key, "UTF-8") + "&value=" + value
      : orchestrator.getServer().getUrl() + "/api/properties/" + encode(key, "UTF-8") + "?value=" + value;
    url += componentKey != null ? "&resource=" + componentKey : "";
    return call(new Request.Builder()
      .put(new FormBody.Builder().build())
      .url(url));
  }

  private static Response deleteProperty(String key, @Nullable String componentKey, boolean useIdParameter) throws UnsupportedEncodingException {
    String url = useIdParameter ? orchestrator.getServer().getUrl() + "/api/properties?id=" + encode(key, "UTF-8")
      : orchestrator.getServer().getUrl() + "/api/properties/" + encode(key, "UTF-8");
    url += componentKey != null ? "?resource=" + componentKey : "";
    return call(new Request.Builder()
      .delete(new FormBody.Builder().build())
      .url(url));
  }

  private static Response call(Request.Builder requestBuilder) {
    try {
      requestBuilder.header("Authorization", Credentials.basic("admin", "admin"));
      return new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        .newCall(requestBuilder.build())
        .execute();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
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
