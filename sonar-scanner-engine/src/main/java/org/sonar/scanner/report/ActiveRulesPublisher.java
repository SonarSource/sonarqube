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
package org.sonar.scanner.report;

import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRule;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import static java.util.stream.Collectors.toList;

public class ActiveRulesPublisher implements ReportPublisherStep {

  private final ActiveRules activeRules;

  public ActiveRulesPublisher(ActiveRules activeRules) {
    this.activeRules = activeRules;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    final ScannerReport.ActiveRule.Builder builder = ScannerReport.ActiveRule.newBuilder();
    writer.writeActiveRules(activeRules.findAll().stream()
      .map(DefaultActiveRule.class::cast)
      .map(input -> {
        builder.clear();
        builder.setRuleRepository(input.ruleKey().repository());
        builder.setRuleKey(input.ruleKey().rule());
        builder.setSeverity(Constants.Severity.valueOf(input.severity()));
        builder.setCreatedAt(input.createdAt());
        builder.setUpdatedAt(input.updatedAt());
        builder.setQProfileKey(input.qpKey());
        builder.getMutableParamsByKey().putAll(input.params());
        return builder.build();
      }).collect(toList()));
  }

}
