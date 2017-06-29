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
import org.sonarqube.ws.QualityProfiles;
import org.sonarqube.ws.QualityProfiles.CopyWsResponse;
import org.sonarqube.ws.QualityProfiles.CreateWsResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.ShowResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ADD_PROJECT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_CHANGE_PARENT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_COPY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DEACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DELETE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_REMOVE_PROJECT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_RESTORE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SET_DEFAULT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.CONTROLLER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_COMPARE_TO_SONAR_WAY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_FROM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARAMS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_UUID;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RESET;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SEVERITY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_TO_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.RestoreActionParameters.PARAM_BACKUP;

public class QualityProfilesService extends BaseService {

  public QualityProfilesService(WsConnector wsConnector) {
    super(wsConnector, CONTROLLER_QUALITY_PROFILES);
  }

  public void activateRule(ActivateRuleWsRequest request) {
    PostRequest httpRequest = new PostRequest(path(ACTION_ACTIVATE_RULE));
    httpRequest.setParam(PARAM_ORGANIZATION, request.getOrganization().orElse(null));
    httpRequest.setParam(PARAM_PARAMS, request.getParams().orElse(null));
    httpRequest.setParam(PARAM_PROFILE, request.getProfileKey());
    httpRequest.setParam(PARAM_RESET, request.getReset().orElse(null));
    httpRequest.setParam(PARAM_RULE, request.getRuleKey());
    httpRequest.setParam(PARAM_SEVERITY, request.getSeverity().map(Enum::name).orElse(null));
    call(httpRequest);
  }

  public void deactivateRule(String profileKey, String ruleKey) {
    PostRequest httpRequest = new PostRequest(path(ACTION_DEACTIVATE_RULE));
    httpRequest.setParam(PARAM_PROFILE, profileKey);
    httpRequest.setParam(PARAM_RULE, ruleKey);
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

  public QualityProfiles.ShowResponse show(ShowRequest request) {
    return call(
      new GetRequest(path(ACTION_SHOW))
        .setParam(PARAM_PROFILE, request.getProfile())
        .setParam(PARAM_COMPARE_TO_SONAR_WAY, request.getCompareToSonarWay()),
      ShowResponse.parser());
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
      .setParam(PARAM_ORGANIZATION, request.getOrganizationKey())
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_PROFILE_NAME, request.getProfileName());
    return call(postRequest, CreateWsResponse.parser());
  }

  public CopyWsResponse copy(CopyRequest request) {
    PostRequest postRequest = new PostRequest(path(ACTION_COPY))
      .setParam(PARAM_FROM_KEY, request.getFromKey())
      .setParam(PARAM_TO_NAME, request.getToName());

    return call(postRequest, CopyWsResponse.parser());
  }

  public void changeParent(ChangeParentRequest request) {
    call(new PostRequest(path(ACTION_CHANGE_PARENT))
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_PARENT_PROFILE, request.getParentKey())
      .setParam(PARAM_PARENT_NAME, request.getParentName())
      .setParam(PARAM_PROFILE, request.getProfileKey())
      .setParam(PARAM_PROFILE_NAME, request.getProfileName())
      .setParam(PARAM_ORGANIZATION, request.getOrganization()));
  }

  public void setDefault(SetDefaultRequest request) {
    PostRequest postRequest = new PostRequest(path(ACTION_SET_DEFAULT))
      .setParam(PARAM_PROFILE_KEY, request.getProfileKey());

    call(postRequest);
  }

  public void delete(String profileKey) {
    PostRequest postRequest = new PostRequest(path(ACTION_DELETE))
      .setParam(PARAM_PROFILE_KEY, profileKey);

    call(postRequest);
  }
}
