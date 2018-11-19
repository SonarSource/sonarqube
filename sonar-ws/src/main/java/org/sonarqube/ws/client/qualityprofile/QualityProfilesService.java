/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonarqube.ws.QualityProfiles.SearchGroupsResponse;
import org.sonarqube.ws.QualityProfiles.SearchUsersResponse;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse;
import org.sonarqube.ws.QualityProfiles.ShowResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.SELECTED;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ADD_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ADD_PROJECT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ADD_USER;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_CHANGE_PARENT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_COPY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DEACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DELETE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_REMOVE_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_REMOVE_PROJECT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_REMOVE_USER;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_RESTORE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH_GROUPS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SEARCH_USERS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SET_DEFAULT;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.CONTROLLER_QUALITY_PROFILES;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_COMPARE_TO_SONAR_WAY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_FROM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_GROUP;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LANGUAGE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARAMS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARENT_QUALITY_PROFILE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_UUID;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_QUALITY_PROFILE;
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
    httpRequest.setParam(PARAM_KEY, request.getKey());
    httpRequest.setParam(PARAM_RESET, request.getReset().orElse(null));
    httpRequest.setParam(PARAM_RULE, request.getRuleKey());
    httpRequest.setParam(PARAM_SEVERITY, request.getSeverity().map(Enum::name).orElse(null));
    call(httpRequest);
  }

  public void deactivateRule(String profileKey, String ruleKey) {
    PostRequest httpRequest = new PostRequest(path(ACTION_DEACTIVATE_RULE));
    httpRequest.setParam(PARAM_KEY, profileKey);
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
        .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
        .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
        .setParam(PARAM_ORGANIZATION, request.getOrganizationKey()),
      SearchWsResponse.parser());
  }

  public QualityProfiles.ShowResponse show(ShowRequest request) {
    return call(
      new GetRequest(path(ACTION_SHOW))
        .setParam(PARAM_KEY, request.getKey())
        .setParam(PARAM_COMPARE_TO_SONAR_WAY, request.getCompareToSonarWay()),
      ShowResponse.parser());
  }

  public void addProject(AddProjectRequest request) {
    call(new PostRequest(path(ACTION_ADD_PROJECT))
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_ORGANIZATION, request.getOrganization().orElse(null))
      .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
      .setParam(QualityProfileWsParameters.PARAM_KEY, request.getKey())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_PROJECT_UUID, request.getProjectUuid()));
  }

  public void removeProject(RemoveProjectRequest request) {
    call(new PostRequest(path(ACTION_REMOVE_PROJECT))
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
      .setParam(QualityProfileWsParameters.PARAM_KEY, request.getKey())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_PROJECT_UUID, request.getProjectUuid()));
  }

  public CreateWsResponse create(CreateRequest request) {
    PostRequest postRequest = new PostRequest(path(ACTION_CREATE))
      .setParam(PARAM_ORGANIZATION, request.getOrganizationKey())
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_NAME, request.getName());
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
      .setParam(PARAM_PARENT_KEY, request.getParentKey())
      .setParam(PARAM_PARENT_QUALITY_PROFILE, request.getParentQualityProfile())
      .setParam(PARAM_KEY, request.getKey())
      .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
      .setParam(PARAM_ORGANIZATION, request.getOrganization()));
  }

  public void setDefault(SetDefaultRequest request) {
    PostRequest postRequest = new PostRequest(path(ACTION_SET_DEFAULT))
      .setParam(QualityProfileWsParameters.PARAM_KEY, request.getKey());

    call(postRequest);
  }

  public void delete(String profileKey) {
    PostRequest postRequest = new PostRequest(path(ACTION_DELETE))
      .setParam(QualityProfileWsParameters.PARAM_KEY, profileKey);

    call(postRequest);
  }

  public void addUser(AddUserRequest request) {
    call(new PostRequest(path(ACTION_ADD_USER))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_LOGIN, request.getUserLogin()));
  }

  public void removeUser(RemoveUserRequest request) {
    call(new PostRequest(path(ACTION_REMOVE_USER))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_LOGIN, request.getUserLogin()));
  }

  public SearchUsersResponse searchUsers(SearchUsersRequest request) {
    return call(
      new GetRequest(path(ACTION_SEARCH_USERS))
        .setParam(PARAM_ORGANIZATION, request.getOrganization())
        .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
        .setParam(PARAM_LANGUAGE, request.getLanguage())
        .setParam(TEXT_QUERY, request.getQuery())
        .setParam(SELECTED, request.getSelected())
        .setParam(PAGE, request.getPage())
        .setParam(PAGE_SIZE, request.getPageSize()),
      SearchUsersResponse.parser());
  }

  public void addGroup(AddGroupRequest request) {
    call(new PostRequest(path(ACTION_ADD_GROUP))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_GROUP, request.getGroup()));
  }

  public void removeGroup(RemoveGroupRequest request) {
    call(new PostRequest(path(ACTION_REMOVE_GROUP))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
      .setParam(PARAM_LANGUAGE, request.getLanguage())
      .setParam(PARAM_GROUP, request.getGroup()));
  }

  public SearchGroupsResponse searchGroups(SearchGroupsRequest request) {
    return call(
      new GetRequest(path(ACTION_SEARCH_GROUPS))
        .setParam(PARAM_ORGANIZATION, request.getOrganization())
        .setParam(PARAM_QUALITY_PROFILE, request.getQualityProfile())
        .setParam(PARAM_LANGUAGE, request.getLanguage())
        .setParam(TEXT_QUERY, request.getQuery())
        .setParam(SELECTED, request.getSelected())
        .setParam(PAGE, request.getPage())
        .setParam(PAGE_SIZE, request.getPageSize()),
      SearchGroupsResponse.parser());
  }

  public String changelog(ChangelogWsRequest request) {
    PostRequest postRequest = new PostRequest(path("changelog"))
      .setParam("language", request.getLanguage())
      .setParam("organization", request.getOrganization())
      .setParam("qualityProfile", request.getQualityProfile());
    if (request.getP() != null) {
      postRequest.setParam("p", request.getP());
    }
    if (request.getPs() != null) {
      postRequest.setParam("ps", request.getPs());
    }
    if (request.getSince() != null) {
      postRequest.setParam("since", request.getSince());
    }
    if (request.getTo() != null) {
      postRequest.setParam("to", request.getTo());
    }
    return call(postRequest).content();
  }
}
