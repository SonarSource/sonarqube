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
package org.sonar.db.version.v451;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.version.MigrationStep;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteUnescapedActivitiesTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, DeleteUnescapedActivitiesTest.class, "schema.sql");

  MigrationStep migration;

  @Test
  public void execute() throws Exception {
    migration = new DeleteUnescapedActivities(db.database());
    db.prepareDbUnit(getClass(), "execute.xml");
    migration.execute();
    db.assertDbUnit(getClass(), "execute-result.xml", "activities");
  }

  @Test
  public void is_unescaped() {
    assertThat(DeleteUnescapedActivities.isUnescaped(
      "ruleKey=findbugs:PT_RELATIVE_PATH_TRAVERSAL;profileKey=java-findbugs-74105;severity=MAJOR;" +
        "key=java-findbugs-74105:findbugs:PT_RELATIVE_PATH_TRAVERSAL"))
      .isFalse();
    assertThat(DeleteUnescapedActivities.isUnescaped(null)).isFalse();
    assertThat(DeleteUnescapedActivities.isUnescaped("")).isFalse();
    assertThat(DeleteUnescapedActivities.isUnescaped("foo=bar")).isFalse();
    assertThat(DeleteUnescapedActivities.isUnescaped("param_xpath=/foo/bar")).isFalse();

    assertThat(DeleteUnescapedActivities.isUnescaped("param_xpath=/foo/bar;foo;ruleKey=S001")).isTrue();
    assertThat(DeleteUnescapedActivities.isUnescaped("param_xpath=/foo=foo;ruleKey=S001")).isTrue();

  }
}
