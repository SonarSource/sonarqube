/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.rule.ws;

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleQuery;
import org.sonar.server.rule.Rules;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Collections;

public class RuleSearchWsHandler implements RequestHandler {

  private final Rules rules;
  private final Languages languages;

  public RuleSearchWsHandler(Rules rules, Languages languages) {
    this.rules = rules;
    this.languages = languages;
  }

  @Override
  public void handle(Request request, Response response) {
    final String ruleKeyParam = request.param("k");
    Collection<Rule> foundRules = Collections.emptyList();
    boolean hasMore = false;
    if (ruleKeyParam == null) {
      final String ruleSearchParam = request.param("s");
      final int pageSize = request.paramAsInt("ps", 25);
      final int pageIndex = request.paramAsInt("p", 1);
      PagedResult<Rule> searchResult = rules.find(RuleQuery.builder()
          .withSearchQuery(ruleSearchParam)
          .withPageSize(pageSize)
          .withPage(pageIndex)
          .build());
      foundRules = searchResult.results();
      hasMore = searchResult.paging().hasNextPage();
    } else {
      RuleKey ruleKey = RuleKey.parse(ruleKeyParam);
      Rule rule = findRule(ruleKey);
      if (rule != null) {
        foundRules = Collections.singleton(rule);
      }
      hasMore = false;
    }

    JsonWriter json = response.newJsonWriter();
    json.beginObject().name("results").beginArray();
    for(Rule rule: foundRules) {
      json.beginObject();
      writeRule(rule, json);
      json.endObject();
    }
    json.endArray().prop("more", hasMore).endObject().close();
  }

  @CheckForNull
  private Rule findRule(RuleKey ruleKey) {
    return rules.findByKey(ruleKey);
  }

  private void writeRule(Rule rule, JsonWriter json) {
    String languageName = null;
    String languageKey = rule.language();
    if (languageKey != null) {
      Language language = languages.get(languageKey);
      if (language != null) {
        languageName = language.getName();
      } else {
        languageName = languageKey;
      }
    }
    json
      .prop("key", rule.ruleKey().toString())
      .prop("name", rule.name())
      .prop("language", languageName)
    ;
  }
}
