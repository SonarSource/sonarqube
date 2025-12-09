/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonarqube.ws.client.hotspots;

import java.util.stream.Collectors;
import jakarta.annotation.Generated;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Hotspots;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class HotspotsService extends BaseService {

  public HotspotsService(WsConnector wsConnector) {
    super(wsConnector, "api/hotspots");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots/add_comment">Further information about this action online (including a response example)</a>
   * @since 8.1
   */
  public void addComment(AddCommentRequest request) {
    call(
      new PostRequest(path("add_comment"))
        .setParam("comment", request.getComment())
        .setParam("hotspot", request.getHotspot())
        .setMediaType(MediaTypes.JSON)).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots/assign">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public void assign(AssignRequest request) {
    call(
      new PostRequest(path("assign"))
        .setParam("assignee", request.getAssignee())
        .setParam("comment", request.getComment())
        .setParam("hotspot", request.getHotspot())
        .setMediaType(MediaTypes.JSON)).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots/change_status">Further information about this action online (including a response example)</a>
   * @since 8.1
   */
  public void changeStatus(ChangeStatusRequest request) {
    call(
      new PostRequest(path("change_status"))
        .setParam("comment", request.getComment())
        .setParam("hotspot", request.getHotspot())
        .setParam("resolution", request.getResolution())
        .setParam("status", request.getStatus())
        .setMediaType(MediaTypes.JSON)).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots/edit_comment">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public Common.Comment editComment(EditCommentRequest request) {
    return call(
      new PostRequest(path("edit_comment"))
        .setParam("comment", request.getComment())
        .setParam("text", request.getText())
        .setMediaType(MediaTypes.JSON),
      Common.Comment.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots/delete_comment">Further information about this action online (including a response example)</a>
   * @since 8.2
   */
  public void deleteComment(DeleteCommentRequest request) {
    call(
      new PostRequest(path("delete_comment"))
        .setParam("comment", request.getComment())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots/search">Further information about this action online (including a response example)</a>
   * @since 8.1
   */
  public Hotspots.SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("branch", request.getBranch())
        .setParam("hotspots", request.getHotspots() == null ? null : request.getHotspots().stream().collect(Collectors.joining(",")))
        .setParam("onlyMine", request.getOnlyMine())
        .setParam("p", request.getP())
        .setParam("projectKey", request.getProjectKey())
        .setParam("ps", request.getPs())
        .setParam("pullRequest", request.getPullRequest())
        .setParam("resolution", request.getResolution())
        .setParam("inNewCodePeriod", request.getInNewCodePeriod())
        .setParam("status", request.getStatus())
        .setMediaType(MediaTypes.JSON),
      Hotspots.SearchWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots/show">Further information about this action online (including a response example)</a>
   * @since 8.1
   */
  public Hotspots.ShowWsResponse show(ShowRequest request) {
    return call(
      new GetRequest(path("show"))
        .setParam("hotspot", request.getHotspot())
        .setMediaType(MediaTypes.JSON),
      Hotspots.ShowWsResponse.parser());
  }
}
