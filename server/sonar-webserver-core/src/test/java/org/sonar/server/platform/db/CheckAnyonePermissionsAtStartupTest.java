/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
  public void force_auth_false_anyone_global_permissions() {
    setForceAuthentication(false);
    dbTester.users().insertPermissionOnAnyone("perm-anyone");
    createPublicProjects(3, false);
    assertGlobalLevelAnyonePermissionWarningInLogs();
    assertProjectLevelAnyonePermissionWarningNotInLogs();
  }

  @Test
  public void force_auth_false_project_level_anyone_permissions_exactly_three() {
    setForceAuthentication(false);
    createPublicProjects(3, true);
    assertGlobalLevelAnyonePermissionWarningNotInLogs();
    assertProjectLevelAnyonePermissionWarningInLogs(3, "key-1", "key-2", "key-3");
  }

  @Test
  public void force_auth_false_project_level_anyone_permissions_less_than_three() {
    setForceAuthentication(false);
    createPublicProjects(1, true);
    assertGlobalLevelAnyonePermissionWarningNotInLogs();
    assertProjectLevelAnyonePermissionWarningInLogs(1, "key-1");
  }

  @Test
  public void force_auth_false_project_level_anyone_permissions_more_than_three() {
    setForceAuthentication(false);
    createPublicProjects(9, true);
    assertGlobalLevelAnyonePermissionWarningNotInLogs();
    assertProjectLevelAnyonePermissionWarningInLogs(9, "key-1", "key-2", "key-3");
  }

  @Test
  public void force_auth_false_no_projects() {
    setForceAuthentication(false);
    assertGlobalLevelAnyonePermissionWarningNotInLogs();
    assertProjectLevelAnyonePermissionWarningNotInLogs();
  }

  @Test
  public void force_auth_false_no_anyone_permissions() {
    setForceAuthentication(false);
    createPublicProjectsWithNonAnyoneGroupPermissions();
    assertGlobalLevelAnyonePermissionWarningNotInLogs();
    assertProjectLevelAnyonePermissionWarningNotInLogs();
  }

  @Test
  public void force_auth_false_project_and_global_level_anyone_permissions() {
    setForceAuthentication(false);
    dbTester.users().insertPermissionOnAnyone("perm-anyone");
    createPublicProjects(3, true);
    assertGlobalLevelAnyonePermissionWarningInLogs();
    assertProjectLevelAnyonePermissionWarningInLogs(3, "key-1", "key-2", "key-3");
  }

  @Test
  public void force_auth_true_anyone_global_level_permissions() {
    setForceAuthentication(true);
    dbTester.users().insertPermissionOnAnyone("perm-anyone");
    createPublicProjects(3, false);
    assertGlobalLevelAnyonePermissionWarningNotInLogs();
    assertProjectLevelAnyonePermissionWarningNotInLogs();
  }

  @Test
  public void force_auth_true_project_level_anyone_permissions() {
    setForceAuthentication(true);
    createPublicProjects(3, true);
    assertGlobalLevelAnyonePermissionWarningNotInLogs();
    assertProjectLevelAnyonePermissionWarningNotInLogs();
  }

  @Test
  public void force_auth_true_no_anyone_permissions() {
    setForceAuthentication(true);
    createPublicProjectsWithNonAnyoneGroupPermissions();
    assertGlobalLevelAnyonePermissionWarningNotInLogs();
    assertProjectLevelAnyonePermissionWarningNotInLogs();
  }

  @Test
  public void force_auth_true_project_and_global_anyone_permissions() {
    setForceAuthentication(true);
    dbTester.users().insertPermissionOnAnyone("perm-anyone");
    createPublicProjects(3, true);
    assertGlobalLevelAnyonePermissionWarningNotInLogs();
    assertProjectLevelAnyonePermissionWarningNotInLogs();
  }

  private void setForceAuthentication(Boolean isForceAuthentication) {
    settings.setProperty("sonar.forceAuthentication", isForceAuthentication.toString());
  }

  private void createPublicProjectsWithNonAnyoneGroupPermissions() {
    GroupDto group = dbTester.users().insertGroup();
    IntStream.rangeClosed(1, 3).forEach(i -> {
      ComponentDto project = dbTester.components().insertPublicProject(p -> p.setKey("key-" + i));
      dbTester.users().insertProjectPermissionOnGroup(group, "perm-" + i, project);
    });
  }

  private void createPublicProjects(int projectCount, boolean includeAnyonePerm) {
    IntStream.rangeClosed(1, projectCount).forEach(i -> {
      ComponentDto project = dbTester.components().insertPublicProject(p -> p.setKey("key-" + i));
      if (includeAnyonePerm) {
        dbTester.users().insertProjectPermissionOnAnyone("perm-" + i, project);
      }
    });
    underTest.start();
  }

  private void assertProjectLevelAnyonePermissionWarningNotInLogs() {
    boolean noneMatch = logTester.logs(LoggerLevel.WARN).stream()
      .noneMatch(s -> s.startsWith("Authentication is not enforced, and project permissions assigned to the 'Anyone' group expose"));
    assertThat(noneMatch).isTrue();
  }

  private void assertProjectLevelAnyonePermissionWarningInLogs(int expectedProjectCount, String... expectedListedProjects) {
    String expected = String.format("Authentication is not enforced, and project permissions assigned to the 'Anyone' group expose %d " +
      "public project(s) to security risks, including: %s. Unauthenticated visitors have permissions on these project(s).",
      expectedProjectCount, String.join(", ", expectedListedProjects));
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(expected);
  }

  private void assertGlobalLevelAnyonePermissionWarningNotInLogs() {
    boolean noneMatch = !logTester.logs(LoggerLevel.WARN).contains(
      "Authentication is not enforced, and permissions assigned to the 'Anyone' group globally expose the " +
        "instance to security risks. Unauthenticated visitors may unintentionally have permissions on projects.");
    assertThat(noneMatch).isTrue();
  }

  private void assertGlobalLevelAnyonePermissionWarningInLogs() {
    String expected = "Authentication is not enforced, and permissions assigned to the 'Anyone' group globally " +
      "expose the instance to security risks. Unauthenticated visitors may unintentionally have permissions on projects.";
    assertThat(logTester.logs(LoggerLevel.WARN)).contains(expected);
  }

}
