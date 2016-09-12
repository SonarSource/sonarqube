/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Preconditions;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfile;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.LanguageParamUtils;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class SetDefaultAction implements QProfileWsAction {

  private static final String PARAM_LANGUAGE = "language";
  private static final String PARAM_PROFILE_NAME = "profileName";
  private static final String PARAM_PROFILE_KEY = "profileKey";

  private final Languages languages;

  private final QProfileLookup profileLookup;

  private final QProfileFactory profileFactory;
  private final UserSession userSession;

  public SetDefaultAction(Languages languages, QProfileLookup profileLookup, QProfileFactory profileFactory, UserSession userSession) {
    this.languages = languages;
    this.profileLookup = profileLookup;
    this.profileFactory = profileFactory;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction setDefault = controller.createAction("set_default")
      .setSince("5.2")
      .setDescription("Select the default profile for a given language. Require Administer Quality Profiles permission.")
      .setPost(true)
      .setHandler(this);

    setDefault.createParam(PARAM_LANGUAGE)
      .setDescription("The key of a language supported by the platform. If specified, profileName must be set to select the default profile for the selected language.")
      .setExampleValue("js")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages));

    setDefault.createParam(PARAM_PROFILE_NAME)
      .setDescription("The name of a quality profile. If specified, language must be set. The matching profile will be used as default for the selected language.")
      .setExampleValue("Sonar way");

    setDefault.createParam(PARAM_PROFILE_KEY)
      .setDescription("The key of a quality profile. If specified, language and profileName must not be set. The matching profile will be used as default for its language.")
      .setExampleValue("sonar-way-js-12345");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);

    String language = request.param(PARAM_LANGUAGE);
    String profileName = request.param(PARAM_PROFILE_NAME);
    String profileKey = request.param(PARAM_PROFILE_KEY);

    Preconditions.checkArgument(
        (!isEmpty(language) && !isEmpty(profileName)) ^ !isEmpty(profileKey), "Either profileKey or profileName + language must be set");

    if(profileKey == null) {
      profileKey = getProfileKeyFromLanguageAndName(language, profileName);
    }

    profileFactory.setDefault(profileKey);

    response.noContent();
  }

  private String getProfileKeyFromLanguageAndName(String language, String profileName) {
    QProfile profile = profileLookup.profile(profileName, language);
    if (profile == null) {
      throw new NotFoundException(String.format("Unable to find a profile for language '%s' with name '%s'", language, profileName));
    }
    return profile.key();
  }
}
