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
package org.sonar.server.qualityprofile;

import com.google.common.base.Function;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;

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

    QProfileComparisonResult result = new QProfileComparisonResult(left, right);
    for (RuleKey ruleKey : allRules) {
      if (!leftActiveRulesByRuleKey.containsKey(ruleKey)) {
        result.inRight.put(ruleKey, rightActiveRulesByRuleKey.get(ruleKey));
      } else if (!rightActiveRulesByRuleKey.containsKey(ruleKey)) {
        result.inLeft.put(ruleKey, leftActiveRulesByRuleKey.get(ruleKey));
      } else {
        compareActivationParams(dbSession, leftActiveRulesByRuleKey.get(ruleKey), rightActiveRulesByRuleKey.get(ruleKey), result);
      }
    }
    return result;
  }

  private void compareActivationParams(DbSession session, ActiveRuleDto leftRule, ActiveRuleDto rightRule, QProfileComparisonResult result) {
    RuleKey key = leftRule.getRuleKey();
    Map<String, String> leftParams = paramDtoToMap(dbClient.activeRuleDao().selectParamsByActiveRuleId(session, leftRule.getId()));
    Map<String, String> rightParams = paramDtoToMap(dbClient.activeRuleDao().selectParamsByActiveRuleId(session, rightRule.getId()));
    if (leftParams.equals(rightParams) && leftRule.getSeverityString().equals(rightRule.getSeverityString())) {
      result.same.put(key, leftRule);
    } else {
      ActiveRuleDiff diff = new ActiveRuleDiff();

      diff.leftSeverity = leftRule.getSeverityString();
      diff.rightSeverity = rightRule.getSeverityString();

      diff.paramDifference = Maps.difference(leftParams, rightParams);
      result.modified.put(key, diff);
    }
  }

  private Map<RuleKey, OrgActiveRuleDto> loadActiveRules(DbSession dbSession, QProfileDto profile) {
    return Maps.uniqueIndex(dbClient.activeRuleDao().selectByProfile(dbSession, profile), ActiveRuleToRuleKey.INSTANCE);
  }

  public static class QProfileComparisonResult {

    private final QProfileDto left;
    private final QProfileDto right;
    private final Map<RuleKey, ActiveRuleDto> inLeft = Maps.newHashMap();
    private final Map<RuleKey, ActiveRuleDto> inRight = Maps.newHashMap();
    private final Map<RuleKey, ActiveRuleDiff> modified = Maps.newHashMap();
    private final Map<RuleKey, ActiveRuleDto> same = Maps.newHashMap();

    public QProfileComparisonResult(QProfileDto left, QProfileDto right) {
      this.left = left;
      this.right = right;
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

    public Collection<RuleKey> collectRuleKeys() {
      Set<RuleKey> keys = new HashSet<>();
      keys.addAll(inLeft.keySet());
      keys.addAll(inRight.keySet());
      keys.addAll(modified.keySet());
      keys.addAll(same.keySet());
      return keys;
    }
  }

  public static class ActiveRuleDiff {
    private String leftSeverity;
    private String rightSeverity;
    private MapDifference<String, String> paramDifference;

    public String leftSeverity() {
      return leftSeverity;
    }

    public String rightSeverity() {
      return rightSeverity;
    }

    public MapDifference<String, String> paramDifference() {
      return paramDifference;
    }
  }

  private enum ActiveRuleToRuleKey implements Function<ActiveRuleDto, RuleKey> {
    INSTANCE;

    @Override
    public RuleKey apply(@Nonnull ActiveRuleDto input) {
      return input.getRuleKey();
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
