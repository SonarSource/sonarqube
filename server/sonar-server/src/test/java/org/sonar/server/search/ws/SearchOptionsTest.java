/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.search.ws;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.internal.SimpleGetRequest;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.test.JsonAssert;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchOptionsTest {

  @Test
  public void create_from_http_request() throws Exception {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "3");
    request.setParam("ps", "10");
    request.setParam("f", "name,repo");
    request.setParam("severities", "BLOCKER");

    SearchOptions options = SearchOptions.create(request);

    assertThat(options.fields()).containsOnly("name", "repo");
    assertThat(options.page()).isEqualTo(3);
    assertThat(options.pageSize()).isEqualTo(10);
  }

  @Test
  public void hasField() throws Exception {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "3");
    request.setParam("ps", "10");
    request.setParam("f", "name,repo");
    SearchOptions options = SearchOptions.create(request);

    assertThat(options.hasField("repo")).isTrue();
    assertThat(options.hasField("severity")).isFalse();
  }

  @Test
  public void hasField_always_true_by_default() throws Exception {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "3");
    request.setParam("ps", "10");
    SearchOptions options = SearchOptions.create(request);

    assertThat(options.hasField("repo")).isTrue();
  }

  @Test
  public void hasField_no_if_empty_value() throws Exception {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "3");
    request.setParam("ps", "10");
    request.setParam("f", "");
    SearchOptions options = SearchOptions.create(request);

    assertThat(options.hasField("repo")).isFalse();
  }

  @Test
  public void write_statistics_to_json_response() throws Exception {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam("p", "3");
    request.setParam("ps", "10");
    request.setParam("f", "name,repo");
    request.setParam("severities", "BLOCKER");

    SearchOptions options = SearchOptions.create(request);
    StringWriter json = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(json).beginObject();
    Result result = mock(Result.class);
    when(result.getTotal()).thenReturn(42L);
    options.writeStatistics(jsonWriter, result);
    jsonWriter.endObject().close();

    JsonAssert.assertJson(json.toString()).isSimilarTo("{\"total\": 42, \"p\": 3, \"ps\": 10}");
  }

  @Test
  public void defineFieldsParam() throws Exception {
    WebService.Context context = new WebService.Context();
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/foo");
        NewAction action = newController.createAction("search").setHandler(mock(RequestHandler.class));
        SearchOptions.defineFieldsParam(action, Lists.newArrayList("name", "lang", "severity"));
        newController.done();
      }
    }.define(context);

    WebService.Action searchAction = context.controller("api/foo").action("search");
    WebService.Param param = searchAction.param("f");
    assertThat(param).isNotNull();
    assertThat(param.possibleValues()).containsOnly("name", "lang", "severity");
    assertThat(param.exampleValue()).isEqualTo("name,lang");
  }

  @Test
  public void definePageParams() throws Exception {
    WebService.Context context = new WebService.Context();
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/foo");
        NewAction action = newController.createAction("search").setHandler(mock(RequestHandler.class));
        SearchOptions.definePageParams(action);
        newController.done();
      }
    }.define(context);

    WebService.Action searchAction = context.controller("api/foo").action("search");
    WebService.Param page = searchAction.param("p");
    assertThat(page).isNotNull();
    assertThat(page.defaultValue()).isEqualTo("1");
    WebService.Param pageSize = searchAction.param("ps");
    assertThat(pageSize).isNotNull();
    assertThat(pageSize.defaultValue()).isEqualTo("" + QueryContext.DEFAULT_LIMIT);

  }
}
