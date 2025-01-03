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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.http.JakartaHttpRequest;
import org.sonar.server.http.JakartaHttpResponse;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamlIdentityProviderIT {
  private static final String SQ_CALLBACK_URL = "http://localhost:9000/oauth2/callback/saml";

  /** IDP private key (keep here for future tests with signature)
  -----BEGIN PRIVATE KEY-----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-----END PRIVATE KEY-----
   */

  private static final String IDP_CERTIFICATE = "-----BEGIN CERTIFICATE-----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-----END CERTIFICATE-----";

  //Certificate valid until June 13, 2032
  private static final String SP_CERTIFICATE = "MIICoTCCAYkCBgGBXPscaDANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDDAlzb25hcnF1YmUwHhcNMjIwNjEzMTIxMTA5WhcNMzIwNjEzMTIxMjQ5WjAUMRIwEAYDVQQDDAlzb25hcnF1YmUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDSFoT371C0/klZuPgvKbGItkmTaf5CweNXL8u389d98aOXRpDQ7maTXdV/W+VcL8vUWg8yG6nn8CRwweYnGTNdn9UAdhgknvxQe3pq3EwOJyls4Fpiq6YTh+DQfiZUQizjFjDOr/GG5O2lNvTRkI4XZj/XnWjRqVZwttiA5tm1sKkvGdyOQljwn4Jja/VbITdV8GASumx66Bil/wamSsqIzm2RjsOOGSsf5VjYUPwDobpuSf+j4DLtWjem/9vIzI2wcE30uC8LBAgO3JAlIS9NQrchjS9xhMJRohOoitaSPmqsOy7D2BH0h7XX6TNgv/WYTkBY4eZPao3PsL2A6AmhAgMBAAEwDQYJKoZIhvcNAQELBQADggEBAMBmTHUK4w+DX21tmhqdwq0WqLH5ZAkwtiocDxFXiJ4GRrUWUh3BaXsgOHB8YYnNTDfScjaU0sZMEyfC0su1zsN8B7NFckg7RcZCHuBYdgIEAmvK4YM6s6zNsiKKwt66p2MNeL+o0acrT2rYjQ1L5QDj0gpfJQAT4N7xTZfuSc2iwjotaQfvcgsO8EZlcDVrL4UuyWLbuRUlSQjxHWGYaxCW+I3enK1+8fGpF3O+k9ZQ8xt5nJsalpsZvHcPLA4IBOmjsSHqSkhg4EIAWL/sJZ1KNct4hHh5kToUTu+Q6e949VeBkWgj4O+rcGDgiN2frGiEEc0EMv8KCSENRRRrO2k=";
  private static final String SP_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDSFoT371C0/klZuPgvKbGItkmTaf5CweNXL8u389d98aOXRpDQ7maTXdV/W+VcL8vUWg8yG6nn8CRwweYnGTNdn9UAdhgknvxQe3pq3EwOJyls4Fpiq6YTh+DQfiZUQizjFjDOr/GG5O2lNvTRkI4XZj/XnWjRqVZwttiA5tm1sKkvGdyOQljwn4Jja/VbITdV8GASumx66Bil/wamSsqIzm2RjsOOGSsf5VjYUPwDobpuSf+j4DLtWjem/9vIzI2wcE30uC8LBAgO3JAlIS9NQrchjS9xhMJRohOoitaSPmqsOy7D2BH0h7XX6TNgv/WYTkBY4eZPao3PsL2A6AmhAgMBAAECggEBAJj11HJAR96/leBBkFGmZaBIOGGgNoOcb023evfADhGgsZ8evamhKgX5t8w2uFPaaOl/eLje82Hvslh2lH+7FW8BRDBFy2Y+ay6d+I99PdLAKKUg5C4bE5v8vm6OqpGGbPAZ5AdYit3QKEa2MKG0QgA/bhQqg3rDdDA0sIWJjtF9MLv7LI7Tm0qgiHOKsI0MEBFk+ZoibgKWYh/dnfGDRWyC3Puqe13rdSheNJYUDR/0QMkd/EJNpLWv06uk+w8w2lU4RgN6TiV76ZZUIGZAAHFgMELJysgtBTCkOQY5roPu17OmMZjKfxngeIfNyd42q3/T6DmUbbwNYfP2HRMoiMECgYEA6SVc1mZ4ykytC9M61rZwT+2zXtJKudQVa0qpTtkf0aznRmnDOuc1bL7ewKIIIp9r5HKVteO6SKgpHmrP+qmvbwZ0Pz51Zg0MetoSmT9m0599/tOU2k6OI09dvQ4Xa3ccN5Czl61Q/HkMeAIDny8MrhGVBwhallE4J4fm/OjuVK0CgYEA5q6IVgqZtfcV1azIF6uOFt6blfn142zrwq0fF39jog2f+4jXaBKw6L4aP0HvIL83UArGppYY31894bLb6YL4EjS2JNbABM2VnJpJd4oGopOE42GCZlZRpf751zOptYAN23NFSujLlfaUfMbyrqIbRFC2DCdzNTU50GT5SAXX80UCgYEAlyvQvHwJCjMZaTd3SU1WGZ1o1qzIIyHvGXh5u1Rxm0TfWPquyfys2WwRhxoI6FoyXRgnFp8oZIAU2VIstL1dsUGgEnnvKVKAqw/HS3Keu80IpziNpdeVtjN59mGysc2zkBvVNx38Cxh6Cz5TFt4s/JkN5ld2VU0oeglWrtph3qkCgYALszZ/BrKdJBcba1QKv0zJpCjIBpGOI2whx54YFwH6qi4/F8W1JZ2LcHjsVG/IfWpUyPciY+KHEdGVrPiyc04Zvkquu6WpmLPJ6ZloUrvbaxgGYF+4yRADF1ecrqYg6onJY6NUFVKeHI+TdJPCf75aTK2vGCEjxbtU8ooiOQmm8QKBgEGe9ZdrwTP9rMQ35jYtzU+dT06k1r9BE9Q8CmrXl0HwK717ZWboX4J0YoFjxZC8PDsMl3p46MJ83rKbLU728uKig1AkZo7/OedxTWvezjZ1+lDyjC2EguXbgY1ecSC2HbJh9g+v8RUuhWxuA7RYoW92xVtKj+6l4vMadVP4Myp8-----END PRIVATE KEY-----";

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public LogTester log = new LogTester();

  private static MockedStatic<Instant> instantMockedStatic;

  private final MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, SamlSettings.definitions()));
  private final SamlSettings samlSettings = new SamlSettings(settings.asConfig());

  SamlCertificateConverter samlCertificateConverter = new SamlCertificateConverter();
  SamlPrivateKeyConverter samlPrivateKeyConverter = new SamlPrivateKeyConverter();
  private final RelyingPartyRegistrationRepositoryProvider relyingPartyRegistrationRepositoryProvider =
    new RelyingPartyRegistrationRepositoryProvider(samlSettings, samlCertificateConverter, samlPrivateKeyConverter);
  private final RedirectToUrlProvider redirectToUrlProvider = new RedirectToUrlProvider(relyingPartyRegistrationRepositoryProvider);

  private final Clock clock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());
  private final SamlMessageIdChecker samlMessageIdChecker = new SamlMessageIdChecker(db.getDbClient(), clock);
  SonarqubeSaml2ResponseValidator sonarqubeSaml2ResponseValidator = new SonarqubeSaml2ResponseValidator(samlMessageIdChecker);

  private final SamlConfiguration samlConfiguration = new SamlConfiguration();
  private final OpenSaml4AuthenticationProvider openSaml4AuthenticationProvider = samlConfiguration.openSaml4AuthenticationProvider(sonarqubeSaml2ResponseValidator);
  private final SamlResponseAuthenticator samlResponseAuthenticator = new SamlResponseAuthenticator(openSaml4AuthenticationProvider, relyingPartyRegistrationRepositoryProvider);

  private final PrincipalToUserIdentityConverter principalToUserIdentityConverter = new PrincipalToUserIdentityConverter(samlSettings);

  private final SamlStatusChecker samlStatusChecker = new SamlStatusChecker(samlSettings);
  private final SamlAuthStatusPageGenerator samlAuthStatusPageGenerator = new SamlAuthStatusPageGenerator();
  private final SamlAuthenticator samlAuthenticator = new SamlAuthenticator(redirectToUrlProvider, samlResponseAuthenticator, principalToUserIdentityConverter, samlStatusChecker, samlAuthStatusPageGenerator);
  private final SamlIdentityProvider underTest = new SamlIdentityProvider(samlSettings, samlAuthenticator);

  private HttpServletResponse response = mock(HttpServletResponse.class);
  private HttpServletRequest request;

  private HttpServletRequest createHttpRequest() {
    HttpServletRequest mockRequest = mock();
    when(mockRequest.getScheme()).thenReturn("https");
    when(mockRequest.getServerName()).thenReturn("localhost");
    when(mockRequest.getServerPort()).thenReturn(9000);
    when(mockRequest.getRequestURI()).thenReturn("/oauth2/callback/saml");
    when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(SQ_CALLBACK_URL));
    return mockRequest;
  }

  @Before
  public void setup() {
    request = createHttpRequest();
    response = mock(HttpServletResponse.class);
    setInstanceTime("2020-06-05T23:10:28.438Z");
  }

  private void setInstanceTime(String time) {
    if (instantMockedStatic != null) {
      instantMockedStatic.close();
    }
    Clock fixedClock = Clock.fixed(Instant.parse(time), ZoneId.of("UTC"));
    var mockedInstant = Instant.now(fixedClock);
    instantMockedStatic = mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS);
    instantMockedStatic.when(Instant::now).thenReturn(mockedInstant);
  }


  @Test
  public void check_fields() {
    setSettings(true);
    assertThat(underTest.getKey()).isEqualTo("saml");
    assertThat(underTest.getName()).isEqualTo("SAML");
    assertThat(underTest.getDisplay().getIconPath()).isEqualTo("/images/saml.png");
    assertThat(underTest.getDisplay().getBackgroundColor()).isEqualTo("#444444");
    assertThat(underTest.allowsUsersToSignUp()).isTrue();
  }

  @Test
  public void provider_name_is_provided_by_setting() {
    // Default value
    assertThat(underTest.getName()).isEqualTo("SAML");

    settings.setProperty("sonar.auth.saml.providerName", "My Provider");
    assertThat(underTest.getName()).isEqualTo("My Provider");
  }

  @Test
  public void is_enabled() {
    setSettings(true);
    assertThat(underTest.isEnabled()).isTrue();

    setSettings(false);
    assertThat(underTest.isEnabled()).isFalse();
  }

  @Test
  public void init() throws IOException {
    setSettings(true);
    DumbInitContext context = new DumbInitContext();

    underTest.init(context);

    verify(context.response).sendRedirect(anyString());
    assertThat(context.generateCsrfState.get()).isTrue();
  }

  @Test
  public void fail_to_init_when_login_url_is_invalid() {
    setSettings(true);
    settings.setProperty("sonar.auth.saml.loginUrl", "invalid");
    DumbInitContext context = new DumbInitContext();

    assertThatThrownBy(() -> underTest.init(context))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Invalid SAML Login URL");
  }

  @Test
  public void callback() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_full_response.txt", SQ_CALLBACK_URL);

    underTest.callback(callbackContext);

    assertThat(callbackContext.redirectedToRequestedPage.get()).isTrue();
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("johndoe");
    assertThat(callbackContext.verifyState.get()).isTrue();
  }

  @Test
  public void callback_on_full_response() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_full_response.txt", SQ_CALLBACK_URL);

    underTest.callback(callbackContext);

    assertThat(callbackContext.userIdentity.getName()).isEqualTo("John Doe");
    assertThat(callbackContext.userIdentity.getEmail()).isEqualTo("johndoe@email.com");
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("johndoe");
    assertThat(callbackContext.userIdentity.getGroups()).containsExactlyInAnyOrder("developer", "product-manager");
  }

  @Test
  @Ignore("Test is broken. The feature was tested on a real instance and throught end to end tests and it is working. SONAR-24058.")
  public void callback_on_encrypted_response() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_encrypted_response.txt", SQ_CALLBACK_URL);

    underTest.callback(callbackContext);

    assertThat(callbackContext.userIdentity.getName()).isEqualTo("John Doe");
    assertThat(callbackContext.userIdentity.getEmail()).isEqualTo("johndoe@email.com");
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("johndoe");
    assertThat(callbackContext.userIdentity.getGroups()).containsExactlyInAnyOrder("developer", "product-manager");
  }

  @Test
  public void callback_on_signed_request() throws IOException {
    setSettings(true);
    settings.setProperty("sonar.auth.saml.signature.enabled", true);
    DumbInitContext context = new DumbInitContext();

    underTest.init(context);

    String expectedUrl = "http://localhost:8080/auth/realms/sonarqube/protocol/saml";
    verify(context.response).sendRedirect(argThat(url ->
      url.startsWith(expectedUrl) &&
        url.contains("SigAlg=http%3A%2F%2Fwww.w3.org%2F2001%2F04%2Fxmldsig-more%23rsa-sha256") &&
        url.contains("SAMLRequest=") &&
        url.contains("Signature=")
    ));
  }

  @Test
  public void callback_on_minimal_response() {
    setSettings(true);
    setInstanceTime("2020-06-05T23:02:28.438Z");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_minimal_response.txt", SQ_CALLBACK_URL);

    underTest.callback(callbackContext);

    assertThat(callbackContext.userIdentity.getName()).isEqualTo("John Doe");
    assertThat(callbackContext.userIdentity.getEmail()).isNull();
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("johndoe");
    assertThat(callbackContext.userIdentity.getGroups()).isEmpty();
  }

  @Test
  public void callback_givenPrivateKeyIsNotPkcs8_ShouldThrowlog_clear_error_when_private_key_is_not_pkcs8() {
    String wrongFormatPrivateKey = "MIIEpAIBAAKCAQEA0haE9+9QtP5JWbj4LymxiLZJk2n+QsHjVy/Lt/PXffGjl0aQ0O5mk13Vf1vlXC/L1FoPMhup5/AkcMHmJxkzXZ/VAHYYJJ78UHt6atxMDicpbOBaYqumE4fg0H4mVEIs4xYwzq/xhuTtpTb00ZCOF2Y/151o0alWcLbYgObZtbCpLxncjkJY8J+CY2v1WyE3VfBgErpseugYpf8GpkrKiM5tkY7DjhkrH+VY2FD8A6G6bkn/o+Ay7Vo3pv/byMyNsHBN9LgvCwQIDtyQJSEvTUK3IY0vcYTCUaITqIrWkj5qrDsuw9gR9Ie11+kzYL/1mE5AWOHmT2qNz7C9gOgJoQIDAQABAoIBAQCY9dRyQEfev5XgQZBRpmWgSDhhoDaDnG9Nt3r3wA4RoLGfHr2poSoF+bfMNrhT2mjpf3i43vNh77JYdpR/uxVvAUQwRctmPmsunfiPfT3SwCilIOQuGxOb/L5ujqqRhmzwGeQHWIrd0ChGtjChtEIAP24UKoN6w3QwNLCFiY7RfTC7+yyO05tKoIhzirCNDBARZPmaIm4ClmIf3Z3xg0Vsgtz7qntd63UoXjSWFA0f9EDJHfxCTaS1r9OrpPsPMNpVOEYDek4le+mWVCBmQABxYDBCycrILQUwpDkGOa6D7tezpjGYyn8Z4HiHzcneNqt/0+g5lG28DWHz9h0TKIjBAoGBAOklXNZmeMpMrQvTOta2cE/ts17SSrnUFWtKqU7ZH9Gs50ZpwzrnNWy+3sCiCCKfa+RylbXjukioKR5qz/qpr28GdD8+dWYNDHraEpk/ZtOfff7TlNpOjiNPXb0OF2t3HDeQs5etUPx5DHgCA58vDK4RlQcIWpZROCeH5vzo7lStAoGBAOauiFYKmbX3FdWsyBerjhbem5X59eNs68KtHxd/Y6INn/uI12gSsOi+Gj9B7yC/N1AKxqaWGN9fPeGy2+mC+BI0tiTWwATNlZyaSXeKBqKThONhgmZWUaX++dczqbWADdtzRUroy5X2lHzG8q6iG0RQtgwnczU1OdBk+UgF1/NFAoGBAJcr0Lx8CQozGWk3d0lNVhmdaNasyCMh7xl4ebtUcZtE31j6rsn8rNlsEYcaCOhaMl0YJxafKGSAFNlSLLS9XbFBoBJ57ylSgKsPx0tynrvNCKc4jaXXlbYzefZhsrHNs5Ab1Tcd/AsYegs+UxbeLPyZDeZXdlVNKHoJVq7aYd6pAoGAC7M2fwaynSQXG2tUCr9MyaQoyAaRjiNsIceeGBcB+qouPxfFtSWdi3B47FRvyH1qVMj3ImPihxHRlaz4snNOGb5KrrulqZizyemZaFK722sYBmBfuMkQAxdXnK6mIOqJyWOjVBVSnhyPk3STwn++WkytrxghI8W7VPKKIjkJpvECgYBBnvWXa8Ez/azEN+Y2Lc1PnU9OpNa/QRPUPApq15dB8Cu9e2Vm6F+CdGKBY8WQvDw7DJd6eOjCfN6ymy1O9vLiooNQJGaO/znncU1r3s42dfpQ8owthILl24GNXnEgth2yYfYPr/EVLoVsbgO0WKFvdsVbSo/upeLzGnVT+DMqfA==";
    setSettings(true);
    setInstanceTime("2020-06-05T23:02:28.438Z");
    settings.setProperty("sonar.auth.saml.sp.privateKey.secured", wrongFormatPrivateKey);
    settings.setProperty("sonar.auth.saml.signature.enabled", true);
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_minimal_response.txt", SQ_CALLBACK_URL);

    assertThatThrownBy(() -> underTest.callback(callbackContext))
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Error while loading PKCS8 private key, please check the format");
  }

  @Test
  public void callback_does_not_sync_group_when_group_setting_is_not_set() {
    setSettings(true);
    settings.setProperty("sonar.auth.saml.group.name", (String) null);
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_full_response.txt", SQ_CALLBACK_URL);

    underTest.callback(callbackContext);

    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("johndoe");
    assertThat(callbackContext.userIdentity.getGroups()).isEmpty();
    assertThat(callbackContext.userIdentity.shouldSyncGroups()).isFalse();
  }

  @Test
  public void fail_to_callback_when_login_is_missing() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_response_without_login.txt", SQ_CALLBACK_URL);

    assertThatThrownBy(() -> underTest.callback(callbackContext))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("login is missing");

  }

  @Test
  public void fail_to_callback_when_name_is_missing() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_response_without_name.txt", SQ_CALLBACK_URL);

    assertThatThrownBy(() -> underTest.callback(callbackContext))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("name is missing");
  }

  @Test
  public void fail_to_callback_when_certificate_is_invalid() {
    setSettings(true);
    settings.setProperty("sonar.auth.saml.certificate.secured", "invalid");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_full_response.txt", SQ_CALLBACK_URL);

    assertThatThrownBy(() -> underTest.callback(callbackContext))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Invalid certificate");
  }

  @Test
  public void fail_to_callback_when_using_wrong_certificate() {
    setSettings(true);
    settings.setProperty("sonar.auth.saml.certificate.secured", """
      -----BEGIN CERTIFICATE-----
      MIIEIzCCAwugAwIBAgIUHUzPjy5E2TmnsmTRT2sIUBRXFF8wDQYJKoZIhvcNAQEF
      BQAwXDELMAkGA1UEBhMCVVMxFDASBgNVBAoMC1NvbmFyU291cmNlMRUwEwYDVQQL
      DAxPbmVMb2dpbiBJZFAxIDAeBgNVBAMMF09uZUxvZ2luIEFjY291bnQgMTMxMTkx
      MB4XDTE4MDcxOTA4NDUwNVoXDTIzMDcxOTA4NDUwNVowXDELMAkGA1UEBhMCVVMx
      FDASBgNVBAoMC1NvbmFyU291cmNlMRUwEwYDVQQLDAxPbmVMb2dpbiBJZFAxIDAe
      BgNVBAMMF09uZUxvZ2luIEFjY291bnQgMTMxMTkxMIIBIjANBgkqhkiG9w0BAQEF
      AAOCAQ8AMIIBCgKCAQEArlpKHm4EkJiQyy+4GtZBixcy7fWnreB96T7cOoWLmWkK
      05FM5M/boWHZsvaNAuHsoCAMzIY3/l+55WbORzAxsloH7rvDaDrdPYQN+sU9bzsD
      ZkmDGDmA3QBSm/h/p5SiMkWU5Jg34toDdM0rmzUStIOMq6Gh/Ykx3fRRSjswy48x
      wfZLy+0wU7lasHqdfk54dVbb7mCm9J3iHZizvOt2lbtzGbP6vrrjpzvZm43ZRgP8
      FapYA8G3lczdIaG4IaLW6kYIRORd0UwI7IAwkao3uIo12rh1T6DLVyzjOs9PdIkb
      HbICN2EehB/ut3wohuPwmwp2UmqopIMVVaBSsmSlYwIDAQABo4HcMIHZMAwGA1Ud
      EwEB/wQCMAAwHQYDVR0OBBYEFAXGFMKYgtpzCpfpBUPQ1H/9AeDrMIGZBgNVHSME
      gZEwgY6AFAXGFMKYgtpzCpfpBUPQ1H/9AeDroWCkXjBcMQswCQYDVQQGEwJVUzEU
      MBIGA1UECgwLU29uYXJTb3VyY2UxFTATBgNVBAsMDE9uZUxvZ2luIElkUDEgMB4G
      A1UEAwwXT25lTG9naW4gQWNjb3VudCAxMzExOTGCFB1Mz48uRNk5p7Jk0U9rCFAU
      VxRfMA4GA1UdDwEB/wQEAwIHgDANBgkqhkiG9w0BAQUFAAOCAQEAPHgi9IdDaTxD
      R5R8KHMdt385Uq8XC5pd0Li6y5RR2k6SKjThCt+eQU7D0Y2CyYU27vfCa2DQV4hJ
      4v4UfQv3NR/fYfkVSsNpxjBXBI3YWouxt2yg7uwdZBdgGYd37Yv3g9PdIZenjOhr
      Ck6WjdleMAWHRgJpocmB4IOESSyTfUul3jFupWnkbnn8c0ue6zwXd7LA1/yjVT2l
      Yh45+lz25aIOlyyo7OUw2TD15LIl8OOIuWRS4+UWy5+VdhXMbmpSEQH+Byod90g6
      A1bKpOFhRBzcxaZ6B2hB4SqjTBzS9zdmJyyFs/WNJxHri3aorcdqG9oUakjJJqqX
      E13skIMV2g==
      -----END CERTIFICATE-----
      """);
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_full_response.txt", SQ_CALLBACK_URL);

    assertThatThrownBy(() -> underTest.callback(callbackContext))
      .isInstanceOf(Saml2AuthenticationException.class)
      .hasMessageContaining("Invalid signature for object");
  }

  @Test
  public void fail_callback_when_message_was_already_sent() {
    setSettings(true);
    setInstanceTime("2020-06-05T23:02:28.438Z");
    DumbCallbackContext callbackContext = new DumbCallbackContext(request, response, "encoded_minimal_response.txt", SQ_CALLBACK_URL);

    underTest.callback(callbackContext);

    assertThatThrownBy(() -> underTest.callback(callbackContext))
      .isInstanceOf(Saml2AuthenticationException.class)
      .hasMessage("A message with the same ID was already processed");
  }

  private void setSettings(boolean enabled) {
    if (enabled) {
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
      settings.setProperty("sonar.auth.saml.enabled", true);
    } else {
      settings.setProperty("sonar.auth.saml.enabled", false);
    }
  }

  private static class DumbInitContext implements OAuth2IdentityProvider.InitContext {
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final AtomicBoolean generateCsrfState = new AtomicBoolean(false);

    @Override
    public String generateCsrfState() {
      generateCsrfState.set(true);
      return null;
    }

    @Override
    public void redirectTo(String url) {
      // Do nothing
    }

    @Override
    public String getCallbackUrl() {
      return SQ_CALLBACK_URL;
    }

    @Override
    public HttpRequest getHttpRequest() {
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getScheme()).thenReturn("http");
      when(request.getServerName()).thenReturn("localhost");
      when(request.getServerPort()).thenReturn(9000);
      when(request.getRequestURI()).thenReturn("/oauth2/callback/saml");
      return new JakartaHttpRequest(request);
    }

    @Override
    public HttpResponse getHttpResponse() {
      return new JakartaHttpResponse(response);
    }

  }

  private static class DumbCallbackContext implements OAuth2IdentityProvider.CallbackContext {
    private final HttpServletResponse response;
    private final HttpServletRequest request;
    private final String expectedCallbackUrl;
    private final AtomicBoolean redirectedToRequestedPage = new AtomicBoolean(false);
    private final AtomicBoolean verifyState = new AtomicBoolean(false);

    private UserIdentity userIdentity = null;

    public DumbCallbackContext(HttpServletRequest request, HttpServletResponse response, String encodedResponseFile, String expectedCallbackUrl) {
      this.request = request;
      this.response = response;
      this.expectedCallbackUrl = expectedCallbackUrl;
      when(((JakartaHttpRequest) getHttpRequest()).getDelegate().getParameter("SAMLResponse")).thenReturn(loadResponse(encodedResponseFile));
    }

    private String loadResponse(String file) {
      try (InputStream json = getClass().getResourceAsStream(SamlIdentityProviderIT.class.getSimpleName() + "/" + file)) {
        return IOUtils.toString(json, StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public void verifyCsrfState() {
      throw new IllegalStateException("This method should not be called !");
    }

    @Override
    public void verifyCsrfState(String parameterName) {
      assertThat(parameterName).isEqualTo("RelayState");
      verifyState.set(true);
    }

    @Override
    public void redirectToRequestedPage() {
      redirectedToRequestedPage.set(true);
    }

    @Override
    public void authenticate(UserIdentity userIdentity) {
      this.userIdentity = userIdentity;
    }

    @Override
    public String getCallbackUrl() {
      return this.expectedCallbackUrl;
    }

    @Override
    public HttpRequest getHttpRequest() {
      return new JakartaHttpRequest(request);
    }

    @Override
    public HttpResponse getHttpResponse() {
      return new JakartaHttpResponse(response);
    }

  }
}
