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
package org.sonar.server.favorite.ws;

import java.util.List;
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static org.sonar.api.resources.Qualifiers.APP;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.api.resources.Qualifiers.SUBVIEW;
import static org.sonar.api.resources.Qualifiers.VIEW;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.favorite.ws.FavoritesWsParameters.PARAM_COMPONENT;

public class AddAction implements FavoritesWsAction {

  private static final List<String> SUPPORTED_QUALIFIERS = asList(PROJECT, VIEW, SUBVIEW, APP);
  private static final String SUPPORTED_QUALIFIERS_AS_STRING = join(", ", SUPPORTED_QUALIFIERS);

  private final UserSession userSession;
  private final DbClient dbClient;
  private final FavoriteUpdater favoriteUpdater;

  public AddAction(UserSession userSession, DbClient dbClient, FavoriteUpdater favoriteUpdater) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.favoriteUpdater = favoriteUpdater;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("add")
      .setDescription("Add a component (project, portfolio, etc.) as favorite for the authenticated user.<br>" +
        "Only 100 components by qualifier can be added as favorite.<br>" +
        "Requires authentication and the following permission: 'Browse' on the component.")
      .setSince("6.3")
      .setChangelog(
        new Change("10.1", String.format("The use of module keys in parameter '%s' is removed", PARAM_COMPONENT)),
        new Change("8.4", "It's no longer possible to set a file as favorite"),
        new Change("7.7", "It's no longer possible to have more than 100 favorites by qualifier"),
        new Change("7.7", "It's no longer possible to set a directory as favorite"),
        new Change("7.6", format("The use of module keys in parameter '%s' is deprecated", PARAM_COMPONENT)))
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setDescription(format("Component key. Only components with qualifiers %s are supported", SUPPORTED_QUALIFIERS_AS_STRING))
      .setRequired(true)
      .setExampleValue(KeyExamples.KEY_FILE_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    addFavorite().accept(request);
    response.noContent();
  }

  private Consumer<Request> addFavorite() {
    return request -> {
      try (DbSession dbSession = dbClient.openSession(false)) {
        EntityDto entity = dbClient.entityDao().selectByKey(dbSession, request.mandatoryParam(PARAM_COMPONENT))
          .orElseThrow(() -> new NotFoundException(format("Entity with key '%s' not found", request.mandatoryParam(PARAM_COMPONENT))));

        checkArgument(SUPPORTED_QUALIFIERS.contains(entity.getQualifier()), "Only components with qualifiers %s are supported", SUPPORTED_QUALIFIERS_AS_STRING);
        userSession
          .checkLoggedIn()
          .checkEntityPermission(USER, entity);
        String userUuid = userSession.isLoggedIn() ? userSession.getUuid() : null;
        String userLogin = userSession.isLoggedIn() ? userSession.getLogin() : null;
        favoriteUpdater.add(dbSession, entity, userUuid, userLogin, true);
        dbSession.commit();
      }
    };
  }
}
