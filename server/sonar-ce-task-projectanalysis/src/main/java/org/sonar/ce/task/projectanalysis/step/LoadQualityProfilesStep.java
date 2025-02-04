/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.issue.ImpactMapper;
import org.sonar.ce.task.projectanalysis.issue.Rule;
import org.sonar.ce.task.projectanalysis.issue.RuleRepository;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolderImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

public class LoadQualityProfilesStep implements ComputationStep {

  private final ScannerReportReader scannerReportReader;
  private final ActiveRulesHolderImpl activeRulesHolder;
  private final RuleRepository ruleRepository;

  public LoadQualityProfilesStep(ScannerReportReader scannerReportReader, ActiveRulesHolderImpl activeRulesHolder, RuleRepository ruleRepository) {
    this.scannerReportReader = scannerReportReader;
    this.activeRulesHolder = activeRulesHolder;
    this.ruleRepository = ruleRepository;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    List<ActiveRule> activeRules = new ArrayList<>();
    try (CloseableIterator<ScannerReport.ActiveRule> batchActiveRules = scannerReportReader.readActiveRules()) {
      while (batchActiveRules.hasNext()) {
        ScannerReport.ActiveRule scannerReportActiveRule = batchActiveRules.next();
        Optional<Rule> rule = ruleRepository.findByKey(RuleKey.of(scannerReportActiveRule.getRuleRepository(), scannerReportActiveRule.getRuleKey()));
        if (rule.isPresent() && rule.get().getStatus() != RuleStatus.REMOVED && !rule.get().isExternal()) {
          activeRules.add(convert(scannerReportActiveRule, rule.get()));
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
    Map<String, String> params = new HashMap<>(input.getParamsByKeyMap());
    Map<SoftwareQuality, Severity> impacts = input.getImpactsList().stream()
      .collect(Collectors.toMap(
        i -> SoftwareQuality.valueOf(i.getSoftwareQuality().name()),
        i -> ImpactMapper.mapImpactSeverity(i.getSeverity())));
    return new ActiveRule(key, input.getSeverity().name(), params, input.getUpdatedAt(), rule.getPluginKey(), input.getQProfileKey(), impacts);
  }
}
