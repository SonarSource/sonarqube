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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.apachecommons.io.IOUtils;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamlIdentityProviderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MapSettings settings = new MapSettings(new PropertyDefinitions(SamlSettings.definitions()));

  private SamlIdentityProvider underTest = new SamlIdentityProvider(new SamlSettings(settings.asConfig()));

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

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to create Auth");

    underTest.init(context);
  }

  @Test
  public void callback() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext("encoded_full_response.txt");

    underTest.callback(callbackContext);

    assertThat(callbackContext.redirectedToRequestedPage.get()).isTrue();
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("johndoe");
    assertThat(callbackContext.verifyState.get()).isTrue();
  }

  @Test
  public void callback_on_full_response() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext("encoded_full_response.txt");

    underTest.callback(callbackContext);

    assertThat(callbackContext.userIdentity.getName()).isEqualTo("John Doe");
    assertThat(callbackContext.userIdentity.getEmail()).isEqualTo("johndoe@email.com");
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("johndoe");
    assertThat(callbackContext.userIdentity.getGroups()).containsExactlyInAnyOrder("developer", "product-manager");
  }

  @Test
  public void callback_on_minimal_response() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext("encoded_minimal_response.txt");

    underTest.callback(callbackContext);

    assertThat(callbackContext.userIdentity.getName()).isEqualTo("John Doe");
    assertThat(callbackContext.userIdentity.getEmail()).isNull();
    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("johndoe");
    assertThat(callbackContext.userIdentity.getGroups()).isEmpty();
  }

  @Test
  public void callback_does_not_sync_group_when_group_setting_is_not_set() {
    setSettings(true);
    settings.setProperty("sonar.auth.saml.group.name", (String) null);
    DumbCallbackContext callbackContext = new DumbCallbackContext("encoded_full_response.txt");

    underTest.callback(callbackContext);

    assertThat(callbackContext.userIdentity.getProviderLogin()).isEqualTo("johndoe");
    assertThat(callbackContext.userIdentity.getGroups()).isEmpty();
  }

  @Test
  public void fail_to_callback_when_login_is_missing() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext("encoded_response_without_login.txt");

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("login is missing");

    underTest.callback(callbackContext);
  }

  @Test
  public void fail_to_callback_when_name_is_missing() {
    setSettings(true);
    DumbCallbackContext callbackContext = new DumbCallbackContext("encoded_response_without_name.txt");

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name is missing");

    underTest.callback(callbackContext);
  }

  @Test
  public void fail_to_callback_when_certificate_is_invalid() {
    setSettings(true);
    settings.setProperty("sonar.auth.saml.certificate.secured", "invalid");
    DumbCallbackContext callbackContext = new DumbCallbackContext("encoded_full_response.txt");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to create Auth");

    underTest.callback(callbackContext);
  }

  @Test
  public void fail_to_callback_when_using_wrong_certificate() {
    setSettings(true);
    settings.setProperty("sonar.auth.saml.certificate.secured", "-----BEGIN CERTIFICATE-----\n" +
      "MIIEIzCCAwugAwIBAgIUHUzPjy5E2TmnsmTRT2sIUBRXFF8wDQYJKoZIhvcNAQEF\n" +
      "BQAwXDELMAkGA1UEBhMCVVMxFDASBgNVBAoMC1NvbmFyU291cmNlMRUwEwYDVQQL\n" +
      "DAxPbmVMb2dpbiBJZFAxIDAeBgNVBAMMF09uZUxvZ2luIEFjY291bnQgMTMxMTkx\n" +
      "MB4XDTE4MDcxOTA4NDUwNVoXDTIzMDcxOTA4NDUwNVowXDELMAkGA1UEBhMCVVMx\n" +
      "FDASBgNVBAoMC1NvbmFyU291cmNlMRUwEwYDVQQLDAxPbmVMb2dpbiBJZFAxIDAe\n" +
      "BgNVBAMMF09uZUxvZ2luIEFjY291bnQgMTMxMTkxMIIBIjANBgkqhkiG9w0BAQEF\n" +
      "AAOCAQ8AMIIBCgKCAQEArlpKHm4EkJiQyy+4GtZBixcy7fWnreB96T7cOoWLmWkK\n" +
      "05FM5M/boWHZsvaNAuHsoCAMzIY3/l+55WbORzAxsloH7rvDaDrdPYQN+sU9bzsD\n" +
      "ZkmDGDmA3QBSm/h/p5SiMkWU5Jg34toDdM0rmzUStIOMq6Gh/Ykx3fRRSjswy48x\n" +
      "wfZLy+0wU7lasHqdfk54dVbb7mCm9J3iHZizvOt2lbtzGbP6vrrjpzvZm43ZRgP8\n" +
      "FapYA8G3lczdIaG4IaLW6kYIRORd0UwI7IAwkao3uIo12rh1T6DLVyzjOs9PdIkb\n" +
      "HbICN2EehB/ut3wohuPwmwp2UmqopIMVVaBSsmSlYwIDAQABo4HcMIHZMAwGA1Ud\n" +
      "EwEB/wQCMAAwHQYDVR0OBBYEFAXGFMKYgtpzCpfpBUPQ1H/9AeDrMIGZBgNVHSME\n" +
      "gZEwgY6AFAXGFMKYgtpzCpfpBUPQ1H/9AeDroWCkXjBcMQswCQYDVQQGEwJVUzEU\n" +
      "MBIGA1UECgwLU29uYXJTb3VyY2UxFTATBgNVBAsMDE9uZUxvZ2luIElkUDEgMB4G\n" +
      "A1UEAwwXT25lTG9naW4gQWNjb3VudCAxMzExOTGCFB1Mz48uRNk5p7Jk0U9rCFAU\n" +
      "VxRfMA4GA1UdDwEB/wQEAwIHgDANBgkqhkiG9w0BAQUFAAOCAQEAPHgi9IdDaTxD\n" +
      "R5R8KHMdt385Uq8XC5pd0Li6y5RR2k6SKjThCt+eQU7D0Y2CyYU27vfCa2DQV4hJ\n" +
      "4v4UfQv3NR/fYfkVSsNpxjBXBI3YWouxt2yg7uwdZBdgGYd37Yv3g9PdIZenjOhr\n" +
      "Ck6WjdleMAWHRgJpocmB4IOESSyTfUul3jFupWnkbnn8c0ue6zwXd7LA1/yjVT2l\n" +
      "Yh45+lz25aIOlyyo7OUw2TD15LIl8OOIuWRS4+UWy5+VdhXMbmpSEQH+Byod90g6\n" +
      "A1bKpOFhRBzcxaZ6B2hB4SqjTBzS9zdmJyyFs/WNJxHri3aorcdqG9oUakjJJqqX\n" +
      "E13skIMV2g==\n" +
      "-----END CERTIFICATE-----\n");
    DumbCallbackContext callbackContext = new DumbCallbackContext("encoded_full_response.txt");

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Signature validation failed. SAML Response rejected");

    underTest.callback(callbackContext);
  }

  private void setSettings(boolean enabled) {
    if (enabled) {
      settings.setProperty("sonar.auth.saml.applicationId", "MyApp");
      settings.setProperty("sonar.auth.saml.providerId", "http://localhost:8080/auth/realms/sonarqube");
      settings.setProperty("sonar.auth.saml.loginUrl", "http://localhost:8080/auth/realms/sonarqube/protocol/saml");
      settings.setProperty("sonar.auth.saml.certificate.secured",
        "MIICoTCCAYkCBgFksusMzTANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDDAlzb25hcnF1YmUwHhcNMTgwNzE5MTQyMDA2WhcNMjgwNzE5MTQyMTQ2WjAUMRIwEAYDVQQDDAlzb25hcnF1YmUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDEOth5gxpTs1f3bFGUD8hO97eMIsDZvvE3PZeKoeTRG7mOLu6rfLXphG3fE3E6/xqUhPP5p9hJl9DwgaMewhdZhfHqtOw6/SPMCQNFVNw9FQ7lprWKg8cZygYLDxhObEvCWPek8KcMb/vlKD8c8ha374O9qET51CVogDM5ropp02q0ELxoUKXqphKH4+sGXRVnDHaEsFHxse1HnciZT5mF1G45vxDItdAnWKkXYKVHC+Et52tCieqM0ygpQF1lWVJFXVOqsi03YkMu7IkWvSSfAw+uEcfmquT7FbxJ2n5gp94odAkQB0HK3fABrHr+G+n2QvWG6WwQPJTL0Ov0w+tNAgMBAAEwDQYJKoZIhvcNAQELBQADggEBACQfOrJF98nunKz6CN+YZXXMYhzQiqTD0MlzCg+Rdhir+WC/ru3Kz8omv52W/sXEMNQbEZBksVLl8W/1xeBS41Sf1nfutU560v/j3/OmOcnCw4qebqFH7nB8RL8vA4rGx430W/PeeUMikY1mSjlwhnJGiICQ3Y8I2qM6QWEr/Df2/gFCW2YnHbnS6Q/OwRQi+UFIzKklSQQa0gAnqfM4oSKU2OMhzScinWg1buMYfJSXgd4qIhPvRsZpqBsdt/OSrU2D5Y2YfSu8oIcxBRgJoERH5BV9GdOID4fS+TYw0M0QO/ORetNw1mA/8Npsy8okF8Cn7fDgbnWC8uz+/xDc14I=");
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

    private HttpServletResponse response = mock(HttpServletResponse.class);
    private final AtomicBoolean generateCsrfState = new AtomicBoolean(false);

    @Override
    public String generateCsrfState() {
      generateCsrfState.set(true);
      return null;
    }

    @Override
    public void redirectTo(String url) {
    }

    @Override
    public String getCallbackUrl() {
      return "http://localhost/oauth/callback/saml";
    }

    @Override
    public HttpServletRequest getRequest() {
      return mock(HttpServletRequest.class);
    }

    @Override
    public HttpServletResponse getResponse() {
      return response;
    }
  }

  private static class DumbCallbackContext implements OAuth2IdentityProvider.CallbackContext {

    private HttpServletResponse response = mock(HttpServletResponse.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);

    private final AtomicBoolean redirectedToRequestedPage = new AtomicBoolean(false);
    private final AtomicBoolean verifyState = new AtomicBoolean(false);

    private UserIdentity userIdentity = null;

    public DumbCallbackContext(String encodedResponseFile) {
      when(getRequest().getRequestURL()).thenReturn(new StringBuffer("http://localhost/oauth/callback/saml"));
      Map<String, String[]> parameterMap = new HashMap<>();
      parameterMap.put("SAMLResponse", new String[] {loadResponse(encodedResponseFile)});
      when(getRequest().getParameterMap()).thenReturn(parameterMap);
    }

    private String loadResponse(String file) {
      try (InputStream json = getClass().getResourceAsStream("SamlIdentityProviderTest/" + file)) {
        return IOUtils.toString(json, StandardCharsets.UTF_8.name());
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
      return "http://localhost/oauth/callback/saml";
    }

    @Override
    public HttpServletRequest getRequest() {
      return request;
    }

    @Override
    public HttpServletResponse getResponse() {
      return response;
    }
  }
}
