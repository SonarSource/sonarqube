/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.project.Project;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.Metric.ValueType.INT;

public class ProjectNclocComputationStepIT {
  @Rule
  public DbTester db = DbTester.create();
  private final DbClient dbClient = db.getDbClient();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private final ProjectNclocComputationStep step = new ProjectNclocComputationStep(analysisMetadataHolder, dbClient);

  @Test
  public void test_computing_branch_ncloc() {
    MetricDto ncloc = db.measures().insertMetric(m -> m.setKey("ncloc").setValueType(INT.toString()));
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    db.measures().insertMeasure(branch1, m -> m.addValue(ncloc.getKey(), 200d));
    BranchDto branch2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.BRANCH));
    db.measures().insertMeasure(branch2, m -> m.addValue(ncloc.getKey(), 10d));
    analysisMetadataHolder.setProject(new Project(project.getUuid(), project.getKey(), project.getName(), project.getDescription(), emptyList()));
    step.execute(TestComputationStepContext.TestStatistics::new);

    assertThat(dbClient.projectDao().getNclocSum(db.getSession())).isEqualTo(200L);
  }

  @Test
  public void description_is_not_missing() {
    assertThat(step.getDescription()).isNotBlank();
  }
}
