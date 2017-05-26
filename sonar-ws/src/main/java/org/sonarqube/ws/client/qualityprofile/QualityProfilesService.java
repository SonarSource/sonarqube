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
package org.sonarqube.ws.client.qualityprofile;

import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ADD_PROJECT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_REMOVE_PROJECT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_RESTORE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.CONTROLLER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_UUID;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.RestoreActionParameters.PARAM_BACKUP;

public class QualityProfilesService extends BaseService {

  public QualityProfilesService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_QUALITY_PROFILES);
  }

  public void activateRule(ActivateRuleWsRequest request) {
    PostRequest httpRequest = new PostRequest(path(ACTION_ACTIVATE_RULE));
    httpRequest.setParam(PARAM_ORGANIZATION, request.getOrganization().orElse(null));
    httpRequest.setParam(ActivateActionParameters.PARAM_PARAMS, request.getParams().orElse(null));
    httpRequest.setParam(ActivateActionParameters.PARAM_PROFILE_KEY, request.getProfileKey());
    httpRequest.setParam(ActivateActionParameters.PARAM_RESET, request.getReset().orElse(null));
    httpRequest.setParam(ActivateActionParameters.PARAM_RULE_KEY, request.getRuleKey());
    httpRequest.setParam(ActivateActionParameters.PARAM_SEVERITY, request.getSeverity().map(Enum::name).orElse(null));
    call(httpRequest);
  }

  public void restoreProfile(RestoreWsRequest request) {
    PostRequest httpRequest = new PostRequest(path(ACTION_RESTORE));
    httpRequest.setParam(PARAM_ORGANIZATION, request.getOrganization().orElse(null));
    httpRequest.setPart(PARAM_BACKUP, new PostRequest.Part(MediaTypes.XML, request.getBackup()));
    call(httpRequest);
  }

  public SearchWsResponse search(SearchWsRequest request) {
    return call(
      new GetRequest(path(ACTION_SEARCH))
        .setParam(PARAM_DEFAULTS, request.getDefaults())
        .setParam(PARAM_LANGUAGE, request.getLanguage())
        .setParam(PARAM_PROFILE_NAME, request.getProfileName())
        .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
        .setParam(PARAM_ORGANIZATION, request.getOrganizationKey()),
      SearchWsResponse.parser());
  }

  public void addProject(AddProjectRequest request) {
    call(new PostRequest(path(ACTION_ADD_PROJECT))
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_ORGANIZATION, request.getOrganization().orElse(null))
      .setParam(PARAM_PROFILE_NAME, request.getProfileName())
      .setParam(PARAM_PROFILE_KEY, request.getProfileKey())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_PROJECT_UUID, request.getProjectUuid()));
  }

  public void removeProject(RemoveProjectRequest request) {
    call(new PostRequest(path(ACTION_REMOVE_PROJECT))
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_PROFILE_NAME, request.getProfileName())
      .setParam(PARAM_PROFILE_KEY, request.getProfileKey())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_PROJECT_UUID, request.getProjectUuid()));
  }

  public CreateWsResponse create(CreateRequest request) {
    PostRequest postRequest = new PostRequest(path(ACTION_CREATE))
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_PROFILE_NAME, request.getProfileName());
    return call(postRequest, CreateWsResponse.parser());
  }
}
