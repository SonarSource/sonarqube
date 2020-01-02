/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v82;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
import static org.sonar.api.rules.RuleType.VULNERABILITY;

public class MigrateManualVulnerabilitiesToSecurityHotSpotsTest {

  private final static String ISSUES_TABLE_NAME = "issues";
  private final static int TOTAL_NUMBER_OF_ISSUES = 9;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MigrateManualVulnerabilitiesToSecurityHotSpotsTest.class, "schema.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system2 = System2.INSTANCE;

  private DataChange underTest = new MigrateManualVulnerabilitiesToSecurityHotSpots(db.database(), system2);

  @Test
  public void should_migrate_manual_vulnerabilities_only() throws SQLException {
    Random random = new Random();
    List<Integer> range = IntStream.range(0, TOTAL_NUMBER_OF_ISSUES).boxed()
      .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(range);

    insertIssue(range.get(0), CODE_SMELL.getDbConstant(), random.nextBoolean());
    insertIssue(range.get(1), BUG.getDbConstant(), random.nextBoolean());
    insertIssue(range.get(2), VULNERABILITY.getDbConstant(), false);
    insertIssue(range.get(3), SECURITY_HOTSPOT.getDbConstant(), random.nextBoolean());
    insertIssue(range.get(4), -1, random.nextBoolean());

    insertIssue(range.get(5), VULNERABILITY.getDbConstant(), true);
    insertIssue(range.get(6), VULNERABILITY.getDbConstant(), true);
    insertIssue(range.get(7), VULNERABILITY.getDbConstant(), true);
    insertIssue(range.get(8), VULNERABILITY.getDbConstant(), true);

    underTest.execute();

    assertIssueNotChanged(range.get(0), CODE_SMELL.getDbConstant());
    assertIssueNotChanged(range.get(1), BUG.getDbConstant());
    assertIssueNotChanged(range.get(2), VULNERABILITY.getDbConstant());
    assertIssueNotChanged(range.get(3), SECURITY_HOTSPOT.getDbConstant());
    assertIssueNotChanged(range.get(4), -1);

    assertIssueChanged(range.get(5));
    assertIssueChanged(range.get(6));
    assertIssueChanged(range.get(7));
    assertIssueChanged(range.get(8));

    // should not fail if executed twice
    underTest.execute();
  }

  @Test
  public void should_not_fail_if_no_issues() throws SQLException {
    underTest.execute();
    assertThat(db.countRowsOfTable("issues")).isEqualTo(0);
  }

  private void assertIssueChanged(int issueId) {
    List<Map<String, Object>> row = db.select(String.format("select status from issues where kee = '%s'", "issue-key-" + issueId));
    assertThat(row).hasSize(1);
    assertThat(row.get(0).get("STATUS"))
      .isEqualTo("TO_REVIEW");

    List<Map<String, Object>> changelogRows = db.select(String.format("select change_type, change_data, created_at, updated_at, issue_change_creation_date" +
      " from issue_changes where issue_key = '%s'", "issue-key-" + issueId));
    assertThat(changelogRows).hasSize(1);

    Map<String, Object> changelogRow = changelogRows.get(0);
    assertThat(changelogRow.get("CHANGE_TYPE")).isEqualTo("diff");
    assertThat(changelogRow.get("CHANGE_DATA")).isEqualTo("type=VULNERABILITY|SECURITY_HOTSPOT,status=OPEN|TO_REVIEW");

    assertThat(changelogRow.get("CREATED_AT")).isNotNull();
    assertThat(changelogRow.get("UPDATED_AT")).isNotNull();
    assertThat(changelogRow.get("ISSUE_CHANGE_CREATION_DATE")).isNotNull();
  }

  private void assertIssueNotChanged(int issueId, int expectedType) {
    List<Map<String, Object>> row = db.select(String.format("select issue_type, status from issues where kee = '%s'", "issue-key-" + issueId));
    assertThat(row).hasSize(1);

    Map<String, Object> issueData = row.get(0);
    assertThat(issueData.get("STATUS"))
      .isNull();
    assertThat(issueData.get("ISSUE_TYPE"))
      .isEqualTo(expectedType);

    List<Map<String, Object>> changelogRows = db.select(String.format("select change_type, change_data, created_at, updated_at, issue_change_creation_date" +
      " from issue_changes where issue_key = '%s'", "issue-key-" + issueId));
    assertThat(changelogRows).isEmpty();
  }

  private void insertIssue(int issueId, int issueType, boolean fromHotspot) {
    db.executeInsert(ISSUES_TABLE_NAME,
      "kee", "issue-key-" + issueId,
      "issue_type", issueType,
      "from_hotspot", fromHotspot,
      "manual_severity", false);
  }

}
