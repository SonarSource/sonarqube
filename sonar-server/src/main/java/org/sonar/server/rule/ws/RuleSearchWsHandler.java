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

package org.sonar.server.rule.ws;

import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleQuery;
import org.sonar.server.rule.Rules;
import org.sonar.server.util.RubyUtils;

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
    long total = 0L;
    if (ruleKeyParam == null) {
      PagedResult<Rule> searchResult = rules.find(RuleQuery.builder()
        .searchQuery(request.param("s"))
        .languages(RubyUtils.toStrings(request.param("languages")))
        .repositories(RubyUtils.toStrings(request.param("repositories")))
        .severities(RubyUtils.toStrings(request.param("severities")))
        .statuses(RubyUtils.toStrings(request.param("statuses")))
        .tags(RubyUtils.toStrings(request.param("tags")))
        .debtCharacteristics(RubyUtils.toStrings(request.param("debtCharacteristics")))
        .hasDebtCharacteristic(request.paramAsBoolean("hasDebtCharacteristic"))
        .pageSize(request.paramAsInt("ps"))
        .pageIndex(request.paramAsInt("p"))
        .build());
      foundRules = searchResult.results();
      hasMore = searchResult.paging().hasNextPage();
      total = searchResult.paging().total();
    } else {
      RuleKey ruleKey = RuleKey.parse(ruleKeyParam);
      Rule rule = findRule(ruleKey);
      if (rule != null) {
        foundRules = Collections.singleton(rule);
        total = 1L;
      }
      hasMore = false;
    }

    JsonWriter json = response.newJsonWriter();
    json.beginObject().name("results").beginArray();
    for (Rule rule : foundRules) {
      json.beginObject();
      writeRule(rule, json);
      json.endObject();
    }
    json.endArray().prop("more", hasMore).prop("total", total).endObject().close();
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
      .prop("repository", rule.ruleKey().repository())
      .prop("name", rule.name())
      .prop("language", languageName)
      .prop("status", rule.status())
    ;
    DebtRemediationFunction function = rule.debtRemediationFunction();
    if (function != null) {
      json
        .prop("debtCharacteristic", rule.debtCharacteristicKey())
        .prop("debtSubCharacteristic", rule.debtSubCharacteristicKey())
        .prop("debtRemediationFunction", function.type().name())
        .prop("debtRemediationCoefficient", function.coefficient())
        .prop("debtRemediationOffset", function.offset())
      ;
    }
  }
}
