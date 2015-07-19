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
package org.sonar.batch.report;

import java.io.File;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.util.CloseableIterator;

import static org.assertj.core.api.Assertions.assertThat;

public class ActiveRulesPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void write() throws Exception {
    File outputDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(outputDir);

    NewActiveRule ar = new ActiveRulesBuilder().create(RuleKey.of("java", "S001")).setSeverity("BLOCKER").setParam("p1", "v1");
    ActiveRules activeRules = new DefaultActiveRules(Arrays.asList(ar));

    ActiveRulesPublisher underTest = new ActiveRulesPublisher(activeRules);
    underTest.publish(writer);

    BatchReportReader reader = new BatchReportReader(outputDir);
    try (CloseableIterator<BatchReport.ActiveRule> readIt = reader.readActiveRules()) {
      BatchReport.ActiveRule reportAr = readIt.next();
      assertThat(reportAr.getRuleRepository()).isEqualTo("java");
      assertThat(reportAr.getRuleKey()).isEqualTo("S001");
      assertThat(reportAr.getSeverity()).isEqualTo(Constants.Severity.BLOCKER);
      assertThat(reportAr.getParamCount()).isEqualTo(1);
      assertThat(reportAr.getParam(0).getKey()).isEqualTo("p1");
      assertThat(reportAr.getParam(0).getValue()).isEqualTo("v1");

      assertThat(readIt.hasNext()).isFalse();
    }
  }
}
