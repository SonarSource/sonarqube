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
package org.sonar.server.authentication;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.servlet.ServletException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.sonar.api.platform.Server;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class SamlValidationRedirectionFilterTest {

  public static final List<String> CSP_HEADERS = List.of("Content-Security-Policy", "X-Content-Security-Policy", "X-WebKit-CSP");

  SamlValidationRedirectionFilter underTest;

  @Before
  public void setup() throws ServletException {
    Server server = mock(Server.class);
    doReturn("contextPath").when(server).getContextPath();
    underTest = new SamlValidationRedirectionFilter(server);
    underTest.init();
  }


  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern().matches("/oauth2/callback/saml")).isTrue();
    assertThat(underTest.doGetPattern().matches("/oauth2/callback/")).isFalse();
    assertThat(underTest.doGetPattern().matches("/oauth2/callback/test")).isFalse();
    assertThat(underTest.doGetPattern().matches("/oauth2/")).isFalse();
  }

  @Test
  public void do_filter_validation_relay_state_with_csrfToken() throws IOException {
    HttpRequest servletRequest = mock(HttpRequest.class);
    HttpResponse servletResponse = mock(HttpResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    String validSample = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    when(servletRequest.getParameter(matches("SAMLResponse"))).thenReturn(validSample);
    when(servletRequest.getParameter(matches("RelayState"))).thenReturn("validation-query/CSRF_TOKEN");
    when(servletRequest.getContextPath()).thenReturn("contextPath");
    PrintWriter pw = mock(PrintWriter.class);
    when(servletResponse.getWriter()).thenReturn(pw);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    ArgumentCaptor<String> htmlProduced = ArgumentCaptor.forClass(String.class);
    verify(pw).print(htmlProduced.capture());
    CSP_HEADERS.forEach(h -> verify(servletResponse).setHeader(eq(h), anyString()));
    assertThat(htmlProduced.getValue()).contains(validSample);
    assertThat(htmlProduced.getValue()).contains("action=\"contextPath/saml/validation\"");
    assertThat(htmlProduced.getValue()).contains("value=\"CSRF_TOKEN\"");
  }

  @Test
  public void do_filter_validation_relay_state_with_malicious_csrfToken() throws IOException {
    HttpRequest servletRequest = mock(HttpRequest.class);
    HttpResponse servletResponse = mock(HttpResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    String validSample = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    when(servletRequest.getParameter(matches("SAMLResponse"))).thenReturn(validSample);

    String maliciousToken = "test\"</input><script>*Malicious Token*</script><input value=\"";

    when(servletRequest.getParameter(matches("RelayState"))).thenReturn("validation-query/" + maliciousToken);
    PrintWriter pw = mock(PrintWriter.class);
    when(servletResponse.getWriter()).thenReturn(pw);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    ArgumentCaptor<String> htmlProduced = ArgumentCaptor.forClass(String.class);
    verify(pw).print(htmlProduced.capture());
    CSP_HEADERS.forEach(h -> verify(servletResponse).setHeader(eq(h), anyString()));
    assertThat(htmlProduced.getValue()).contains(validSample);
    assertThat(htmlProduced.getValue()).doesNotContain("<script>/*Malicious Token*/</script>");

  }

  @Test
  public void do_filter_validation_wrong_SAML_response() throws IOException {
    HttpRequest servletRequest = mock(HttpRequest.class);
    HttpResponse servletResponse = mock(HttpResponse.class);
    FilterChain filterChain = mock(FilterChain.class);

    String maliciousSaml = "test\"</input><script>/*hack website*/</script><input value=\"";

    when(servletRequest.getParameter(matches("SAMLResponse"))).thenReturn(maliciousSaml);
    when(servletRequest.getParameter(matches("RelayState"))).thenReturn("validation-query/CSRF_TOKEN");
    PrintWriter pw = mock(PrintWriter.class);
    when(servletResponse.getWriter()).thenReturn(pw);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    ArgumentCaptor<String> htmlProduced = ArgumentCaptor.forClass(String.class);
    verify(pw).print(htmlProduced.capture());
    CSP_HEADERS.forEach(h -> verify(servletResponse).setHeader(eq(h), anyString()));
    assertThat(htmlProduced.getValue()).doesNotContain("<script>/*hack website*/</script>");
    assertThat(htmlProduced.getValue()).contains("action=\"contextPath/saml/validation\"");
  }

  @Test
  @UseDataProvider("invalidRelayStateValues")
  public void do_filter_invalid_relayState_values(String relayStateValue) throws IOException {
    HttpRequest servletRequest = mock(HttpRequest.class);
    HttpResponse servletResponse = mock(HttpResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    doReturn(relayStateValue).when(servletRequest).getParameter("RelayState");

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verifyNoInteractions(servletResponse);
  }

  @Test
  public void extract_nonexistant_template() {
    assertThrows(IllegalStateException.class, () -> underTest.extractTemplate("not-there"));
  }

  @DataProvider
  public static Object[] invalidRelayStateValues() {
    return new Object[]{"random_query", "validation-query", null};
  }
}
