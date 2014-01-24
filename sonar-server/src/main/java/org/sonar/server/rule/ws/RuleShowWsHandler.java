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

import org.sonar.api.i18n.I18n;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.markdown.Markdown;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.Date;

public class RuleShowWsHandler implements RequestHandler {

  private final RuleFinder ruleFinder;
  private final I18n i18n;

  public RuleShowWsHandler(RuleFinder ruleFinder, I18n i18n) {
    this.ruleFinder = ruleFinder;
    this.i18n = i18n;
  }

  @Override
  public void handle(Request request, Response response) {
    final String ruleKey = request.requiredParam("key");
    Rule rule = ruleFinder.findByKey(RuleKey.parse(ruleKey));
    if (rule == null) {
      throw new NotFoundException("Rule not found: " + ruleKey);
    }

    JsonWriter json = response.newJsonWriter();
    json.beginObject().name("rule").beginObject();
    writeRule(rule, json);
    writeTags(rule, json);
    json.endObject().endObject().close();
  }

  private void writeRule(Rule rule, JsonWriter json) {
    json
      .prop("key", rule.ruleKey().toString())
      .prop("name", rule.getName())
      .prop("description", rule.getDescription())
    ;
    addNote(rule, json);
    addDate(rule.getCreatedAt(), "createdAt", json);
    addFormattedDate(rule.getCreatedAt(), "fCreatedAt", json);
    addDate(rule.getUpdatedAt(), "updatedAt", json);
    addFormattedDate(rule.getUpdatedAt(), "fUpdatedAt", json);
  }

  private void addNote(Rule rule, JsonWriter json) {
    // TODO
    json.prop("noteRaw", "*Extended rule description*")
      .prop("noteHtml", Markdown.convertToHtml("<em>Extended rule description</em>"))
    ;
  }

  // TODO
  private void writeTags(Rule rule, JsonWriter json) {
    json.name("tags").beginArray()
      .value("Complexity")
      .endArray();

    json.name("sysTags").beginArray()
      .value("Security")
      .endArray();
  }

  private void addDate(@Nullable Date date, String dateKey, JsonWriter json) {
    if (date != null) {
      json.prop(dateKey, DateUtils.formatDateTime(date));
    }
  }

  private void addFormattedDate(@Nullable Date date, String dateKey, JsonWriter json) {
    if (date != null) {
      json.prop(dateKey, i18n.formatDateTime(UserSession.get().locale(), date));
    }
  }

}
