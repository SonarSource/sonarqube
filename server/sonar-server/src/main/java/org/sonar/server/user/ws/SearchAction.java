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

package org.sonar.server.user.ws;

import com.google.common.collect.ImmutableSet;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Set;

public class SearchAction implements BaseUsersWsAction {

  private static final String FIELD_LOGIN = "login";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_EMAIL = "email";
  private static final String FIELD_SCM_ACCOUNTS = "scmAccounts";
  private static final Set<String> FIELDS = ImmutableSet.of(FIELD_LOGIN, FIELD_NAME, FIELD_EMAIL, FIELD_SCM_ACCOUNTS);

  private final UserIndex userIndex;

  public SearchAction(UserIndex userIndex) {
    this.userIndex = userIndex;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search2")
      .setDescription("Get a list of active users.")
      .setSince("3.6")
      .setHandler(this);

    action.addFieldsParam(FIELDS);
    action.addPagingParams(50);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Filter on login or name.");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchOptions options = new SearchOptions()
      .setPage(request.mandatoryParamAsInt(Param.PAGE), request.mandatoryParamAsInt(Param.PAGE_SIZE));
    List<String> fields = request.paramAsStrings(Param.FIELDS);
    SearchResult<UserDoc> result = userIndex.search(request.param(Param.TEXT_QUERY), options);

    JsonWriter json = response.newJsonWriter().beginObject();
    options.writeJson(json, result.getTotal());
    writeUsers(json, result, fields);
    json.endObject().close();
  }

  private void writeUsers(JsonWriter json, SearchResult<UserDoc> result, @Nullable List<String> fields) {

    json.name("users").beginArray();
    for (UserDoc user : result.getDocs()) {
      json.beginObject();
      writeIfNeeded(json, user.login(), FIELD_LOGIN, fields);
      writeIfNeeded(json, user.name(), FIELD_NAME, fields);
      writeIfNeeded(json, user.email(), FIELD_EMAIL, fields);
      if (fieldIsWanted(FIELD_SCM_ACCOUNTS, fields)) {
        json.name(FIELD_SCM_ACCOUNTS)
          .beginArray()
          .values(user.scmAccounts())
          .endArray();
      }
      json.endObject();
    }
    json.endArray();
  }

  private void writeIfNeeded(JsonWriter json, String value, String field, @Nullable List<String> fields) {
    if (fieldIsWanted(field, fields)) {
      json.prop(field, value);
    }
  }

  private boolean fieldIsWanted(String field, @Nullable List<String> fields) {
    return fields == null || fields.isEmpty() || fields.contains(field);
  }
}
