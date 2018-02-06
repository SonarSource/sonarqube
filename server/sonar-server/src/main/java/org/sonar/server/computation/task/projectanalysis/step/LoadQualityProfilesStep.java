/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.issue.Rule;
import org.sonar.server.computation.task.projectanalysis.issue.RuleRepository;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.server.computation.task.projectanalysis.qualityprofile.ActiveRulesHolderImpl;
import org.sonar.server.computation.task.step.ComputationStep;

public class LoadQualityProfilesStep implements ComputationStep {

  private final BatchReportReader batchReportReader;
  private final ActiveRulesHolderImpl activeRulesHolder;
  private final RuleRepository ruleRepository;

  public LoadQualityProfilesStep(BatchReportReader batchReportReader, ActiveRulesHolderImpl activeRulesHolder, RuleRepository ruleRepository) {
    this.batchReportReader = batchReportReader;
    this.activeRulesHolder = activeRulesHolder;
    this.ruleRepository = ruleRepository;
  }

  @Override
  public void execute() {
    List<ActiveRule> activeRules = new ArrayList<>();
    try (CloseableIterator<ScannerReport.ActiveRule> batchActiveRules = batchReportReader.readActiveRules()) {
      while (batchActiveRules.hasNext()) {
        ScannerReport.ActiveRule scannerReportActiveRule = batchActiveRules.next();
        Optional<Rule> rule = ruleRepository.findByKey(RuleKey.of(scannerReportActiveRule.getRuleRepository(), scannerReportActiveRule.getRuleKey()));
        if (rule.isPresent() && rule.get().getStatus() != RuleStatus.REMOVED) {
          ActiveRule activeRule = convert(scannerReportActiveRule, rule.get());
          activeRules.add(activeRule);
        }
      }
    }

    activeRulesHolder.set(activeRules);
  }

  @Override
  public String getDescription() {
    return "Load quality profiles";
  }

  private static ActiveRule convert(ScannerReport.ActiveRule input, Rule rule) {
    RuleKey key = RuleKey.of(input.getRuleRepository(), input.getRuleKey());
    Map<String, String> params = new HashMap<>(input.getParamsByKey());
    return new ActiveRule(key, input.getSeverity().name(), params, input.getCreatedAt(), rule.getPluginKey());
  }
}
