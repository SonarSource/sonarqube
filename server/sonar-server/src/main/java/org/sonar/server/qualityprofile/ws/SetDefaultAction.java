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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.user.UserSession;

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;

public class SetDefaultAction implements QProfileWsAction {

  private final Languages languages;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final QProfileWsSupport qProfileWsSupport;

  public SetDefaultAction(Languages languages, DbClient dbClient, UserSession userSession, QProfileWsSupport qProfileWsSupport) {
    this.languages = languages;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.qProfileWsSupport = qProfileWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction setDefault = controller.createAction("set_default")
      .setSince("5.2")
      .setDescription("Select the default profile for a given language. Require Administer Quality Profiles permission.")
      .setPost(true)
      .setHandler(this);

    qProfileWsSupport.createOrganizationParam(setDefault)
      .setSince("6.4");

    QProfileReference.defineParams(setDefault, languages);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityProfileDto qualityProfile = qProfileWsSupport.getProfile(dbSession, QProfileReference.from(request));
      userSession.checkPermission(ADMINISTER_QUALITY_PROFILES, qualityProfile.getOrganizationUuid());
      setDefault(dbSession, qualityProfile);
      dbSession.commit();
    }
    response.noContent();
  }

  public void setDefault(DbSession session, QualityProfileDto qualityProfile) {
    QualityProfileDto previousDefault = dbClient.qualityProfileDao().selectDefaultProfile(session, qualityProfile.getLanguage());
    if (previousDefault != null) {
      dbClient.qualityProfileDao().update(session, previousDefault.setDefault(false));
    }
    dbClient.qualityProfileDao().update(session, qualityProfile.setDefault(true));
  }
}
