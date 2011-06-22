/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.batch.components;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TimeMachineConfigurationTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldSkipTendencies() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.SKIP_TENDENCIES_PROPERTY, true);
    assertThat(new TimeMachineConfiguration(getSession(), new Project("my:project"), conf, mock(PastSnapshotFinder.class)).skipTendencies(), is(true));
  }

  @Test
  public void shouldNotSkipTendenciesByDefault() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    assertThat(new TimeMachineConfiguration(getSession(), new Project("my:project"), conf, mock(PastSnapshotFinder.class)).skipTendencies(), is(false));
  }

  @Test
  public void shouldInitPastSnapshots() {
    setupData("shared");
    PropertiesConfiguration conf = new PropertiesConfiguration();
    PastSnapshotFinder pastSnapshotFinder = mock(PastSnapshotFinder.class);

    new TimeMachineConfiguration(getSession(), new Project("my:project"), conf, pastSnapshotFinder);

    verify(pastSnapshotFinder).find(argThat(new BaseMatcher<Snapshot>() {
      public boolean matches(Object o) {
        return ((Snapshot) o).getResourceId() == 2 /* see database in shared.xml */;
      }

      public void describeTo(Description description) {
      }
    }), eq(conf), eq(1));
  }

  @Test
  public void shouldNotInitPastSnapshotsIfFirstAnalysis() {
    setupData("shared");
    PropertiesConfiguration conf = new PropertiesConfiguration();
    PastSnapshotFinder pastSnapshotFinder = mock(PastSnapshotFinder.class);

    new TimeMachineConfiguration(getSession(), new Project("new:project"), conf, pastSnapshotFinder);

    verifyZeroInteractions(pastSnapshotFinder);
  }
}
