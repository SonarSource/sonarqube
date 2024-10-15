/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
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
  private static final String ATTRIBUTE_CLEAN_CODE_ATTRIBUTE_CATEGORY = "cleanCodeAttributeCategory";
  private static final String ATTRIBUTE_IMPACTS = "impacts";
  private static final String ATTRIBUTE_IMPACT_SOFTWARE_QUALITY = "softwareQuality";
  private static final String ATTRIBUTE_IMPACT_SEVERITY = "severity";

  private static final String PARAM_LEFT_KEY = "leftKey";
  private static final String PARAM_RIGHT_KEY = "rightKey";

  private final DbClient dbClient;
  private final QProfileComparison comparator;
  private final Languages languages;

  public CompareAction(DbClient dbClient, QProfileComparison comparator, Languages languages) {
    this.dbClient = dbClient;
    this.comparator = comparator;
    this.languages = languages;
  }

  @Override
  public void define(NewController context) {
    NewAction compare = context.createAction("compare")
      .setDescription("Compare two quality profiles.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("compare-example.json"))
      .setSince("5.2")
      .setChangelog(
        new Change("10.3", String.format("Added '%s' and '%s' fields", ATTRIBUTE_CLEAN_CODE_ATTRIBUTE_CATEGORY, ATTRIBUTE_IMPACTS)),
        new Change("10.3", String.format("Dropped '%s' field from '%s', '%s' and '%s' objects",
          ATTRIBUTE_SEVERITY, ATTRIBUTE_SAME, ATTRIBUTE_IN_LEFT, ATTRIBUTE_IN_RIGHT)),
    new Change("10.8", "'impacts' are part of the 'left' and 'right' sections of the 'modified' array"));

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

      QProfileComparisonResult result = comparator.compare(dbSession, left, right);

      Map<RuleKey, RuleDto> rulesByKey = result.getImpactedRules();
      Map<String, RuleRepositoryDto> repositoriesByKey = Maps.uniqueIndex(dbClient.ruleRepositoryDao().selectAll(dbSession), RuleRepositoryDto::getKey);
      writeResult(response.newJsonWriter(), result, rulesByKey, repositoriesByKey);
    }
  }

  private void writeResult(JsonWriter json, QProfileComparisonResult result, Map<RuleKey, RuleDto> rulesByKey, Map<String, RuleRepositoryDto> repositoriesByKey) {
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

  private void writeRules(JsonWriter json, Map<RuleKey, ActiveRuleDto> activeRules, Map<RuleKey, RuleDto> rulesByKey,
    Map<String, RuleRepositoryDto> repositoriesByKey) {
    json.beginArray();
    for (ActiveRuleDto activeRuleDto : activeRules.values()) {
      json.beginObject();
      RuleDto rule = rulesByKey.get(activeRuleDto.getRuleKey());
      writeRule(json, rule, repositoriesByKey.get(rule.getRepositoryKey()));
      writeActiveRuleImpacts(json, rule, activeRuleDto);
      json.endObject();
    }
    json.endArray();
  }

  private void writeRule(JsonWriter json, RuleDto rule, @Nullable RuleRepositoryDto repository) {
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

    CleanCodeAttribute cleanCodeAttribute = rule.getCleanCodeAttribute();
    if (cleanCodeAttribute != null) {
      json.prop(ATTRIBUTE_CLEAN_CODE_ATTRIBUTE_CATEGORY, cleanCodeAttribute.getAttributeCategory().toString());
    }
  }

  private static void writeActiveRuleImpacts(JsonWriter json, RuleDto rule, ActiveRuleDto activeRuleDto) {
    json.name(ATTRIBUTE_IMPACTS);
    json.beginArray();
    List<ImpactDto> effectiveImpacts = QProfileComparison.computeEffectiveImpacts(rule, activeRuleDto.getImpacts());
    writeImpacts(json, effectiveImpacts);
    json.endArray();
  }

  private static void writeImpacts(JsonWriter json, List<ImpactDto> effectiveImpacts) {
    for (ImpactDto impact : effectiveImpacts) {
      json.beginObject();
      json.prop(ATTRIBUTE_IMPACT_SOFTWARE_QUALITY, impact.getSoftwareQuality().toString());
      json.prop(ATTRIBUTE_IMPACT_SEVERITY, impact.getSeverity().toString());
      json.endObject();
    }
  }

  private void writeDifferences(JsonWriter json, Map<RuleKey, ActiveRuleDiff> modified, Map<RuleKey, RuleDto> rulesByKey,
    Map<String, RuleRepositoryDto> repositoriesByKey) {
    json.beginArray();
    for (Entry<RuleKey, ActiveRuleDiff> diffEntry : modified.entrySet()) {
      RuleKey key = diffEntry.getKey();
      ActiveRuleDiff value = diffEntry.getValue();
      json.beginObject();
      RuleDto rule = rulesByKey.get(key);
      writeRule(json, rule, repositoriesByKey.get(rule.getRepositoryKey()));

      json.name(ATTRIBUTE_LEFT).beginObject();
      json.prop(ATTRIBUTE_SEVERITY, value.leftSeverity());


      List<ImpactDto> leftImpacts = getLeftImpactDtos(value);
      json.name(ATTRIBUTE_IMPACTS);
      json.beginArray();
      writeImpacts(json, leftImpacts);
      json.endArray();

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

      List<ImpactDto> rightImpacts = getRightImpactDtos(value);

      json.name(ATTRIBUTE_IMPACTS);
      json.beginArray();
      writeImpacts(json, rightImpacts);
      json.endArray();

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

  private static @NotNull List<ImpactDto> getRightImpactDtos(ActiveRuleDiff activeRuleDiff) {
    List<ImpactDto> rightImpacts = new ArrayList<>();
    for (Entry<SoftwareQuality, ValueDifference<Severity>> valueDiff : activeRuleDiff.impactDifference().entriesDiffering().entrySet()) {
      rightImpacts.add(new ImpactDto(valueDiff.getKey(), valueDiff.getValue().rightValue()));
    }
    for (Entry<SoftwareQuality, Severity> valueDiff : activeRuleDiff.impactDifference().entriesOnlyOnRight().entrySet()) {
      rightImpacts.add(new ImpactDto(valueDiff.getKey(), valueDiff.getValue()));
    }
    return rightImpacts;
  }

  private static @NotNull List<ImpactDto> getLeftImpactDtos(ActiveRuleDiff activeRuleDiff) {
    List<ImpactDto> leftImpacts = new ArrayList<>();
    for (Entry<SoftwareQuality, ValueDifference<Severity>> valueDiff : activeRuleDiff.impactDifference().entriesDiffering().entrySet()) {
      leftImpacts.add(new ImpactDto(valueDiff.getKey(), valueDiff.getValue().leftValue()));
    }
    for (Entry<SoftwareQuality, Severity> valueDiff : activeRuleDiff.impactDifference().entriesOnlyOnLeft().entrySet()) {
      leftImpacts.add(new ImpactDto(valueDiff.getKey(), valueDiff.getValue()));
    }
    return leftImpacts;
  }

}
