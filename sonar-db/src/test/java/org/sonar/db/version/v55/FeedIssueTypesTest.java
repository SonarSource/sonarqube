/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v55;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.version.MigrationStep;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.version.v55.FeedIssueTypes.tagsToType;

public class FeedIssueTypesTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, FeedIssueTypesTest.class, "schema.sql");

  @Test
  public void test_tagsToType() {
    assertThat(tagsToType(asList("misra", "bug"))).isEqualTo(RuleType.BUG);
    assertThat(tagsToType(asList("misra", "security"))).isEqualTo(RuleType.VULNERABILITY);

    // "bug" has priority on "security"
    assertThat(tagsToType(asList("security", "bug"))).isEqualTo(RuleType.BUG);

    // default is "code smell"
    assertThat(tagsToType(asList("clumsy", "spring"))).isEqualTo(RuleType.CODE_SMELL);
    assertThat(tagsToType(Collections.<String>emptyList())).isEqualTo(RuleType.CODE_SMELL);
  }

  @Test
  public void test_migration() throws Exception {
    try (DbSession dbSession = db.getSession()) {
      IssueDto codeSmell = new IssueDto().setKee("code_smell").setTags(Arrays.asList("clumsy", "spring"));
      IssueDto withoutTags = new IssueDto().setKee("without_tags");
      IssueDto bug = new IssueDto().setKee("bug").setTags(Arrays.asList("clumsy", "bug"));
      db.getDbClient().issueDao().insert(dbSession, codeSmell, withoutTags, bug);
      dbSession.commit();

      MigrationStep underTest = new FeedIssueTypes(db.database(), mock(System2.class));
      underTest.execute();

      assertType("code_smell", RuleType.CODE_SMELL);
      assertType("without_tags", RuleType.CODE_SMELL);
      assertType("bug", RuleType.BUG);
    }
  }

  private void assertType(String issueKey, RuleType expectedType) {
    Number type = (Number)db.selectFirst("select * from issues where kee='" + issueKey + "'").get("ISSUE_TYPE");
    assertThat(type.intValue()).isEqualTo(expectedType.getDbConstant());
  }

}
