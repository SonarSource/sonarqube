/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonarqube.ws.client.rules;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Rules.CreateResponse;
import org.sonarqube.ws.Rules.ListResponse;
import org.sonarqube.ws.Rules.SearchResponse;
import org.sonarqube.ws.Rules.ShowResponse;
import org.sonarqube.ws.Rules.UpdateResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class RulesService extends BaseService {

  public RulesService(WsConnector wsConnector) {
    super(wsConnector, "api/rules");
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/app">Further information about this action online (including a response example)</a>
   * @since 4.5
   */
  public String app(AppRequest request) {
    return call(
      new GetRequest(path("app"))
        .setParam("organization", request.getOrganization())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/create">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public void create(CreateRequest request) {
    call(
      new PostRequest(path("create"))
        .setParam("custom_key", request.getCustomKey())
        .setParam("markdown_description", request.getMarkdownDescription())
        .setParam("name", request.getName())
        .setParam("params", request.getParams() == null ? null : request.getParams().stream().collect(Collectors.joining(",")))
        .setParam("prevent_reactivation", request.getPreventReactivation())
        .setParam("severity", request.getSeverity())
        .setParam("status", request.getStatus())
        .setParam("template_key", request.getTemplateKey())
        .setParam("type", request.getType()),
      CreateResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/delete">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public void delete(DeleteRequest request) {
    call(
      new PostRequest(path("delete"))
        .setParam("key", request.getKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/list">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public ListResponse list() {
    return call(
      new GetRequest(path("list")),
      ListResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/repositories">Further information about this action online (including a response example)</a>
   * @since 4.5
   */
  public String repositories(RepositoriesRequest request) {
    return call(
      new GetRequest(path("repositories"))
        .setParam("language", request.getLanguage())
        .setParam("q", request.getQ())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/search">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public SearchResponse search(SearchRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam("activation", request.getActivation())
        .setParam("active_severities", request.getActiveSeverities() == null ? null : request.getActiveSeverities().stream().collect(Collectors.joining(",")))
        .setParam("asc", request.getAsc())
        .setParam("available_since", request.getAvailableSince())
        .setParam("compareToProfile", request.getCompareToProfile())
        .setParam("f", request.getF() == null ? null : request.getF().stream().collect(Collectors.joining(",")))
        .setParam("facets", request.getFacets() == null ? null : request.getFacets().stream().collect(Collectors.joining(",")))
        .setParam("inheritance", request.getInheritance() == null ? null : request.getInheritance().stream().collect(Collectors.joining(",")))
        .setParam("is_template", request.getIsTemplate())
        .setParam("include_external", request.getIncludeExternal())
        .setParam("languages", request.getLanguages() == null ? null : request.getLanguages().stream().collect(Collectors.joining(",")))
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qprofile", request.getQprofile())
        .setParam("repositories", request.getRepositories() == null ? null : request.getRepositories().stream().collect(Collectors.joining(",")))
        .setParam("rule_key", request.getRuleKey())
        .setParam("s", request.getS())
        .setParam("severities", request.getSeverities() == null ? null : request.getSeverities().stream().collect(Collectors.joining(",")))
        .setParam("statuses", request.getStatuses() == null ? null : request.getStatuses().stream().collect(Collectors.joining(",")))
        .setParam("cwe", request.getCwe() == null ? null : request.getCwe().stream().collect(Collectors.joining(",")))
        .setParam("owaspTop10", request.getOwaspTop10() == null ? null : request.getOwaspTop10().stream().collect(Collectors.joining(",")))
        .setParam("sansTop25", request.getSansTop25() == null ? null : request.getSansTop25().stream().collect(Collectors.joining(",")))
        .setParam("sonarsourceSecurity", request.getSonarsourceSecurity() == null ? null : request.getSonarsourceSecurity().stream().collect(Collectors.joining(",")))
        .setParam("tags", request.getTags() == null ? null : request.getTags().stream().collect(Collectors.joining(",")))
        .setParam("template_key", request.getTemplateKey())
        .setParam("types", request.getTypes() == null ? null : request.getTypes().stream().collect(Collectors.joining(","))),
      SearchResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/show">Further information about this action online (including a response example)</a>
   * @since 4.2
   */
  public ShowResponse show(ShowRequest request) {
    return call(
      new GetRequest(path("show"))
        .setParam("actives", request.getActives())
        .setParam("key", request.getKey())
        .setParam("organization", request.getOrganization()),
      ShowResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/tags">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public String tags(TagsRequest request) {
    return call(
      new GetRequest(path("tags"))
        .setParam("organization", request.getOrganization())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/rules/update">Further information about this action online (including a response example)</a>
   * @since 4.4
   */
  public void update(UpdateRequest request) {
    call(
      new PostRequest(path("update"))
        .setParam("key", request.getKey())
        .setParam("markdown_description", request.getMarkdownDescription())
        .setParam("markdown_note", request.getMarkdownNote())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setParam("params", request.getParams() == null ? null : request.getParams().stream().collect(Collectors.joining(",")))
        .setParam("remediation_fn_base_effort", request.getRemediationFnBaseEffort())
        .setParam("remediation_fn_type", request.getRemediationFnType())
        .setParam("remediation_fy_gap_multiplier", request.getRemediationFyGapMultiplier())
        .setParam("severity", request.getSeverity())
        .setParam("status", request.getStatus())
        .setParam("tags", request.getTags() == null ? null : request.getTags().stream().collect(Collectors.joining(","))),
      UpdateResponse.parser());
  }
}
