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

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileCopier;
import org.sonar.server.user.UserSession;

public class CopyAction implements QProfileWsAction {

  private static final String PARAM_PROFILE_NAME = "toName";
  private static final String PARAM_PROFILE_KEY = "fromKey";

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
    NewAction action = controller.createAction("copy")
        .setSince("5.2")
        .setDescription("Copy a quality profile. Require Administer Quality Profiles permission.")
        .setPost(true)
        .setHandler(this);

    action.createParam(PARAM_PROFILE_NAME)
        .setDescription("The name for the new quality profile.")
        .setExampleValue("My Sonar way")
        .setRequired(true);

    action.createParam(PARAM_PROFILE_KEY)
        .setDescription("The key of a quality profile.")
        .setExampleValue(Uuids.UUID_EXAMPLE_01)
        .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String newName = request.mandatoryParam(PARAM_PROFILE_NAME);
    String profileKey = request.mandatoryParam(PARAM_PROFILE_KEY);

    try (DbSession dbSession = dbClient.openSession(false)) {
      QualityProfileDto sourceProfile = wsSupport.getProfile(dbSession, QProfileReference.fromKey(profileKey));
      userSession.checkPermission(OrganizationPermission.ADMINISTER_QUALITY_PROFILES, sourceProfile.getOrganizationUuid());

      QualityProfileDto copiedProfile = profileCopier.copyToName(dbSession, sourceProfile, newName);

      String languageKey = copiedProfile.getLanguage();
      Language language = languages.get(copiedProfile.getLanguage());
      String parentKey = copiedProfile.getParentKee();
      response.newJsonWriter()
          .beginObject()
          .prop("key", copiedProfile.getKey())
          .prop("name", copiedProfile.getName())
          .prop("language", languageKey)
          .prop("languageName", language == null ? null : language.getName())
          .prop("isDefault", copiedProfile.isDefault())
          .prop("isInherited", parentKey != null)
          .prop("parentKey", parentKey)
          .endObject().close();
    }
  }
}
