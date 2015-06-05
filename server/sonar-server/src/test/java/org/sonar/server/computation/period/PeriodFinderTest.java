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

package org.sonar.server.computation.period;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.CoreProperties;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class PeriodFinderTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  DbClient dbClient;

  DbSession dbSession;

  PeriodFinder periodFinder;

  @Before
  public void setUp() throws Exception {
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new SnapshotDao());
    dbSession = dbClient.openSession(false);
    periodFinder = new PeriodFinder(dbClient);
  }

  @After
  public void tearDown() throws Exception {
    dbSession.close();
  }

  @Test
  public void find_by_date() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_date.xml");
    String textDate = "2008-11-22";
    Date date = DATE_FORMAT.parse(textDate);

    Period result = periodFinder.findByDate(dbSession, 1L, date);

    // Return analysis from given date 2008-11-22
    assertThat(result.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DATE);
    assertThat(result.getModeParameter()).isEqualTo(textDate);
    assertThat(result.getSnapshotDate()).isEqualTo(1227358680000L);
    assertThat(result.getTargetDate()).isEqualTo(date.getTime());
    assertThat(result.getProjectSnapshot().getId()).isEqualTo(1006L);
  }

  @Test
  public void find_by_date_search_for_nearest_later_analysis() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_date.xml");
    String date = "2008-11-24";

    Period result = periodFinder.findByDate(dbSession, 1L, DATE_FORMAT.parse(date));

    // No analysis have been done at this date, it should return the one from the date after (2008-11-25)
    assertThat(result.getSnapshotDate()).isEqualTo(1227617880000L);
    assertThat(result.getProjectSnapshot().getId()).isEqualTo(1009L);
  }

  @Test
  public void not_find_by_date() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_date.xml");
    String date = "2008-11-22";

    // No analysis for this project
    assertThat(periodFinder.findByDate(dbSession, 123L, DATE_FORMAT.parse(date))).isNull();
  }

  @Test
  public void find_by_days() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_days.xml");

    Period result = periodFinder.findByDays(dbSession, 1L, DATE_FORMAT.parse("2008-11-16").getTime(), 50);

    // Return analysis from the 2008-11-01
    assertThat(result.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DAYS);
    assertThat(result.getModeParameter()).isEqualTo("50");
    assertThat(result.getSnapshotDate()).isEqualTo(1225544280000L);
    assertThat(result.getTargetDate()).isBetween(DATE_FORMAT.parse("2008-09-26").getTime(), DATE_FORMAT.parse("2008-09-27").getTime());
    assertThat(result.getProjectSnapshot().getId()).isEqualTo(1000L);
  }

  @Test
  public void ignore_unprocessed_snapshots() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_days.xml");

    Period result = periodFinder.findByDays(dbSession, 1L, DATE_FORMAT.parse("2008-11-16").getTime(), 7);

    // Return analysis from the 2008-11-13, the matching one from 2008-11-12 should be ignored
    assertThat(result.getProjectSnapshot().getId()).isEqualTo(1006L);
  }

  @Test
  public void not_find_by_days() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_days.xml");

    // No analysis for this project
    assertThat(periodFinder.findByDays(dbSession, 123L, DATE_FORMAT.parse("2008-11-16").getTime(), 7)).isNull();
  }

  @Test
  public void locate_nearest_snapshot_before() throws ParseException {
    Date current = DATE_FORMAT.parse("2010-10-20");
    // distance: 15 => target is 2010-10-05

    List<SnapshotDto> snapshots = Arrays.asList(
      new SnapshotDto().setId(1L).setCreatedAt(DATE_FORMAT.parse("2010-09-30").getTime()),
      new SnapshotDto().setId(2L).setCreatedAt(DATE_FORMAT.parse("2010-10-03").getTime()), // -2 days
      new SnapshotDto().setId(3L).setCreatedAt(DATE_FORMAT.parse("2010-10-08").getTime()), // +3 days
      new SnapshotDto().setId(4L).setCreatedAt(DATE_FORMAT.parse("2010-10-12").getTime())  // +7 days
    );
    assertThat(PeriodFinder.findNearestSnapshotToTargetDate(snapshots, DateUtils.addDays(current, -15).getTime()).getId()).isEqualTo(2);
  }

  @Test
  public void locate_nearest_snapshot_after() throws ParseException {
    Date current = DATE_FORMAT.parse("2010-10-20");
    // distance: 15 => target is 2010-10-05

    List<SnapshotDto> snapshots = Arrays.asList(
      new SnapshotDto().setId(1L).setCreatedAt(DATE_FORMAT.parse("2010-09-30").getTime()),
      new SnapshotDto().setId(2L).setCreatedAt(DATE_FORMAT.parse("2010-10-01").getTime()), // -4 days
      new SnapshotDto().setId(3L).setCreatedAt(DATE_FORMAT.parse("2010-10-08").getTime()), // +3 days
      new SnapshotDto().setId(4L).setCreatedAt(DATE_FORMAT.parse("2010-10-12").getTime()) // +7 days
    );
    assertThat(PeriodFinder.findNearestSnapshotToTargetDate(snapshots, DateUtils.addDays(current, -15).getTime()).getId()).isEqualTo(3);
  }

  @Test
  public void find_by_previous_analysis() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_previous_analysis.xml");

    Period result = periodFinder.findByPreviousAnalysis(dbSession, 1L, DATE_FORMAT.parse("2008-11-27").getTime());

    // Return analysis from given date 2008-11-22
    assertThat(result.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);
    assertThat(result.getModeParameter()).isEqualTo(DATE_FORMAT.format(new Date(1227617880000L)));
    assertThat(result.getSnapshotDate()).isEqualTo(1227617880000L);
    assertThat(result.getTargetDate()).isEqualTo(1227617880000L);
    assertThat(result.getProjectSnapshot().getId()).isEqualTo(1009L);
  }

  @Test
  public void not_find_by_previous_analysis() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_previous_analysis.xml");

    // No analysis for this project
    assertThat(periodFinder.findByPreviousAnalysis(dbSession, 2L, DATE_FORMAT.parse("2008-11-27").getTime())).isNull();
  }

  @Test
  public void find_by_previous_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_previous_version.xml");

    Period result = periodFinder.findByPreviousVersion(dbSession, 1L, "1.2-SNAPSHOT");

    // Return analysis from given date 2008-11-22
    assertThat(result.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_VERSION);
    assertThat(result.getModeParameter()).isEqualTo("1.1");
    assertThat(result.getSnapshotDate()).isEqualTo(1225803480000L);
    assertThat(result.getTargetDate()).isEqualTo(1225803480000L);
    assertThat(result.getProjectSnapshot().getId()).isEqualTo(1001L);
  }

  @Test
  public void find_by_previous_version_when_previous_version_deleted() throws Exception {
    dbTester.prepareDbUnit(getClass(), "previous_version_deleted.xml");

    Period result = periodFinder.findByPreviousVersion(dbSession, 1L, "1.2-SNAPSHOT");

    // Return analysis from given date 2008-11-22
    assertThat(result.getModeParameter()).isEqualTo("1.0");
    assertThat(result.getSnapshotDate()).isEqualTo(1225630680000L);
    assertThat(result.getTargetDate()).isEqualTo(1225630680000L);
    assertThat(result.getProjectSnapshot().getId()).isEqualTo(1000L);
  }

  @Test
  public void not_find_previous_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "no_previous_version.xml");

    assertThat(periodFinder.findByPreviousVersion(dbSession, 1L, "1.2-SNAPSHOT")).isNull();
  }

  @Test
  public void find_by_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_version.xml");

    Period result = periodFinder.findByVersion(dbSession, 1L, "1.1");

    assertThat(result.getMode()).isEqualTo(CoreProperties.TIMEMACHINE_MODE_VERSION);
    assertThat(result.getModeParameter()).isEqualTo("1.1");
    assertThat(result.getSnapshotDate()).isEqualTo(1225803480000L);
    assertThat(result.getTargetDate()).isEqualTo(1225803480000L);
    assertThat(result.getProjectSnapshot().getId()).isEqualTo(1009L);
  }

  @Test
  public void not_find_by_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "find_by_version.xml");

    assertThat(periodFinder.findByVersion(dbSession, 1L, "1.0")).isNull();
  }

}
