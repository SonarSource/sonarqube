/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.report;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import javax.annotation.Nonnull;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

public class ActiveRulesPublisher implements ReportPublisherStep {

  private final ActiveRules activeRules;

  public ActiveRulesPublisher(ActiveRules activeRules) {
    this.activeRules = activeRules;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    Iterable<ScannerReport.ActiveRule> activeRuleMessages = FluentIterable.from(activeRules.findAll()).transform(new ToMessage());
    writer.writeActiveRules(activeRuleMessages);
  }

  private static class ToMessage implements Function<ActiveRule, ScannerReport.ActiveRule> {
    private final ScannerReport.ActiveRule.Builder builder = ScannerReport.ActiveRule.newBuilder();

    @Override
    public ScannerReport.ActiveRule apply(@Nonnull ActiveRule input) {
      builder.clear();
      builder.setRuleRepository(input.ruleKey().repository());
      builder.setRuleKey(input.ruleKey().rule());
      builder.setSeverity(Constants.Severity.valueOf(input.severity()));
      builder.getMutableParamsByKey().putAll(input.params());
      return builder.build();
    }
  }
}
