/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.notification;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class ReportAnalysisFailureNotificationTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Random random = new Random();
  private ReportAnalysisFailureNotification.Task task = new ReportAnalysisFailureNotification.Task(
    randomAlphanumeric(2), random.nextInt(5_996), random.nextInt(9_635));
  private ReportAnalysisFailureNotification.Project project = new ReportAnalysisFailureNotification.Project(
    randomAlphanumeric(6), randomAlphanumeric(7), randomAlphanumeric(8), randomAlphanumeric(9));

  @Test
  public void project_constructor_fails_if_uuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    new ReportAnalysisFailureNotification.Project(null, randomAlphanumeric(2), randomAlphanumeric(3), randomAlphanumeric(4));
  }

  @Test
  public void project_constructor_fails_if_key_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("key can't be null");

    new ReportAnalysisFailureNotification.Project(randomAlphanumeric(2), null, randomAlphanumeric(3), randomAlphanumeric(4));
  }

  @Test
  public void project_constructor_fails_if_name_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can't be null");

    new ReportAnalysisFailureNotification.Project(randomAlphanumeric(2), randomAlphanumeric(3), null, randomAlphanumeric(4));
  }

  @Test
  public void verify_report_getters() {
    String uuid = randomAlphanumeric(2);
    String key = randomAlphanumeric(3);
    String name = randomAlphanumeric(4);
    String branchName = randomAlphanumeric(5);
    ReportAnalysisFailureNotification.Project underTest = new ReportAnalysisFailureNotification.Project(uuid, key, name, branchName);

    assertThat(underTest.getUuid()).isEqualTo(uuid);
    assertThat(underTest.getName()).isEqualTo(name);
    assertThat(underTest.getKey()).isEqualTo(key);
    assertThat(underTest.getBranchName()).isEqualTo(branchName);
  }

  @Test
  public void project_supports_null_branch() {
    ReportAnalysisFailureNotification.Project underTest = new ReportAnalysisFailureNotification.Project(randomAlphanumeric(2), randomAlphanumeric(3), randomAlphanumeric(4), null);

    assertThat(underTest.getBranchName()).isNull();
  }

  @Test
  public void task_constructor_fails_if_uuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can't be null");

    new ReportAnalysisFailureNotification.Task(null, random.nextInt(5_996), random.nextInt(9_635));
  }

  @Test
  public void verify_task_getters() {
    String uuid = randomAlphanumeric(6);
    int createdAt = random.nextInt(5_996);
    int failedAt = random.nextInt(9_635);

    ReportAnalysisFailureNotification.Task underTest = new ReportAnalysisFailureNotification.Task(uuid, createdAt, failedAt);

    assertThat(underTest.getUuid()).isEqualTo(uuid);
    assertThat(underTest.getCreatedAt()).isEqualTo(createdAt);
    assertThat(underTest.getFailedAt()).isEqualTo(failedAt);
  }

  @Test
  public void constructor_fails_with_NPE_if_Project_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("project can't be null");

    new ReportAnalysisFailureNotification(null, task, randomAlphanumeric(99));
  }

  @Test
  public void constructor_fails_with_NPE_if_Task_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("task can't be null");

    new ReportAnalysisFailureNotification(project, null, randomAlphanumeric(99));
  }

  @Test
  public void verify_getters() {
    String message = randomAlphanumeric(99);
    ReportAnalysisFailureNotification underTest = new ReportAnalysisFailureNotification(project, task, message);

    assertThat(underTest.getProject()).isSameAs(project);
    assertThat(underTest.getTask()).isSameAs(task);
    assertThat(underTest.getErrorMessage()).isSameAs(message);
  }

  @Test
  public void null_error_message_is_supported() {
    ReportAnalysisFailureNotification underTest = new ReportAnalysisFailureNotification(project, task, null);

    assertThat(underTest.getErrorMessage()).isNull();
  }
}
