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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;

@RunWith(DataProviderRunner.class)
public class EnsureHotspotDefaultStatusIsToReviewTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(EnsureHotspotDefaultStatusIsToReviewTest.class, "issues.sql");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Random random = new Random();

  private EnsureHotspotDefaultStatusIsToReview underTest = new EnsureHotspotDefaultStatusIsToReview(db.database());

  @Test
  public void does_not_fail_if_table_is_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("issues")).isEqualTo(0);
  }

  @Test
  public void changes_OPEN_security_hotspot_to_TO_REVIEW_whatever_resolution() throws SQLException {
    insertIssue("Kee_none", SECURITY_HOTSPOT, STATUS_OPEN, null);
    Issue.RESOLUTIONS.forEach(resolution -> insertIssue("Kee_" + resolution, SECURITY_HOTSPOT, STATUS_OPEN, resolution));

    underTest.execute();

    assertThat(db.select("select distinct STATUS from issues").stream().map(t -> t.get("STATUS")))
      .containsExactly(STATUS_TO_REVIEW);
  }

  @Test
  @UseDataProvider("allStatusesButOpen")
  public void changes_non_OPEN_security_hotspot_to_TO_REVIEW_whatever_resolution(String status) throws SQLException {
    insertIssue("Kee_none", SECURITY_HOTSPOT, status, null);
    Issue.RESOLUTIONS.forEach(resolution -> insertIssue("Kee_" + resolution, SECURITY_HOTSPOT, status, resolution));

    underTest.execute();

    assertThat(db.select("select distinct STATUS from issues").stream().map(t -> t.get("STATUS")))
      .containsExactly(status);
  }

  @DataProvider
  public static Object[][] allStatusesButOpen() {
    return Issue.STATUSES.stream()
      .filter(t -> !STATUS_OPEN.equals(t))
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("allRuleTypeButHotspot")
  public void does_not_change_OPEN_issues_to_TO_REVIEW_whatever_resolution(RuleType ruleType) throws SQLException {
    insertIssue("Kee_none", ruleType, STATUS_OPEN, null);
    Issue.RESOLUTIONS.forEach(resolution -> insertIssue("Kee_" + resolution, ruleType, STATUS_OPEN, resolution));

    underTest.execute();

    assertThat(db.select("select distinct STATUS from issues").stream().map(t -> t.get("STATUS")))
      .containsExactly(STATUS_OPEN);
  }

  @DataProvider
  public static Object[][] allRuleTypeButHotspot() {
    return Arrays.stream(RuleType.values())
      .filter(t -> t != SECURITY_HOTSPOT)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  private void insertIssue(String kee, RuleType ruleType, String status, @Nullable String resolution) {
    db.executeInsert(
      "ISSUES",
      "KEE", kee,
      "MANUAL_SEVERITY", random.nextBoolean(),
      "ISSUE_TYPE", ruleType.getDbConstant(),
      "STATUS", status,
      "RESOLUTION", resolution);
  }
}
