/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileRef;

public class DeleteAction implements QProfileWsAction {

  private final Languages languages;
  private final QProfileFactory profileFactory;
  private final DbClient dbClient;
  private final QProfileWsSupport qProfileWsSupport;

  public DeleteAction(Languages languages, QProfileFactory profileFactory, DbClient dbClient, QProfileWsSupport qProfileWsSupport) {
    this.languages = languages;
    this.profileFactory = profileFactory;
    this.dbClient = dbClient;
    this.qProfileWsSupport = qProfileWsSupport;
  }

  @Override
  public void define(NewController controller) {
    NewAction action = controller.createAction("delete")
      .setDescription("Delete a quality profile and all its descendants. The default quality profile cannot be deleted. " +
        "Require Administer Quality Profiles permission.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    QProfileRef.defineParams(action, languages);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    qProfileWsSupport.checkQProfileAdminPermission();

    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityProfileDto profile = profileFactory.find(dbSession, QProfileRef.from(request));
      profileFactory.delete(dbSession, profile.getKey(), false);
      dbSession.commit();
    }

    response.noContent();
  }
}
