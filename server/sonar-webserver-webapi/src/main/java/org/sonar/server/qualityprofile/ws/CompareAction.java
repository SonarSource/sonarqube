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
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleRepositoryDto;
import org.sonar.server.qualityprofile.QProfileComparison;
import org.sonar.server.qualityprofile.QProfileComparison.ActiveRuleDiff;
import org.sonar.server.qualityprofile.QProfileComparison.QProfileComparisonResult;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;

public class CompareAction implements QProfileWsAction {

  private static final String ATTRIBUTE_LEFT = "left";
  private static final String ATTRIBUTE_RIGHT = "right";
  private static final String ATTRIBUTE_IN_LEFT = "inLeft";
  private static final String ATTRIBUTE_IN_RIGHT = "inRight";
  private static final String ATTRIBUTE_MODIFIED = "modified";
  private static final String ATTRIBUTE_SAME = "same";
  private static final String ATTRIBUTE_KEY = "key";
  private static final String ATTRIBUTE_NAME = "name";
  private static final String ATTRIBUTE_SEVERITY = "severity";
  private static final String ATTRIBUTE_PLUGIN_KEY = "pluginKey";
  private static final String ATTRIBUTE_PLUGIN_NAME = "pluginName";
  private static final String ATTRIBUTE_LANGUAGE_KEY = "languageKey";
  private static final String ATTRIBUTE_LANGUAGE_NAME = "languageName";
  private static final String ATTRIBUTE_PARAMS = "params";

  private static final String PARAM_LEFT_KEY = "leftKey";
  private static final String PARAM_RIGHT_KEY = "rightKey";

  private final DbClient dbClient;
  private final QProfileComparison comparator;
  private final Languages languages;
  private final QProfileWsSupport wsSupport;

