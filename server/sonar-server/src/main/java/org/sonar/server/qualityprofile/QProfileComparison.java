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
package org.sonar.server.qualityprofile;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.util.NonNullInputFunction;
import org.sonar.server.db.DbClient;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@ServerSide
public class QProfileComparison {

  private final DbClient dbClient;

  private final QProfileLoader profileLoader;

  public QProfileComparison(DbClient dbClient, QProfileLoader profileLoader) {
    this.dbClient = dbClient;
    this.profileLoader = profileLoader;
  }

  public QProfileComparisonResult compare(String leftKey, String rightKey) {
    QProfileComparisonResult result = new QProfileComparisonResult();

    DbSession dbSession = dbClient.openSession(false);

    try {
      compare(leftKey, rightKey, dbSession, result);
    } finally {
      dbSession.close();
    }

    return result;
  }

  private void compare(String leftKey, String rightKey, DbSession session, QProfileComparisonResult result) {
    result.left = dbClient.qualityProfileDao().getByKey(session, leftKey);
    Preconditions.checkArgument(result.left != null, String.format("Could not find left profile '%s'", leftKey));
    result.right = dbClient.qualityProfileDao().getByKey(session, rightKey);
    Preconditions.checkArgument(result.right != null, String.format("Could not find right profile '%s'", leftKey));

    Map<RuleKey, ActiveRule> leftActiveRulesByRuleKey = loadActiveRules(leftKey);
    Map<RuleKey, ActiveRule> rightActiveRulesByRuleKey = loadActiveRules(rightKey);

    Set<RuleKey> allRules = Sets.newHashSet();
    allRules.addAll(leftActiveRulesByRuleKey.keySet());
    allRules.addAll(rightActiveRulesByRuleKey.keySet());

    for (RuleKey ruleKey : allRules) {
      if (!leftActiveRulesByRuleKey.containsKey(ruleKey)) {
        result.inRight.put(ruleKey, rightActiveRulesByRuleKey.get(ruleKey));
      } else if (!rightActiveRulesByRuleKey.containsKey(ruleKey)) {
        result.inLeft.put(ruleKey, leftActiveRulesByRuleKey.get(ruleKey));
      } else {
        compareActivationParams(leftActiveRulesByRuleKey.get(ruleKey), rightActiveRulesByRuleKey.get(ruleKey), result);
      }
    }
  }

  private void compareActivationParams(ActiveRule leftRule, ActiveRule rightRule, QProfileComparisonResult result) {
    RuleKey key = leftRule.key().ruleKey();
    if (leftRule.params().equals(rightRule.params()) && leftRule.severity().equals(rightRule.severity())) {
      result.same.put(key, leftRule);
    } else {
      ActiveRuleDiff diff = new ActiveRuleDiff();

      diff.leftSeverity = leftRule.severity();
      diff.rightSeverity = rightRule.severity();

      diff.paramDifference = Maps.difference(leftRule.params(), rightRule.params());
      result.modified.put(key, diff);
    }
  }

  private Map<RuleKey, ActiveRule> loadActiveRules(String profileKey) {
    return Maps.uniqueIndex(profileLoader.findActiveRulesByProfile(profileKey), new NonNullInputFunction<ActiveRule, RuleKey>() {
      @Override
      protected RuleKey doApply(ActiveRule input) {
        return input.key().ruleKey();
      }
    });
  }

  public static class QProfileComparisonResult {

    private QualityProfileDto left;
    private QualityProfileDto right;
    private Map<RuleKey, ActiveRule> inLeft = Maps.newHashMap();
    private Map<RuleKey, ActiveRule> inRight = Maps.newHashMap();
    private Map<RuleKey, ActiveRuleDiff> modified = Maps.newHashMap();
    private Map<RuleKey, ActiveRule> same = Maps.newHashMap();

    public QualityProfileDto left() {
      return left;
    }

    public QualityProfileDto right() {
      return right;
    }

    public Map<RuleKey, ActiveRule> inLeft() {
      return inLeft;
    }

    public Map<RuleKey, ActiveRule> inRight() {
      return inRight;
    }

    public Map<RuleKey, ActiveRuleDiff> modified() {
      return modified;
    }

    public Map<RuleKey, ActiveRule> same() {
      return same;
    }

    public Collection<RuleKey> collectRuleKeys() {
      Set<RuleKey> keys = Sets.newHashSet();
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

}
