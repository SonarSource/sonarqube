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
package org.sonar.batch.components;

import org.sonar.batch.components.TimeMachineConfiguration;

import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.deprecated.components.PeriodsDefinition;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Date;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeMachineConfigurationTest extends AbstractDbUnitTestCase {

  private PeriodsDefinition periodsDefinition;

  @Before
  public void before() {
    setupData("shared");
    periodsDefinition = mock(PeriodsDefinition.class);
  }

  @Test
  public void get_project_past_snapshot() {
    Snapshot projectSnapshot = new Snapshot();
    projectSnapshot.setId(1010);
    projectSnapshot.setResourceId(1);
    PastSnapshot projectPastSnapshot = new PastSnapshot("mode", new Date(), projectSnapshot);

    when(periodsDefinition.getRootProjectPastSnapshots()).thenReturn(newArrayList(projectPastSnapshot));

    TimeMachineConfiguration timeMachineConfiguration = new TimeMachineConfiguration(getSession(), periodsDefinition);
    assertThat(timeMachineConfiguration.periods()).hasSize(1);
    assertThat(timeMachineConfiguration.periods().get(0).getDate()).isNotNull();
    assertThat(timeMachineConfiguration.getProjectPastSnapshots()).hasSize(1);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots().get(0).getProjectSnapshot().getId()).isEqualTo(1010);
  }

  @Test
  public void get_module_past_snapshot() {
    Snapshot projectSnapshot = new Snapshot();
    projectSnapshot.setId(1010);
    projectSnapshot.setResourceId(2);
    PastSnapshot projectPastSnapshot = new PastSnapshot("mode", new Date(), projectSnapshot);

    when(periodsDefinition.getRootProjectPastSnapshots()).thenReturn(newArrayList(projectPastSnapshot));

    TimeMachineConfiguration timeMachineConfiguration = new TimeMachineConfiguration(getSession(), periodsDefinition);
    assertThat(timeMachineConfiguration.periods()).hasSize(1);
    assertThat(timeMachineConfiguration.periods().get(0).getDate()).isNotNull();
    assertThat(timeMachineConfiguration.getProjectPastSnapshots()).hasSize(1);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots().get(0).getProjectSnapshot().getId()).isEqualTo(1010);
  }

  @Test
  public void complete_past_snapshot_from_project_past_snapshot() {
    Snapshot projectSnapshot = new Snapshot();
    projectSnapshot.setId(1010);

    PastSnapshot projectPastSnapshot = new PastSnapshot("mode", new Date(), projectSnapshot);
    projectPastSnapshot.setIndex(1);
    projectPastSnapshot.setMode("mode");
    projectPastSnapshot.setModeParameter("modeParam");

    when(periodsDefinition.getRootProjectPastSnapshots()).thenReturn(newArrayList(projectPastSnapshot));

    TimeMachineConfiguration timeMachineConfiguration = new TimeMachineConfiguration(getSession(), periodsDefinition);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots()).hasSize(1);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots().get(0).getProjectSnapshot().getId()).isEqualTo(1010);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots().get(0).getIndex()).isEqualTo(1);
    assertThat(timeMachineConfiguration.getProjectPastSnapshots().get(0).getMode()).isEqualTo("mode");
    assertThat(timeMachineConfiguration.getProjectPastSnapshots().get(0).getModeParameter()).isEqualTo("modeParam");
  }

  @Test
  public void get_no_date_on_new_project() {
    Snapshot projectSnapshot = new Snapshot();
    projectSnapshot.setId(1010);

    PastSnapshot projectPastSnapshot = new PastSnapshot("mode", new Date(), projectSnapshot);

    when(periodsDefinition.getRootProjectPastSnapshots()).thenReturn(newArrayList(projectPastSnapshot));

    TimeMachineConfiguration timeMachineConfiguration = new TimeMachineConfiguration(getSession(), periodsDefinition);
    assertThat(timeMachineConfiguration.periods()).hasSize(1);
    assertThat(timeMachineConfiguration.periods().get(0).getDate()).isNull();
  }

}