  public CompareAction(DbClient dbClient, QProfileComparison comparator, Languages languages, QProfileWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.comparator = comparator;
    this.languages = languages;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(NewController context) {
    NewAction compare = context.createAction("compare")
      .setDescription("Compare two quality profiles.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("compare-example.json"))
      .setSince("5.2");

    compare.createParam(PARAM_LEFT_KEY)
      .setDescription("Profile key.")
      .setExampleValue(UUID_EXAMPLE_01)
      .setRequired(true);

    compare.createParam(PARAM_RIGHT_KEY)
      .setDescription("Another profile key.")
      .setExampleValue(UUID_EXAMPLE_02)
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String leftKey = request.mandatoryParam(PARAM_LEFT_KEY);
    String rightKey = request.mandatoryParam(PARAM_RIGHT_KEY);

    try (DbSession dbSession = dbClient.openSession(false)) {
      QProfileDto left = dbClient.qualityProfileDao().selectByUuid(dbSession, leftKey);
      checkArgument(left != null, "Could not find left profile '%s'", leftKey);
      QProfileDto right = dbClient.qualityProfileDao().selectByUuid(dbSession, rightKey);
      checkArgument(right != null, "Could not find right profile '%s'", rightKey);

      checkArgument(Objects.equals(left.getOrganizationUuid(), right.getOrganizationUuid()),
        "Cannot compare quality profiles of different organizations. Quality profile left with key '%s' belongs to organization '%s', " +
          "quality profile right with key '%s' belongs to organization '%s'.",
        leftKey, left.getOrganizationUuid(), rightKey, right.getOrganizationUuid());
      wsSupport.getOrganization(dbSession, left);

      QProfileComparisonResult result = comparator.compare(dbSession, left, right);

      List<RuleDefinitionDto> referencedRules = dbClient.ruleDao().selectDefinitionByKeys(dbSession, new ArrayList<>(result.collectRuleKeys()));
      Map<RuleKey, RuleDefinitionDto> rulesByKey = Maps.uniqueIndex(referencedRules, RuleDefinitionDto::getKey);
      Map<String, RuleRepositoryDto> repositoriesByKey = Maps.uniqueIndex(dbClient.ruleRepositoryDao().selectAll(dbSession), RuleRepositoryDto::getKey);
      writeResult(response.newJsonWriter(), result, rulesByKey, repositoriesByKey);
    }
  }

  private void writeResult(JsonWriter json, QProfileComparisonResult result, Map<RuleKey, RuleDefinitionDto> rulesByKey, Map<String, RuleRepositoryDto> repositoriesByKey) {
    json.beginObject();

    json.name(ATTRIBUTE_LEFT).beginObject();
    writeProfile(json, result.left());
    json.endObject();

    json.name(ATTRIBUTE_RIGHT).beginObject();
    writeProfile(json, result.right());
    json.endObject();

    json.name(ATTRIBUTE_IN_LEFT);
    writeRules(json, result.inLeft(), rulesByKey, repositoriesByKey);

    json.name(ATTRIBUTE_IN_RIGHT);
    writeRules(json, result.inRight(), rulesByKey, repositoriesByKey);

    json.name(ATTRIBUTE_MODIFIED);
    writeDifferences(json, result.modified(), rulesByKey, repositoriesByKey);

    json.name(ATTRIBUTE_SAME);
    writeRules(json, result.same(), rulesByKey, repositoriesByKey);

    json.endObject().close();
  }

  private static void writeProfile(JsonWriter json, QProfileDto profile) {
    json.prop(ATTRIBUTE_KEY, profile.getKee())
      .prop(ATTRIBUTE_NAME, profile.getName());
  }

  private void writeRules(JsonWriter json, Map<RuleKey, ActiveRuleDto> activeRules, Map<RuleKey, RuleDefinitionDto> rulesByKey,
    Map<String, RuleRepositoryDto> repositoriesByKey) {
    json.beginArray();
    for (Entry<RuleKey, ActiveRuleDto> activeRule : activeRules.entrySet()) {
      RuleKey key = activeRule.getKey();
      ActiveRuleDto value = activeRule.getValue();

      json.beginObject();
      RuleDefinitionDto rule = rulesByKey.get(key);
      writeRule(json, rule, repositoriesByKey.get(rule.getRepositoryKey()));
      json.prop(ATTRIBUTE_SEVERITY, value.getSeverityString());
      json.endObject();
    }
    json.endArray();
  }

  private void writeRule(JsonWriter json, RuleDefinitionDto rule, @Nullable RuleRepositoryDto repository) {
    String repositoryKey = rule.getRepositoryKey();
    json.prop(ATTRIBUTE_KEY, rule.getKey().toString())
      .prop(ATTRIBUTE_NAME, rule.getName())
      .prop(ATTRIBUTE_PLUGIN_KEY, repositoryKey);

    if (repository != null) {
      String languageKey = repository.getLanguage();
      Language language = languages.get(languageKey);

      json.prop(ATTRIBUTE_PLUGIN_NAME, repository.getName());
      json.prop(ATTRIBUTE_LANGUAGE_KEY, languageKey);
      json.prop(ATTRIBUTE_LANGUAGE_NAME, language == null ? null : language.getName());
    }
  }

  private void writeDifferences(JsonWriter json, Map<RuleKey, ActiveRuleDiff> modified, Map<RuleKey, RuleDefinitionDto> rulesByKey,
    Map<String, RuleRepositoryDto> repositoriesByKey) {
    json.beginArray();
    for (Entry<RuleKey, ActiveRuleDiff> diffEntry : modified.entrySet()) {
      RuleKey key = diffEntry.getKey();
      ActiveRuleDiff value = diffEntry.getValue();
      json.beginObject();
      RuleDefinitionDto rule = rulesByKey.get(key);
      writeRule(json, rule, repositoriesByKey.get(rule.getRepositoryKey()));

      json.name(ATTRIBUTE_LEFT).beginObject();
      json.prop(ATTRIBUTE_SEVERITY, value.leftSeverity());
      json.name(ATTRIBUTE_PARAMS).beginObject();
      for (Entry<String, ValueDifference<String>> valueDiff : value.paramDifference().entriesDiffering().entrySet()) {
        json.prop(valueDiff.getKey(), valueDiff.getValue().leftValue());
      }
      for (Entry<String, String> valueDiff : value.paramDifference().entriesOnlyOnLeft().entrySet()) {
        json.prop(valueDiff.getKey(), valueDiff.getValue());
      }
      // params
      json.endObject();
      // left
      json.endObject();

      json.name(ATTRIBUTE_RIGHT).beginObject();
      json.prop(ATTRIBUTE_SEVERITY, value.rightSeverity());
      json.name(ATTRIBUTE_PARAMS).beginObject();
      for (Entry<String, ValueDifference<String>> valueDiff : value.paramDifference().entriesDiffering().entrySet()) {
        json.prop(valueDiff.getKey(), valueDiff.getValue().rightValue());
      }
      for (Entry<String, String> valueDiff : value.paramDifference().entriesOnlyOnRight().entrySet()) {
        json.prop(valueDiff.getKey(), valueDiff.getValue());
      }
      // params
      json.endObject();
      // right
      json.endObject();

      // rule
      json.endObject();
    }
    json.endArray();
  }

}
