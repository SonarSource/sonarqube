/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.saml.ws;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.auth.saml.SamlAuthenticator;
import org.sonar.server.authentication.OAuth2ContextFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SamlValidationInitActionTest {

  private SamlValidationInitAction underTest;
  private SamlAuthenticator samlAuthenticator;
  private OAuth2ContextFactory oAuth2ContextFactory;

  @Before
  public void setUp() throws Exception {
    samlAuthenticator = mock(SamlAuthenticator.class);
    oAuth2ContextFactory = mock(OAuth2ContextFactory.class);
    underTest = new SamlValidationInitAction(samlAuthenticator, oAuth2ContextFactory);
  }

  @Test
  public void do_get_pattern() {
    assertThat(underTest.doGetPattern().matches("/api/saml/validation_init")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/saml")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/saml/validation_init2")).isFalse();
  }


  @Test
  public void do_filter() throws IOException, ServletException {
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    String callbackUrl = "http://localhost:9000/api/validation_test";
    when(oAuth2ContextFactory.generateCallbackUrl(anyString()))
      .thenReturn(callbackUrl);

    underTest.doFilter(servletRequest, servletResponse, filterChain);

    verify(samlAuthenticator).initLogin(matches(callbackUrl),
      matches(SamlValidationInitAction.VALIDATION_RELAY_STATE),
      any(), any());
  }

  @Test
  public void verify_definition() {
    String controllerKey = "foo";
    WebService.Context context = new WebService.Context();
    WebService.NewController newController = context.createController(controllerKey);
    underTest.define(newController);
    newController.done();

    WebService.Action validationInitAction = context.controller(controllerKey).action("validation_init");
    assertThat(validationInitAction).isNotNull();
    assertThat(validationInitAction.description()).isNotEmpty();
    assertThat(validationInitAction.handler()).isNotNull();
  }
}
