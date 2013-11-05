/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.components;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.resources.Project;

import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeMachineConfigurationTest {

  private PeriodsDefinition periodsDefinition;

  @Before
  public void before() {
    periodsDefinition = mock(PeriodsDefinition.class);
  }

  @Test
  @Ignore
  public void get_module_past_snapshot() {
    Integer projectId = 1;
    Date targetDate = new Date();

    PastSnapshot projectPastSnapshot = new PastSnapshot("mode", targetDate);
    PastSnapshot modulePastSnapshot = new PastSnapshot("mode", targetDate);

    when(periodsDefinition.getRootProjectPastSnapshots()).thenReturn(newArrayList(projectPastSnapshot));
    //when(pastSnapshotFinderByDate.findByDate(anyInt(), any(Date.class))).thenReturn(modulePastSnapshot);

    TimeMachineConfiguration timeMachineConfiguration = new TimeMachineConfiguration(null, (Project) new Project("my:project").setId(projectId), periodsDefinition);
    assertThat(timeMachineConfiguration.periods()).hasSize(1);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots()).hasSize(1);
  }

  @Test
  @Ignore
  public void complete_module_past_snapshot_from_project_past_snapshot() {
    Integer projectId = 1;
    Date targetDate = new Date();

    PastSnapshot projectPastSnapshot = new PastSnapshot("mode", targetDate);
    projectPastSnapshot.setIndex(1);
    projectPastSnapshot.setMode("mode");
    projectPastSnapshot.setModeParameter("modeParam");

    PastSnapshot modulePastSnapshot = new PastSnapshot("mode", targetDate);

    when(periodsDefinition.getRootProjectPastSnapshots()).thenReturn(newArrayList(projectPastSnapshot));
    //when(pastSnapshotFinderByDate.findByDate(anyInt(), any(Date.class))).thenReturn(modulePastSnapshot);

    TimeMachineConfiguration timeMachineConfiguration = new TimeMachineConfiguration(null, (Project) new Project("my:project").setId(projectId), periodsDefinition);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots()).hasSize(1);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots().get(0).getIndex()).isEqualTo(1);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots().get(0).getMode()).isEqualTo("mode");
    assertThat(timeMachineConfiguration.getProjectPastSnapshots().get(0).getModeParameter()).isEqualTo("modeParam");
  }

  @Test
  @Ignore
  public void get_no_module_past_snapshot() {
    Integer projectId = 1;
    Date targetDate = new Date();

    PastSnapshot projectPastSnapshot = new PastSnapshot("mode", targetDate);

    when(periodsDefinition.getRootProjectPastSnapshots()).thenReturn(newArrayList(projectPastSnapshot));
    //when(pastSnapshotFinderByDate.findByDate(eq(projectId), eq(targetDate))).thenReturn(null);

    TimeMachineConfiguration timeMachineConfiguration = new TimeMachineConfiguration(null, (Project) new Project("my:project").setId(projectId), periodsDefinition);
    assertThat(timeMachineConfiguration.periods()).isEmpty();
  }

}
