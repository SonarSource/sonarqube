/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Arrays;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QProfileEditGroupsDto;
import org.sonar.db.user.GroupDto;

import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ADD_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;

public class AddGroupAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final UuidFactory uuidFactory;
  private final QProfileWsSupport wsSupport;
  private final Languages languages;

  public AddGroupAction(DbClient dbClient, UuidFactory uuidFactory, QProfileWsSupport wsSupport, Languages languages) {
    this.dbClient = dbClient;
    this.uuidFactory = uuidFactory;
    this.wsSupport = wsSupport;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction(ACTION_ADD_GROUP)
      .setDescription("Allow a group to edit a Quality Profile.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setHandler(this)
      .setPost(true)
      .setInternal(true)
      .setSince("6.6");

    action.createParam(PARAM_QUALITY_PROFILE)
      .setDescription("Quality Profile name")
      .setRequired(true)
      .setExampleValue("Recommended quality profile");

    action
      .createParam(PARAM_LANGUAGE)
      .setDescription("Quality profile language")
      .setRequired(true)
      .setPossibleValues(Arrays.stream(languages.all()).map(Language::getKey).collect(toSet()));

    action.createParam(PARAM_GROUP)
      .setDescription("Group name")
      .setRequired(true)
      .setExampleValue("sonar-administrators");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganizationByKey(dbSession, request.mandatoryParam(PARAM_ORGANIZATION));
      QProfileDto profile = wsSupport.getProfile(dbSession, organization, request.mandatoryParam(PARAM_QUALITY_PROFILE), request.mandatoryParam(PARAM_LANGUAGE));
      wsSupport.checkCanEdit(dbSession, organization, profile);
      GroupDto user = wsSupport.getGroup(dbSession, organization, request.mandatoryParam(PARAM_GROUP));
      addGroup(dbSession, profile, user);
    }
    response.noContent();
  }

  private void addGroup(DbSession dbSession, QProfileDto profile, GroupDto group) {
    if (dbClient.qProfileEditGroupsDao().exists(dbSession, profile, group)) {
      return;
    }
    dbClient.qProfileEditGroupsDao().insert(dbSession,
      new QProfileEditGroupsDto()
        .setUuid(uuidFactory.create())
        .setGroupUuid(group.getUuid())
        .setQProfileUuid(profile.getKee()),
      profile.getName(),
      group.getName());
    dbSession.commit();
  }
}
