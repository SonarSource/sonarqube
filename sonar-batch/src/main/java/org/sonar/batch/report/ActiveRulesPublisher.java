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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;

public class ActiveRulesPublisher implements ReportPublisherStep {

  private final ActiveRules activeRules;

  public ActiveRulesPublisher(ActiveRules activeRules) {
    this.activeRules = activeRules;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    Iterable<BatchReport.ActiveRule> activeRuleMessages = FluentIterable.from(activeRules.findAll()).transform(new ToMessage());
    writer.writeActiveRules(activeRuleMessages);
  }

  private class ToMessage implements Function<ActiveRule, BatchReport.ActiveRule> {
    private final BatchReport.ActiveRule.Builder builder = BatchReport.ActiveRule.newBuilder();

    @Override
    public BatchReport.ActiveRule apply(@Nonnull ActiveRule input) {
      builder.clear();
      builder.setRuleRepository(input.ruleKey().repository());
      builder.setRuleKey(input.ruleKey().rule());
      builder.setSeverity(Constants.Severity.valueOf(input.severity()));
      for (Map.Entry<String, String> entry : input.params().entrySet()) {
        builder.addParamBuilder().setKey(entry.getKey()).setValue(entry.getValue()).build();

      }
      return builder.build();
    }
  }
}
