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
package org.sonar.server.platform.web;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.sonar.api.web.ServletFilter;
import org.sonar.server.property.ws.IndexAction;
import org.sonar.server.setting.ws.SettingsWsParameters;
import org.sonar.server.ws.ServletResponse;
import org.sonar.server.ws.WebServiceEngine;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.server.property.ws.PropertiesWs.CONTROLLER_PROPERTIES;

/**
 * This filter is used to execute deprecated api/properties WS, that were using REST
 */
public class DeprecatedPropertiesWsFilter extends ServletFilter {

  private static final Splitter VALUE_SPLITTER = Splitter.on(",").omitEmptyStrings();
  private static final String SEPARATOR = "/";

  private final WebServiceEngine webServiceEngine;

  public DeprecatedPropertiesWsFilter(WebServiceEngine webServiceEngine) {
    this.webServiceEngine = webServiceEngine;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.builder()
      .includes(SEPARATOR + CONTROLLER_PROPERTIES + "/*")
      .build();
  }

  @Override
  public void doFilter(javax.servlet.ServletRequest servletRequest, javax.servlet.ServletResponse servletResponse, FilterChain chain) {
    HttpServletRequest request = (HttpServletRequest) servletRequest;
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    RestServletRequest wsRequest = new RestServletRequest(request);
    ServletResponse wsResponse = new ServletResponse(response);
    webServiceEngine.execute(wsRequest, wsResponse);
  }

  @Override
  public void init(FilterConfig filterConfig) {
    // Nothing to do
  }

  @Override
  public void destroy() {
    // Nothing to do
  }

  static class RestServletRequest extends org.sonar.server.ws.ServletRequest {

    private final Response restResponse;

    RestServletRequest(HttpServletRequest request) {
      super(request);
      this.restResponse = new Response(request);
    }

    @Override
    public String getPath() {
      return restResponse.redirectedPath;
    }

    @Override
    public boolean hasParam(String key) {
      return restResponse.additionalParams.containsKey(key) || restResponse.additionalMultiParams.containsKey(key);
    }

    @Override
    public String readParam(String key) {
      return restResponse.additionalParams.get(key);
    }

    @Override
    public List<String> readMultiParam(String key) {
      return new ArrayList<>(restResponse.additionalMultiParams.get(key));
    }

    @Override
    public String method() {
      return restResponse.redirectedMethod;
    }
  }

  private static class Response {
    private final HttpServletRequest request;
    private final Map<String, String> additionalParams = new HashMap<>();
    private final Multimap<String, String> additionalMultiParams = ArrayListMultimap.create();
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
      Optional<String> id = getKeyOrId();
      switch (method) {
        case "POST":
        case "PUT":
          handlePutAndPost(id, getValues(), getComponent());
          break;
        case "DELETE":
          handleDelete(id, getComponent());
          break;
        default:
          handleGet(id, getComponent());
      }
    }

    private void handlePutAndPost(Optional<String> key, List<String> values, Optional<String> component) {
      if (values.isEmpty()) {
        redirectToReset(key, component);
      } else {
        redirectToSet(key, values, component);
      }
    }

    private void handleDelete(Optional<String> key, Optional<String> component) {
      redirectToReset(key, component);
    }

    private void handleGet(Optional<String> key, Optional<String> component) {
      addParameterIfPresent(IndexAction.PARAM_ID, key);
      addParameterIfPresent(IndexAction.PARAM_COMPONENT, component);
      addParameterIfPresent(IndexAction.PARAM_FORMAT, readParam(IndexAction.PARAM_FORMAT));
      redirectedPath = CONTROLLER_PROPERTIES + "/index";
      redirectedMethod = "GET";
    }

    private Optional<String> getKeyOrId() {
      Optional<String> key = getKey();
      return key.isPresent() ? key : readParam("id");
    }

    private Optional<String> getKey() {
      if (originalPath.equals(SEPARATOR + CONTROLLER_PROPERTIES)) {
        return Optional.empty();
      }
      String key = originalPath.replace(SEPARATOR + CONTROLLER_PROPERTIES + SEPARATOR, "");
      return key.isEmpty() ? Optional.empty() : Optional.of(key);
    }

    private Optional<String> getComponent() {
      return readParam(IndexAction.PARAM_COMPONENT);
    }

    private List<String> getValues() {
      Optional<String> value = readParam("value");
      if (!value.isPresent()) {
        List<String> values = new ArrayList<>();
        readBody().ifPresent(values::add);
        return values;
      }
      return VALUE_SPLITTER.splitToList(value.get());
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

    private void redirectToSet(Optional<String> key, List<String> values, Optional<String> component) {
      addParameterIfPresent(SettingsWsParameters.PARAM_KEY, key);
      if (values.size() == 1) {
        additionalParams.put(SettingsWsParameters.PARAM_VALUE, values.get(0));
      } else {
        additionalMultiParams.putAll(SettingsWsParameters.PARAM_VALUES, values);
      }
      addParameterIfPresent(SettingsWsParameters.PARAM_COMPONENT, component);
      redirectedPath = "api/settings/set";
      redirectedMethod = "POST";
    }

    private void redirectToReset(Optional<String> key, Optional<String> component) {
      addParameterIfPresent(SettingsWsParameters.PARAM_KEYS, key);
      addParameterIfPresent(SettingsWsParameters.PARAM_COMPONENT, component);
      redirectedPath = "api/settings/reset";
      redirectedMethod = "POST";
    }

    private void addParameterIfPresent(String parameterKey, Optional<String> value) {
      value.ifPresent(s -> additionalParams.put(parameterKey, s));
    }

  }

}
