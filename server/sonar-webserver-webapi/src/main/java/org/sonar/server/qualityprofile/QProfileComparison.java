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
package org.sonar.server.qualityprofile;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;

@ServerSide
public class QProfileComparison {

  private final DbClient dbClient;
  public QProfileComparison(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public QProfileComparisonResult compare(DbSession dbSession, QProfileDto left, QProfileDto right) {
    Map<RuleKey, OrgActiveRuleDto> leftActiveRulesByRuleKey = loadActiveRules(dbSession, left);
    Map<RuleKey, OrgActiveRuleDto> rightActiveRulesByRuleKey = loadActiveRules(dbSession, right);

    Set<RuleKey> allRules = new HashSet<>();
    allRules.addAll(leftActiveRulesByRuleKey.keySet());
    allRules.addAll(rightActiveRulesByRuleKey.keySet());

    List<RuleDto> referencedRules = dbClient.ruleDao().selectByKeys(dbSession, allRules);
    Map<RuleKey, RuleDto> rulesByKey = Maps.uniqueIndex(referencedRules, RuleDto::getKey);

    QProfileComparisonResult result = new QProfileComparisonResult(left, right, rulesByKey);
    for (RuleKey ruleKey : allRules) {
      if (!leftActiveRulesByRuleKey.containsKey(ruleKey)) {
        result.inRight.put(ruleKey, rightActiveRulesByRuleKey.get(ruleKey));
      } else if (!rightActiveRulesByRuleKey.containsKey(ruleKey)) {
        result.inLeft.put(ruleKey, leftActiveRulesByRuleKey.get(ruleKey));
      } else {
        compareActiveRules(dbSession,
          rulesByKey.get(ruleKey),
          leftActiveRulesByRuleKey.get(ruleKey),
          rightActiveRulesByRuleKey.get(ruleKey),
          result);
      }
    }
    return result;
  }

  private void compareActiveRules(DbSession session, RuleDto ruleDto, ActiveRuleDto leftRule, ActiveRuleDto rightRule,
    QProfileComparisonResult result) {
    RuleKey key = leftRule.getRuleKey();
    Map<String, String> leftParams = paramDtoToMap(dbClient.activeRuleDao().selectParamsByActiveRuleUuid(session, leftRule.getUuid()));
    Map<String, String> rightParams = paramDtoToMap(dbClient.activeRuleDao().selectParamsByActiveRuleUuid(session, rightRule.getUuid()));
    if (leftParams.equals(rightParams)
      && leftRule.getSeverityString().equals(rightRule.getSeverityString())
      && leftRule.getImpacts().equals(rightRule.getImpacts())) {
      result.same.put(key, leftRule);
    } else {
      ActiveRuleDiff diff = new ActiveRuleDiff();

      diff.leftSeverity = leftRule.getSeverityString();
      diff.rightSeverity = rightRule.getSeverityString();

      diff.impactDifference = Maps.difference(
        computeEffectiveImpactMap(ruleDto, leftRule.getImpacts()),
        computeEffectiveImpactMap(ruleDto, rightRule.getImpacts()));

      diff.paramDifference = Maps.difference(leftParams, rightParams);
      result.modified.put(key, diff);
    }
  }

  public static Map<SoftwareQuality, Severity> computeEffectiveImpactMap(RuleDto ruleDto, Map<SoftwareQuality, Severity> activeRuleImpacts) {
    Map<SoftwareQuality, Severity> impacts = ruleDto.getDefaultImpactsMap();
    impacts.replaceAll(activeRuleImpacts::getOrDefault);
    return impacts;
  }

  public static List<ImpactDto> computeEffectiveImpacts(RuleDto ruleDto, Map<SoftwareQuality, Severity> activeRuleImpacts) {
    Map<SoftwareQuality, Severity> impacts = computeEffectiveImpactMap(ruleDto, activeRuleImpacts);
    return impacts.entrySet().stream().map(e -> new ImpactDto(e.getKey(), e.getValue())).toList();
  }

  private Map<RuleKey, OrgActiveRuleDto> loadActiveRules(DbSession dbSession, QProfileDto profile) {
    return Maps.uniqueIndex(dbClient.activeRuleDao().selectByProfile(dbSession, profile), ActiveRuleDto::getRuleKey);
  }

  public static class QProfileComparisonResult {

    private final Map<RuleKey, RuleDto> rulesByKey;
    private final QProfileDto left;
    private final QProfileDto right;
    private final Map<RuleKey, ActiveRuleDto> inLeft = new HashMap<>();
    private final Map<RuleKey, ActiveRuleDto> inRight = new HashMap<>();
    private final Map<RuleKey, ActiveRuleDiff> modified = new HashMap<>();
    private final Map<RuleKey, ActiveRuleDto> same = new HashMap<>();

    public QProfileComparisonResult(QProfileDto left, QProfileDto right, Map<RuleKey, RuleDto> rulesByKey) {
      this.left = left;
      this.right = right;
      this.rulesByKey = rulesByKey;
    }

    public QProfileDto left() {
      return left;
    }

    public QProfileDto right() {
      return right;
    }

    public Map<RuleKey, ActiveRuleDto> inLeft() {
      return inLeft;
    }

    public Map<RuleKey, ActiveRuleDto> inRight() {
      return inRight;
    }

    public Map<RuleKey, ActiveRuleDiff> modified() {
      return modified;
    }

    public Map<RuleKey, ActiveRuleDto> same() {
      return same;
    }

    public Map<RuleKey, RuleDto> getImpactedRules() {
      return rulesByKey;
    }
  }

  public static class ActiveRuleDiff {
    private String leftSeverity;
    private String rightSeverity;
    private MapDifference<SoftwareQuality, Severity> impactDifference;
    private MapDifference<String, String> paramDifference;

    public String leftSeverity() {
      return leftSeverity;
    }

    public String rightSeverity() {
      return rightSeverity;
    }

    public MapDifference<SoftwareQuality, Severity> impactDifference() {
      return impactDifference;
    }

    public MapDifference<String, String> paramDifference() {
      return paramDifference;
    }
  }

  private static Map<String, String> paramDtoToMap(List<ActiveRuleParamDto> params) {
    Map<String, String> map = new HashMap<>();
    for (ActiveRuleParamDto dto : params) {
      map.put(dto.getKey(), dto.getValue());
    }
    return map;
  }

}
