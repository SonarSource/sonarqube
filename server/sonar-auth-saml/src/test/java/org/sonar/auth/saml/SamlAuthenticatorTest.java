/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.utils.System2;
import org.sonar.server.http.JavaxHttpRequest;
import org.sonar.server.http.JavaxHttpResponse;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SamlAuthenticatorTest {

  private MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, SamlSettings.definitions()));

  private SamlSettings samlSettings = new SamlSettings(settings.asConfig());

  private final SamlAuthenticator underTest = new SamlAuthenticator(samlSettings, mock(SamlMessageIdChecker.class));

  @Test
  public void authentication_status_with_errors_returned_when_init_fails() {
    HttpRequest request = new JavaxHttpRequest(mock(HttpServletRequest.class));
    HttpResponse response = new JavaxHttpResponse(mock(HttpServletResponse.class));
    when(request.getContextPath()).thenReturn("context");

    String authenticationStatus = underTest.getAuthenticationStatusPage(request, response);

    assertFalse(authenticationStatus.isEmpty());
  }

  @Test
  public void givenPrivateKeyIsNotPkcs8Encrypted_whenInitializingTheAuthentication_thenExceptionIsThrown() {
    initBasicSamlSettings();

    settings.setProperty("sonar.auth.saml.signature.enabled", true);
    settings.setProperty("sonar.auth.saml.sp.certificate.secured", "CERTIFICATE");
    settings.setProperty("sonar.auth.saml.sp.privateKey.secured", "Not a PKCS8 key");

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.initLogin("","", mock(JavaxHttpRequest.class), mock(JavaxHttpResponse.class)))
      .withMessage("Failed to create a SAML Auth")
      .havingCause()
      .withMessage("Error in parsing service provider private key, please make sure that it is in PKCS 8 format.");
  }

  @Test
  public void givenMissingSpCertificate_whenInitializingTheAuthentication_thenExceptionIsThrown() {
    initBasicSamlSettings();

    settings.setProperty("sonar.auth.saml.signature.enabled", true);
    settings.setProperty("sonar.auth.saml.sp.privateKey.secured", "PRIVATE_KEY");

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.initLogin("","", mock(JavaxHttpRequest.class), mock(JavaxHttpResponse.class)))
      .withMessage("Failed to create a SAML Auth")
      .havingCause()
      .withMessage("Service provider certificate is missing");
  }

  private void initBasicSamlSettings() {
    settings.setProperty("sonar.auth.saml.applicationId", "MyApp");
    settings.setProperty("sonar.auth.saml.providerId", "http://localhost:8080/auth/realms/sonarqube");
    settings.setProperty("sonar.auth.saml.loginUrl", "http://localhost:8080/auth/realms/sonarqube/protocol/saml");
    settings.setProperty("sonar.auth.saml.certificate.secured", "ABCDEFG");
    settings.setProperty("sonar.auth.saml.user.login", "login");
    settings.setProperty("sonar.auth.saml.user.name", "name");
    settings.setProperty("sonar.auth.saml.enabled", true);
  }

}
