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

import org.sonar.api.resources.Languages;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.component.ws.LanguageParamUtils;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class QProfileFinder {

  private final DbClient dbClient;
  private final Languages languages;

  public QProfileFinder(DbClient dbClient, Languages languages) {
    this.dbClient = dbClient;
    this.languages = languages;
  }

  public void defineProfileParams(WebService.NewAction action) {
    action.createParam(QProfileIdentificationParamUtils.PARAM_PROFILE_KEY)
      .setDescription("A quality profile key. Either this parameter, or a combination of profileName + language must be set.")
      .setExampleValue("sonar-way-java-12345");
    action.createParam(QProfileIdentificationParamUtils.PARAM_PROFILE_NAME)
      .setDescription("A quality profile name. If this parameter is set, profileKey must not be set and language must be set to disambiguate.")
      .setExampleValue("Sonar way");
    action.createParam(QProfileIdentificationParamUtils.PARAM_LANGUAGE)
      .setDescription("A quality profile language. If this parameter is set, profileKey must not be set and profileName must be set to disambiguate.")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setExampleValue("js");
  }

  public QualityProfileDto find(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return find(request, dbSession);
    }
  }

  public QualityProfileDto find(Request request, DbSession dbSession) {
    String language = request.param(QProfileIdentificationParamUtils.PARAM_LANGUAGE);
    String profileName = request.param(QProfileIdentificationParamUtils.PARAM_PROFILE_NAME);
    String profileKey = request.param(QProfileIdentificationParamUtils.PARAM_PROFILE_KEY);

    checkArgument(
      (!isEmpty(language) && !isEmpty(profileName)) ^ !isEmpty(profileKey), "Either profileKey or profileName + language must be set");

    if (profileKey != null) {
      return findByKey(dbSession, profileKey);
    }
    return findByName(dbSession, language, profileName);
  }

  private QualityProfileDto findByKey(DbSession dbSession, String profileKey) {
    QualityProfileDto profile;
    profile = dbClient.qualityProfileDao().selectByKey(dbSession, profileKey);
    if (profile == null) {
      throw new NotFoundException(format("Unable to find a profile for with key '%s'", profileKey));
    }
    return profile;
  }

  private QualityProfileDto findByName(DbSession dbSession, String language, String profileName) {
    QualityProfileDto profile;
    profile = dbClient.qualityProfileDao().selectByNameAndLanguage(profileName, language, dbSession);
    if (profile == null) {
      throw new NotFoundException(format("Unable to find a profile for language '%s' with name '%s'", language, profileName));
    }
    return profile;
  }
}
