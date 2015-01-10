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

package org.sonar.server.computation;

import com.google.common.collect.Iterables;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.activity.Activity;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.tester.ServerTester;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.activity.Activity.Type.ANALYSIS_REPORT;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.FAILED;

public class AnalysisReportLogMediumTest {
  @ClassRule
  public static ServerTester tester = new ServerTester();

  private ActivityService service = tester.get(ActivityService.class);
  private ActivityIndex index = tester.get(ActivityIndex.class);
  private DbSession dbSession;

  private AnalysisReportLog sut;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void insert_find_analysis_report_log() {
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L)
      .setProjectKey("projectKey")
      .setStatus(FAILED)
      .setCreatedAt(DateUtils.parseDate("2014-10-15"))
      .setUpdatedAt(DateUtils.parseDate("2014-10-16"))
      .setStartedAt(DateUtils.parseDate("2014-10-17"))
      .setFinishedAt(DateUtils.parseDate("2014-10-18"));
    ComponentDto project = ComponentTesting.newProjectDto();

    service.write(dbSession, ANALYSIS_REPORT, new AnalysisReportLog(report, project));
    dbSession.commit();

    // 0. AssertBase case
    assertThat(index.findAll().getHits()).hasSize(1);

    Activity activity = Iterables.getFirst(index.findAll().getHits(), null);
    assertThat(activity).isNotNull();
    Map<String, String> details = activity.details();
    assertThat(details.get("key")).isEqualTo(String.valueOf(report.getId()));
    assertThat(details.get("projectKey")).isEqualTo(project.key());
    assertThat(details.get("projectName")).isEqualTo(project.name());
    assertThat(details.get("projectUuid")).isEqualTo(project.uuid());
    assertThat(details.get("status")).isEqualTo("FAILED");
    assertThat(details.get("submittedAt")).isEqualTo("2014-10-15T00:00:00+0200");
    assertThat(details.get("startedAt")).isEqualTo("2014-10-17T00:00:00+0200");
    assertThat(details.get("finishedAt")).isEqualTo("2014-10-18T00:00:00+0200");
  }

}
