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

import com.google.common.base.Strings;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.markdown.Markdown;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleNote;
import org.sonar.server.rule.Rules;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;

public class RuleShowWsHandler implements RequestHandler {

  private final Rules rules;
  private final Languages languages;

  // Only used to get manual rules
  private final RuleFinder ruleFinder;
  private final I18n i18n;

  public RuleShowWsHandler(Rules rules, RuleFinder ruleFinder, I18n i18n, Languages languages) {
    this.rules = rules;
    this.ruleFinder = ruleFinder;
    this.i18n = i18n;
    this.languages = languages;
  }

  @Override
  public void handle(Request request, Response response) {
    final String ruleKeyParam = request.mandatoryParam("key");
    RuleKey ruleKey = RuleKey.parse(ruleKeyParam);
    Rule rule = findRule(ruleKey);
    if (rule == null) {
      throw new NotFoundException("Rule not found: " + ruleKey);
    }

    JsonWriter json = response.newJsonWriter();
    json.beginObject().name("rule").beginObject();
    writeRule(rule, json);
    writeTags(rule, json);
    json.endObject().endObject().close();
  }

  @CheckForNull
  private Rule findRule(RuleKey ruleKey) {
    // TODO remove this when manual rules are indexed in E/S
    if (ruleKey.repository().equals(Rule.MANUAL_REPOSITORY_KEY)) {
      org.sonar.api.rules.Rule rule = ruleFinder.findByKey(ruleKey);
      if (rule != null) {
        RulePriority severity = rule.getSeverity();
        return new Rule.Builder()
          .setKey(rule.getKey())
          .setRepositoryKey(rule.getRepositoryKey())
          .setName(rule.getName())
          .setDescription(rule.getDescription())
          .setSeverity(severity != null ? severity.name() : null)
          .setStatus(rule.getStatus())
          .setCreatedAt(rule.getCreatedAt())
          .setUpdatedAt(rule.getUpdatedAt()).build();
      }
      return null;
    } else {
      return rules.findByKey(ruleKey);
    }
  }

  private void writeRule(Rule rule, JsonWriter json) {
    json
      .prop("key", rule.ruleKey().toString())
      .prop("name", rule.name())
      .prop("description", rule.description())
    ;
    addLanguage(rule, json);
    addNote(rule, json);
    addDate(rule.createdAt(), "createdAt", json);
    addFormattedDate(rule.createdAt(), "fCreatedAt", json);
    addDate(rule.updatedAt(), "updatedAt", json);
    addFormattedDate(rule.updatedAt(), "fUpdatedAt", json);
  }

  private void addLanguage(Rule rule, JsonWriter json) {
    String languageKey = rule.language();
    if (languageKey != null) {
      Language language = languages.get(languageKey);
      json.prop("language", language == null ? languageKey : language.getName());
    }

  }

  private void addNote(Rule rule, JsonWriter json) {
    RuleNote ruleNote = rule.ruleNote();
    if (ruleNote != null && !Strings.isNullOrEmpty(ruleNote.data())) {
      json.prop("noteRaw", ruleNote.data())
        .prop("noteHtml", Markdown.convertToHtml(ruleNote.data()));
    }
  }

  private void writeTags(Rule rule, JsonWriter json) {
    json.name("tags").beginArray();
    for (String adminTag : rule.adminTags()) {
      json.value(adminTag);
    }
    json.endArray();

    json.name("sysTags").beginArray();
    for (String systemTag : rule.systemTags()) {
      json.value(systemTag);
    }
    json.endArray();
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
