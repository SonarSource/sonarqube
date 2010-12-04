/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class PastSnapshotFinderTest {

  private PastSnapshotFinderByDays finderByDays;
  private PastSnapshotFinderByDate finderByDate;
  private PastSnapshotFinderByVersion finderByVersion;
  private PastSnapshotFinderByLastAnalysis finderByLastAnalysis;
  private PastSnapshotFinder finder;

  @Before
  public void initFinders() {
    finderByDays = mock(PastSnapshotFinderByDays.class);
    finderByDate = mock(PastSnapshotFinderByDate.class);
    finderByVersion = mock(PastSnapshotFinderByVersion.class);
    finderByLastAnalysis = mock(PastSnapshotFinderByLastAnalysis.class);
    finder = new PastSnapshotFinder(finderByDays, finderByVersion, finderByDate, finderByLastAnalysis);
  }

  @Test
  public void shouldFindByNumberOfDays() {
    when(finderByDays.findInDays(30)).thenReturn(new Snapshot());

    PastSnapshot variationSnapshot = finder.find(1, "30");

    verify(finderByDays).findInDays(30);
    assertNotNull(variationSnapshot);
    assertThat(variationSnapshot.getIndex(), is(1));
    assertThat(variationSnapshot.getMode(), is("days"));
    assertThat(variationSnapshot.getModeParameter(), is("30"));
  }

  @Test
  public void shouldNotFindByNumberOfDays() {
    PastSnapshot variationSnapshot = finder.find(1, "30");

    verify(finderByDays).findInDays(30);
    assertNull(variationSnapshot);
  }

  @Test
  public void shouldFindByDate() throws ParseException {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date date = format.parse("2010-05-18");
    when(finderByDate.findByDate(date)).thenReturn(new Snapshot());

    PastSnapshot variationSnapshot = finder.find(2, "2010-05-18");

    verify(finderByDate).findByDate(argThat(new BaseMatcher<Date>() {
      public boolean matches(Object o) {
        return o.equals(date);
      }

      public void describeTo(Description description) {

      }
    }));
    assertThat(variationSnapshot.getIndex(), is(2));
    assertThat(variationSnapshot.getMode(), is("date"));
    assertThat(variationSnapshot.getModeParameter(), is("2010-05-18"));
  }

  @Test
  public void shouldNotFindByDate() throws ParseException {
    when(finderByDate.findByDate((Date) anyObject())).thenReturn(null);

    PastSnapshot variationSnapshot = finder.find(2, "2010-05-18");

    verify(finderByDate).findByDate((Date) anyObject());
    assertNull(variationSnapshot);
  }

  @Test
  public void shouldFindLastAnalysis() throws ParseException {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date date = format.parse("2010-05-18");
    Snapshot snapshot = new Snapshot();
    snapshot.setCreatedAt(date);
    when(finderByLastAnalysis.findLastAnalysis()).thenReturn(snapshot);

    PastSnapshot variationSnapshot = finder.find(2, "last_analysis");

    verify(finderByLastAnalysis).findLastAnalysis();
    assertThat(variationSnapshot.getIndex(), is(2));
    assertThat(variationSnapshot.getMode(), is("last_analysis"));
    assertThat(variationSnapshot.getModeParameter(), is("2010-05-18"));
  }

  @Test
  public void shouldNotFindLastAnalysis() {
    when(finderByLastAnalysis.findLastAnalysis()).thenReturn(null);

    PastSnapshot variationSnapshot = finder.find(2, "last_analysis");

    verify(finderByLastAnalysis).findLastAnalysis();

    assertNull(variationSnapshot);
  }

  @Test
  public void shouldFindByVersion() {
    when(finderByVersion.findVersion("1.2")).thenReturn(new Snapshot());

    PastSnapshot variationSnapshot = finder.find(2, "1.2");

    verify(finderByVersion).findVersion("1.2");
    assertThat(variationSnapshot.getIndex(), is(2));
    assertThat(variationSnapshot.getMode(), is("version"));
    assertThat(variationSnapshot.getModeParameter(), is("1.2"));
  }

  @Test
  public void shouldNotFindVersion() {
    when(finderByVersion.findVersion("1.2")).thenReturn(null);

    PastSnapshot variationSnapshot = finder.find(2, "1.2");

    verify(finderByVersion).findVersion("1.2");
    assertNull(variationSnapshot);
  }

  @Test
  public void shouldNotFailIfUnknownFormat() {
    when(finderByLastAnalysis.findLastAnalysis()).thenReturn(new Snapshot()); // should not be called
    assertNull(finder.find(2, "foooo"));
  }
}
