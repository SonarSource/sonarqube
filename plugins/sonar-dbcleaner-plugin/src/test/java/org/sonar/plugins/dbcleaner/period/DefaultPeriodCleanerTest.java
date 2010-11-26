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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Project;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Date;
import java.util.GregorianCalendar;

public class DefaultPeriodCleanerTest extends AbstractDbUnitTestCase {

  DefaultPeriodCleaner cleaner;

  @Before
  public void init() {
    cleaner = new DefaultPeriodCleaner(getSession());
  }

  @Test
  public void integrationTests() {
    setupData("dbContent");

    Project project = new Project("myproject");
    project.setConfiguration(new PropertiesConfiguration());

    GregorianCalendar calendar = new GregorianCalendar(2010, 10, 1);
    Date dateToStartKeepingOneSnapshotByWeek = calendar.getTime();
    calendar.set(2010, 7, 1);
    Date dateToStartKeepingOneSnapshotByMonth = calendar.getTime();
    calendar.set(2010, 2, 1);
    Date dateToStartDeletingAllSnapshots = calendar.getTime();
    Periods periods = new Periods(dateToStartKeepingOneSnapshotByWeek, dateToStartKeepingOneSnapshotByMonth, dateToStartDeletingAllSnapshots);

    cleaner.purge(project, 1010, periods);
    checkTables("dbContent", "snapshots");

    //After a first run, no more snapshot should be deleted
    setupData("dbContent-result");
    cleaner.purge(project, 1010, periods);
    checkTables("dbContent");
  }
}
