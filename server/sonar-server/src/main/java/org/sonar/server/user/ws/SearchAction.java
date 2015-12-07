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

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.DbClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

import static org.sonar.server.es.SearchOptions.MAX_LIMIT;

public class SearchAction implements UsersWsAction {

  private final UserIndex userIndex;
  private final DbClient dbClient;
  private final UserJsonWriter userWriter;

  public SearchAction(UserIndex userIndex, DbClient dbClient, UserJsonWriter userWriter) {
    this.userIndex = userIndex;
    this.dbClient = dbClient;
    this.userWriter = userWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Get a list of active users. Administer System permission is required to show the 'groups' field.")
      .setSince("3.6")
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-search.json"));

    action.addFieldsParam(UserJsonWriter.FIELDS);
    action.addPagingParams(50, MAX_LIMIT);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Filter on login or name.");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchOptions options = new SearchOptions()
      .setPage(request.mandatoryParamAsInt(Param.PAGE), request.mandatoryParamAsInt(Param.PAGE_SIZE));
    List<String> fields = request.paramAsStrings(Param.FIELDS);
    SearchResult<UserDoc> result = userIndex.search(request.param(Param.TEXT_QUERY), options);

    Multimap<String, String> groupsByLogin = ArrayListMultimap.create();
    DbSession dbSession = dbClient.openSession(false);
    try {
      Collection<String> logins = Collections2.transform(result.getDocs(), new Function<UserDoc, String>() {
        @Override
        public String apply(@Nonnull UserDoc input) {
          return input.login();
        }
      });
      groupsByLogin = dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, logins);
    } finally {
      MyBatis.closeQuietly(dbSession);
    }

    JsonWriter json = response.newJsonWriter().beginObject();
    options.writeJson(json, result.getTotal());
    writeUsers(json, result, fields, groupsByLogin);
    json.endObject().close();
  }

  private void writeUsers(JsonWriter json, SearchResult<UserDoc> result, @Nullable List<String> fields, Multimap<String, String> groupsByLogin) {

    json.name("users").beginArray();
    for (UserDoc user : result.getDocs()) {
      Collection<String> groups = groupsByLogin.get(user.login());
      userWriter.write(json, user, groups, fields);
    }
    json.endArray();
  }
}
