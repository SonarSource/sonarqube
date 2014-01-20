/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.ws;

import org.apache.commons.io.IOUtils;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.SimpleResponse;
import org.sonar.api.server.ws.WebService;

import javax.annotation.CheckForNull;
import java.net.URL;

import static org.junit.Assert.assertEquals;

/**
 * TODO move to sonar-plugin-api with type "test-jar"
 */
public class WsTester {

  private final WebService.Context context = new WebService.Context();
  private String wsPath = null;

  public WsTester(WebService ws) {
    ws.define(context);
    if (!context.controllers().isEmpty()) {
      wsPath = context.controllers().get(0).path();
    }
  }

  public WebService.Context context() {
    return context;
  }

  @CheckForNull
  public WebService.Controller controller(String path) {
    return context.controller(path);
  }

  public Result execute(String actionKey, Request request) throws Exception {
    if (wsPath == null) {
      throw new IllegalStateException("Ws path is not defined");
    }
    SimpleResponse response = new SimpleResponse();
    RequestHandler handler = context.controller(wsPath).action(actionKey).handler();
    handler.handle(request, response);
    return new Result(response);
  }

  public static class Result {
    private final SimpleResponse response;

    private Result(SimpleResponse response) {
      this.response = response;
    }

    public Result assertHttpStatus(int httpStatus) {
      assertEquals(httpStatus, response.status());
      return this;
    }

    public Result assertJson(String expectedJson) throws Exception {
      String json = response.outputAsString();
      JSONAssert.assertEquals(expectedJson, json, true);
      return this;
    }

    public Result assertJson(Class clazz, String jsonResourcePath) throws Exception {
      String json = response.outputAsString();
      URL url = clazz.getResource(clazz.getSimpleName() + "/" + jsonResourcePath);
      JSONAssert.assertEquals(IOUtils.toString(url), json, true);
      return this;
    }
  }
}
