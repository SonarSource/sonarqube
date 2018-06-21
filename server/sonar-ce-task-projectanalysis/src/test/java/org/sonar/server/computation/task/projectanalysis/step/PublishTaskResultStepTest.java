/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.taskprocessor.MutableTaskResultHolder;
import org.sonar.server.computation.taskprocessor.MutableTaskResultHolderImpl;

import static org.assertj.core.api.Assertions.assertThat;

public class PublishTaskResultStepTest {

  private static final String AN_ANALYSIS_UUID = "U1";

  private MutableTaskResultHolder taskResultHolder = new MutableTaskResultHolderImpl();

  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  private PublishTaskResultStep underTest = new PublishTaskResultStep(taskResultHolder, analysisMetadataHolder);

  @Test
  public void verify_getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Publish task results");
  }

  @Test
  public void execute_populate_TaskResultHolder_with_a_TaskResult_with_snapshot_id_of_the_root_taken_from_DbIdsRepository() {
    analysisMetadataHolder.setUuid(AN_ANALYSIS_UUID);

    underTest.execute();

    assertThat(taskResultHolder.getResult().getAnalysisUuid().get()).isEqualTo(AN_ANALYSIS_UUID);
  }
}
