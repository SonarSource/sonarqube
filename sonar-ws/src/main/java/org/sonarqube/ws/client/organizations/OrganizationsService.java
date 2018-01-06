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
package org.sonarqube.ws.client.organizations;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Organizations.AddMemberWsResponse;
import org.sonarqube.ws.Organizations.CreateWsResponse;
import org.sonarqube.ws.Organizations.SearchWsResponse;
import org.sonarqube.ws.Organizations.SearchMembersWsResponse;
import org.sonarqube.ws.Organizations.UpdateWsResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class OrganizationsService extends BaseService {

  public OrganizationsService(WsConnector wsConnector) {
    super(wsConnector, "api/organizations");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/add_member">Further information about this action online (including a response example)</a>
   * @since 6.4
   */
  public AddMemberWsResponse addMember(AddMemberRequest request) {
    return call(
      new PostRequest(path("add_member"))
        .setParam("login", request.getLogin())
        .setParam("organization", request.getOrganization()),
      AddMemberWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/create">Further information about this action online (including a response example)</a>
   * @since 6.2
   */
  public CreateWsResponse create(CreateRequest request) {
    return call(
      new PostRequest(path("create"))
        .setParam("avatar", request.getAvatar())
        .setParam("description", request.getDescription())
        .setParam("key", request.getKey())
        .setParam("name", request.getName())
        .setParam("url", request.getUrl()),
      CreateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/delete">Further information about this action online (including a response example)</a>
   * @since 6.2
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/enable_support">Further information about this action online (including a response example)</a>
   * @since 6.3
   */
  public void enableSupport() {
    call(
      new PostRequest(path("enable_support"))
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/remove_member">Further information about this action online (including a response example)</a>
   * @since 6.4
   */
  public void removeMember(RemoveMemberRequest request) {
    call(
      new PostRequest(path("remove_member"))
        .setParam("login", request.getLogin())
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/search">Further information about this action online (including a response example)</a>
   * @since 6.2
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("member", request.getMember())
        .setParam("organizations", request.getOrganizations() == null ? null : request.getOrganizations().stream().collect(Collectors.joining(",")))
        .setParam("p", request.getP())
        .setParam("ps", request.getPs()),
      SearchWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/search_members">Further information about this action online (including a response example)</a>
   * @since 6.4
   */
  public SearchMembersWsResponse searchMembers(SearchMembersRequest request) {
    return call(
      new GetRequest(path("search_members"))
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("selected", request.getSelected()),
      SearchMembersWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/update">Further information about this action online (including a response example)</a>
   * @since 6.2
   */
  public void update(UpdateRequest request) {
    call(
      new PostRequest(path("update"))
        .setParam("avatar", request.getAvatar())
        .setParam("description", request.getDescription())
        .setParam("key", request.getKey())
        .setParam("name", request.getName())
        .setParam("url", request.getUrl()),
      UpdateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/organizations/update_project_visibility">Further information about this action online (including a response example)</a>
   * @since 6.4
   */
  public void updateProjectVisibility(UpdateProjectVisibilityRequest request) {
    call(
      new PostRequest(path("update_project_visibility"))
        .setParam("organization", request.getOrganization())
        .setParam("projectVisibility", request.getProjectVisibility())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }
}
