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

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.qualityprofile.QProfileCopier;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualityprofiles.CopyWsResponse;

import static java.util.Optional.ofNullable;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_COPY;

public class CopyAction implements QProfileWsAction {

  private static final String PARAM_TO_NAME = "toName";
  private static final String PARAM_FROM_KEY = "fromKey";

  private final DbClient dbClient;
  private final QProfileCopier profileCopier;
  private final Languages languages;
  private final UserSession userSession;
  private final QProfileWsSupport wsSupport;

  public CopyAction(DbClient dbClient, QProfileCopier profileCopier, Languages languages, UserSession userSession, QProfileWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.profileCopier = profileCopier;
    this.languages = languages;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_COPY)
      .setSince("5.2")
      .setDescription("Copy a quality profile.<br> " +
        "Requires to be logged in and the 'Administer Quality Profiles' permission.")
      .setResponseExample(getClass().getResource("copy-example.json"))
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_TO_NAME)
      .setDescription("Name for the new quality profile.")
      .setExampleValue("My Sonar way")
      .setRequired(true);

    action.createParam(PARAM_FROM_KEY)
      .setDescription("Quality profile key")
      .setExampleValue(UUID_EXAMPLE_01)
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String newName = request.mandatoryParam(PARAM_TO_NAME);
    String profileKey = request.mandatoryParam(PARAM_FROM_KEY);

    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto sourceProfile = wsSupport.getProfile(dbSession, QProfileReference.fromKey(profileKey));
      userSession.checkPermission(ADMINISTER_QUALITY_PROFILES, sourceProfile.getOrganizationUuid());

      QProfileDto copiedProfile = profileCopier.copyToName(dbSession, sourceProfile, newName);
      boolean isDefault = dbClient.defaultQProfileDao().isDefault(dbSession, copiedProfile.getOrganizationUuid(), copiedProfile.getKee());

      CopyWsResponse wsResponse = buildResponse(copiedProfile, isDefault);

      writeProtobuf(wsResponse, request, response);
    }
  }

  private CopyWsResponse buildResponse(QProfileDto copiedProfile, boolean isDefault) {
    String languageKey = copiedProfile.getLanguage();
    Language language = languages.get(copiedProfile.getLanguage());
    String parentKey = copiedProfile.getParentKee();

    CopyWsResponse.Builder wsResponse = CopyWsResponse.newBuilder();

    wsResponse.setKey(copiedProfile.getKee());
    wsResponse.setName(copiedProfile.getName());
    wsResponse.setLanguage(languageKey);
    ofNullable(language).ifPresent(l -> wsResponse.setLanguageName(l.getName()));
    wsResponse.setIsDefault(isDefault);
    wsResponse.setIsInherited(parentKey != null);
    ofNullable(parentKey).ifPresent(wsResponse::setParentKey);
    return wsResponse.build();
  }
}
