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

import org.sonar.batch.components.PastSnapshot;

import org.sonar.batch.deprecated.components.PastSnapshotFinderByDate;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PastSnapshotFinderByDateTest extends AbstractDbUnitTestCase {
  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @Test
  public void shouldFindDate() throws ParseException {
    setupData("shared");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByDate finder = new PastSnapshotFinderByDate(getSession());

    Date date = DATE_FORMAT.parse("2008-11-22");

    PastSnapshot pastSnapshot = finder.findByDate(projectSnapshot, date);
    assertThat(pastSnapshot.getProjectSnapshotId(), is(1006));
  }

  @Test
  public void shouldFindNearestLaterDate() throws ParseException {
    setupData("shared");

    Snapshot projectSnapshot = getSession().getSingleResult(Snapshot.class, "id", 1010);
    PastSnapshotFinderByDate finder = new PastSnapshotFinderByDate(getSession());

    Date date = DATE_FORMAT.parse("2008-11-24");
   
    PastSnapshot pastSnapshot = finder.findByDate(projectSnapshot, date);
    assertThat(pastSnapshot.getProjectSnapshotId(), is(1009));
  }
}
