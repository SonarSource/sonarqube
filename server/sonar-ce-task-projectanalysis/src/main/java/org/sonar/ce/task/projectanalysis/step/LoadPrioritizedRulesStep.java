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

import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.qualityprofile.PrioritizedRulesHolderImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.qualityprofile.QualityProfile;

/**
 * Populates the {@link org.sonar.ce.task.projectanalysis.qualityprofile.PrioritizedRulesHolder}
 */
public class LoadPrioritizedRulesStep implements ComputationStep {

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final PrioritizedRulesHolderImpl prioritizedRulesHolder;
  private final DbClient dbClient;

  public LoadPrioritizedRulesStep(AnalysisMetadataHolder analysisMetadataHolder, PrioritizedRulesHolderImpl prioritizedRulesHolder,
    DbClient dbClient) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.prioritizedRulesHolder = prioritizedRulesHolder;
    this.dbClient = dbClient;
  }

  @Override
  public String getDescription() {
    return "Load prioritized rules";
  }

  @Override
  public void execute(Context context) {

    Set<String> qpKeys =
      analysisMetadataHolder.getQProfilesByLanguage().values().stream().map(QualityProfile::getQpKey).collect(Collectors.toSet());

    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<RuleKey> prioritizedRules = dbClient.activeRuleDao().selectPrioritizedRules(dbSession, qpKeys);
      prioritizedRulesHolder.setPrioritizedRules(prioritizedRules);
    }
  }
}
