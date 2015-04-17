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

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.qualityprofile.QProfileCopier;
import org.sonar.server.user.UserSession;

public class QProfileCopyAction implements BaseQProfileWsAction {

  private static final String PARAM_PROFILE_NAME = "toName";
  private static final String PARAM_PROFILE_KEY = "fromKey";

  private final QProfileCopier profileCopier;
  private final Languages languages;

  public QProfileCopyAction(QProfileCopier profileCopier, Languages languages) {
    this.profileCopier = profileCopier;
    this.languages = languages;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction setDefault = controller.createAction("copy")
      .setSince("5.2")
      .setDescription("Copy a quality profile.")
      .setPost(true)
      .setHandler(this);

    setDefault.createParam(PARAM_PROFILE_NAME)
      .setDescription("The name for the new quality profile.")
      .setExampleValue("My Sonar way")
      .setRequired(true);

    setDefault.createParam(PARAM_PROFILE_KEY)
      .setDescription("The key of a quality profile.")
      .setExampleValue("sonar-way-js-12345")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession.get().checkLoggedIn().checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    String newName = request.mandatoryParam(PARAM_PROFILE_NAME);
    String profileKey = request.mandatoryParam(PARAM_PROFILE_KEY);

    QualityProfileDto copiedProfile = profileCopier.copyToName(profileKey, newName);

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
