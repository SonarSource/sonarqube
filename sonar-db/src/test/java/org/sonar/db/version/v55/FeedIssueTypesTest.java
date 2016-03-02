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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class FeedIssueTypesTest {

  static final Joiner TAGS_JOINER = Joiner.on(",");

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, FeedIssueTypesTest.class, "schema.sql");

  @Test
  public void test_migration() throws Exception {
    insertIssue("code_smell", "clumsy", "spring");
    insertIssue("without_tags");
    insertIssue("bug", "clumsy", "bug");

    MigrationStep underTest = new FeedIssueTypes(db.database(), mock(System2.class));
    underTest.execute();

    assertType("code_smell", RuleType.CODE_SMELL);
    assertType("without_tags", RuleType.CODE_SMELL);
    assertType("bug", RuleType.BUG);
  }

  private void insertIssue(String key, String... tags) {
    db.executeInsert("issues", ImmutableMap.of(
      "kee", key,
      "tags", TAGS_JOINER.join(tags)
      ));
  }

  private void assertType(String issueKey, RuleType expectedType) {
    Number type = (Number) db.selectFirst("select * from issues where kee='" + issueKey + "'").get("ISSUE_TYPE");
    assertThat(type.intValue()).isEqualTo(expectedType.getDbConstant());
  }

}
