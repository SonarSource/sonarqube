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
import org.sonar.api.server.ws.WebService;
import org.sonar.server.util.LanguageParamUtils;

import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_UUID;

public class ProjectAssociationParameters {

  private final Languages languages;

  public ProjectAssociationParameters(Languages languages) {
    this.languages = languages;
  }

  void addParameters(WebService.NewAction action) {
    action.setPost(true);
    action.createParam(PARAM_PROJECT_UUID)
      .setDescription("A project UUID. Either this parameter, or projectKey must be set.")
      .setExampleValue("69e57151-be0d-4157-adff-c06741d88879");
    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("A project key. Either this parameter, or projectUuid must be set.")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
    action.createParam(PARAM_PROFILE_KEY)
      .setDescription("A quality profile key. Either this parameter, or a combination of profileName + language must be set.")
      .setExampleValue("sonar-way-java-12345");
    action.createParam(PARAM_PROFILE_NAME)
      .setDescription("A quality profile name. If this parameter is set, profileKey must not be set and language must be set to disambiguate.")
      .setExampleValue("Soanr way");
    action.createParam(PARAM_LANGUAGE)
      .setDescription("A quality profile language. If this parameter is set, profileKey must not be set and profileName must be set to disambiguate.")
      .setPossibleValues(LanguageParamUtils.getLanguageKeys(languages))
      .setExampleValue("js");
  }

}
