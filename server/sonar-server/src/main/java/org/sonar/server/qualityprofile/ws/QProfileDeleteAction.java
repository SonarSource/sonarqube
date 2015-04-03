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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.user.UserSession;

public class QProfileDeleteAction implements BaseQProfileWsAction {

  private final Languages languages;
  private final QProfileFactory profileFactory;
  private final DbClient dbClient;

  public QProfileDeleteAction(Languages languages, QProfileFactory profileFactory, DbClient dbClient) {
    this.languages = languages;
    this.profileFactory = profileFactory;
    this.dbClient = dbClient;
  }

  @Override
  public void define(NewController controller) {
    NewAction action = controller.createAction("delete")
      .setDescription("Delete a quality profile and all its descendants. The default quality profile cannot be deleted.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    QProfileIdentificationParamUtils.defineProfileParams(action, languages);
  }


  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession.get().checkLoggedIn();
    UserSession.get().checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);


    DbSession session = dbClient.openSession(false);
    try {
      String profileKey = QProfileIdentificationParamUtils.getProfileKeyFromParameters(request, profileFactory, session);
      profileFactory.delete(session, profileKey, false);

      session.commit();
    } finally {
      session.close();
    }

    response.noContent();
  }
}
