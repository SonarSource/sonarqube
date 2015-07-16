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

import com.google.common.base.Function;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.qualityprofile.ActiveRule;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolderImpl;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.core.rule.RuleKeyFunctions.stringToRuleKey;

public class FeedActiveRulesStep implements ComputationStep {

  private final BatchReportReader batchReportReader;
  private final ActiveRulesHolderImpl activeRulesHolder;

  public FeedActiveRulesStep(BatchReportReader batchReportReader, ActiveRulesHolderImpl activeRulesHolder) {
    this.batchReportReader = batchReportReader;
    this.activeRulesHolder = activeRulesHolder;
  }

  @Override
  public void execute() {
    Collection<String> keys = batchReportReader.readMetadata().getActiveRuleKeyList();
    activeRulesHolder.set(from(keys).transform(stringToRuleKey()).transform(ToActiveRule.INSTANCE).toList());
  }

  @Override
  public String getDescription() {
    return getClass().getSimpleName();
  }

  private enum ToActiveRule implements Function<RuleKey, ActiveRule> {
    INSTANCE;
    @Override
    public ActiveRule apply(@Nonnull RuleKey ruleKey) {
      // FIXME load severity
      return new ActiveRule(ruleKey, Severity.MAJOR);
    }
  }
}
