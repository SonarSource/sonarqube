/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.qualityprofile.PrioritizedRulesHolderImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoadPrioritizedRulesStepTest {

  AnalysisMetadataHolder analysisMetadataHolder = mock();
  DbClient dbClient = mock();
  PrioritizedRulesHolderImpl prioritizedRulesHolder = new PrioritizedRulesHolderImpl();
  LoadPrioritizedRulesStep underTest = new LoadPrioritizedRulesStep(analysisMetadataHolder, prioritizedRulesHolder, dbClient);

  @Test
  void execute_whenNoPrioritizedRules_shouldHaveEmptyHolder() {
    when(dbClient.activeRuleDao()).thenReturn(mock());
    when(dbClient.activeRuleDao().selectPrioritizedRules(any(), any())).thenReturn(new HashSet<>());
    underTest.execute(mock());
    assertThat(prioritizedRulesHolder.getPrioritizedRules()).isEmpty();
  }

  @Test
  void execute_whenPrioritizedRules_shouldHaveNonEmptyHolder() {
    when(dbClient.activeRuleDao()).thenReturn(mock());
    when(dbClient.activeRuleDao().selectPrioritizedRules(any(), any())).thenReturn(Set.of(RuleKey.of("repositoryKey", "ruleKey")));
    underTest.execute(mock());
    assertThat(prioritizedRulesHolder.getPrioritizedRules()).isNotEmpty();
  }

  @Test
  void execute_whenDBError_shouldThrow() {
    when(dbClient.activeRuleDao()).thenReturn(mock());
    when(dbClient.activeRuleDao().selectPrioritizedRules(any(), any())).thenThrow(new RuntimeException());

    ComputationStep.Context context = mock();
    assertThatThrownBy(() -> underTest.execute(context)).isInstanceOf(RuntimeException.class);
  }

  @Test
  void getDescription_shouldReturnValue() {
    assertThat(underTest.getDescription()).isEqualTo("Load prioritized rules");
  }
}
