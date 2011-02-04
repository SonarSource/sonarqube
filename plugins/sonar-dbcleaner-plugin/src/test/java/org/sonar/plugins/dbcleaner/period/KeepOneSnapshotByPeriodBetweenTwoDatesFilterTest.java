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
package org.sonar.plugins.dbcleaner.period;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.plugins.dbcleaner.Utils;

import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class KeepOneSnapshotByPeriodBetweenTwoDatesFilterTest {

  @Test
  public void testFilter() {
    List<Snapshot> snapshots = Lists.newLinkedList();
    snapshots.add(Utils.createSnapshot(1, Utils.week(-7, 1)));
    snapshots.add(Utils.createSnapshot(2, Utils.week(-7, 2)));
    snapshots.add(Utils.createSnapshot(3, Utils.week(-7, 3)));
    snapshots.add(Utils.createSnapshot(4, Utils.week(-6, 3)));
    snapshots.add(Utils.createSnapshot(5, Utils.week(-6, 4)));

    assertThat(new KeepOneSnapshotByPeriodBetweenTwoDatesFilter(GregorianCalendar.WEEK_OF_YEAR, Utils.week(-3, 1), Utils.week(-9, 1)).filter(snapshots), is(2));
    assertThat(snapshots.size(), is(3));
    assertThat(snapshots.get(0).getId(), is(2));
    assertThat(snapshots.get(1).getId(), is(3));
    assertThat(snapshots.get(2).getId(), is(5));
  }
}
