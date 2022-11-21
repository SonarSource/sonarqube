/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db;

import java.util.stream.IntStream;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckAnyonePermissionsAtStartupTest {

  @ClassRule
  public static LogTester logTester = new LogTester().setLevel(LoggerLevel.WARN);
  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);
  private final DbClient dbClient = dbTester.getDbClient();
  private final MapSettings settings = new MapSettings();
  private final CheckAnyonePermissionsAtStartup underTest = new CheckAnyonePermissionsAtStartup(dbClient, settings.asConfig());

  @After
  public void tearDown() {
    logTester.clear();
    underTest.stop();
  }

  @Test
  public void test_logs_present_when_exactly_3_projects_contain_anyone_permissions_and_force_authentication_false() {
    int expectedProjectCount = 3;
    setForceAuthentication(false);
    execute(expectedProjectCount);
    assertAnyonePermissionWarningInLogs(expectedProjectCount, "key-1", "key-2", "key-3");
  }

  @Test
  public void test_logs_present_when_less_than_3_projects_contain_anyone_permissions_and_force_authentication_false() {
    int expectedProjectCount = 1;
    setForceAuthentication(false);
    execute(expectedProjectCount);
    assertAnyonePermissionWarningInLogs(expectedProjectCount, "key-1");
  }

  @Test
  public void test_logs_present_when_more_than_3_projects_contain_anyone_permissions_and_force_authentication_false() {
    int expectedProjectCount = 8;
    setForceAuthentication(false);
    execute(expectedProjectCount);
    assertAnyonePermissionWarningInLogs(expectedProjectCount, "key-1", "key-2", "key-3");
  }

  @Test
  public void test_logs_not_present_when_no_projects_contain_anyone_permissions_and_force_authentication_false() {
    setForceAuthentication(false);
    generatePublicProjectsWithGroupPermissions();
    assertAnyonePermissionWarningNotInLogs();
  }

  @Test
  public void test_logs_present_when_1_projects_contain_anyone_permissions_and_full_anyone_group_permission_and_force_authentication_false() {
    // Although saved in the same table (group_roles), this should not be included in the logs as not assigned to single project.
    dbTester.users().insertPermissionOnAnyone("perm-anyone");

    int expectedProjectCount = 1;
    setForceAuthentication(false);
    execute(expectedProjectCount);
    assertAnyonePermissionWarningInLogs(expectedProjectCount, "key-1");
  }

  @Test
  public void test_logs_not_present_when_some_projects_contain_anyone_permissions_and_force_authentication_true() {
    setForceAuthentication(true);
    execute(3);
    assertAnyonePermissionWarningNotInLogs();
  }

  @Test
  public void test_logs_not_present_when_no_projects_contain_anyone_permissions_and_force_authentication_true() {
    setForceAuthentication(true);
    generatePublicProjectsWithGroupPermissions();
    assertAnyonePermissionWarningNotInLogs();
  }

  @Test
  public void test_logs_not_present_when_projects_contain_anyone_permissions_and_force_authentication_default() {
    settings.clear();
    execute(3);
    assertAnyonePermissionWarningNotInLogs();
  }

  private void generatePublicProjectsWithGroupPermissions() {
    GroupDto group = dbTester.users().insertGroup();
    IntStream.rangeClosed(1, 3).forEach(i -> {
      ComponentDto project = dbTester.components().insertPublicProject(p -> p.setKey("key-" + i));
      dbTester.users().insertProjectPermissionOnGroup(group, "perm-" + i, project);
    });
    underTest.start();
  }

  private void execute(int projectCount) {
    IntStream.rangeClosed(1, projectCount).forEach(i -> {
      ComponentDto project = dbTester.components().insertPublicProject(p -> p.setKey("key-" + i));
      dbTester.users().insertProjectPermissionOnAnyone("perm-" + i, project);
    });
    underTest.start();
  }

  private void setForceAuthentication(Boolean isForceAuthentication) {
    settings.setProperty("sonar.forceAuthentication", isForceAuthentication.toString());
  }

  private void assertAnyonePermissionWarningNotInLogs() {
    boolean noneMatch = logTester.logs().stream()
      .noneMatch(s -> s.matches(".*A total of [0-9]+ public project\\(s\\) are found to have enabled 'Anyone' group permissions, including: %s. " +
        "Make sure your project permissions are set as intended.*"));
    assertThat(noneMatch).isTrue();
  }

  private void assertAnyonePermissionWarningInLogs(int expectedProjectCountString, String... expectedListedProjects) {
    String expected = String.format("A total of %d public project(s) are found to have enabled 'Anyone' group permissions, including: %s. " +
        "Make sure your project permissions are set as intended.",
      expectedProjectCountString, String.join(", ", expectedListedProjects));
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(expected);
  }

}
