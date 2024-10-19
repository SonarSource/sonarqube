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
package org.sonar.server.user.ws;

import static com.google.common.base.Preconditions.checkState;
import static org.sonarqube.ws.client.user.UsersWsParameters.ONBOARDED;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;

public class OnboardedAction implements UsersWsAction{
    private final UserSession userSession;
    private final DbClient dbClient;

    public OnboardedAction(UserSession userSession, DbClient dbClient) {
        this.userSession = userSession;
        this.dbClient = dbClient;
    }

    @Override
    public void define(NewController context) {
        context.createAction(ONBOARDED)
                .setPost(true)
                .setInternal(true)
                .setDescription("Stores that the user onboarding")
                .setSince("6.5")
                .setHandler(this);
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        userSession.checkLoggedIn();
        try (DbSession dbSession = dbClient.openSession(false)) {
            String userLogin = userSession.getLogin();
            UserDto userDto = dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin);
            checkState(userDto != null, "User login '%s' cannot be found", userLogin);
            if (!userDto.isOnboarded()) {
                userDto.setOnboarded(true);
                // no need to update Elasticsearch, the field onBoarded
                // is not indexed
                dbClient.userDao().update(dbSession, userDto);
                dbSession.commit();
            }
        }
        response.noContent();
    }

}
