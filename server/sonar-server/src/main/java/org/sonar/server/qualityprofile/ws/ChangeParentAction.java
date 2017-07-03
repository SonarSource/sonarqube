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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_PROFILE;

public class ChangeParentAction implements QProfileWsAction {

  private static final String PARAM_PARENT_NAME = "parentName";

  private DbClient dbClient;
  private final RuleActivator ruleActivator;
  private final Languages languages;
  private final QProfileWsSupport wsSupport;
  private final UserSession userSession;

  public ChangeParentAction(DbClient dbClient, RuleActivator ruleActivator,
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
        "Requires to be logged in and the 'Administer Quality Profiles' permission.")
      .setHandler(this);

    QProfileWsSupport.createOrganizationParam(inheritance)
      .setSince("6.4");
    QProfileReference.defineParams(inheritance, languages);

    inheritance.createParam(PARAM_PARENT_PROFILE)
      .setDescription("New parent profile key.<br> " +
        "If no profile is provided, the inheritance link with current parent profile (if any) is broken, which deactivates all rules " +
        "which come from the parent and are not overridden.")
      .setDeprecatedKey("parentKey", "6.5")
      .setExampleValue(UUID_EXAMPLE_02);

    inheritance.createParam(PARAM_PARENT_NAME)
      .setDescription("Quality profile name. If this parameter is set, '%s' must not be set and '%s' must be set to disambiguate.", PARAM_PARENT_PROFILE, PARAM_LANGUAGE)
      .setDeprecatedSince("6.5")
      .setExampleValue("Sonar way");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    QProfileReference reference = QProfileReference.from(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto profile = wsSupport.getProfile(dbSession, reference);
      String organizationUuid = profile.getOrganizationUuid();
      OrganizationDto organization = dbClient.organizationDao().selectByUuid(dbSession, organizationUuid)
        .orElseThrow(() -> new IllegalStateException(String.format("Could not find organization with uuid '%s' of profile '%s'", organizationUuid, profile.getKee())));
      userSession.checkPermission(ADMINISTER_QUALITY_PROFILES, organization);
      wsSupport.checkNotBuiltInt(profile);

      String parentKey = request.param(PARAM_PARENT_PROFILE);
      String parentName = request.param(PARAM_PARENT_NAME);
      if (isEmpty(parentKey) && isEmpty(parentName)) {
        ruleActivator.setParentAndCommit(dbSession, profile, null);
      } else {
        String parentOrganizationKey = parentKey == null ? organization.getKey() : null;
        String parentLanguage = parentKey == null ? request.param(PARAM_LANGUAGE) : null;
        QProfileReference parentRef = QProfileReference.from(parentKey, parentOrganizationKey, parentLanguage, parentName);
        QProfileDto parent = wsSupport.getProfile(dbSession, parentRef);
        ruleActivator.setParentAndCommit(dbSession, profile, parent);
      }

      response.noContent();
    }
  }
}
