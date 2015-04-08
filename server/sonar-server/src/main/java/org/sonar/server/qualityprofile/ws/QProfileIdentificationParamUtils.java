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

import com.google.common.base.Preconditions;
import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.QProfileFactory;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class QProfileIdentificationParamUtils {

  public static final String PARAM_LANGUAGE = "language";
  public static final String PARAM_PROFILE_NAME = "profileName";
  public static final String PARAM_PROFILE_KEY = "profileKey";

  private QProfileIdentificationParamUtils() {
    // Utility class
  }

  public static void defineProfileParams(NewAction action, Languages languages) {
    action.createParam(PARAM_PROFILE_KEY)
      .setDescription("A quality profile key. Either this parameter, or a combination of profileName + language must be set.")
      .setExampleValue("sonar-way-java-12345");
    action.createParam(PARAM_PROFILE_NAME)
      .setDescription("A quality profile name. If this parameter is set, profileKey must not be set and language must be set to disambiguate.")
      .setExampleValue("Sonar way");
    action.createParam(PARAM_LANGUAGE)
      .setDescription("A quality profile language. If this parameter is set, profileKey must not be set and profileName must be set to disambiguate.")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setExampleValue("js");
  }

  public static String getProfileKeyFromParameters(Request request, QProfileFactory profileFactory, DbSession session) {
    String language = request.param(PARAM_LANGUAGE);
    String profileName = request.param(PARAM_PROFILE_NAME);
    String profileKey = request.param(PARAM_PROFILE_KEY);

    Preconditions.checkArgument(
      (!isEmpty(language) && !isEmpty(profileName)) ^ !isEmpty(profileKey), "Either profileKey or profileName + language must be set");

    if (profileKey == null) {
      profileKey = getProfileKeyFromLanguageAndName(language, profileName, profileFactory, session);
    }
    return profileKey;
  }

  public static String getProfileKeyFromLanguageAndName(String language, String profileName, QProfileFactory profileFactory, DbSession session) {
    QualityProfileDto profile = profileFactory.getByNameAndLanguage(session, profileName, language);
    if (profile == null) {
      throw new NotFoundException(String.format("Unable to find a profile for language '%s' with name '%s'", language, profileName));
    }
    return profile.getKey();
  }

}
