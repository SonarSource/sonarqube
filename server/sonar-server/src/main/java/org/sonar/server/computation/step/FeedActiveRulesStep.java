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
package org.sonar.server.computation.step;

import com.google.common.base.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.issue.Rule;
import org.sonar.server.computation.issue.RuleRepository;
import org.sonar.server.computation.qualityprofile.ActiveRule;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolderImpl;

import static com.google.common.collect.FluentIterable.from;

public class FeedActiveRulesStep implements ComputationStep {

  private final BatchReportReader batchReportReader;
  private final ActiveRulesHolderImpl activeRulesHolder;
  private final RuleRepository ruleRepository;

  public FeedActiveRulesStep(BatchReportReader batchReportReader, ActiveRulesHolderImpl activeRulesHolder, RuleRepository ruleRepository) {
    this.batchReportReader = batchReportReader;
    this.activeRulesHolder = activeRulesHolder;
    this.ruleRepository = ruleRepository;
  }

  @Override
  public void execute() {
    List<ActiveRule> activeRules = new ArrayList<>();
    try (CloseableIterator<BatchReport.ActiveRule> batchActiveRules = batchReportReader.readActiveRules()) {
      while (batchActiveRules.hasNext()) {
        BatchReport.ActiveRule batchActiveRule = batchActiveRules.next();
        activeRules.add(convert(batchActiveRule));
      }
    }

    List<ActiveRule> validActiveRules = from(activeRules).filter(new IsValid()).toList();
    activeRulesHolder.set(validActiveRules);
  }

  private class IsValid implements Predicate<ActiveRule> {
    @Override
    public boolean apply(@Nonnull ActiveRule input) {
      if (ruleRepository.hasKey(input.getRuleKey())) {
        Rule rule = ruleRepository.getByKey(input.getRuleKey());
        return rule.getStatus() != RuleStatus.REMOVED;
      }
      return false;
    }
  }

  @Override
  public String getDescription() {
    return getClass().getSimpleName();
  }

  private ActiveRule convert(BatchReport.ActiveRule input) {
    RuleKey key = RuleKey.of(input.getRuleRepository(), input.getRuleKey());
    Map<String, String> params = new HashMap<>();
    for (BatchReport.ActiveRule.ActiveRuleParam inputParam : input.getParamList()) {
      params.put(inputParam.getKey(), inputParam.getValue());
    }
    return new ActiveRule(key, input.getSeverity().name(), params);
  }
}
