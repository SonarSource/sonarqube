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

package org.sonar.server.ws;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.sonar.api.web.ServletFilter;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.server.property.ws.PropertiesWs.CONTROLLER_PROPERTIES;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_RESET;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.ACTION_SET;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.CONTROLLER_SETTINGS;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_KEYS;
import static org.sonarqube.ws.client.setting.SettingsWsParameters.PARAM_VALUE;

/**
 * This filter is used to execute some deprecated Java WS, that were using REST
 */
public class DeprecatedRestWebServiceFilter extends ServletFilter {

  private final WebServiceEngine webServiceEngine;

  public DeprecatedRestWebServiceFilter(WebServiceEngine webServiceEngine) {
    this.webServiceEngine = webServiceEngine;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes("/" + CONTROLLER_PROPERTIES + "/*")
      .build();
  }

  @Override
  public void doFilter(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    RestServletRequest wsRequest = new RestServletRequest(request);
    ServletResponse wsResponse = new ServletResponse(response);
    webServiceEngine.execute(wsRequest, wsResponse);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }

  static class RestServletRequest extends org.sonar.server.ws.ServletRequest {

    private final Response restResponse;

    public RestServletRequest(HttpServletRequest request) {
      super(request);
      this.restResponse = new Response(request);
    }

    @Override
    public String getPath() {
      return restResponse.getRedirectedPath();
    }

    @Override
    public boolean hasParam(String key) {
      return restResponse.getAdditionalParams().containsKey(key) || super.hasParam(key);
    }

    @Override
    protected String readParam(String key) {
      String param = restResponse.getAdditionalParams().get(key);
      if (param != null) {
        return param;
      }
      return super.readParam(key);
    }

    @Override
    public String method() {
      return restResponse.redirectedMethod;
    }
  }

  private static class Response {
    private final HttpServletRequest request;
    private final Map<String, String> additionalParams = new HashMap<>();
    private final String originalPath;
    private String redirectedPath;
    private String redirectedMethod;

    Response(HttpServletRequest request) {
      this.request = request;
      this.originalPath = request.getRequestURI().replaceFirst(request.getContextPath(), "");
      init();
    }

    void init() {
      String method = request.getMethod();
      Optional<String> key = getKeyOrId();
      Optional<String> value = getValue();
      Optional<String> component = getComponent();
      switch (method) {
        case "POST":
        case "PUT":
          if (value.isPresent()) {
            addParameterIfPresent(PARAM_KEY, key);
            addParameterIfPresent(PARAM_VALUE, value);
            addParameterIfPresent(PARAM_COMPONENT, component);
            redirectedPath = CONTROLLER_SETTINGS + "/" + ACTION_SET;
            redirectedMethod = "POST";
          } else {
            redirectToReset(key, component);
          }
          break;
        case "DELETE":
          redirectToReset(key, component);
          break;
        default:
          addParameterIfPresent(PARAM_KEY, key);
          redirectedPath = CONTROLLER_PROPERTIES + "/index";
          redirectedMethod = "GET";
      }
    }

    private Optional<String> getKeyOrId() {
      Optional<String> key = getKey();
      return key.isPresent() ? key : readParam("id");
    }

    private Optional<String> getKey() {
      if (originalPath.equals("/" + CONTROLLER_PROPERTIES)) {
        return Optional.empty();
      }
      String key = originalPath.replace("/" + CONTROLLER_PROPERTIES + "/", "");
      return key.isEmpty() ? Optional.empty() : Optional.of(key);
    }

    private Optional<String> getComponent() {
      return readParam("resource");
    }

    private Optional<String> getValue() {
      Optional<String> value = readParam("value");
      return value.isPresent() ? value : readBody();
    }

    private Optional<String> readParam(String paramKey) {
      String paramValue = request.getParameter(paramKey);
      return isNullOrEmpty(paramValue) ? Optional.empty() : Optional.of(paramValue);
    }

    private Optional<String> readBody() {
      StringWriter writer = new StringWriter();
      try {
        ServletInputStream inputStream = request.getInputStream();
        if (inputStream == null) {
          return Optional.empty();
        }
        IOUtils.copy(inputStream, writer, UTF_8.name());
        return Optional.of(writer.toString());
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    private void redirectToReset(Optional<String> key, Optional<String> component) {
      addParameterIfPresent(PARAM_KEYS, key);
      addParameterIfPresent(PARAM_COMPONENT, component);
      redirectedPath = CONTROLLER_SETTINGS + "/" + ACTION_RESET;
      redirectedMethod = "POST";
    }

    private void addParameterIfPresent(String parameterKey, Optional<String> value) {
      value.ifPresent(s -> additionalParams.put(parameterKey, s));
    }

    String getRedirectedPath() {
      return redirectedPath;
    }

    Map<String, String> getAdditionalParams() {
      return additionalParams;
    }
  }

}
