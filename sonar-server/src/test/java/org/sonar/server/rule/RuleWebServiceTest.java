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
package org.sonar.server.rule;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.api.web.ws.RequestHandler;
import org.sonar.api.web.ws.SimpleRequest;
import org.sonar.api.web.ws.SimpleResponse;
import org.sonar.api.web.ws.WebService;

import static org.fest.assertions.Assertions.assertThat;

public class RuleWebServiceTest {

  WebService.Context context = new WebService.Context();
  RuleWebService ws = new RuleWebService();

  @Test
  public void define_ws() throws Exception {
    ws.define(context);

    WebService.Controller controller = context.controller("api/rules");
    assertThat(controller).isNotNull();
    assertThat(controller.path()).isEqualTo("api/rules");
    assertThat(controller.description()).isNotEmpty();

    WebService.Action search = controller.action("search");
    assertThat(search).isNotNull();
    assertThat(search.key()).isEqualTo("search");
    assertThat(search.handler()).isNotNull();
    assertThat(search.since()).isEqualTo("4.2");
    assertThat(search.isPost()).isFalse();
  }

  @Test
  public void search_for_rules() throws Exception {
    ws.define(context);
    RequestHandler handler = context.controller("api/rules").action("search").handler();
    SimpleRequest request = new SimpleRequest();
    SimpleResponse response = new SimpleResponse();

    handler.handle(request, response);

    String json = response.outputAsString();
    JSONAssert.assertEquals(
      IOUtils.toString(getClass().getResource("/org/sonar/server/rule/RuleWebServiceTest/search.json")),
      json, true
    );
  }
}
