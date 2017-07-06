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
package org.sonarqube.ws.client.rule;

import javax.annotation.Nullable;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.SearchResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TYPES;

public class RulesService extends BaseService {

  public RulesService(WsConnector wsConnector) {
    super(wsConnector, "api/rules");
  }

  public SearchResponse search(SearchWsRequest request) {
    return call(
      new GetRequest(path("search"))
        .setParam(PARAM_ACTIVATION, request.getActivation())
        .setParam(PARAM_ACTIVE_SEVERITIES, inlineMultipleParamValue(request.getActiveSeverities()))
        .setParam("asc", request.getAsc())
        .setParam(PARAM_AVAILABLE_SINCE, request.getAvailableSince())
        .setParam("f", inlineMultipleParamValue(request.getFields()))
        .setParam("facets", inlineMultipleParamValue(request.getFacets()))
        .setParam(PARAM_INHERITANCE, inlineMultipleParamValue(request.getInheritance()))
        .setParam(PARAM_IS_TEMPLATE, request.getIsTemplate())
        .setParam(PARAM_LANGUAGES, inlineMultipleParamValue(request.getLanguages()))
        .setParam("p", request.getPage())
        .setParam("ps", request.getPageSize())
        .setParam("q", request.getQuery())
        .setParam(PARAM_QPROFILE, request.getQProfile())
        .setParam(PARAM_COMPARE_TO_PROFILE, request.getCompareToProfile())
        .setParam(PARAM_REPOSITORIES, inlineMultipleParamValue(request.getRepositories()))
        .setParam(PARAM_RULE_KEY, request.getRuleKey())
        .setParam("s", request.getSort())
        .setParam(PARAM_SEVERITIES, inlineMultipleParamValue(request.getSeverities()))
        .setParam(PARAM_STATUSES, inlineMultipleParamValue(request.getStatuses()))
        .setParam(PARAM_TAGS, inlineMultipleParamValue(request.getTags()))
        .setParam(PARAM_TEMPLATE_KEY, request.getTemplateKey())
        .setParam(PARAM_TYPES, inlineMultipleParamValue(request.getTypes())),
      SearchResponse.parser());
  }

  public Rules.ShowResponse show(@Nullable String organization, String key) {
    GetRequest request = new GetRequest(path("show"))
      .setParam("organization", organization)
      .setParam("key", key);
    return call(request, Rules.ShowResponse.parser());
  }

  public void create(CreateWsRequest request) {
    PostRequest httpRequest = new PostRequest(path("create"));
    httpRequest.setParam("custom_key", request.getCustomKey());
    httpRequest.setParam("markdown_description", request.getMarkdownDescription());
    httpRequest.setParam("name", request.getName());
    httpRequest.setParam("params", request.getParams());
    httpRequest.setParam("prevent_reactivation", request.getPreventReactivation());
    httpRequest.setParam("severity", request.getSeverity());
    httpRequest.setParam("status", request.getStatus());
    httpRequest.setParam("template_key", request.getTemplateKey());
    call(httpRequest);
  }
}
