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

import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.server.component.ComponentTesting;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.FAILED;

public class ReportActivityTest {

  @Test
  public void insert_find_analysis_report_log() {
    AnalysisReportDto report = AnalysisReportDto.newForTests(1L)
      .setProjectKey("projectKey")
      .setStatus(FAILED)
      .setCreatedAt(DateUtils.parseDate("2014-10-15").getTime())
      .setUpdatedAt(DateUtils.parseDate("2014-10-16").getTime())
      .setStartedAt(DateUtils.parseDate("2014-10-17").getTime())
      .setFinishedAt(DateUtils.parseDate("2014-10-18").getTime());
    ComponentDto project = ComponentTesting.newProjectDto();

    ReportActivity activity = new ReportActivity(report, project);

    Map<String, String> details = activity.getDetails();
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
