/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dbcleaner.runner;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.plugins.dbcleaner.api.Purge;
import org.sonar.plugins.dbcleaner.api.PurgeContext;

import javax.persistence.PersistenceException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class DeprecatedPurgePostJobTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldExecutePurges() {
    setupData("shared");
    final int currentSID = 400;
    final int previousSID = 300;
    Snapshot snapshot = getSession().getSingleResult(Snapshot.class, "id", currentSID);

    Purge purge1 = mock(Purge.class);
    Purge purge2 = mock(Purge.class);
    Purge[] purges = new Purge[]{purge1, purge2};

    new DeprecatedPurgePostJob(getSession(), new Project("key"), snapshot, purges).purge();

    verify(purge1).purge(argThat(new BaseMatcher<PurgeContext>() {
      public boolean matches(Object o) {
        PurgeContext context = (PurgeContext) o;
        return context.getSnapshotId() == currentSID && context.getPreviousSnapshotId() == previousSID;
      }

      public void describeTo(Description description) {
      }
    }));
  }

  @Test
  public void shouldExecutePurgesEvenIfSingleAnalysis() {
    setupData("shared");
    final int currentSID = 1000;
    Snapshot snapshot = getSession().getSingleResult(Snapshot.class, "id", currentSID);

    Purge purge1 = mock(Purge.class);
    Purge purge2 = mock(Purge.class);
    Purge[] purges = new Purge[]{purge1, purge2};

    new DeprecatedPurgePostJob(getSession(), new Project("key"), snapshot, purges).purge();

    verify(purge1).purge(argThat(new BaseMatcher<PurgeContext>() {
      public boolean matches(Object o) {
        PurgeContext context = (PurgeContext) o;
        return context.getSnapshotId() == currentSID && context.getPreviousSnapshotId() == null;
      }

      public void describeTo(Description description) {
      }
    }));
  }

  @Test
  public void shouldExecuteOnlyOnRootProjects() {
    Project project = mock(Project.class);
    when(project.isRoot()).thenReturn(true);
    assertTrue(DeprecatedPurgePostJob.shouldExecuteOn(project));

    when(project.isRoot()).thenReturn(false);
    assertFalse(DeprecatedPurgePostJob.shouldExecuteOn(project));
  }

  /**
   * See https://jira.codehaus.org/browse/SONAR-2961
   * Temporarily ignore MySQL deadlocks
   */
  @Test
  public void shouldIgnoreDeadlocks() {
    Purge purge = mock(Purge.class);
    doThrow(new PersistenceException()).when(purge).purge((PurgeContext) anyObject());

    DeprecatedPurgePostJob runner = new DeprecatedPurgePostJob(getSession(), new Project(""), new Snapshot(), new Purge[]{purge});
    runner.purge();// must not raise any exceptions

    verify(purge).purge((PurgeContext) anyObject());
  }
}
