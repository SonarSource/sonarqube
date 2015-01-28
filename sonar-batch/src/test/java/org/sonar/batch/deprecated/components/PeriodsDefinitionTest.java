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

package org.sonar.batch.deprecated.components;

import org.sonar.batch.components.PastSnapshotFinder;

import org.sonar.batch.deprecated.components.PeriodsDefinition;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.batch.ProjectTree;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PeriodsDefinitionTest extends AbstractDbUnitTestCase {

  private Settings settings;
  private PastSnapshotFinder pastSnapshotFinder;

  @Before
  public void before() {
    setupData("shared");
    settings = new Settings();
    pastSnapshotFinder = mock(PastSnapshotFinder.class);
  }

  @Test
  public void should_init_past_snapshots() {
    ProjectTree projectTree = mock(ProjectTree.class);
    when(projectTree.getRootProject()).thenReturn(new Project("my:project"));
    new PeriodsDefinition(getSession(), projectTree, settings, pastSnapshotFinder);

    verify(pastSnapshotFinder).find(argThat(new ArgumentMatcher<Snapshot>() {
      @Override
      public boolean matches(Object o) {
        return ((Snapshot) o).getResourceId() == 2 /* see database in shared.xml */;
      }
    }), anyString(), eq(settings), eq(1));
  }

  @Test
  public void should_not_init_past_snapshots_if_first_analysis() {
    ProjectTree projectTree = mock(ProjectTree.class);
    when(projectTree.getRootProject()).thenReturn(new Project("new:project"));

    new PeriodsDefinition(getSession(), projectTree, settings, pastSnapshotFinder);

    verifyZeroInteractions(pastSnapshotFinder);
  }
}
