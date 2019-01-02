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
package org.sonar.ce.task.projectanalysis.step;

import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.issue.DumbRule;
import org.sonar.ce.task.projectanalysis.issue.RuleRepositoryRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolderImpl;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.rule.RuleTesting.XOO_X1;
import static org.sonar.db.rule.RuleTesting.XOO_X2;

public class LoadQualityProfilesStepTest {

  @Rule
  public BatchReportReaderRule batchReportReader = new BatchReportReaderRule();

  @Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule();

  private ActiveRulesHolderImpl activeRulesHolder = new ActiveRulesHolderImpl();
  private LoadQualityProfilesStep underTest = new LoadQualityProfilesStep(batchReportReader, activeRulesHolder, ruleRepository);

  @Test
  public void feed_active_rules() {
    ruleRepository.add(XOO_X1)
      .setPluginKey("xoo");
    ruleRepository.add(XOO_X2)
      .setPluginKey("xoo");

    ScannerReport.ActiveRule.Builder batch1 = ScannerReport.ActiveRule.newBuilder()
      .setRuleRepository(XOO_X1.repository())
      .setRuleKey(XOO_X1.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .setCreatedAt(1000L)
      .setUpdatedAt(1200L);
    batch1.getMutableParamsByKey().put("p1", "v1");

    ScannerReport.ActiveRule.Builder batch2 = ScannerReport.ActiveRule.newBuilder()
      .setRuleRepository(XOO_X2.repository()).setRuleKey(XOO_X2.rule()).setSeverity(Constants.Severity.MAJOR);
    batchReportReader.putActiveRules(asList(batch1.build(), batch2.build()));

    underTest.execute(new TestComputationStepContext());

    assertThat(activeRulesHolder.getAll()).hasSize(2);

    ActiveRule ar1 = activeRulesHolder.get(XOO_X1).get();
    assertThat(ar1.getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(ar1.getParams()).containsExactly(MapEntry.entry("p1", "v1"));
    assertThat(ar1.getPluginKey()).isEqualTo("xoo");
    assertThat(ar1.getUpdatedAt()).isEqualTo(1200L);

    ActiveRule ar2 = activeRulesHolder.get(XOO_X2).get();
    assertThat(ar2.getSeverity()).isEqualTo(Severity.MAJOR);
    assertThat(ar2.getParams()).isEmpty();
    assertThat(ar2.getPluginKey()).isEqualTo("xoo");
    assertThat(ar1.getUpdatedAt()).isEqualTo(1200L);
  }

  @Test
  public void ignore_rules_with_status_REMOVED() {
    ruleRepository.add(new DumbRule(XOO_X1).setStatus(RuleStatus.REMOVED));

    ScannerReport.ActiveRule.Builder batch1 = ScannerReport.ActiveRule.newBuilder()
      .setRuleRepository(XOO_X1.repository()).setRuleKey(XOO_X1.rule())
      .setSeverity(Constants.Severity.BLOCKER);
    batchReportReader.putActiveRules(asList(batch1.build()));

    underTest.execute(new TestComputationStepContext());

    assertThat(activeRulesHolder.getAll()).isEmpty();
  }

  @Test
  public void ignore_not_found_rules() {
    ScannerReport.ActiveRule.Builder batch1 = ScannerReport.ActiveRule.newBuilder()
      .setRuleRepository(XOO_X1.repository()).setRuleKey(XOO_X1.rule())
      .setSeverity(Constants.Severity.BLOCKER);
    batchReportReader.putActiveRules(asList(batch1.build()));

    underTest.execute(new TestComputationStepContext());

    assertThat(activeRulesHolder.getAll()).isEmpty();
  }
}
