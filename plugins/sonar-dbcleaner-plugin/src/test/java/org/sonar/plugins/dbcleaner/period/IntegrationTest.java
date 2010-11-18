/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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
package org.sonar.plugins.dbcleaner.period;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.GregorianCalendar;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.plugins.dbcleaner.api.PurgeContext;

public class IntegrationTest extends AbstractDbUnitTestCase {

  PeriodCleaner purge;

  @Before
  public void init() {

    Project project = new Project("myproject");
    project.setConfiguration(new PropertiesConfiguration());
    purge = new PeriodCleaner(getSession(), project);
    GregorianCalendar calendar = new GregorianCalendar(2010, 10, 1);
    purge.dateToStartKeepingOneSnapshotByWeek = calendar.getTime();
    calendar.set(2010, 7, 1);
    purge.dateToStartKeepingOneSnapshotByMonth = calendar.getTime();
    calendar.set(2010, 2, 1);
    purge.dateToStartDeletingAllSnapshots = calendar.getTime();
  }

  @Test
  public void dbCleanerITTest() {
    setupData("dbContent");
    PurgeContext context = mock(PurgeContext.class);
    when(context.getSnapshotId()).thenReturn(1010);
    purge.purge(context);
    checkTables("dbContent", "snapshots");
    
    //After a first run, no more snapshot should be deleted
    setupData("dbContent-result");
    context = mock(PurgeContext.class);
    when(context.getSnapshotId()).thenReturn(1010);
    purge.purge(context);
    checkTables("dbContent");
  }
}
