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
package org.sonar.server.platform.db.migration.version.v65;

import java.sql.SQLException;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class EnsureIssueProjectUuidConsistencyOnIssuesTest {
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(EnsureIssueProjectUuidConsistencyOnIssuesTest.class, "issues_and_projects.sql");

  private final Random random = new Random();
  private EnsureIssueProjectUuidConsistencyOnIssues underTest = new EnsureIssueProjectUuidConsistencyOnIssues(db.database());

  @Test
  public void execute_has_no_effect_if_tables_are_empty() throws SQLException {
    underTest.execute();
  }

  @Test
  public void execute_fixes_project_uuid_of_issue_when_inconsistent_with_PROJECTS_PROJECT_UUID() throws SQLException {
    String projectUuid = randomAlphabetic(5);
    String componentUuid = insertComponent(projectUuid);
    String inconsistentIssueKey = insertIssue(componentUuid, randomAlphabetic(9));
    String consistentIssueKey = insertIssue(componentUuid, projectUuid);

    underTest.execute();

    assertThat(getProjectUuid(inconsistentIssueKey)).isEqualTo(projectUuid);
    assertThat(getProjectUuid(consistentIssueKey)).isEqualTo(projectUuid);
  }

  @Test
  public void execute_ignores_issues_which_component_does_not_exist() throws SQLException {
    String projectUuid = randomAlphabetic(3);
    String issueKey = insertIssue(randomAlphabetic(8), projectUuid);

    underTest.execute();

    assertThat(getProjectUuid(issueKey)).isEqualTo(projectUuid);
  }

  @Test
  public void execute_ignores_issues_which_null_project_uuid() throws SQLException {
    String issueKey = insertIssue(randomAlphabetic(8), null);

    underTest.execute();

    assertThat(getProjectUuid(issueKey)).isNull();
  }

  @Test
  public void execute_ignores_issues_which_null_component_uuid() throws SQLException {
    String projectUuid = randomAlphabetic(5);
    String issueKey = insertIssue(null, projectUuid);

    underTest.execute();

    assertThat(getProjectUuid(issueKey)).isEqualTo(projectUuid);
  }

  private String getProjectUuid(String issueKey) {
    return (String) db.selectFirst("select project_uuid as \"PROJECT_UUID\" from issues where kee = '" + issueKey + "'")
      .get("PROJECT_UUID");
  }

  private String insertIssue(@Nullable String componentUuid, @Nullable String projectUuid) {
    String issueKey = randomAlphabetic(3);
    db.executeInsert(
      "ISSUES",
      "KEE", issueKey,
      "COMPONENT_UUID", componentUuid,
      "PROJECT_UUID", projectUuid,
      "MANUAL_SEVERITY", String.valueOf(random.nextBoolean()));
    return issueKey;
  }

  private String insertComponent(String projectUuid) {
    String uuid = randomAlphabetic(5);
    db.executeInsert(
      "PROJECTS",
      "ORGANIZATION_UUID", randomAlphabetic(5),
      "UUID", uuid,
      "UUID_PATH", "path_" + uuid,
      "ROOT_UUID", "root_uuid_" + uuid,
      "PROJECT_UUID", projectUuid,
      "PRIVATE", String.valueOf(random.nextBoolean()),
      "ENABLED", String.valueOf(random.nextBoolean()));
    return uuid;
  }
}
