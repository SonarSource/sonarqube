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

import com.onelogin.saml2.Auth;
import com.onelogin.saml2.settings.Saml2Settings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.auth.saml.SamlSettings.GROUP_NAME_ATTRIBUTE;
import static org.sonar.auth.saml.SamlSettings.USER_EMAIL_ATTRIBUTE;
import static org.sonar.auth.saml.SamlSettings.USER_LOGIN_ATTRIBUTE;
import static org.sonar.auth.saml.SamlSettings.USER_NAME_ATTRIBUTE;
import static org.sonar.auth.saml.SamlStatusChecker.getSamlAuthenticationStatus;

public class SamlStatusCheckerTest {

  private static final String IDP_CERTIFICATE = "-----BEGIN CERTIFICATE-----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-----END CERTIFICATE-----";
  private static final String SP_CERTIFICATE = "MIICoTCCAYkCBgGBXPscaDANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDDAlzb25hcnF1YmUwHhcNMjIwNjEzMTIxMTA5WhcNMzIwNjEzMTIxMjQ5WjAUMRIwEAYDVQQDDAlzb25hcnF1YmUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDSFoT371C0/klZuPgvKbGItkmTaf5CweNXL8u389d98aOXRpDQ7maTXdV/W+VcL8vUWg8yG6nn8CRwweYnGTNdn9UAdhgknvxQe3pq3EwOJyls4Fpiq6YTh+DQfiZUQizjFjDOr/GG5O2lNvTRkI4XZj/XnWjRqVZwttiA5tm1sKkvGdyOQljwn4Jja/VbITdV8GASumx66Bil/wamSsqIzm2RjsOOGSsf5VjYUPwDobpuSf+j4DLtWjem/9vIzI2wcE30uC8LBAgO3JAlIS9NQrchjS9xhMJRohOoitaSPmqsOy7D2BH0h7XX6TNgv/WYTkBY4eZPao3PsL2A6AmhAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAMBmTHUK4w+DX21tmhqdwq0WqLH5ZAkwtiocDxFXiJ4GRrUWUh3BaXsgOHB8YYnNTDfScjaU0sZMEyfC0su1zsN8B7NFckg7RcZCHuBYdgIEAmvK4YM6s6zNsiKKwt66p2MNeL+o0acrT2rYjQ1L5QDj0gpfJQAT4N7xTZfuSc2iwjotaQfvcgsO8EZlcDVrL4UuyWLbuRUlSQjxHWGYaxCW+I3enK1+8fGpF3O+k9ZQ8xt5nJsalpsZvHcPLA4IBOmjsSHqSkhg4EIAWL/sJZ1KNct4hHh5kToUTu+Q6e949VeBkWgj4O+rcGDgiN2frGiEEc0EMv8KCSENRRRrO2k=";
  private static final String SP_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDSFoT371C0/klZuPgvKbGItkmTaf5CweNXL8u389d98aOXRpDQ7maTXdV/W+VcL8vUWg8yG6nn8CRwweYnGTNdn9UAdhgknvxQe3pq3EwOJyls4Fpiq6YTh+DQfiZUQizjFjDOr/GG5O2lNvTRkI4XZj/XnWjRqVZwttiA5tm1sKkvGdyOQljwn4Jja/VbITdV8GASumx66Bil/wamSsqIzm2RjsOOGSsf5VjYUPwDobpuSf+j4DLtWjem/9vIzI2wcE30uC8LBAgO3JAlIS9NQrchjS9xhMJRohOoitaSPmqsOy7D2BH0h7XX6TNgv/WYTkBY4eZPao3PsL2A6AmhAgMBAAECggEBAJj11HJAR96/leBBkFGmZaBIOGGgNoOcb023evfADhGgsZ8evamhKgX5t8w2uFPaaOl/eLje82Hvslh2lH+7FW8BRDBFy2Y+ay6d+I99PdLAKKUg5C4bE5v8vm6OqpGGbPAZ5AdYit3QKEa2MKG0QgA/bhQqg3rDdDA0sIWJjtF9MLv7LI7Tm0qgiHOKsI0MEBFk+ZoibgKWYh/dnfGDRWyC3Puqe13rdSheNJYUDR/0QMkd/EJNpLWv06uk+w8w2lU4RgN6TiV76ZZUIGZAAHFgMELJysgtBTCkOQY5roPu17OmMZjKfxngeIfNyd42q3/T6DmUbbwNYfP2HRMoiMECgYEA6SVc1mZ4ykytC9M61rZwT+2zXtJKudQVa0qpTtkf0aznRmnDOuc1bL7ewKIIIp9r5HKVteO6SKgpHmrP+qmvbwZ0Pz51Zg0MetoSmT9m0599/tOU2k6OI09dvQ4Xa3ccN5Czl61Q/HkMeAIDny8MrhGVBwhallE4J4fm/OjuVK0CgYEA5q6IVgqZtfcV1azIF6uOFt6blfn142zrwq0fF39jog2f+4jXaBKw6L4aP0HvIL83UArGppYY31894bLb6YL4EjS2JNbABM2VnJpJd4oGopOE42GCZlZRpf751zOptYAN23NFSujLlfaUfMbyrqIbRFC2DCdzNTU50GT5SAXX80UCgYEAlyvQvHwJCjMZaTd3SU1WGZ1o1qzIIyHvGXh5u1Rxm0TfWPquyfys2WwRhxoI6FoyXRgnFp8oZIAU2VIstL1dsUGgEnnvKVKAqw/HS3Keu80IpziNpdeVtjN59mGysc2zkBvVNx38Cxh6Cz5TFt4s/JkN5ld2VU0oeglWrtph3qkCgYALszZ/BrKdJBcba1QKv0zJpCjIBpGOI2whx54YFwH6qi4/F8W1JZ2LcHjsVG/IfWpUyPciY+KHEdGVrPiyc04Zvkquu6WpmLPJ6ZloUrvbaxgGYF+4yRADF1ecrqYg6onJY6NUFVKeHI+TdJPCf75aTK2vGCEjxbtU8ooiOQmm8QKBgEGe9ZdrwTP9rMQ35jYtzU+dT06k1r9BE9Q8CmrXl0HwK717ZWboX4J0YoFjxZC8PDsMl3p46MJ83rKbLU728uKig1AkZo7/OedxTWvezjZ1+lDyjC2EguXbgY1ecSC2HbJh9g+v8RUuhWxuA7RYoW92xVtKj+6l4vMadVP4Myp8-----END PRIVATE KEY-----";
  public static final String ENCRYPTED_SAML_RESPONSE = """
    <samlp:Response xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion" xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                    Destination="https:/localhost:8080/oauth2/callback/saml" ID="ID_3f1d5c29-691c-41b6-a9d4-c5ed2edc75bc"
                    InResponseTo="ONELOGIN_3cb68484-6883-4bfe-926a-88b705312eae" IssueInstant="2023-11-02T10:40:12.085Z" Version="2.0">
    <saml:EncryptedAssertion> Assertion Very Encrypted
    </saml:EncryptedAssertion>
    </samlp:Response>
    """;
  public static final String PLAIN_SAML_RESPONSE = """
    <samlp:Response xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion" xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                    Destination="https:/localhost:8080/oauth2/callback/saml" ID="ID_3f1d5c29-691c-41b6-a9d4-c5ed2edc75bc"
                    InResponseTo="ONELOGIN_3cb68484-6883-4bfe-926a-88b705312eae" IssueInstant="2023-11-02T10:40:12.085Z" Version="2.0">
    <saml:Assertion Assertion
    </saml:Assertion>
    </samlp:Response>
    """;
  public static final String BASE64_ENCRYPTED_SAML_RESPONSE = new String(Base64.getEncoder().encode(ENCRYPTED_SAML_RESPONSE.getBytes()), StandardCharsets.UTF_8);
  public static final String BASE64_SAML_RESPONSE = new String(Base64.getEncoder().encode(PLAIN_SAML_RESPONSE.getBytes()), StandardCharsets.UTF_8);

