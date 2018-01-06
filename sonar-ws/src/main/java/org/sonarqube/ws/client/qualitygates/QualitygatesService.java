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
package org.sonarqube.ws.client.qualitygates;

import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Qualitygates.CreateConditionResponse;
import org.sonarqube.ws.Qualitygates.CreateResponse;
import org.sonarqube.ws.Qualitygates.GetByProjectResponse;
import org.sonarqube.ws.Qualitygates.ListWsResponse;
import org.sonarqube.ws.Qualitygates.ProjectStatusResponse;
import org.sonarqube.ws.Qualitygates.SearchResponse;
import org.sonarqube.ws.Qualitygates.ShowWsResponse;
import org.sonarqube.ws.Qualitygates.UpdateConditionResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class QualitygatesService extends BaseService {

  public QualitygatesService(WsConnector wsConnector) {
    super(wsConnector, "api/qualitygates");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/copy">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public void copy(CopyRequest request) {
    call(
      new PostRequest(path("copy"))
        .setParam("id", request.getId())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/create">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public CreateResponse create(CreateRequest request) {
    return call(
      new PostRequest(path("create"))
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization()),
      CreateResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/create_condition">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public CreateConditionResponse createCondition(CreateConditionRequest request) {
    return call(
      new PostRequest(path("create_condition"))
        .setParam("error", request.getError())
        .setParam("gateId", request.getGateId())
        .setParam("metric", request.getMetric())
        .setParam("op", request.getOp())
        .setParam("organization", request.getOrganization())
        .setParam("period", request.getPeriod())
        .setParam("warning", request.getWarning()),
      CreateConditionResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/delete_condition">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public void deleteCondition(DeleteConditionRequest request) {
    call(
      new PostRequest(path("delete_condition"))
        .setParam("id", request.getId())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/deselect">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public void deselect(DeselectRequest request) {
    call(
      new PostRequest(path("deselect"))
        .setParam("organization", request.getOrganization())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/destroy">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public void destroy(DestroyRequest request) {
    call(
      new PostRequest(path("destroy"))
        .setParam("id", request.getId())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/get_by_project">Further information about this action online (including a response example)</a>
   * @since 6.1
   */
  public GetByProjectResponse getByProject(GetByProjectRequest request) {
    return call(
      new GetRequest(path("get_by_project"))
        .setParam("organization", request.getOrganization())
        .setParam("project", request.getProject()),
      GetByProjectResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/list">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public ListWsResponse list(ListRequest request) {
    return call(
      new GetRequest(path("list"))
        .setParam("organization", request.getOrganization()),
      ListWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/project_status">Further information about this action online (including a response example)</a>
   * @since 5.3
   */
  public ProjectStatusResponse projectStatus(ProjectStatusRequest request) {
    return call(
      new GetRequest(path("project_status"))
        .setParam("analysisId", request.getAnalysisId())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey()),
      ProjectStatusResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/rename">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public void rename(RenameRequest request) {
    call(
      new PostRequest(path("rename"))
        .setParam("id", request.getId())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/search">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public SearchResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("gateId", request.getGateId())
        .setParam("organization", request.getOrganization())
        .setParam("page", request.getPage())
        .setParam("pageSize", request.getPageSize())
        .setParam("query", request.getQuery())
        .setParam("selected", request.getSelected()),
      SearchResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/select">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public void select(SelectRequest request) {
    call(
      new PostRequest(path("select"))
        .setParam("gateId", request.getGateId())
        .setParam("organization", request.getOrganization())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/set_as_default">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public void setAsDefault(SetAsDefaultRequest request) {
    call(
      new PostRequest(path("set_as_default"))
        .setParam("id", request.getId())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/show">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public ShowWsResponse show(ShowRequest request) {
    return call(
      new GetRequest(path("show"))
        .setParam("id", request.getId())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization()),
      ShowWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/unset_default">Further information about this action online (including a response example)</a>
   * @since 4.3
   * @deprecated since 7.0
   */
  @Deprecated
  public String unsetDefault() {
    return call(
      new PostRequest(path("unset_default"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygates/update_condition">Further information about this action online (including a response example)</a>
   * @since 4.3
   */
  public void updateCondition(UpdateConditionRequest request) {
    call(
      new PostRequest(path("update_condition"))
        .setParam("error", request.getError())
        .setParam("id", request.getId())
        .setParam("metric", request.getMetric())
        .setParam("op", request.getOp())
        .setParam("organization", request.getOrganization())
        .setParam("period", request.getPeriod())
        .setParam("warning", request.getWarning()),
      UpdateConditionResponse.parser());
  }
}
