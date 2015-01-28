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
package org.sonar.batch.components;

import org.sonar.batch.components.PastSnapshotFinder;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByDate;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByDays;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByPreviousAnalysis;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByPreviousVersion;
import org.sonar.batch.deprecated.components.PastSnapshotFinderByVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.utils.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PastSnapshotFinderTest {

  @Mock
  private PastSnapshotFinderByDays finderByDays;
  @Mock
  private PastSnapshotFinderByDate finderByDate;
  @Mock
  private PastSnapshotFinderByVersion finderByVersion;
  @Mock
  private PastSnapshotFinderByPreviousAnalysis finderByPreviousAnalysis;
  @Mock
  private PastSnapshotFinderByPreviousVersion finderByPreviousVersion;

  private PastSnapshotFinder finder;

  @Before
  public void initFinders() {
    MockitoAnnotations.initMocks(this);

    finder = new PastSnapshotFinder(finderByDays, finderByVersion, finderByDate, finderByPreviousAnalysis, finderByPreviousVersion);
  }

  @Test
  public void should_find() {
    Settings settings = new Settings().setProperty("sonar.timemachine.period5", "1.2");

    when(finderByVersion.findByVersion(null, "1.2")).thenReturn(new PastSnapshot("version", new Date(), new Snapshot()));

    PastSnapshot variationSnapshot = finder.find(null, null, settings, 5);

    verify(finderByVersion).findByVersion(null, "1.2");
    assertThat(variationSnapshot.getIndex(), is(5));
    assertThat(variationSnapshot.getMode(), is("version"));
    assertThat(variationSnapshot.getProjectSnapshot(), not(nullValue()));
  }

  @Test
  public void should_find_with_qualifier_suffix() {
    Settings settings = new Settings().setProperty("sonar.timemachine.period5.TRK", "1.2");

    when(finderByVersion.findByVersion(null, "1.2")).thenReturn(new PastSnapshot("version", new Date(), new Snapshot()));

    PastSnapshot variationSnapshot = finder.find(null, "TRK", settings, 5);

    verify(finderByVersion).findByVersion(null, "1.2");
    assertThat(variationSnapshot.getIndex(), is(5));
    assertThat(variationSnapshot.getMode(), is("version"));
    assertThat(variationSnapshot.getProjectSnapshot(), not(nullValue()));
  }

  @Test
  public void should_find_by_number_of_days() {
    when(finderByDays.findFromDays(null, 30)).thenReturn(new PastSnapshot("days", null).setModeParameter("30"));

    PastSnapshot variationSnapshot = finder.find(null, 1, "30");

    verify(finderByDays).findFromDays(null, 30);
    assertNotNull(variationSnapshot);
    assertThat(variationSnapshot.getIndex(), is(1));
    assertThat(variationSnapshot.getMode(), is("days"));
    assertThat(variationSnapshot.getModeParameter(), is("30"));
  }

  @Test
  public void should_not_find_by_number_of_days() {
    PastSnapshot variationSnapshot = finder.find(null, 1, "30");

    verify(finderByDays).findFromDays(null, 30);
    assertNull(variationSnapshot);
  }

  @Test
  public void should_find_by_date() throws ParseException {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date date = format.parse("2010-05-18");
    when(finderByDate.findByDate((Snapshot) null, date)).thenReturn(new PastSnapshot("date", date, new Snapshot()));

    PastSnapshot variationSnapshot = finder.find(null, 2, "2010-05-18");

    verify(finderByDate).findByDate(any(Snapshot.class), argThat(new ArgumentMatcher<Date>() {
      @Override
      public boolean matches(Object o) {
        return o.equals(date);
      }
    }));
    assertThat(variationSnapshot.getIndex(), is(2));
    assertThat(variationSnapshot.getMode(), is("date"));
    assertThat(variationSnapshot.getProjectSnapshot(), not(nullValue()));
  }

  @Test
  public void should_not_find_by_date() {
    when(finderByDate.findByDate(any(Snapshot.class), any(Date.class))).thenReturn(null);

    PastSnapshot variationSnapshot = finder.find(null, 2, "2010-05-18");

    verify(finderByDate).findByDate(any(Snapshot.class), any(Date.class));
    assertNull(variationSnapshot);
  }

  @Test
  public void should_find_by_previous_analysis() throws ParseException {
    final Date date = DateUtils.parseDate("2010-05-18");
    Snapshot snapshot = new Snapshot();
    snapshot.setCreatedAtMs(date.getTime());
    when(finderByPreviousAnalysis.findByPreviousAnalysis(null)).thenReturn(new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, date, snapshot));

    PastSnapshot variationSnapshot = finder.find(null, 2, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);

    verify(finderByPreviousAnalysis).findByPreviousAnalysis(null);
    assertThat(variationSnapshot.getIndex(), is(2));
    assertThat(variationSnapshot.getMode(), is(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS));
    assertThat(variationSnapshot.getProjectSnapshot(), not(nullValue()));
  }

  @Test
  public void should_not_find_previous_analysis() {
    when(finderByPreviousAnalysis.findByPreviousAnalysis(null)).thenReturn(null);

    PastSnapshot variationSnapshot = finder.find(null, 2, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);

    verify(finderByPreviousAnalysis).findByPreviousAnalysis(null);

    assertNull(variationSnapshot);
  }

  @Test
  public void should_find_by_previous_version() throws ParseException {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date date = format.parse("2010-05-18");
    Snapshot snapshot = new Snapshot();
    snapshot.setCreatedAtMs(date.getTime());
    when(finderByPreviousVersion.findByPreviousVersion(null)).thenReturn(new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION, date, snapshot));

    PastSnapshot variationSnapshot = finder.find(null, 2, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);

    verify(finderByPreviousVersion).findByPreviousVersion(null);
    assertThat(variationSnapshot.getIndex(), is(2));
    assertThat(variationSnapshot.getMode(), is(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION));
    assertThat(variationSnapshot.getProjectSnapshot(), not(nullValue()));
  }

  @Test
  public void should_find_by_version() {
    when(finderByVersion.findByVersion(null, "1.2")).thenReturn(new PastSnapshot("version", new Date(), new Snapshot()));

    PastSnapshot variationSnapshot = finder.find(null, 2, "1.2");

    verify(finderByVersion).findByVersion(null, "1.2");
    assertThat(variationSnapshot.getIndex(), is(2));
    assertThat(variationSnapshot.getMode(), is("version"));
    assertThat(variationSnapshot.getProjectSnapshot(), not(nullValue()));
  }

  @Test
  public void should_not_find_version() {
    when(finderByVersion.findByVersion(null, "1.2")).thenReturn(null);

    PastSnapshot variationSnapshot = finder.find(null, 2, "1.2");

    verify(finderByVersion).findByVersion(null, "1.2");
    assertNull(variationSnapshot);
  }

  @Test
  public void should_not_fail_if_unknown_format() {
    // should not be called
    when(finderByPreviousAnalysis.findByPreviousAnalysis(null)).thenReturn(new PastSnapshot(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, new Date(), new Snapshot()));

    assertNull(finder.find(null, 2, "foooo"));
  }

  @Test
  public void should_get_property_value() {
    Settings settings = new Settings().setProperty("sonar.timemachine.period1", "5");

    assertThat(PastSnapshotFinder.getPropertyValue("FIL", settings, 1), is("5"));
    assertThat(PastSnapshotFinder.getPropertyValue("FIL", settings, 999), nullValue());
  }
}
