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
package org.sonar.server.search.ws;

import com.google.common.collect.Lists;
import java.io.StringWriter;
import org.junit.Test;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.internal.SimpleGetRequest;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.test.JsonAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SearchOptionsTest {

  @Test
  public void create_from_http_request() {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "3");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "name,repo");
    request.setParam("severities", "BLOCKER");

    SearchOptions options = SearchOptions.create(request);

    assertThat(options.fields()).containsOnly("name", "repo");
    assertThat(options.page()).isEqualTo(3);
    assertThat(options.pageSize()).isEqualTo(10);
  }

  @Test
  public void hasField() {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "3");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "name,repo");
    SearchOptions options = SearchOptions.create(request);

    assertThat(options.hasField("repo")).isTrue();
    assertThat(options.hasField("severity")).isFalse();
  }

  @Test
  public void hasField_always_true_by_default() {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "3");
    request.setParam(Param.PAGE_SIZE, "10");
    SearchOptions options = SearchOptions.create(request);

    assertThat(options.hasField("repo")).isTrue();
  }

  @Test
  public void hasField_no_if_empty_value() {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "3");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "");
    SearchOptions options = SearchOptions.create(request);

    assertThat(options.hasField("repo")).isFalse();
  }

  @Test
  public void write_statistics_to_json_response() {
    SimpleGetRequest request = new SimpleGetRequest();
    request.setParam(Param.PAGE, "3");
    request.setParam(Param.PAGE_SIZE, "10");
    request.setParam(Param.FIELDS, "name,repo");
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
  public void defineFieldsParam() {
    WebService.Context context = new WebService.Context();
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/foo");
        NewAction action = newController
          .createAction("search")
          .setSince("5.3")
          .setDescription("Search Description")
          .setPost(true)
          .setHandler(mock(RequestHandler.class));
        SearchOptions.defineFieldsParam(action, Lists.newArrayList("name", "lang", "severity"));
        newController.done();
      }
    }.define(context);

    WebService.Action searchAction = context.controller("api/foo").action("search");
    Param param = searchAction.param(Param.FIELDS);
    assertThat(param).isNotNull();
    assertThat(param.possibleValues()).containsOnly("name", "lang", "severity");
    assertThat(param.exampleValue()).isEqualTo("name,lang");
  }

  @Test
  public void definePageParams() {
    WebService.Context context = new WebService.Context();
    new WebService() {
      @Override
      public void define(Context context) {
        NewController newController = context.createController("api/foo");
        NewAction action = newController
          .createAction("search")
          .setSince("5.3")
          .setDescription("Search Description")
          .setPost(true)
          .setHandler(mock(RequestHandler.class));
        SearchOptions.definePageParams(action);
        newController.done();
      }
    }.define(context);

    WebService.Action searchAction = context.controller("api/foo").action("search");
    Param page = searchAction.param(Param.PAGE);
    assertThat(page).isNotNull();
    assertThat(page.defaultValue()).isEqualTo("1");
    Param pageSize = searchAction.param(Param.PAGE_SIZE);
    assertThat(pageSize).isNotNull();
    assertThat(pageSize.defaultValue()).isEqualTo("" + QueryContext.DEFAULT_LIMIT);

  }
}
