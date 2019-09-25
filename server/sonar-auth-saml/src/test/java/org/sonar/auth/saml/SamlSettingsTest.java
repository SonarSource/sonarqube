/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.auth.saml;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class SamlSettingsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings(new PropertyDefinitions(SamlSettings.definitions()));

  private SamlSettings underTest = new SamlSettings(settings.asConfig());

  @Test
  public void return_application_id() {
    settings.setProperty("sonar.auth.saml.applicationId", "MyApp");

    assertThat(underTest.getApplicationId()).isEqualTo("MyApp");
  }

  @Test
  public void return_default_value_of_application_id() {
    assertThat(underTest.getApplicationId()).isEqualTo("sonarqube");
  }

  @Test
  public void return_provider_name() {
    settings.setProperty("sonar.auth.saml.providerName", "MyProviderName");

    assertThat(underTest.getProviderName()).isEqualTo("MyProviderName");
  }

  @Test
  public void return_default_value_of_application_name() {
    assertThat(underTest.getProviderName()).isEqualTo("SAML");
  }

  @Test
  public void return_provider_id() {
    settings.setProperty("sonar.auth.saml.applicationId", "http://localhost:8080/auth/realms/sonarqube");

    assertThat(underTest.getApplicationId()).isEqualTo("http://localhost:8080/auth/realms/sonarqube");
  }

  @Test
  public void return_login_url() {
    settings.setProperty("sonar.auth.saml.loginUrl", "http://localhost:8080/");
    assertThat(underTest.getLoginUrl()).isEqualTo("http://localhost:8080/");

    settings.setProperty("sonar.auth.saml.loginUrl", "http://localhost:8080");
    assertThat(underTest.getLoginUrl()).isEqualTo("http://localhost:8080");
  }

  @Test
  public void return_certificate() {
    settings.setProperty("sonar.auth.saml.certificate.secured", "ABCDEFG");

    assertThat(underTest.getCertificate()).isEqualTo("ABCDEFG");
  }

  @Test
  public void return_user_login_attribute() {
    settings.setProperty("sonar.auth.saml.user.login", "userLogin");

    assertThat(underTest.getUserLogin()).isEqualTo("userLogin");
  }

  @Test
  public void return_user_name_attribute() {
    settings.setProperty("sonar.auth.saml.user.name", "userName");

    assertThat(underTest.getUserName()).isEqualTo("userName");
  }

  @Test
  public void return_user_email_attribute() {
    settings.setProperty("sonar.auth.saml.user.email", "userEmail");

    assertThat(underTest.getUserEmail().get()).isEqualTo("userEmail");
  }

  @Test
  public void return_empty_user_email_when_no_setting() {
    assertThat(underTest.getUserEmail()).isNotPresent();
  }

  @Test
  public void return_group_name_attribute() {
    settings.setProperty("sonar.auth.saml.group.name", "groupName");

    assertThat(underTest.getGroupName().get()).isEqualTo("groupName");
  }

  @Test
  public void return_empty_group_name_when_no_setting() {
    assertThat(underTest.getGroupName()).isNotPresent();
  }

  @Test
  public void is_enabled() {
    settings.setProperty("sonar.auth.saml.applicationId", "MyApp");
    settings.setProperty("sonar.auth.saml.providerId", "http://localhost:8080/auth/realms/sonarqube");
    settings.setProperty("sonar.auth.saml.loginUrl", "http://localhost:8080/auth/realms/sonarqube/protocol/saml");
    settings.setProperty("sonar.auth.saml.certificate.secured", "ABCDEFG");
    settings.setProperty("sonar.auth.saml.user.login", "login");
    settings.setProperty("sonar.auth.saml.user.name", "name");

    settings.setProperty("sonar.auth.saml.enabled", true);
    assertThat(underTest.isEnabled()).isTrue();

    settings.setProperty("sonar.auth.saml.enabled", false);
    assertThat(underTest.isEnabled()).isFalse();
  }

  @Test
  public void is_enabled_using_default_values() {
    settings.setProperty("sonar.auth.saml.providerId", "http://localhost:8080/auth/realms/sonarqube");
    settings.setProperty("sonar.auth.saml.loginUrl", "http://localhost:8080/auth/realms/sonarqube/protocol/saml");
    settings.setProperty("sonar.auth.saml.certificate.secured", "ABCDEFG");
    settings.setProperty("sonar.auth.saml.user.login", "login");
    settings.setProperty("sonar.auth.saml.user.name", "name");
    settings.setProperty("sonar.auth.saml.enabled", true);
    assertThat(underTest.isEnabled()).isTrue();
  }

  @DataProvider
  public static Object[][] settingsRequiredToEnablePlugin() {
    return new Object[][] {
      {"sonar.auth.saml.providerId"},
      {"sonar.auth.saml.loginUrl"},
      {"sonar.auth.saml.certificate.secured"},
      {"sonar.auth.saml.user.login"},
      {"sonar.auth.saml.user.name"},
      {"sonar.auth.saml.enabled"},
    };
  }

  @Test
  @UseDataProvider("settingsRequiredToEnablePlugin")
  public void is_enabled_return_false_when_one_required_setting_is_missing(String setting) {
    initAllSettings();
    settings.setProperty(setting, (String) null);

    assertThat(underTest.isEnabled()).isFalse();
  }

  @Test
  public void fail_to_get_provider_id_when_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Provider ID is missing");

    underTest.getProviderId();
  }

  @Test
  public void fail_to_get_login_url_when_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Login URL is missing");

    underTest.getLoginUrl();
  }

  @Test
  public void fail_to_get_certificate_when_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Certificate is missing");

    underTest.getCertificate();
  }

  @Test
  public void fail_to_get_user_login_attribute_when_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("User login attribute is missing");

    underTest.getUserLogin();
  }

  @Test
  public void fail_to_get_user_name_attribute_when_null() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("User name attribute is missing");

    underTest.getUserName();
  }

  private void initAllSettings() {
    settings.setProperty("sonar.auth.saml.applicationId", "MyApp");
    settings.setProperty("sonar.auth.saml.providerId", "http://localhost:8080/auth/realms/sonarqube");
    settings.setProperty("sonar.auth.saml.loginUrl", "http://localhost:8080/auth/realms/sonarqube/protocol/saml");
    settings.setProperty("sonar.auth.saml.certificate.secured", "ABCDEFG");
    settings.setProperty("sonar.auth.saml.user.login", "login");
    settings.setProperty("sonar.auth.saml.user.name", "name");
    settings.setProperty("sonar.auth.saml.enabled", true);
  }

}
