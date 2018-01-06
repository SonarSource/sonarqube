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
package org.sonarqube.ws.client.issues;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Issues.AddCommentResponse;
import org.sonarqube.ws.Issues.AssignResponse;
import org.sonarqube.ws.Issues.AuthorsResponse;
import org.sonarqube.ws.Issues.BulkChangeWsResponse;
import org.sonarqube.ws.Issues.ChangelogWsResponse;
import org.sonarqube.ws.Issues.DeleteCommentResponse;
import org.sonarqube.ws.Issues.DoTransitionResponse;
import org.sonarqube.ws.Issues.SearchWsResponse;
import org.sonarqube.ws.Issues.SetSeverityResponse;
import org.sonarqube.ws.Issues.SetTagsResponse;
import org.sonarqube.ws.Issues.SetTypeResponse;
import org.sonarqube.ws.Issues.TagsResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class IssuesService extends BaseService {

  public IssuesService(WsConnector wsConnector) {
    super(wsConnector, "api/issues");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/add_comment">Further information about this action online (including a response example)</a>
   * @since 3.6
   */
  public AddCommentResponse addComment(AddCommentRequest request) {
    return call(
      new PostRequest(path("add_comment"))
        .setParam("issue", request.getIssue())
        .setParam("text", request.getText()),
      AddCommentResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/assign">Further information about this action online (including a response example)</a>
   * @since 3.6
   */
  public AssignResponse assign(AssignRequest request) {
    return call(
      new PostRequest(path("assign"))
        .setParam("assignee", request.getAssignee())
        .setParam("issue", request.getIssue())
        .setParam("me", request.getMe()),
      AssignResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/authors">Further information about this action online (including a response example)</a>
   * @since 5.1
   */
  public AuthorsResponse authors(AuthorsRequest request) {
    return call(
      new GetRequest(path("authors"))
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ()),
      AuthorsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/bulk_change">Further information about this action online (including a response example)</a>
   * @since 3.7
   */
  public BulkChangeWsResponse bulkChange(BulkChangeRequest request) {
    return call(
      new PostRequest(path("bulk_change"))
        .setParam("add_tags", request.getAddTags())
        .setParam("assign", request.getAssign() == null ? null : request.getAssign().stream().collect(Collectors.joining(",")))
        .setParam("comment", request.getComment() == null ? null : request.getComment().stream().collect(Collectors.joining(",")))
        .setParam("do_transition", request.getDoTransition())
        .setParam("issues", request.getIssues() == null ? null : request.getIssues().stream().collect(Collectors.joining(",")))
        .setParam("plan", request.getPlan() == null ? null : request.getPlan().stream().collect(Collectors.joining(",")))
        .setParam("remove_tags", request.getRemoveTags())
        .setParam("sendNotifications", request.getSendNotifications())
        .setParam("set_severity", request.getSetSeverity() == null ? null : request.getSetSeverity().stream().collect(Collectors.joining(",")))
        .setParam("set_type", request.getSetType() == null ? null : request.getSetType().stream().collect(Collectors.joining(","))),
      BulkChangeWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/changelog">Further information about this action online (including a response example)</a>
   * @since 4.1
   */
  public ChangelogWsResponse changelog(ChangelogRequest request) {
    return call(
      new GetRequest(path("changelog"))
        .setParam("issue", request.getIssue()),
      ChangelogWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/component_tags">Further information about this action online (including a response example)</a>
   * @since 5.1
   */
  public String componentTags(ComponentTagsRequest request) {
    return call(
      new GetRequest(path("component_tags"))
        .setParam("componentUuid", request.getComponentUuid())
        .setParam("createdAfter", request.getCreatedAfter())
        .setParam("ps", request.getPs())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/delete_comment">Further information about this action online (including a response example)</a>
   * @since 3.6
   */
  public DeleteCommentResponse deleteComment(DeleteCommentRequest request) {
    return call(
      new PostRequest(path("delete_comment"))
        .setParam("comment", request.getComment()),
      DeleteCommentResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/do_transition">Further information about this action online (including a response example)</a>
   * @since 3.6
   */
  public DoTransitionResponse doTransition(DoTransitionRequest request) {
    return call(
      new PostRequest(path("do_transition"))
        .setParam("issue", request.getIssue())
        .setParam("transition", request.getTransition()),
      DoTransitionResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/edit_comment">Further information about this action online (including a response example)</a>
   * @since 3.6
   */
  public String editComment(EditCommentRequest request) {
    return call(
      new PostRequest(path("edit_comment"))
        .setParam("comment", request.getComment())
        .setParam("text", request.getText())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/search">Further information about this action online (including a response example)</a>
   * @since 3.6
   */
  public SearchWsResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("additionalFields", request.getAdditionalFields() == null ? null : request.getAdditionalFields().stream().collect(Collectors.joining(",")))
        .setParam("asc", request.getAsc())
        .setParam("assigned", request.getAssigned())
        .setParam("assignees", request.getAssignees() == null ? null : request.getAssignees().stream().collect(Collectors.joining(",")))
        .setParam("authors", request.getAuthors() == null ? null : request.getAuthors().stream().collect(Collectors.joining(",")))
        .setParam("branch", request.getBranch())
        .setParam("componentKeys", request.getComponentKeys() == null ? null : request.getComponentKeys().stream().collect(Collectors.joining(",")))
        .setParam("componentRootUuids", request.getComponentRootUuids())
        .setParam("componentRoots", request.getComponentRoots())
        .setParam("componentUuids", request.getComponentUuids() == null ? null : request.getComponentUuids().stream().collect(Collectors.joining(",")))
        .setParam("components", request.getComponents())
        .setParam("createdAfter", request.getCreatedAfter())
        .setParam("createdAt", request.getCreatedAt())
        .setParam("createdBefore", request.getCreatedBefore())
        .setParam("createdInLast", request.getCreatedInLast())
        .setParam("directories", request.getDirectories() == null ? null : request.getDirectories().stream().collect(Collectors.joining(",")))
        .setParam("facetMode", request.getFacetMode())
        .setParam("facets", request.getFacets() == null ? null : request.getFacets().stream().collect(Collectors.joining(",")))
        .setParam("fileUuids", request.getFileUuids() == null ? null : request.getFileUuids().stream().collect(Collectors.joining(",")))
        .setParam("issues", request.getIssues() == null ? null : request.getIssues().stream().collect(Collectors.joining(",")))
        .setParam("languages", request.getLanguages() == null ? null : request.getLanguages().stream().collect(Collectors.joining(",")))
        .setParam("moduleUuids", request.getModuleUuids() == null ? null : request.getModuleUuids().stream().collect(Collectors.joining(",")))
        .setParam("onComponentOnly", request.getOnComponentOnly())
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("projectUuids", request.getProjectUuids() == null ? null : request.getProjectUuids().stream().collect(Collectors.joining(",")))
        .setParam("projects", request.getProjects() == null ? null : request.getProjects().stream().collect(Collectors.joining(",")))
        .setParam("ps", request.getPs())
        .setParam("resolutions", request.getResolutions() == null ? null : request.getResolutions().stream().collect(Collectors.joining(",")))
        .setParam("resolved", request.getResolved())
        .setParam("rules", request.getRules() == null ? null : request.getRules().stream().collect(Collectors.joining(",")))
        .setParam("s", request.getS())
        .setParam("severities", request.getSeverities() == null ? null : request.getSeverities().stream().collect(Collectors.joining(",")))
        .setParam("sinceLeakPeriod", request.getSinceLeakPeriod())
        .setParam("statuses", request.getStatuses() == null ? null : request.getStatuses().stream().collect(Collectors.joining(",")))
        .setParam("tags", request.getTags() == null ? null : request.getTags().stream().collect(Collectors.joining(",")))
        .setParam("types", request.getTypes() == null ? null : request.getTypes().stream().collect(Collectors.joining(","))),
      SearchWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/set_severity">Further information about this action online (including a response example)</a>
   * @since 3.6
   */
  public SetSeverityResponse setSeverity(SetSeverityRequest request) {
    return call(
      new PostRequest(path("set_severity"))
        .setParam("issue", request.getIssue())
        .setParam("severity", request.getSeverity()),
      SetSeverityResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/set_tags">Further information about this action online (including a response example)</a>
   * @since 5.1
   */
  public SetTagsResponse setTags(SetTagsRequest request) {
    return call(
      new PostRequest(path("set_tags"))
        .setParam("issue", request.getIssue())
        .setParam("tags", request.getTags() == null ? null : request.getTags().stream().collect(Collectors.joining(","))),
      SetTagsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/set_type">Further information about this action online (including a response example)</a>
   * @since 5.5
   */
  public SetTypeResponse setType(SetTypeRequest request) {
    return call(
      new PostRequest(path("set_type"))
        .setParam("issue", request.getIssue())
        .setParam("type", request.getType()),
      SetTypeResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/tags">Further information about this action online (including a response example)</a>
   * @since 5.1
   */
  public TagsResponse tags(TagsRequest request) {
    return call(
      new GetRequest(path("tags"))
        .setParam("organization", request.getOrganization())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ()),
      TagsResponse.parser());
  }
}
