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

package org.sonar.server.issue.notification;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.index.UserIndex;

import java.util.Date;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.notification.NewIssuesStatistics.METRIC.*;

public class NewIssuesNotificationTest {

  NewIssuesStatistics.Stats stats = new NewIssuesStatistics.Stats();
  UserIndex userIndex = mock(UserIndex.class);
  DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  Durations durations = mock(Durations.class);
  NewIssuesNotification sut = new NewIssuesNotification(userIndex, dbClient, durations);

  @Test
  public void set_project() throws Exception {
    ComponentDto component = ComponentTesting.newProjectDto()
      .setLongName("project-long-name")
      .setUuid("project-uuid")
      .setKey("project-key");

    sut.setProject(component);

    assertThat(sut.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_NAME)).isEqualTo("project-long-name");
    assertThat(sut.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_UUID)).isEqualTo("project-uuid");
    assertThat(sut.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_KEY)).isEqualTo("project-key");
  }

  @Test
  public void set_date() throws Exception {
    Date date = new Date();

    sut.setAnalysisDate(date);

    assertThat(sut.getFieldValue(NewIssuesEmailTemplate.FIELD_PROJECT_DATE)).isEqualTo(DateUtils.formatDateTime(date));
  }

  @Test
  public void set_statistics() throws Exception {
    ComponentDto component = ComponentTesting.newProjectDto()
      .setLongName("project-long-name");
    addIssueNTimes(newIssue1(), 5);
    addIssueNTimes(newIssue2(), 3);
    when(dbClient.componentDao().getByUuid(any(DbSession.class), eq("file-uuid")).name()).thenReturn("file-name");
    when(dbClient.componentDao().getByUuid(any(DbSession.class), eq("directory-uuid")).name()).thenReturn("directory-name");

    sut.setStatistics(component, stats);

    assertThat(sut.getFieldValue(SEVERITY + ".INFO.count")).isEqualTo("5");
    assertThat(sut.getFieldValue(SEVERITY + ".BLOCKER.count")).isEqualTo("3");
    assertThat(sut.getFieldValue(ASSIGNEE + ".1.label")).isEqualTo("maynard");
    assertThat(sut.getFieldValue(ASSIGNEE + ".1.count")).isEqualTo("5");
    assertThat(sut.getFieldValue(ASSIGNEE + ".2.label")).isEqualTo("keenan");
    assertThat(sut.getFieldValue(ASSIGNEE + ".2.count")).isEqualTo("3");
    assertThat(sut.getFieldValue(TAGS + ".1.label")).isEqualTo("owasp");
    assertThat(sut.getFieldValue(TAGS + ".1.count")).isEqualTo("8");
    assertThat(sut.getFieldValue(TAGS + ".2.label")).isEqualTo("bug");
    assertThat(sut.getFieldValue(TAGS + ".2.count")).isEqualTo("5");
    assertThat(sut.getFieldValue(COMPONENT + ".1.label")).isEqualTo("file-name");
    assertThat(sut.getFieldValue(COMPONENT + ".1.count")).isEqualTo("5");
    assertThat(sut.getFieldValue(COMPONENT + ".2.label")).isEqualTo("directory-name");
    assertThat(sut.getFieldValue(COMPONENT + ".2.count")).isEqualTo("3");
    assertThat(sut.getDefaultMessage()).startsWith("8 new issues on project-long-name");
  }

  @Test
  public void set_debt() throws Exception {
    when(durations.format(any(Locale.class), any(Duration.class))).thenReturn("55 min");

    sut.setDebt(Duration.create(55));

    assertThat(sut.getFieldValue(DEBT + ".count")).isEqualTo("55 min");
  }

  private void addIssueNTimes(DefaultIssue issue, int times) {
    for (int i = 0; i < times; i++) {
      stats.add(issue);
    }
  }

  private DefaultIssue newIssue1() {
    return new DefaultIssue()
      .setAssignee("maynard")
      .setComponentUuid("file-uuid")
      .setSeverity(Severity.INFO)
      .setTags(Lists.newArrayList("bug", "owasp"))
      .setDebt(Duration.create(5L));
  }

  private DefaultIssue newIssue2() {
    return new DefaultIssue()
      .setAssignee("keenan")
      .setComponentUuid("directory-uuid")
      .setSeverity(Severity.BLOCKER)
      .setTags(Lists.newArrayList("owasp"))
      .setDebt(Duration.create(10L));
  }
}
