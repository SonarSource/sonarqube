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
package org.sonar.plugins.core.timemachine;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.utils.DateUtils;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TimeMachineConfigurationTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldSkipTendencies() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.SKIP_TENDENCIES_PROPERTY, true);
    assertThat(new TimeMachineConfiguration(conf).skipTendencies(), is(true));
  }

  @Test
  public void shouldNotSkipTendenciesByDefault() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    assertThat(new TimeMachineConfiguration(conf).skipTendencies(), is(false));
  }

  @Test
  public void shouldInitPastSnapshots() throws ParseException {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    PastSnapshotFinder pastSnapshotFinder = mock(PastSnapshotFinder.class);
    when(pastSnapshotFinder.find(null, conf, 1)).thenReturn(new PastSnapshot("days", new Date(), newSnapshot("2010-10-15")));
    when(pastSnapshotFinder.find(null, conf, 3)).thenReturn(new PastSnapshot("days", new Date(), newSnapshot("2010-10-13")));

    TimeMachineConfiguration timeMachineConfiguration = new TimeMachineConfiguration(conf, pastSnapshotFinder, null);

    verify(pastSnapshotFinder).find(null, conf, 1);
    verify(pastSnapshotFinder).find(null, conf, 2);
    verify(pastSnapshotFinder).find(null, conf, 3);

    assertThat(timeMachineConfiguration.getProjectPastSnapshots().size(), is(2));
  }

  private Snapshot newSnapshot(String date) throws ParseException {
    return new Snapshot().setCreatedAt(DateUtils.parseDate(date));
  }
}