  private final MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, SamlSettings.definitions()));

  private final Auth auth = mock(Auth.class);

  private SamlAuthenticationStatus samlAuthenticationStatus;

  @Before
  public void setUp() {
    when(auth.getErrors()).thenReturn(new ArrayList<>());
    when(auth.getSettings()).thenReturn(new Saml2Settings());
    when(auth.getAttributes()).thenReturn(getResponseAttributes());
  }

  @Test
  public void authentication_status_has_errors_when_no_idp_certificate_is_provided() {
    samlAuthenticationStatus = getSamlAuthenticationStatus("error message");

    assertEquals("error", samlAuthenticationStatus.getStatus());
    assertFalse(samlAuthenticationStatus.getErrors().isEmpty());
    assertEquals("error message", samlAuthenticationStatus.getErrors().get(0));
  }

  @Test
  public void authentication_status_is_success_when_no_errors() {
    setSettings();

    getResponseAttributes().forEach((key, value) -> when(auth.getAttribute(key)).thenReturn(value));

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertEquals("success", samlAuthenticationStatus.getStatus());
    assertTrue(samlAuthenticationStatus.getErrors().isEmpty());
  }

  @Test
  public void authentication_status_is_unsuccessful_when_errors_are_reported() {
    setSettings();
    when(auth.getErrors()).thenReturn(Collections.singletonList("Error in Authentication"));
    when(auth.getLastErrorReason()).thenReturn("Authentication failed due to a missing parameter.");
    when(auth.getAttributes()).thenReturn(getEmptyAttributes());

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertEquals("error", samlAuthenticationStatus.getStatus());
    assertFalse(samlAuthenticationStatus.getErrors().isEmpty());
    assertEquals(2, samlAuthenticationStatus.getErrors().size());
    assertTrue(samlAuthenticationStatus.getErrors().contains("Authentication failed due to a missing parameter."));
    assertTrue(samlAuthenticationStatus.getErrors().contains("Error in Authentication"));
  }

  @Test
  public void authentication_status_is_unsuccessful_when_processResponse_throws_exception() {
    setSettings();
    try {
      doThrow(new Exception("Exception when processing the response")).when(auth).processResponse();
    } catch (Exception e) {
      e.printStackTrace();
    }
    when(auth.getAttributes()).thenReturn(getEmptyAttributes());

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertEquals("error", samlAuthenticationStatus.getStatus());
    assertFalse(samlAuthenticationStatus.getErrors().isEmpty());
    assertEquals(1, samlAuthenticationStatus.getErrors().size());
    assertTrue(samlAuthenticationStatus.getErrors().contains("Exception when processing the response"));
  }

  @Test
  public void authentication_has_warnings_when_optional_mappings_are_not_correct() {
    setSettings();
    settings.setProperty(GROUP_NAME_ATTRIBUTE, "wrongGroupField");
    settings.setProperty(USER_EMAIL_ATTRIBUTE, "wrongEmailField");
    settings.setProperty("sonar.auth.saml.sp.privateKey.secured", (String) null);

    getResponseAttributes().forEach((key, value) -> when(auth.getAttribute(key)).thenReturn(value));

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertEquals("success", samlAuthenticationStatus.getStatus());
    assertTrue(samlAuthenticationStatus.getErrors().isEmpty());
    assertEquals(2, samlAuthenticationStatus.getWarnings().size());
    assertTrue(samlAuthenticationStatus.getWarnings()
      .contains(String.format("Mapping not found for the property %s, the field %s is not available in the SAML response.", GROUP_NAME_ATTRIBUTE, "wrongGroupField")));
    assertTrue(samlAuthenticationStatus.getWarnings()
      .contains(String.format("Mapping not found for the property %s, the field %s is not available in the SAML response.", USER_EMAIL_ATTRIBUTE, "wrongEmailField")));
  }

  @Test
  public void authentication_has_errors_when_login_and_name_mappings_are_not_correct() {
    setSettings();
    settings.setProperty(USER_LOGIN_ATTRIBUTE, "wrongLoginField");
    settings.setProperty(USER_NAME_ATTRIBUTE, "wrongNameField");
    getResponseAttributes().forEach((key, value) -> when(auth.getAttribute(key)).thenReturn(value));

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertEquals("error", samlAuthenticationStatus.getStatus());
    assertTrue(samlAuthenticationStatus.getWarnings().isEmpty());
    assertEquals(2, samlAuthenticationStatus.getErrors().size());
    assertTrue(samlAuthenticationStatus.getErrors()
      .contains(String.format("Mapping not found for the property %s, the field %s is not available in the SAML response.", USER_LOGIN_ATTRIBUTE, "wrongLoginField")));
    assertTrue(samlAuthenticationStatus.getErrors()
      .contains(String.format("Mapping not found for the property %s, the field %s is not available in the SAML response.", USER_NAME_ATTRIBUTE, "wrongNameField")));
  }

  @Test
  public void authentication_has_errors_when_login_and_name_are_empty() {
    setSettings();
    when(auth.getAttributes()).thenReturn(getEmptyAttributes());
    getEmptyAttributes().forEach((key, value) -> when(auth.getAttribute(key)).thenReturn(value));

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertEquals("error", samlAuthenticationStatus.getStatus());
    assertTrue(samlAuthenticationStatus.getWarnings().isEmpty());
    assertEquals(2, samlAuthenticationStatus.getErrors().size());
    assertTrue(samlAuthenticationStatus.getErrors()
      .contains(String.format("Mapping found for the property %s, but the field %s is empty in the SAML response.", USER_LOGIN_ATTRIBUTE, "login")));
    assertTrue(samlAuthenticationStatus.getErrors()
      .contains(String.format("Mapping found for the property %s, but the field %s is empty in the SAML response.", USER_NAME_ATTRIBUTE, "name")));
  }

  @Test
  public void authentication_has_no_warnings_when_optional_mappings_are_not_provided() {
    setSettings();
    settings.setProperty("sonar.auth.saml.sp.privateKey.secured", (String) null);
    settings.setProperty(USER_EMAIL_ATTRIBUTE, (String) null);
    settings.setProperty(GROUP_NAME_ATTRIBUTE, (String) null);
    getResponseAttributes().forEach((key, value) -> when(auth.getAttribute(key)).thenReturn(value));

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertEquals("success", samlAuthenticationStatus.getStatus());
    assertTrue(samlAuthenticationStatus.getErrors().isEmpty());
    assertTrue(samlAuthenticationStatus.getWarnings().isEmpty());
  }

  @Test
  public void authentication_has_warnings_when_the_private_key_is_invalid_but_auth_completes() {
    setSettings();
    getResponseAttributes().forEach((key, value) -> when(auth.getAttribute(key)).thenReturn(value));

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertEquals("success", samlAuthenticationStatus.getStatus());
    assertTrue(samlAuthenticationStatus.getErrors().isEmpty());
    assertFalse(samlAuthenticationStatus.getWarnings().isEmpty());
    assertTrue(samlAuthenticationStatus.getWarnings()
      .contains(String.format("Error in parsing service provider private key, please make sure that it is in PKCS 8 format.")));
  }

  @Test
  public void mapped_attributes_are_complete_when_mapping_fields_are_correct() {
    setSettings();
    settings.setProperty("sonar.auth.saml.sp.privateKey.secured", (String) null);
    getResponseAttributes().forEach((key, value) -> when(auth.getAttribute(key)).thenReturn(value));

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertEquals("success", samlAuthenticationStatus.getStatus());
    assertTrue(samlAuthenticationStatus.getErrors().isEmpty());
    assertTrue(samlAuthenticationStatus.getWarnings().isEmpty());
    assertEquals(4, samlAuthenticationStatus.getAvailableAttributes().size());
    assertEquals(4, samlAuthenticationStatus.getMappedAttributes().size());

    assertTrue(samlAuthenticationStatus.getAvailableAttributes().keySet().containsAll(Set.of("login", "name", "email", "groups")));
    assertTrue(samlAuthenticationStatus.getMappedAttributes().keySet().containsAll(Set.of("User login value", "User name value", "User email value", "Groups value")));
  }

  @Test
  public void givenSignatureEnabled_whenUserIsAuthenticated_thenSamlStatusReportsItEnabled() {
    setSettings();
    settings.setProperty("sonar.auth.saml.signature.enabled", true);
    when(auth.isAuthenticated()).thenReturn(true);

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertTrue(samlAuthenticationStatus.isSignatureEnabled());
  }

  @Test
  public void givenSignatureDisabled_whenUserIsAuthenticated_thenSamlStatusReportsItDisabled() {
    setSettings();
    settings.setProperty("sonar.auth.saml.signature.enabled", false);
    when(auth.isAuthenticated()).thenReturn(true);

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertFalse(samlAuthenticationStatus.isSignatureEnabled());
  }

  @Test
  public void givenEncryptionEnabled_whenUserIsAuthenticated_thenSamlStatusReportsItEnabled() {
    setSettings();
    when(auth.isAuthenticated()).thenReturn(true);

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_ENCRYPTED_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertTrue(samlAuthenticationStatus.isEncryptionEnabled());
  }

  @Test
  public void givenEncryptionDisabled_whenUserIsAuthenticated_thenSamlStatusReportsItDisabled() {
    setSettings();
    when(auth.isAuthenticated()).thenReturn(true);

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertFalse(samlAuthenticationStatus.isEncryptionEnabled());
  }

  @Test
  public void whenUserIsNotAuthenticated_thenBothSignatureAndEncryptionAreReportedDisabled() {
    setSettings();
    when(auth.isAuthenticated()).thenReturn(false);
    settings.setProperty("sonar.auth.saml.signature.enabled", true);

    samlAuthenticationStatus = getSamlAuthenticationStatus(BASE64_ENCRYPTED_SAML_RESPONSE, auth, new SamlSettings(settings.asConfig()));

    assertFalse(samlAuthenticationStatus.isEncryptionEnabled());
    assertFalse(samlAuthenticationStatus.isSignatureEnabled());
  }

  @Test
  public void whenSamlResponseIsNull_thenEncryptionIsReportedDisabled() {
    setSettings();

    samlAuthenticationStatus = getSamlAuthenticationStatus(null, auth, new SamlSettings(settings.asConfig()));

    assertFalse(samlAuthenticationStatus.isEncryptionEnabled());
  }

  private void setSettings() {
    settings.setProperty("sonar.auth.saml.applicationId", "MyApp");
    settings.setProperty("sonar.auth.saml.providerId", "http://localhost:8080/auth/realms/sonarqube");
    settings.setProperty("sonar.auth.saml.loginUrl", "http://localhost:8080/auth/realms/sonarqube/protocol/saml");
    settings.setProperty("sonar.auth.saml.certificate.secured", IDP_CERTIFICATE);
    settings.setProperty("sonar.auth.saml.sp.privateKey.secured", SP_PRIVATE_KEY);
    settings.setProperty("sonar.auth.saml.sp.certificate.secured", SP_CERTIFICATE);
    settings.setProperty("sonar.auth.saml.user.login", "login");
    settings.setProperty("sonar.auth.saml.user.name", "name");
    settings.setProperty("sonar.auth.saml.user.email", "email");
    settings.setProperty("sonar.auth.saml.group.name", "groups");
  }

  private Map<String, List<String>> getResponseAttributes() {
    return Map.of(
      "login", Collections.singletonList("loginId"),
      "name", Collections.singletonList("userName"),
      "email", Collections.singletonList("user@sonar.com"),
      "groups", List.of("group1", "group2"));
  }

  private Map<String, List<String>> getEmptyAttributes() {
    return Map.of(
      "login", Collections.singletonList(""),
      "name", Collections.singletonList(""),
      "email", Collections.singletonList(""),
      "groups", Collections.singletonList(""));
  }

}
