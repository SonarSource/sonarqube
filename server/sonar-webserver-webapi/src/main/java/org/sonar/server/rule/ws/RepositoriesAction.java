/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.rule.ws;

import com.google.common.io.Resources;
import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleRepositoryDto;


/**
 * @since 5.1
 */
public class RepositoriesAction implements RulesWsAction {

  private static final String LANGUAGE = "language";

  private final DbClient dbClient;

  public RepositoriesAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("repositories")
      .setDescription("List available rule repositories")
      .setSince("4.5")
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "repositories-example.json"));

    action.createParam(Param.TEXT_QUERY)
      .setDescription("A pattern to match repository keys/names against")
      .setExampleValue("java");
    action.createParam(LANGUAGE)
      .setDescription("A language key; if provided, only repositories for the given language will be returned")
      .setExampleValue("java");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String query = request.param(Param.TEXT_QUERY);
    String languageKey = request.param(LANGUAGE);

    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject().name("repositories").beginArray();
      for (RuleRepositoryDto repo : listMatchingRepositories(query, languageKey)) {
        json
          .beginObject()
          .prop("key", repo.getKey())
          .prop("name", repo.getName())
          .prop(LANGUAGE, repo.getLanguage())
          .endObject();
      }
      json.endArray().endObject();
    }
  }

  private Collection<RuleRepositoryDto> listMatchingRepositories(@Nullable String query, @Nullable String languageKey) {
    return selectFromDb(languageKey,  query);
  }

  private Collection<RuleRepositoryDto> selectFromDb(@Nullable String language, @Nullable  String query) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.ruleRepositoryDao().selectByQueryAndLanguage(dbSession, query, language);
    }
  }
}
