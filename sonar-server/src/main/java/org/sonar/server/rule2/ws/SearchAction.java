/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule2.ws;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.rule2.RuleIndex;
import org.sonar.server.rule2.RuleNormalizer;
import org.sonar.server.rule2.RuleQuery;
import org.sonar.server.rule2.RuleService;
import org.sonar.server.search.Hit;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Results;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @since 4.4
 */
public class SearchAction implements RequestHandler {

  private static final String PARAM_TEXT_QUERY = "q";
  private static final String PARAM_REPOSITORIES = "repositories";
  private static final String PARAM_SEVERITIES = "severities";
  private static final String PARAM_STATUSES = "statuses";
  private static final String PARAM_LANGUAGES = "languages";
  private static final String PARAM_TAGS = "tags";


  private final RuleService service;

  public SearchAction(RuleService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("search")
      .setDescription("Search for a collection of relevant rules matching a specified query")
      .setSince("4.4")
      .setHandler(this);

    action
      .createParam(PARAM_TEXT_QUERY)
      .setDescription("UTF-8 search query")
      .setExampleValue("null pointer");

    action
      .createParam(PARAM_REPOSITORIES)
      .setDescription("Comma-separated list of repositories")
      .setExampleValue("checkstyle,findbugs");

    action
      .createParam(PARAM_SEVERITIES)
      .setDescription("Comma-separated list of default severities. Not the same than severity of rules in Quality profiles.")
      .setPossibleValues(Severity.ALL)
      .setExampleValue("CRITICAL,BLOCKER");

    action
      .createParam(PARAM_STATUSES)
      .setDescription("Comma-separated list of status codes")
      .setPossibleValues(RuleStatus.values())
      .setExampleValue(RuleStatus.READY.toString());

    action
      .createParam("tags")
      .setDescription("Comma-separated list of tags. Returned rules match all the tags (AND operator)")
      .setExampleValue("security,java8");

    action
      .createParam("qProfile")
      .setDescription("Key of Quality profile")
      .setExampleValue("java:Sonar way");

    action
      .createParam("activation")
      .setDescription("Used only if 'qProfile' is set. Possible values are: true | false | all")
      .setExampleValue("java:Sonar way");

    action
      .createParam("fields")
      .setDescription("Comma-separated list of the fields to be returned in response. All the fields are returned by default.")
      .setPossibleValues(RuleIndex.PUBLIC_FIELDS)
      .setExampleValue(String.format("%s,%s,%s", RuleNormalizer.RuleField.KEY, RuleNormalizer.RuleField.REPOSITORY, RuleNormalizer.RuleField.LANGUAGE));
  }

  @Override
  public void handle(Request request, Response response) {
    RuleQuery query = service.newRuleQuery();
    query.setQueryText(request.param(PARAM_TEXT_QUERY));
    query.setSeverities(request.paramAsStrings(PARAM_SEVERITIES));
    query.setRepositories(request.paramAsStrings(PARAM_REPOSITORIES));
    query.setStatuses(toStatuses(request.paramAsStrings(PARAM_STATUSES)));
    query.setLanguages(request.paramAsStrings(PARAM_LANGUAGES));

    Results results = service.search(query, new QueryOptions());
    JsonWriter json = response.newJsonWriter().beginObject().name("hits").beginArray();
    for (Hit hit : results.getHits()) {
      json.beginObject();
      for (Map.Entry<String, Object> entry : hit.getFields().entrySet()) {
        Object value = entry.getValue();
        json.prop(entry.getKey(), value == null ? null : value.toString());
      }
      json.endObject();
    }
    json.endArray();
    json.endObject().close();
  }

  @CheckForNull
  private Collection<RuleStatus> toStatuses(@Nullable List<String> statuses) {
    if (statuses == null) {
      return null;
    }
    return Collections2.transform(statuses, new Function<String, RuleStatus>() {
      @Override
      public RuleStatus apply(@Nullable String input) {
        return input == null ? null : RuleStatus.valueOf(input);
      }
    });
  }
}
