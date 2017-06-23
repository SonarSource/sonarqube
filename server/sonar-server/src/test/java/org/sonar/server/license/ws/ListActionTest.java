/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.license.ws;

import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.setting.ws.SettingsFinder;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Licenses;
import org.sonarqube.ws.Licenses.ListWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.sonar.api.CoreProperties.PERMANENT_SERVER_ID;
import static org.sonar.api.PropertyType.LICENSE;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;

public class ListActionTest {

  private static final String LICENSE_KEY_SAMPLE = "sonar.governance.license.secured";
  private static final String LICENSE_NAME_SAMPLE = "Governance";
  private static final String ORGANIZATION_SAMPLE = "SonarSource";
  private static final String SERVER_ID_SAMPLE = "12345";
  private static final String PRODUCT_SAMPLE = "governance";
  private static final String TYPE_SAMPLE = "PRODUCTION";
  private static final String EXPIRATION_SAMPLE = "2099-01-01";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private PropertyDbTester propertyDb = new PropertyDbTester(db);
  private PropertyDefinitions definitions = new PropertyDefinitions();
  private SettingsFinder settingsFinder = new SettingsFinder(dbClient, definitions);

  private WsActionTester ws = new WsActionTester(new ListAction(userSession, definitions, dbClient, settingsFinder));

  @Test
  public void return_licenses() throws Exception {
    logInAsSystemAdministrator();
    addServerIdSettings("12345");
    String data = createBase64License("SonarSource", "governance", "12345", "2099-01-01", "PRODUCTION", ImmutableMap.of("other", "value"));
    addLicenseSetting("sonar.governance.license.secured", "Governance", data);

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).hasSize(1);
    Licenses.License license = result.getLicenses(0);
    assertThat(license.getKey()).isEqualTo("sonar.governance.license.secured");
    assertThat(license.getName()).isEqualTo("Governance");
    assertThat(license.getValue()).isEqualTo(data);
    assertThat(license.getProduct()).isEqualTo("governance");
    assertThat(license.getOrganization()).isEqualTo("SonarSource");
    assertThat(license.getExpiration()).isEqualTo("2099-01-01");
    assertThat(license.getType()).isEqualTo("PRODUCTION");
    assertThat(license.getServerId()).isEqualTo("12345");
    assertThat(license.getAdditionalProperties().getAdditionalProperties()).containsOnly(entry("other", "value"));

