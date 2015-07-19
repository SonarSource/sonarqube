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

import com.google.common.base.Optional;
import java.util.Arrays;
import org.assertj.core.data.MapEntry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.qualityprofile.ActiveRule;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolderImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class FeedActiveRulesStepTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public BatchReportReaderRule batchReportReader = new BatchReportReaderRule();

  ActiveRulesHolderImpl activeRulesHolder = new ActiveRulesHolderImpl();
  FeedActiveRulesStep underTest = new FeedActiveRulesStep(batchReportReader, activeRulesHolder);

  @Test
  public void write() throws Exception {
    BatchReport.ActiveRule.Builder batch1 = BatchReport.ActiveRule.newBuilder()
      .setRuleRepository("java").setRuleKey("S001")
      .setSeverity(Constants.Severity.BLOCKER);
    batch1.addParamBuilder().setKey("p1").setValue("v1").build();

    BatchReport.ActiveRule.Builder batch2 = BatchReport.ActiveRule.newBuilder()
      .setRuleRepository("java").setRuleKey("S002").setSeverity(Constants.Severity.MAJOR);
    batchReportReader.putActiveRules(Arrays.asList(batch1.build(), batch2.build()));

    underTest.execute();

    assertThat(activeRulesHolder.getAll()).hasSize(2);
    
    Optional<ActiveRule> ar1 = activeRulesHolder.get(RuleKey.of("java", "S001"));
    assertThat(ar1.get().getSeverity()).isEqualTo(Severity.BLOCKER);
    assertThat(ar1.get().getParams()).containsExactly(MapEntry.entry("p1", "v1"));
    
    Optional<ActiveRule> ar2 = activeRulesHolder.get(RuleKey.of("java", "S002"));
    assertThat(ar2.get().getSeverity()).isEqualTo(Severity.MAJOR);
    assertThat(ar2.get().getParams()).isEmpty();

  }
}
