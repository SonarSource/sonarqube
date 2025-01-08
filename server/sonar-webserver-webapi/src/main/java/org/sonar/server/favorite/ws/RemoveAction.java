/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.favorite.ws;

import java.util.function.Consumer;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;

import static java.lang.String.format;
import static org.sonar.server.favorite.ws.FavoritesWsParameters.PARAM_COMPONENT;

public class RemoveAction implements FavoritesWsAction {
  private final UserSession userSession;
  private final DbClient dbClient;
  private final FavoriteUpdater favoriteUpdater;

  public RemoveAction(UserSession userSession, DbClient dbClient, FavoriteUpdater favoriteUpdater) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.favoriteUpdater = favoriteUpdater;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("remove")
      .setDescription("Remove a component (project, portfolio, application etc.) as favorite for the authenticated user.<br>" +
        "Requires authentication.")
      .setSince("6.3")
      .setChangelog(
        new Change("10.1", format("The use of module keys in parameter '%s' is removed", PARAM_COMPONENT)),
        new Change("7.6", format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)))
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setRequired(true)
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    removeFavorite().accept(request);
    response.noContent();
  }

  private Consumer<Request> removeFavorite() {
    return request -> {
      try (DbSession dbSession = dbClient.openSession(false)) {
        String key = request.mandatoryParam(PARAM_COMPONENT);
        EntityDto entity = dbClient.entityDao().selectByKey(dbSession, key)
          .orElseThrow(() -> new NotFoundException(format("Component with key '%s' not found", key)));
        userSession.checkLoggedIn();
        favoriteUpdater.remove(dbSession, entity,
          userSession.isLoggedIn() ? userSession.getUuid() : null,
          userSession.isLoggedIn() ? userSession.getLogin() : null);
        dbSession.commit();
      }
    };
  }
}