    assertThat(license.hasInvalidProduct()).isFalse();
    assertThat(license.hasInvalidExpiration()).isFalse();
    assertThat(license.hasInvalidServerId()).isFalse();
  }

  @Test
  public void return_licenses_even_if_no_value_set_in_database() throws Exception {
    logInAsSystemAdministrator();
    addServerIdSettings("12345");
    definitions.addComponent(PropertyDefinition.builder("sonar.governance.license.secured").type(LICENSE).build());

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).hasSize(1);
    Licenses.License license = result.getLicenses(0);
    assertThat(license.getKey()).isEqualTo("sonar.governance.license.secured");
    assertThat(license.hasValue()).isFalse();
    assertThat(license.hasProduct()).isFalse();
    assertThat(license.hasOrganization()).isFalse();
    assertThat(license.hasExpiration()).isFalse();
    assertThat(license.hasType()).isFalse();
    assertThat(license.hasServerId()).isFalse();
    assertThat(license.hasAdditionalProperties()).isFalse();

    assertThat(license.hasInvalidProduct()).isFalse();
    assertThat(license.hasInvalidExpiration()).isFalse();
    assertThat(license.hasInvalidServerId()).isFalse();
  }

  @Test
  public void return_information_when_no_licence_set() throws Exception {
    logInAsSystemAdministrator();
    addServerIdSettings(SERVER_ID_SAMPLE);
    addLicenseSetting(LICENSE_KEY_SAMPLE, null, toBase64(""));

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).hasSize(1);
    Licenses.License license = result.getLicenses(0);
    assertThat(license.getKey()).isEqualTo(LICENSE_KEY_SAMPLE);
    assertThat(license.hasName()).isFalse();
    assertThat(license.getValue()).isEmpty();
    assertThat(license.hasProduct()).isFalse();
    assertThat(license.hasOrganization()).isFalse();
    assertThat(license.hasExpiration()).isFalse();
    assertThat(license.hasType()).isFalse();
    assertThat(license.hasServerId()).isFalse();
    assertThat(license.hasAdditionalProperties()).isFalse();

    assertThat(license.hasInvalidProduct()).isTrue();
    assertThat(license.hasInvalidExpiration()).isFalse();
    assertThat(license.hasInvalidServerId()).isTrue();
  }

  @Test
  public void return_license_with_bad_product() throws Exception {
    logInAsSystemAdministrator();
    addServerIdSettings(SERVER_ID_SAMPLE);
    addLicenseSetting(LICENSE_KEY_SAMPLE, LICENSE_NAME_SAMPLE,
      createBase64License(ORGANIZATION_SAMPLE, "Other", SERVER_ID_SAMPLE, EXPIRATION_SAMPLE, TYPE_SAMPLE, Collections.emptyMap()));

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).hasSize(1);
    Licenses.License license = result.getLicenses(0);
    assertThat(license.getProduct()).isEqualTo("Other");
    assertThat(license.getInvalidProduct()).isTrue();
    assertThat(license.hasInvalidExpiration()).isFalse();
    assertThat(license.hasInvalidServerId()).isFalse();
  }

  @Test
  public void return_license_with_bad_server_id() throws Exception {
    logInAsSystemAdministrator();
    addServerIdSettings(SERVER_ID_SAMPLE);
    addLicenseSetting(LICENSE_KEY_SAMPLE, LICENSE_NAME_SAMPLE,
      createBase64License(ORGANIZATION_SAMPLE, PRODUCT_SAMPLE, "Other", EXPIRATION_SAMPLE, TYPE_SAMPLE, Collections.emptyMap()));

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).hasSize(1);
    Licenses.License license = result.getLicenses(0);
    assertThat(license.getServerId()).isEqualTo("Other");
    assertThat(license.getInvalidServerId()).isTrue();
    assertThat(license.hasInvalidProduct()).isFalse();
    assertThat(license.hasInvalidExpiration()).isFalse();
  }

  @Test
  public void return_bad_server_id_when_server_has_no_server_id() throws Exception {
    logInAsSystemAdministrator();
    addLicenseSetting(LICENSE_KEY_SAMPLE, LICENSE_NAME_SAMPLE,
      createBase64License(ORGANIZATION_SAMPLE, PRODUCT_SAMPLE, SERVER_ID_SAMPLE, EXPIRATION_SAMPLE, TYPE_SAMPLE, Collections.emptyMap()));

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).hasSize(1);
    Licenses.License license = result.getLicenses(0);
    assertThat(license.getInvalidServerId()).isTrue();
  }

  @Test
  public void does_not_return_invalid_server_id_when_all_servers_accepted_and_no_server_id_setting() throws Exception {
    logInAsSystemAdministrator();
    addLicenseSetting(LICENSE_KEY_SAMPLE, LICENSE_NAME_SAMPLE,
      createBase64License(ORGANIZATION_SAMPLE, PRODUCT_SAMPLE, "*", EXPIRATION_SAMPLE, TYPE_SAMPLE, Collections.emptyMap()));

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).hasSize(1);
    Licenses.License license = result.getLicenses(0);
    assertThat(license.getServerId()).isEqualTo("*");
    assertThat(license.hasInvalidServerId()).isFalse();
  }

  @Test
  public void return_license_when_all_servers_are_accepted() throws Exception {
    logInAsSystemAdministrator();
    addServerIdSettings(SERVER_ID_SAMPLE);
    addLicenseSetting(LICENSE_KEY_SAMPLE, LICENSE_NAME_SAMPLE,
      createBase64License(ORGANIZATION_SAMPLE, PRODUCT_SAMPLE, "*", EXPIRATION_SAMPLE, TYPE_SAMPLE, Collections.emptyMap()));

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).hasSize(1);
    Licenses.License license = result.getLicenses(0);
    assertThat(license.getServerId()).isEqualTo("*");
    assertThat(license.hasInvalidServerId()).isFalse();
  }

  @Test
  public void return_license_when_expired() throws Exception {
    logInAsSystemAdministrator();
    addServerIdSettings(SERVER_ID_SAMPLE);
    addLicenseSetting(LICENSE_KEY_SAMPLE, LICENSE_NAME_SAMPLE,
      createBase64License(ORGANIZATION_SAMPLE, PRODUCT_SAMPLE, SERVER_ID_SAMPLE, "2010-01-01", TYPE_SAMPLE, Collections.emptyMap()));

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).hasSize(1);
    Licenses.License license = result.getLicenses(0);
    assertThat(license.getExpiration()).isEqualTo("2010-01-01");
    assertThat(license.getInvalidExpiration()).isTrue();
    assertThat(license.hasInvalidProduct()).isFalse();
    assertThat(license.hasInvalidServerId()).isFalse();
  }

  @Test
  public void none_license_type_settings_are_not_returned() throws Exception {
    logInAsSystemAdministrator();
    definitions.addComponent(PropertyDefinition.builder("foo").build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey("foo").setValue("value"));

    ListWsResponse result = executeRequest();

    assertThat(result.getLicensesList()).isEmpty();
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() throws Exception {
    userSession.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);

    executeRequest();
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isFalse();
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.params()).isEmpty();
  }

  private ListWsResponse executeRequest() {
    return ws.newRequest().executeProtobuf(ListWsResponse.class);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }

  private void addLicenseSetting(String key, @Nullable String name, String value) {
    definitions.addComponent(PropertyDefinition.builder(key).name(name).type(LICENSE).build());
    propertyDb.insertProperties(newGlobalPropertyDto().setKey(key).setValue(value));
  }

  private void addServerIdSettings(String serverId) {
    propertyDb.insertProperties(newGlobalPropertyDto().setKey(PERMANENT_SERVER_ID).setValue(serverId));
  }

  private static String toBase64(String data) {
    return Base64.encodeBase64String((data.getBytes(StandardCharsets.UTF_8)));
  }

  private static String createBase64License(@Nullable String organization, @Nullable String product, @Nullable String serverId, @Nullable String expirationDate,
    @Nullable String type, Map<String, String> additionalProperties) {
    StringBuilder data = new StringBuilder();
    data.append("Organisation: ").append(organization).append(" \n");
    data.append("Server: ").append(serverId).append(" \n");
    data.append("Product: ").append(product).append(" \n");
    data.append("Expiration: ").append(expirationDate).append(" \n");
    data.append("Type: ").append(type).append(" \n");
    for (Map.Entry<String, String> entry : additionalProperties.entrySet()) {
      data.append(entry.getKey()).append(": ").append(entry.getValue()).append(" \n");
    }
    return toBase64(data.toString());
  }

}
