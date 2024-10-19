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
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.QProfileTree;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;

public class ChangeParentAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final QProfileTree ruleActivator;
  private final Languages languages;
  private final QProfileWsSupport wsSupport;
  private final UserSession userSession;

  public ChangeParentAction(DbClient dbClient, QProfileTree ruleActivator,
    Languages languages, QProfileWsSupport wsSupport, UserSession userSession) {
    this.dbClient = dbClient;
    this.ruleActivator = ruleActivator;
    this.languages = languages;
    this.wsSupport = wsSupport;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController context) {
    NewAction inheritance = context.createAction("change_parent")
      .setSince("5.2")
      .setPost(true)
      .setDescription("Change a quality profile's parent.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setHandler(this);

    QProfileWsSupport.createOrganizationParam(inheritance);
    QProfileReference.defineParams(inheritance, languages);

    inheritance.createParam(QualityProfileWsParameters.PARAM_PARENT_QUALITY_PROFILE)
      .setDescription("New parent profile name. <br> " +
        "If no profile is provided, the inheritance link with current parent profile (if any) is broken, which deactivates all rules " +
        "which come from the parent and are not overridden.")
      .setExampleValue("Sonar way");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    QProfileReference reference = QProfileReference.fromName(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = wsSupport.getProfile(dbSession, reference);
      OrganizationDto organization = wsSupport.getOrganization(dbSession, profile);
      wsSupport.checkCanEdit(dbSession, organization, profile);

      String parentName = request.param(QualityProfileWsParameters.PARAM_PARENT_QUALITY_PROFILE);
      if (isEmpty(parentName)) {
        ruleActivator.removeParentAndCommit(dbSession, profile);
      } else {
        String parentLanguage = request.mandatoryParam(PARAM_LANGUAGE);
        QProfileReference parentRef = QProfileReference.fromName(organization.getKey(), parentLanguage, parentName);
        QProfileDto parent = wsSupport.getProfile(dbSession, parentRef);
        ruleActivator.setParentAndCommit(dbSession, profile, parent);
      }

      response.noContent();
    }
  }
}
