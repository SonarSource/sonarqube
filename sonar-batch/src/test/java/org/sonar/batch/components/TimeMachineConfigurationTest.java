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

public class TimeMachineConfigurationTest {

  private PeriodsDefinition periodsDefinition;
  private PastSnapshotFinderByDate pastSnapshotFinderByDate;

//  @Before
//  public void before() {
//    periodsDefinition = mock(PeriodsDefinition.class);
//    pastSnapshotFinderByDate = mock(PastSnapshotFinderByDate.class);
//  }
//
//  @Test
//  public void should_init_past_snapshots() {
//    Integer projectId = 1;
//    Date date = new Date();
//
//    PastSnapshot projectPastSnapshot = new PastSnapshot("mode", projectId);
//
//    when(periodsDefinition.projectPastSnapshots()).thenReturn(newArrayList(new PastSnapshot("mode", projectId)));
//    when(pastSnapshotFinderByDate.findByDate(projectId, date)).thenReturn(newArrayList(new PastSnapshot("mode", new Date())));
//
//    TimeMachineConfiguration timeMachineConfiguration = new TimeMachineConfiguration((Project) new Project("my:project").setId(projectId), periodsDefinition, pastSnapshotFinderByDate);
//    assertThat(timeMachineConfiguration.periods()).hasSize(1);
//  }
//
//  @Test
//  public void should_not_init_past_snapshots_if_first_analysis() {
////    new TimeMachineConfiguration(new Project("new:project"), settings, pastSnapshotFinder);
////
////    verifyZeroInteractions(pastSnapshotFinder);
//  }

}
