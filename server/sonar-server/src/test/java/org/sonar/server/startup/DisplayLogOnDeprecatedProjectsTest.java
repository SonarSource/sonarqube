/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.startup;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;

public class DisplayLogOnDeprecatedProjectsTest {

  static final String PROJECT_KEY = "PROJECT_KEY";
  static final String DEPRECATED_PROJECT_KEY = "DEPRECATED_PROJECT_KEY";

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();
  DbSession dbSession = dbTester.getSession();

  DisplayLogOnDeprecatedProjects underTest = new DisplayLogOnDeprecatedProjects(dbClient);

  @Test
  public void display_log_on_deprecated_project() throws Exception {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    dbClient.componentDao().insert(dbSession, ComponentTesting.newPrivateProjectDto(organizationDto)
      .setKey(DEPRECATED_PROJECT_KEY)
      .setDeprecatedKey(null));
    dbSession.commit();

    underTest.start();

    assertThat(logTester.logs()).containsOnly(
      "We detected that the following projects have not been analysed on a SonarQube version greater than 4.2 (included):",
      " - " + DEPRECATED_PROJECT_KEY,
      "As a consequence, some features of the Web UI will be broken for them, and any new analysis will consider all issues as new issues.");
  }

  @Test
  public void not_display_log_when_task_already_executed() throws Exception {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    dbClient.componentDao().insert(dbSession, ComponentTesting.newPrivateProjectDto(organizationDto)
      .setKey(DEPRECATED_PROJECT_KEY)
      .setDeprecatedKey(null));
    dbSession.commit();

    underTest.start();
    assertThat(logTester.logs()).isNotEmpty();
    logTester.clear();

    underTest.start();
    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void nothing_do_when_no_deprecated_project() throws Exception {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    dbClient.componentDao().insert(dbSession, ComponentTesting.newPrivateProjectDto(organizationDto)
      .setKey(PROJECT_KEY)
      .setDeprecatedKey(PROJECT_KEY));
    dbSession.commit();

    underTest.start();

    assertThat(logTester.logs()).isEmpty();
  }

  @Test
  public void nothing_do_when_no_project() throws Exception {
    underTest.start();

    assertThat(logTester.logs()).isEmpty();
  }
}
