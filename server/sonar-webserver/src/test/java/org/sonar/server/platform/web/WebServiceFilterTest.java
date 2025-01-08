/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.web;

import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.FilterChain;
import org.sonar.server.http.JakartaHttpRequest;
import org.sonar.server.http.JakartaHttpResponse;
import org.sonar.server.ws.ServletFilterHandler;
import org.sonar.server.ws.WebServiceEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.web.WebServiceFilterTest.WsUrl.newWsUrl;

public class WebServiceFilterTest {

  private final WebServiceEngine webServiceEngine = mock(WebServiceEngine.class);

  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final FilterChain chain = mock(FilterChain.class);
  private final ServletOutputStream responseOutput = mock(ServletOutputStream.class);
  private WebServiceFilter underTest;

  @Before
  public void setUp() throws Exception {
    when(request.getContextPath()).thenReturn("");
    HttpServletResponse mockResponse = mock(HttpServletResponse.class);
    when(mockResponse.getOutputStream()).thenReturn(responseOutput);
  }

  @Test
  public void match_declared_web_services_with_optional_suffix() {
    initWebServiceEngine(
      newWsUrl("api/issues", "search"),
      newWsUrl("batch", "index"));

    assertThat(underTest.doGetPattern().matches("/api/issues/search")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/issues/search.protobuf")).isTrue();
    assertThat(underTest.doGetPattern().matches("/batch/index")).isTrue();
    assertThat(underTest.doGetPattern().matches("/batch/index.protobuf")).isTrue();

    assertThat(underTest.doGetPattern().matches("/foo")).isFalse();
  }

  @Test
  public void match_undeclared_web_services_starting_with_api() {
    initWebServiceEngine(newWsUrl("api/issues", "search"));

    assertThat(underTest.doGetPattern().matches("/api/resources/index")).isTrue();
    assertThat(underTest.doGetPattern().matches("/api/user_properties")).isTrue();
  }

  @Test
  public void does_not_match_web_services_using_servlet_filter() {
    initWebServiceEngine(newWsUrl("api/authentication", "login").setHandler(ServletFilterHandler.INSTANCE));

    assertThat(underTest.doGetPattern().matches("/api/authentication/login")).isFalse();
  }

  @Test
  public void does_not_match_servlet_filter_that_prefix_a_ws() {
    initWebServiceEngine(
      newWsUrl("api/foo", "action").setHandler(ServletFilterHandler.INSTANCE),
      newWsUrl("api/foo", "action_2"));

    assertThat(underTest.doGetPattern().matches("/api/foo/action")).isFalse();
    assertThat(underTest.doGetPattern().matches("/api/foo/action_2")).isTrue();
  }

  @Test
  public void execute_ws() {
    underTest = new WebServiceFilter(webServiceEngine);

    underTest.doFilter(new JakartaHttpRequest(request), new JakartaHttpResponse(response), chain);

    verify(webServiceEngine).execute(any(), any());
  }

  private void initWebServiceEngine(WsUrl... wsUrls) {
    List<WebService.Controller> controllers = new ArrayList<>();

    for (WsUrl wsUrl : wsUrls) {
      String controller = wsUrl.getController();
      WebService.Controller wsController = mock(WebService.Controller.class);
      when(wsController.path()).thenReturn(controller);

      List<WebService.Action> actions = new ArrayList<>();
      for (String action : wsUrl.getActions()) {
        WebService.Action wsAction = mock(WebService.Action.class);
        when(wsAction.path()).thenReturn(controller + "/" + action);
        when(wsAction.key()).thenReturn(action);
        when(wsAction.handler()).thenReturn(wsUrl.getRequestHandler());
        actions.add(wsAction);
      }
      when(wsController.actions()).thenReturn(actions);
      controllers.add(wsController);
    }
    when(webServiceEngine.controllers()).thenReturn(controllers);
    underTest = new WebServiceFilter(webServiceEngine);
  }

  static final class WsUrl {
    private final String controller;
    private final String[] actions;
    private RequestHandler requestHandler = EmptyRequestHandler.INSTANCE;

    WsUrl(String controller, String... actions) {
      this.controller = controller;
      this.actions = actions;
    }

    WsUrl setHandler(RequestHandler requestHandler) {
      this.requestHandler = requestHandler;
      return this;
    }

    String getController() {
      return controller;
    }

    String[] getActions() {
      return actions;
    }

    RequestHandler getRequestHandler() {
      return requestHandler;
    }

    static WsUrl newWsUrl(String controller, String... actions) {
      return new WsUrl(controller, actions);
    }
  }

  private enum EmptyRequestHandler implements RequestHandler {
    INSTANCE;

    @Override
    public void handle(Request request, Response response) {
      // Nothing to do
    }
  }

}
