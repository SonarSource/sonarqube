/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.qualityprofile.DefaultQProfileDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SET_DEFAULT;

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
    NewAction setDefault = controller.createAction(ACTION_SET_DEFAULT)
      .setSince("5.2")
      .setDescription("Select the default profile for a given language.<br> " +
        "Requires to be logged in and the 'Administer Quality Profiles' permission.")
      .setPost(true)
      .setHandler(this);

    QProfileWsSupport.createOrganizationParam(setDefault).setSince("6.4");
    QProfileReference.defineParams(setDefault, languages);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession.checkLoggedIn();
    QProfileReference reference = QProfileReference.from(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto qualityProfile = qProfileWsSupport.getProfile(dbSession, reference);
      dbClient.organizationDao().selectByUuid(dbSession, qualityProfile.getOrganizationUuid())
        .orElseThrow(() -> new IllegalStateException(
          format("Cannot find organization '%s' for quality profile '%s'", qualityProfile.getOrganizationUuid(), qualityProfile.getKee())));
      userSession.checkPermission(ADMINISTER_QUALITY_PROFILES, qualityProfile.getOrganizationUuid());
      setDefault(dbSession, qualityProfile);
      dbSession.commit();
    }
    response.noContent();
  }

  public void setDefault(DbSession dbSession, QProfileDto profile) {
    dbClient.defaultQProfileDao().insertOrUpdate(dbSession, DefaultQProfileDto.from(profile));
  }
}
