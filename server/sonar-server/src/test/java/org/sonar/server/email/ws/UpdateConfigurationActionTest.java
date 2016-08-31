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

package org.sonar.server.email.ws;

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.property.PropertyTesting.newGlobalPropertyDto;

public class UpdateConfigurationActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private PropertyDbTester propertyDb = new PropertyDbTester(db);
  private Settings settings = new MapSettings();

  private WsActionTester ws = new WsActionTester(new UpdateConfigurationAction(dbClient, userSession, settings));

  @Test
  public void update_email_settings() throws Exception {
    setUserAsSystemAdmin();

    executeRequest("smtp.gmail.com", 25, "starttls", "john", "12345", "noreply@email.com", "[EMAIL]");

    assertSetting("email.smtp_host.secured", "smtp.gmail.com");
    assertSetting("email.smtp_port.secured", "25");
    assertSetting("email.smtp_secure_connection.secured", "starttls");
    assertSetting("email.smtp_username.secured", "john");
    assertSetting("email.smtp_password.secured", "12345");
    assertSetting("email.from", "noreply@email.com");
    assertSetting("email.prefix", "[EMAIL]");
  }

  @Test
  public void does_not_save_settings_when_no_value_sent() throws Exception {
    setUserAsSystemAdmin();

    executeRequest(null, null, null, null, null, null, null);

    assertThat(db.countRowsOfTable(db.getSession(), "properties")).isZero();
  }

  @Test
  public void remove_existing_settings_when_no_value_sent() throws Exception {
    setUserAsSystemAdmin();
    addSetting("email.smtp_host.secured", "smtp.gmail.com");
    addSetting("email.smtp_port.secured", "25");
    addSetting("email.smtp_secure_connection.secured", "starttls");
    addSetting("email.smtp_username.secured", "john");
    addSetting("email.smtp_password.secured", "12345");
    addSetting("email.from", "noreply@email.com");
    addSetting("email.prefix", "[EMAIL]");

    executeRequest(null, null, null, null, null, null, null);

    assertThat(db.countRowsOfTable(db.getSession(), "properties")).isZero();
  }

  @Test
  public void fail_when_secure_param_is_invalid() {
    setUserAsSystemAdmin();

    expectedException.expect(IllegalArgumentException.class);

    executeRequest(null, null, "unknown", null, null, null, null);
  }

  @Test
  public void fail_when_insufficient_privileges() {
    userSession.anonymous().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
    expectedException.expect(ForbiddenException.class);

    ws.newRequest().execute();
  }

  @Test
  public void fail_when_parameter_is_empty() {
    setUserAsSystemAdmin();
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Parameter 'host' cannot have an empty value");

    executeRequest("", null, null, null, null, null, null);
  }

  @Test
  public void test_ws_definition() {
    WebService.Action action = ws.getDef();
    assertThat(action).isNotNull();
    assertThat(action.isInternal()).isTrue();
    assertThat(action.isPost()).isTrue();
    assertThat(action.responseExampleAsString()).isNull();
    assertThat(action.params()).hasSize(7);
  }

  private void assertSetting(String key, String value) {
    PropertyDto result = dbClient.propertiesDao().selectGlobalProperty(key);

    assertThat(result)
      .extracting(PropertyDto::getKey, PropertyDto::getValue, PropertyDto::getResourceId)
      .containsExactly(key, value, null);

    assertThat(settings.getString(key)).isEqualTo(value);
  }

  private void executeRequest(@Nullable String host, @Nullable Integer port, @Nullable String secure,
    @Nullable String username, @Nullable String password, @Nullable String from, @Nullable String prefix) {
    TestRequest request = ws.newRequest();
    if (host != null) {
      request.setParam("host", host);
    }
    if (port != null) {
      request.setParam("port", Integer.toString(port));
    }
    if (secure != null) {
      request.setParam("secure", secure);
    }
    if (username != null) {
      request.setParam("username", username);
    }
    if (password != null) {
      request.setParam("password", password);
    }
    if (from != null) {
      request.setParam("from", from);
    }
    if (prefix != null) {
      request.setParam("prefix", prefix);
    }
    request.execute();
  }

  private void addSetting(String key, String value) {
    propertyDb.insertProperty(newGlobalPropertyDto(key, value));
    settings.setProperty(key, value);
  }

  private void setUserAsSystemAdmin() {
    userSession.login("admin").setGlobalPermissions(SYSTEM_ADMIN);
  }

}
