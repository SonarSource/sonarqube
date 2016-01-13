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
package org.sonar.server.project.ws;

import com.google.common.io.Resources;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Sets.newHashSet;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;

public class GhostsAction implements ProjectsWsAction {
  public static final String ACTION = "ghosts";
  private static final Set<String> POSSIBLE_FIELDS = newHashSet("uuid", "key", "name", "creationDate");

  private final DbClient dbClient;
  private final UserSession userSession;

  public GhostsAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    context
      .createAction(ACTION)
      .setDescription("List ghost projects.<br /> Requires 'Administer System' permission.")
      .setResponseExample(Resources.getResource(getClass(), "projects-example-ghosts.json"))
      .setSince("5.2")
      .addPagingParams(100, MAX_LIMIT)
      .addFieldsParam(POSSIBLE_FIELDS)
      .addSearchQuery("sonar", "names", "keys")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkPermission(UserRole.ADMIN);
    DbSession dbSession = dbClient.openSession(false);
    SearchOptions searchOptions = new SearchOptions()
      .setPage(request.mandatoryParamAsInt(Param.PAGE),
        request.mandatoryParamAsInt(Param.PAGE_SIZE));
    Set<String> desiredFields = fieldsToReturn(request.paramAsStrings(Param.FIELDS));
    String query = request.param(Param.TEXT_QUERY);

    try {
      long nbOfProjects = dbClient.componentDao().countGhostProjects(dbSession, query);
      List<ComponentDto> projects = dbClient.componentDao().selectGhostProjects(dbSession, searchOptions.getOffset(), searchOptions.getLimit(), query);
      JsonWriter json = response.newJsonWriter().beginObject();
      writeProjects(json, projects, desiredFields);
      searchOptions.writeJson(json, nbOfProjects);
      json.endObject().close();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private static void writeProjects(JsonWriter json, List<ComponentDto> projects, Set<String> fieldsToReturn) {
    json.name("projects");
    json.beginArray();
    for (ComponentDto project : projects) {
      json.beginObject();
      json.prop("uuid", project.uuid());
      writeIfWished(json, "key", project.key(), fieldsToReturn);
      writeIfWished(json, "name", project.name(), fieldsToReturn);
      writeIfWished(json, "creationDate", project.getCreatedAt(), fieldsToReturn);
      json.endObject();
    }
    json.endArray();
  }

  private static void writeIfWished(JsonWriter json, String key, String value, Set<String> fieldsToReturn) {
    if (fieldsToReturn.contains(key)) {
      json.prop(key, value);
    }
  }

  private static void writeIfWished(JsonWriter json, String key, Date value, Set<String> desiredFields) {
    if (desiredFields.contains(key)) {
      json.propDateTime(key, value);
    }
  }

  private static Set<String> fieldsToReturn(@Nullable List<String> desiredFieldsFromRequest) {
    if (desiredFieldsFromRequest == null) {
      return POSSIBLE_FIELDS;
    }

    return newHashSet(desiredFieldsFromRequest);
  }
}
